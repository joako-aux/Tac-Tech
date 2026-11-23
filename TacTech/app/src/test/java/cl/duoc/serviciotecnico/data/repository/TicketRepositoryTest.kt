package cl.duoc.serviciotecnico.data.repository

import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.testutil.FECHA_BASE
import cl.duoc.serviciotecnico.testutil.GuiaDaoEnMemoria
import cl.duoc.serviciotecnico.testutil.TicketDaoEnMemoria
import cl.duoc.serviciotecnico.testutil.guia
import cl.duoc.serviciotecnico.testutil.ticket
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Capa REPOSITORY: reglas de negocio del ticket sobre Room (offline-first).
 * Room se reemplaza por DAO en memoria; se prueba la logica, no SQLite.
 */
class TicketRepositoryTest {

    private val ticketDao = TicketDaoEnMemoria()
    private val guiaDao = GuiaDaoEnMemoria()
    private val repo = TicketRepository(ticketDao, guiaDao)

    @Test
    fun elTecnicoSoloVeSusTickets() = runTest {
        ticketDao.guardar(ticket(id = "t-1", tecnicoCorreo = "tecnico@tactech.cl"))
        ticketDao.guardar(ticket(id = "t-2", tecnicoCorreo = "otro@tactech.cl"))

        assertEquals(listOf("t-1"), repo.observarTicketsDe("tecnico@tactech.cl").first().map { it.id })
        assertEquals(2, repo.observarTickets().first().size)
        assertEquals("t-2", repo.observarTicket("t-2").first()?.id)
        assertEquals("t-1", repo.obtenerTicket("t-1")?.id)
    }

    @Test
    fun actualizarDejaElTicketPendienteDeEnviar() = runTest {
        ticketDao.guardar(ticket(pendienteSync = false))

        repo.actualizar(ticket(titulo = "Nuevo titulo"))

        val guardado = ticketDao.buscar("t-1")!!
        assertTrue(guardado.pendienteSync)
        assertEquals("Nuevo titulo", guardado.titulo)
        assertTrue(guardado.fechaActualizacion.isAfter(FECHA_BASE))
    }

    @Test
    fun noSeCambiaUnTicketQueNoExiste() = runTest {
        assertFalse(repo.cambiarEstado("no-existe", EstadoTicket.COORDINADO, "Comentario"))
    }

    @Test
    fun unTicketCerradoNoSeEdita() = runTest {
        ticketDao.guardar(ticket(estado = EstadoTicket.CERRADO))
        assertFalse(repo.cambiarEstado("t-1", EstadoTicket.TRANSITO, "Reabrir"))
    }

    @Test
    fun noSeCierraSinGuias() = runTest {
        ticketDao.guardar(ticket(estado = EstadoTicket.TRABAJO_EN_PROCESO))

        assertFalse(repo.cambiarEstado("t-1", EstadoTicket.CERRADO, "Se cambio el capacitor"))
        assertEquals(EstadoTicket.TRABAJO_EN_PROCESO, ticketDao.buscar("t-1")?.estado)
    }

    @Test
    fun alCerrarConGuiaSeRegistraSolucionYFecha() = runTest {
        ticketDao.guardar(ticket(estado = EstadoTicket.TRABAJO_EN_PROCESO))
        guiaDao.guardar(guia(ticketId = "t-1"))

        assertTrue(repo.cambiarEstado("t-1", EstadoTicket.CERRADO, "Se cambio el capacitor"))

        val cerrado = ticketDao.buscar("t-1")!!
        assertEquals(EstadoTicket.CERRADO, cerrado.estado)
        assertEquals("Se cambio el capacitor", cerrado.solucion)
        assertNotNull(cerrado.fechaCierre)
        assertTrue(cerrado.pendienteSync)
        assertFalse(cerrado.editable)
    }

    @Test
    fun unEstadoIntermedioSoloGuardaElComentario() = runTest {
        ticketDao.guardar(ticket())

        assertTrue(repo.cambiarEstado("t-1", EstadoTicket.COORDINADO, "Visita el lunes"))

        val t = ticketDao.buscar("t-1")!!
        assertEquals(EstadoTicket.COORDINADO, t.estado)
        assertEquals("Visita el lunes", t.comentarioEstado)
        assertNull(t.fechaCierre)
        assertNull(t.solucion)
    }

    @Test
    fun cancelarRegistraFechaPeroNoSolucion() = runTest {
        ticketDao.guardar(ticket())

        assertTrue(repo.cambiarEstado("t-1", EstadoTicket.CANCELADO, "Cliente desiste"))

        val t = ticketDao.buscar("t-1")!!
        assertNotNull(t.fechaCierre)
        assertNull(t.solucion)
    }
}
