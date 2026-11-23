package cl.duoc.serviciotecnico.ui.viewmodel

import cl.duoc.serviciotecnico.domain.model.EstadoSolicitud
import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.testutil.ADMIN
import cl.duoc.serviciotecnico.testutil.EntornoDePrueba
import cl.duoc.serviciotecnico.testutil.MainDispatcherRule
import cl.duoc.serviciotecnico.testutil.observar
import cl.duoc.serviciotecnico.testutil.solicitud
import cl.duoc.serviciotecnico.testutil.ticket
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Capa VIEWMODEL: panel del administrador (filtros, busqueda y avance por tecnico). */
class AdminViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private suspend fun TestScope.crear(
        filtro: FiltroAdmin = FiltroAdmin.PENDIENTES,
        tecnico: String? = null
    ): Pair<EntornoDePrueba, AdminViewModel> {
        val e = EntornoDePrueba(backgroundScope)
        e.iniciarComo(ADMIN)
        e.tickets.guardar(ticket(id = "t-1", numero = 1, estado = EstadoTicket.ABIERTO, tecnicoCorreo = "ana@tactech.cl", titulo = "Split no enfria"))
        e.tickets.guardar(ticket(id = "t-2", numero = 2, estado = EstadoTicket.TRANSITO, tecnicoCorreo = "ana@tactech.cl", cliente = "Hospital Norte"))
        e.tickets.guardar(ticket(id = "t-3", numero = 3, estado = EstadoTicket.CERRADO, tecnicoCorreo = "luis@tactech.cl"))
        e.tickets.guardar(ticket(id = "t-4", numero = 4, estado = EstadoTicket.CANCELADO, tecnicoCorreo = "luis@tactech.cl"))
        e.tickets.guardar(ticket(id = "t-5", numero = 5, estado = EstadoTicket.COORDINADO, tecnicoCorreo = ""))
        e.solicitudes.guardar(solicitud(id = "s-1"))
        e.solicitudes.guardar(solicitud(id = "s-2", estado = EstadoSolicitud.CONFIRMADA))
        val vm = AdminViewModel(e.ticketRepository, e.solicitudRepository, e.sesion, e.sincronizacion, filtro, tecnico)
        observar(vm.estado)
        return e to vm
    }

    @Test
    fun cuentaLasTareasPorEstado() = runTest {
        val (_, vm) = crear()
        val s = vm.estado.value
        assertFalse(s.cargando)
        assertEquals(ADMIN.correo, s.usuario?.correo)
        assertEquals(2, s.pendientes) // ABIERTO y COORDINADO
        assertEquals(1, s.enProceso)
        assertEquals(1, s.terminados)
        assertEquals(1, s.cancelados)
        assertEquals(5, s.cantidad(FiltroAdmin.TODOS))
        assertEquals(1, s.solicitudesPorConfirmar)
    }

    @Test
    fun filtrosDeEstado() {
        assertTrue(FiltroAdmin.TODOS.incluye(EstadoTicket.CERRADO))
        assertTrue(FiltroAdmin.PENDIENTES.incluye(EstadoTicket.PENDIENTE_POR_CLIENTE))
        assertFalse(FiltroAdmin.PENDIENTES.incluye(EstadoTicket.TRANSITO))
        assertTrue(FiltroAdmin.EN_PROCESO.incluye(EstadoTicket.TRABAJO_EN_PROCESO))
        assertTrue(FiltroAdmin.TERMINADOS.incluye(EstadoTicket.CERRADO))
        assertFalse(FiltroAdmin.TERMINADOS.incluye(EstadoTicket.CANCELADO))
        assertTrue(FiltroAdmin.CANCELADOS.incluye(EstadoTicket.CANCELADO))
        assertEquals("En proceso", FiltroAdmin.EN_PROCESO.etiqueta)
    }

    @Test
    fun avancePorTecnico() = runTest {
        val (_, vm) = crear()
        val s = vm.estado.value

        assertEquals(listOf("ana@tactech.cl", "luis@tactech.cl"), s.tecnicos)
        val ana = s.resumenPorTecnico.first()
        assertEquals("ana@tactech.cl", ana.correo)
        assertEquals(2, ana.pendientes)
        assertEquals(0f, ana.avance, 0f)
        val luis = s.resumenPorTecnico.last()
        assertEquals(2, luis.total)
        assertEquals(1f, luis.avance, 0f)
        assertEquals(0f, ResumenTecnico("x", 0, 0).avance, 0f)
    }

    @Test
    fun filtrarPorEstadoTecnicoYBusqueda() = runTest {
        val (_, vm) = crear()
        assertEquals(setOf("t-1", "t-5"), vm.estado.value.filtrados.map { it.id }.toSet())

        vm.cambiarFiltro(FiltroAdmin.TODOS)
        vm.cambiarTecnico("ana@tactech.cl")
        assertEquals(setOf("t-1", "t-2"), vm.estado.value.filtrados.map { it.id }.toSet())

        vm.cambiarBusqueda("hospital")
        assertEquals(listOf("t-2"), vm.estado.value.filtrados.map { it.id })

        vm.cambiarBusqueda("split")
        assertEquals(listOf("t-1"), vm.estado.value.filtrados.map { it.id })

        vm.cambiarTecnico(null)
        vm.cambiarBusqueda(" 3 ")
        assertEquals(listOf("t-3"), vm.estado.value.filtrados.map { it.id })
    }

    @Test
    fun filtroInicialDesdeLaNavegacion() = runTest {
        val (_, vm) = crear(filtro = FiltroAdmin.TERMINADOS, tecnico = "luis@tactech.cl")
        assertEquals(FiltroAdmin.TERMINADOS, vm.estado.value.filtro)
        assertEquals(listOf("t-3"), vm.estado.value.filtrados.map { it.id })
    }

    @Test
    fun sincronizaAlAbrirYCierraSesion() = runTest {
        val (e, vm) = crear()
        assertTrue("tickets(tecnico=null,cliente=null)" in e.api.llamadas)

        e.api.llamadas.clear()
        vm.sincronizarAhora()
        assertTrue(e.api.llamadas.isNotEmpty())

        var volvio = false
        vm.cerrarSesion { volvio = true }
        assertTrue(volvio)
        assertNull(e.sesion.usuarioActual())
    }
}
