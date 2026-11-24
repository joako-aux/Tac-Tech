package cl.duoc.serviciotecnico.data.local.dao

import cl.duoc.serviciotecnico.data.local.AppDatabase
import cl.duoc.serviciotecnico.data.local.entity.Guia
import cl.duoc.serviciotecnico.data.local.entity.Solicitud
import cl.duoc.serviciotecnico.data.local.entity.Ticket
import cl.duoc.serviciotecnico.domain.model.EstadoSolicitud
import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.testutil.BaseEnMemoria
import cl.duoc.serviciotecnico.testutil.FECHA_BASE
import cl.duoc.serviciotecnico.testutil.TECNICO
import cl.duoc.serviciotecnico.testutil.CLIENTE
import cl.duoc.serviciotecnico.testutil.equipo
import cl.duoc.serviciotecnico.testutil.guia
import cl.duoc.serviciotecnico.testutil.solicitud
import cl.duoc.serviciotecnico.testutil.ticket
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Capa DATA (Room): los campos opcionales (comentario, fecha de cierre, solucion, coordenadas,
 * motivo, etc.) deben guardarse y leerse igual cuando tienen valor y cuando vienen vacios (null),
 * en TODAS las operaciones del DAO: insertar, actualizar, upsert y cada consulta.
 */
@RunWith(RobolectricTestRunner::class)
class RegistrosLlenosYVaciosDaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun abrir() {
        db = BaseEnMemoria.crear()
        runBlocking { db.equipoDao().insertar(equipo()) }
    }

    @After
    fun cerrar() = db.close()

    // ---------- Ticket ----------

    private fun ticketLleno(id: String, numero: Int) = ticket(id = id, numero = numero, solicitudId = "s-1", pendienteSync = true)
        .copy(comentarioEstado = "Cliente avisado", fechaCierre = FECHA_BASE.plusDays(2), solucion = "Cambio de capacitor")

    private fun ticketVacio(id: String, numero: Int) = ticket(id = id, numero = numero, pendienteSync = true)

    /** Pasa por todas las consultas del DAO de tickets. */
    private suspend fun leerTickets(): List<Ticket> {
        val dao = db.ticketDao()
        dao.observarPorTecnico(TECNICO.correo).first()
        dao.pendientes()
        dao.observarPorId("t-1").first()
        dao.observarPorId("t-2").first()
        dao.obtenerPorId("t-1")
        dao.obtenerPorId("t-2")
        return dao.observarTodos().first()
    }

    @Test
    fun ticketsConYSinCamposOpcionales() = runBlocking<Unit> {
        val dao = db.ticketDao()
        dao.insertar(ticketLleno("t-1", 1))
        dao.insertar(ticketVacio("t-2", 2))
        assertEquals(2, leerTickets().size)
        assertEquals("Cambio de capacitor", dao.obtenerPorId("t-1")?.solucion)
        assertNull(dao.obtenerPorId("t-2")?.solucion)

        // Se intercambian: el lleno queda vacio y el vacio queda lleno
        dao.actualizar(ticketVacio("t-1", 1))
        dao.actualizar(ticketLleno("t-2", 2))
        assertNull(dao.obtenerPorId("t-1")?.fechaCierre)
        assertEquals("s-1", dao.obtenerPorId("t-2")?.solicitudId)

        // Upsert: actualiza los existentes e inserta los nuevos, llenos y vacios
        dao.upsertTodos(
            listOf(
                ticketLleno("t-1", 1), ticketVacio("t-2", 2),
                ticketLleno("t-3", 3), ticketVacio("t-4", 4)
            )
        )
        assertEquals(4, leerTickets().size)
        assertEquals(EstadoTicket.ABIERTO, dao.obtenerPorId("t-3")?.estado)
        assertEquals("Cliente avisado", dao.obtenerPorId("t-3")?.comentarioEstado)
        assertNull(dao.obtenerPorId("t-4")?.comentarioEstado)
    }

    // ---------- Guia ----------

    private fun guiaConGps(id: String, numero: Int): Guia = guia(id = id, numero = numero, pendienteSync = true)
    private fun guiaSinGps(id: String, numero: Int): Guia = guia(id = id, numero = numero, latitud = null, longitud = null, pendienteSync = true)

    @Test
    fun guiasConYSinCoordenadas() = runBlocking<Unit> {
        db.ticketDao().insertar(ticket())
        val dao = db.guiaDao()
        dao.insertar(guiaConGps("g-1", 1))
        dao.insertar(guiaSinGps("g-2", 2))

        assertEquals(2, dao.observarPorTicket("t-1").first().size)
        assertEquals(2, dao.pendientes().size)
        assertEquals(-33.4172, dao.observarPorId("g-1").first()?.latitud)
        assertNull(dao.observarPorId("g-2").first()?.latitud)
        assertNull(dao.observarPorId("no-existe").first())

        dao.upsertTodos(listOf(guiaSinGps("g-1", 1), guiaConGps("g-2", 2), guiaConGps("g-3", 3), guiaSinGps("g-4", 4)))
        assertNull(dao.observarPorId("g-1").first()?.longitud)
        assertEquals(-70.6044, dao.observarPorId("g-2").first()?.longitud)
        assertEquals(4, dao.observarPorTicket("t-1").first().size)
        assertEquals(4, dao.pendientes().size)
    }

    // ---------- Solicitud ----------

    private fun solicitudLlena(id: String): Solicitud =
        solicitud(id = id, estado = EstadoSolicitud.RECHAZADA, ticketId = "t-1", pendienteSync = true).copy(motivo = "Sin cobertura")

    private fun solicitudVacia(id: String): Solicitud =
        solicitud(id = id, pendienteSync = true).copy(clienteNombre = null, empresaNombre = null)

    private suspend fun leerSolicitudes(): List<Solicitud> {
        val dao = db.solicitudDao()
        dao.observarDeCliente(CLIENTE.correo).first()
        dao.pendientes()
        dao.observarPorId("s-1").first()
        dao.observarPorId("s-2").first()
        dao.obtenerPorId("s-1")
        dao.obtenerPorId("s-2")
        return dao.observarTodas().first()
    }

    @Test
    fun solicitudesConYSinCamposOpcionales() = runBlocking<Unit> {
        val dao = db.solicitudDao()
        dao.insertar(solicitudLlena("s-1"))
        dao.insertar(solicitudVacia("s-2"))
        assertEquals(2, leerSolicitudes().size)
        assertEquals("Sin cobertura", dao.obtenerPorId("s-1")?.motivo)
        assertNull(dao.obtenerPorId("s-2")?.empresaNombre)

        dao.actualizar(solicitudVacia("s-1"))
        dao.actualizar(solicitudLlena("s-2"))
        assertNull(dao.obtenerPorId("s-1")?.ticketId)
        assertEquals("t-1", dao.obtenerPorId("s-2")?.ticketId)

        dao.upsertTodas(listOf(solicitudLlena("s-1"), solicitudVacia("s-2"), solicitudLlena("s-3"), solicitudVacia("s-4")))
        assertEquals(4, leerSolicitudes().size)
        assertEquals("Clinica Las Condes", dao.obtenerPorId("s-3")?.empresaNombre)
        assertNull(dao.obtenerPorId("s-4")?.clienteNombre)
    }
}
