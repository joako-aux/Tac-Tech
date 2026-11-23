package cl.duoc.serviciotecnico.ui.viewmodel

import cl.duoc.serviciotecnico.data.remote.dto.aDto
import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.testutil.EntornoDePrueba
import cl.duoc.serviciotecnico.testutil.MainDispatcherRule
import cl.duoc.serviciotecnico.testutil.TECNICO
import cl.duoc.serviciotecnico.testutil.observar
import cl.duoc.serviciotecnico.testutil.ticket
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Capa VIEWMODEL: panel del tecnico (solo sus tickets, contadores y sincronizacion). */
class ListadoTicketsViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun elTecnicoVeSoloSusTicketsConContadores() = runTest {
        val e = EntornoDePrueba(backgroundScope)
        e.iniciarComo(TECNICO)
        e.tickets.guardar(ticket(id = "t-1", numero = 1))
        e.tickets.guardar(ticket(id = "t-2", numero = 2, estado = EstadoTicket.CERRADO))
        e.tickets.guardar(ticket(id = "t-3", numero = 3, estado = EstadoTicket.CANCELADO))
        e.tickets.guardar(ticket(id = "t-4", numero = 4, tecnicoCorreo = "otro@tactech.cl"))

        val vm = ListadoTicketsViewModel(e.ticketRepository, e.sesion, e.sincronizacion)
        observar(vm.estado)

        val s = vm.estado.value
        assertFalse(s.cargando)
        assertEquals(TECNICO.correo, s.usuario?.correo)
        assertEquals(listOf("t-3", "t-2", "t-1"), s.tickets.map { it.id })
        assertEquals(3, s.total)
        assertEquals(1, s.activos)
        assertEquals(1, s.cerrados)
    }

    @Test
    fun alEntrarDescargaLosTicketsAsignados() = runTest {
        val e = EntornoDePrueba(backgroundScope)
        e.iniciarComo(TECNICO)
        e.api.tickets += ticket(id = "t-9", numero = 9).aDto()

        val vm = ListadoTicketsViewModel(e.ticketRepository, e.sesion, e.sincronizacion)
        observar(vm.estado)

        assertTrue("tickets(tecnico=${TECNICO.correo},cliente=null)" in e.api.llamadas)
        assertEquals(listOf("t-9"), vm.estado.value.tickets.map { it.id })

        e.api.llamadas.clear()
        vm.sincronizarAhora()
        assertTrue(e.api.llamadas.isNotEmpty())
    }

    @Test
    fun sinSesionNoHayTickets() = runTest {
        val e = EntornoDePrueba(backgroundScope)
        e.tickets.guardar(ticket())

        val vm = ListadoTicketsViewModel(e.ticketRepository, e.sesion, e.sincronizacion)
        observar(vm.estado)

        assertNull(vm.estado.value.usuario)
        assertTrue(vm.estado.value.tickets.isEmpty())
    }

    @Test
    fun cerrarSesionVuelveAlLogin() = runTest {
        val e = EntornoDePrueba(backgroundScope)
        e.iniciarComo(TECNICO)
        val vm = ListadoTicketsViewModel(e.ticketRepository, e.sesion, e.sincronizacion)
        var volvio = false

        vm.cerrarSesion { volvio = true }

        assertTrue(volvio)
        assertNull(e.sesion.usuarioActual())
    }
}
