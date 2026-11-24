package cl.duoc.serviciotecnico.ui.screens

import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.domain.model.Usuario
import cl.duoc.serviciotecnico.testutil.ADMIN
import cl.duoc.serviciotecnico.testutil.FECHA_BASE
import cl.duoc.serviciotecnico.testutil.PruebaDePantalla
import cl.duoc.serviciotecnico.testutil.TECNICO
import cl.duoc.serviciotecnico.testutil.equipo
import cl.duoc.serviciotecnico.testutil.guia
import cl.duoc.serviciotecnico.testutil.ticket
import cl.duoc.serviciotecnico.ui.viewmodel.DetalleTicketViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Detalle del ticket: datos, cambio de estado y reportes de visita. */
@RunWith(RobolectricTestRunner::class)
class DetalleTicketScreenTest : PruebaDePantalla() {

    private var volvio = false
    private var nuevaGuia = false
    private var guiaAbierta: String? = null

    private fun abrir(usuario: Usuario = TECNICO, id: String = "t-1") {
        sesion(usuario)
        val vm = DetalleTicketViewModel(id, e.ticketRepository, e.guiaRepository, e.equipoRepository, e.sesion)
        mostrar {
            DetalleTicketScreen(
                ticketId = id,
                onVolver = { volvio = true },
                onNuevaGuia = { nuevaGuia = true },
                onAbrirGuia = { guiaAbierta = it },
                vm = vm
            )
        }
    }

    @Test
    fun elTecnicoCambiaElEstadoDelTicket() {
        e.equipos.guardar(equipo())
        e.tickets.guardar(ticket().copy(comentarioEstado = "Cliente avisado"))
        e.guias.guardar(guia())
        abrir()

        esperarTexto("Ticket N° 1")
        existe("Cambiar estado")
        existe("Cliente avisado")
        existe("MSAFB-12")
        existe("Reportes de visita (1)")

        tocar("Actualizar estado")
        tocarIcono("Abrir lista de estados")
        tocar("Transito")
        escribir("Comentario del cambio", "Voy en camino donde el cliente")
        tocar("Actualizar estado")

        esperar { e.tickets.todas.first().estado == EstadoTicket.TRANSITO }
    }

    @Test
    fun abreUnReporteOCreaUnoNuevo() {
        e.equipos.guardar(equipo())
        e.tickets.guardar(ticket())
        e.guias.guardar(guia())
        abrir()

        esperarTexto("Reporte N° 1")
        tocar("Reporte N° 1")
        assertEquals("g-1", guiaAbierta)

        tocarIcono("Volver")
        assertTrue(volvio)
    }

    @Test
    fun elAdministradorGestionaElEstado() {
        e.tickets.guardar(ticket())
        abrir(ADMIN)
        esperarTexto("Gestionar estado (administrador)")
        existe("El tecnico aun no registra reportes para este ticket.")
        tocarIcono("Abrir lista de estados")
        tocar("Cancelado")
        existe("Motivo de la cancelacion", parcial = true)
    }

    @Test
    fun unTicketCerradoEsSoloLectura() {
        e.tickets.guardar(ticket(estado = EstadoTicket.CERRADO).copy(solucion = "Cambio de capacitor", fechaCierre = FECHA_BASE))
        abrir()
        esperarTexto("Ticket cerrado")
        existe("Solo lectura: ya no admite cambios")
        existe("Cambio de capacitor")
    }

    @Test
    fun ticketInexistente() {
        abrir(id = "no-existe")
        esperarTexto("No se encontro el ticket")
    }
}