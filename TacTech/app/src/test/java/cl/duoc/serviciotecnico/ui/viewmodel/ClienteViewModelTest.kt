package cl.duoc.serviciotecnico.ui.viewmodel

import cl.duoc.serviciotecnico.domain.model.EstadoSolicitud
import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.testutil.CLIENTE
import cl.duoc.serviciotecnico.testutil.EntornoDePrueba
import cl.duoc.serviciotecnico.testutil.MainDispatcherRule
import cl.duoc.serviciotecnico.testutil.observar
import cl.duoc.serviciotecnico.testutil.solicitud
import cl.duoc.serviciotecnico.testutil.ticket
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Capa VIEWMODEL: panel del cliente (sus solicitudes, el avance del ticket y la cancelacion). */
class ClienteViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private suspend fun TestScope.crear(): Pair<EntornoDePrueba, ClienteViewModel> {
        val e = EntornoDePrueba(backgroundScope)
        e.iniciarComo(CLIENTE)
        val vm = ClienteViewModel(e.solicitudRepository, e.ticketRepository, e.sesion, e.sincronizacion)
        observar(vm.estado)
        return e to vm
    }

    @Test
    fun cadaSolicitudMuestraElTicketQueGeneroYLosContadores() = runTest {
        val (e, vm) = crear()
        e.solicitudes.guardar(solicitud(id = "s-1"))
        e.solicitudes.guardar(solicitud(id = "s-2", estado = EstadoSolicitud.CONFIRMADA, ticketId = "t-2"))
        e.solicitudes.guardar(solicitud(id = "s-3", estado = EstadoSolicitud.CONFIRMADA, ticketId = "t-3"))
        e.solicitudes.guardar(solicitud(id = "s-4", clienteCorreo = "otro@cliente.cl"))
        e.tickets.guardar(ticket(id = "t-2", estado = EstadoTicket.TRANSITO))
        e.tickets.guardar(ticket(id = "t-3", estado = EstadoTicket.CERRADO))

        val s = vm.estado.value
        assertEquals(3, s.solicitudes.size)
        assertEquals(EstadoTicket.TRANSITO, s.solicitudes.first { it.solicitud.id == "s-2" }.ticket?.estado)
        assertEquals(1, s.porConfirmar)
        assertEquals(1, s.enAtencion)
        assertEquals(1, s.atendidas)
        assertEquals(CLIENTE.correo, s.usuario?.correo)
    }

    @Test
    fun alEntrarSincronizaComoCliente() = runTest {
        val (e, vm) = crear()
        assertTrue("solicitudes(cliente=${CLIENTE.correo})" in e.api.llamadas)
        e.api.llamadas.clear()
        vm.sincronizarAhora()
        assertTrue(e.api.llamadas.isNotEmpty())
    }

    @Test
    fun cancelarConMotivoValido() = runTest {
        val (e, vm) = crear()
        e.solicitudes.guardar(solicitud())

        vm.pedirCancelacion("s-1")
        vm.cambiarMotivo("Me equivoque de equipo")
        vm.confirmarCancelacion()

        assertEquals(EstadoSolicitud.CANCELADA, e.solicitudes.buscar("s-1")?.estado)
        assertNull(vm.estado.value.cancelacion)
        assertEquals("Solicitud cancelada", vm.estado.value.mensaje)

        vm.mensajeMostrado()
        assertNull(vm.estado.value.mensaje)
    }

    @Test
    fun motivoCortoMuestraErrorYNoCancela() = runTest {
        val (e, vm) = crear()
        e.solicitudes.guardar(solicitud())

        vm.pedirCancelacion("s-1")
        assertNull(vm.estado.value.cancelacion?.error)
        vm.cambiarMotivo("no")
        vm.confirmarCancelacion()

        assertEquals("El motivo debe tener al menos 5 caracteres", vm.estado.value.cancelacion?.error)
        assertEquals(EstadoSolicitud.POR_CONFIRMAR, e.solicitudes.buscar("s-1")?.estado)

        vm.cerrarCancelacion()
        assertNull(vm.estado.value.cancelacion)
    }

    @Test
    fun noSePuedeCancelarLoYaProcesado() = runTest {
        val (e, vm) = crear()
        e.solicitudes.guardar(solicitud(estado = EstadoSolicitud.CONFIRMADA))

        vm.pedirCancelacion("s-1")
        vm.cambiarMotivo("Ya no lo necesito")
        vm.confirmarCancelacion()

        assertEquals("La solicitud ya fue procesada y no se puede cancelar", vm.estado.value.mensaje)
    }

    @Test
    fun confirmarSinDialogoAbiertoNoHaceNada() = runTest {
        val (_, vm) = crear()
        vm.cambiarMotivo("sin dialogo")
        vm.confirmarCancelacion()
        assertNull(vm.estado.value.cancelacion)
        assertNull(vm.estado.value.mensaje)
    }

    @Test
    fun cerrarSesion() = runTest {
        val (e, vm) = crear()
        var volvio = false
        vm.cerrarSesion { volvio = true }
        assertTrue(volvio)
        assertNull(e.sesion.usuarioActual())
        assertNull(vm.estado.value.usuario)
    }
}
