package cl.duoc.serviciotecnico.ui.screens

import cl.duoc.serviciotecnico.domain.model.EstadoSolicitud
import cl.duoc.serviciotecnico.testutil.CLIENTE
import cl.duoc.serviciotecnico.testutil.PruebaDePantalla
import cl.duoc.serviciotecnico.testutil.solicitud
import cl.duoc.serviciotecnico.testutil.ticket
import cl.duoc.serviciotecnico.ui.viewmodel.ClienteViewModel
import androidx.compose.ui.test.onAllNodesWithText
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Panel del cliente: sus solicitudes, el ticket que generaron y la cancelacion con motivo. */
@RunWith(RobolectricTestRunner::class)
class ClienteHomeScreenTest : PruebaDePantalla() {

    private var nuevaSolicitud = false
    private var salio = false

    private fun abrir() {
        sesion(CLIENTE)
        val vm = ClienteViewModel(e.solicitudRepository, e.ticketRepository, e.sesion, e.sincronizacion)
        mostrar {
            ClienteHomeScreen(onNuevaSolicitud = { nuevaSolicitud = true }, onSesionCerrada = { salio = true }, vm = vm)
        }
        esperarTexto("Mis solicitudes")
    }

    private fun conSolicitudes() {
        e.solicitudes.guardar(solicitud(id = "s-1"))
        e.solicitudes.guardar(solicitud(id = "s-2", estado = EstadoSolicitud.CONFIRMADA, ticketId = "t-1"))
        e.tickets.guardar(ticket(id = "t-1", solicitudId = "s-2"))
        e.solicitudes.guardar(solicitud(id = "s-3", estado = EstadoSolicitud.RECHAZADA, pendienteSync = true).copy(motivo = "Fuera de cobertura"))
    }

    @Test
    fun muestraSusSolicitudesConTicketYMotivo() {
        conSolicitudes()
        abrir()
        esperarTexto("Ticket N° 1")
        existe("Fuera de cobertura")
        existe("Tecnico asignado")
        existe("En atencion")
    }

    @Test
    fun soloLaSolicitudPorConfirmarSePuedeCancelar() {
        conSolicitudes()
        abrir()
        esperarTexto("Cancelar solicitud")
        // s-1 (por confirmar) tiene el boton; s-2 (confirmada) y s-3 (rechazada) no
        assertTrue(compose.onAllNodesWithText("Cancelar solicitud").fetchSemanticsNodes().size == 1)
    }

    @Test
    fun sinSolicitudesInvitaACrearLaPrimera() {
        abrir()
        esperarTexto("Aun no tienes solicitudes")
        tocar("Sincronizar")
    }

    @Test
    fun cerrarSesionPideConfirmacion() {
        abrir()
        tocarIcono("Cerrar sesion")
        existe("Tus solicitudes quedan guardadas", parcial = true)
        tocar("Volver")
        assertFalse(salio)

        tocarIcono("Cerrar sesion")
        tocar("Salir")
        esperar { salio }
    }
}