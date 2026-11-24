package cl.duoc.serviciotecnico.ui.screens

import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.testutil.PruebaDePantalla
import cl.duoc.serviciotecnico.testutil.TECNICO
import cl.duoc.serviciotecnico.testutil.ticket
import cl.duoc.serviciotecnico.ui.viewmodel.ListadoTicketsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Panel del tecnico: sus tickets asignados con contadores. */
@RunWith(RobolectricTestRunner::class)
class ListadoTicketsScreenTest : PruebaDePantalla() {

    private var ticketAbierto: String? = null
    private var salio = false

    private fun abrir() {
        sesion(TECNICO)
        val vm = ListadoTicketsViewModel(e.ticketRepository, e.sesion, e.sincronizacion)
        mostrar { ListadoTicketsScreen(onAbrirTicket = { ticketAbierto = it }, onSesionCerrada = { salio = true }, vm = vm) }
        esperarTexto("Tickets asignados")
    }

    @Test
    fun muestraSusTicketsYAbreElDetalle() {
        e.tickets.guardar(ticket(id = "t-1", numero = 1))
        e.tickets.guardar(ticket(id = "t-2", numero = 2, estado = EstadoTicket.CERRADO, titulo = "Mantencion preventiva"))
        abrir()

        esperarTexto("Mis visitas")
        existe("Activos")
        existe("Cerrados")
        existe("Mantencion preventiva")

        tocar("Equipo no enfria")
        assertEquals("t-1", ticketAbierto)
    }

    @Test
    fun sinTicketsMuestraElEstadoVacio() {
        abrir()
        esperarTexto("No tienes tickets asignados")
    }

    @Test
    fun cerrarSesionPideConfirmacion() {
        abrir()
        tocarIcono("Cerrar sesion")
        existe("Tus tickets quedan guardados en el telefono", parcial = true)
        tocar("Cancelar")
        assertFalse(salio)

        tocarIcono("Cerrar sesion")
        tocar("Salir")
        esperar { salio }
    }
}
