package cl.duoc.serviciotecnico.data.local.dao

import cl.duoc.serviciotecnico.data.local.AppDatabase
import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.testutil.BaseEnMemoria
import cl.duoc.serviciotecnico.testutil.FECHA_BASE
import cl.duoc.serviciotecnico.testutil.TECNICO
import cl.duoc.serviciotecnico.testutil.equipo
import cl.duoc.serviciotecnico.testutil.ticket
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Capa DATA (Room): consultas SQL reales del DAO de tickets sobre una base en memoria. */
@RunWith(RobolectricTestRunner::class)
class TicketDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TicketDao

    @Before
    fun abrir() {
        db = BaseEnMemoria.crear()
        dao = db.ticketDao()
        // El ticket tiene clave foranea al equipo
        runBlocking { db.equipoDao().insertar(equipo()) }
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun insertaLeeYOrdena() = runBlocking<Unit> {
        assertNull(dao.ultimoNumero())
        dao.insertar(ticket(id = "t-1", numero = 1))
        dao.insertar(ticket(id = "t-2", numero = 2, tecnicoCorreo = "otro@tactech.cl", fecha = FECHA_BASE.plusDays(1)))

        assertEquals("Equipo no enfria", dao.obtenerPorId("t-1")?.titulo)
        assertNull(dao.obtenerPorId("no-existe"))
        assertEquals(listOf("t-2", "t-1"), dao.observarTodos().first().map { it.id })
        assertEquals(listOf("t-1"), dao.observarPorTecnico(TECNICO.correo).first().map { it.id })
        assertEquals("t-1", dao.observarPorId("t-1").first()?.id)
        assertEquals(2, dao.ultimoNumero())
    }

    @Test
    fun soloMarcaSincronizadoSiNadieLoModifico() = runBlocking<Unit> {
        dao.insertar(ticket(pendienteSync = true))
        assertEquals(1, dao.contarPendientes().first())
        assertEquals(listOf("t-1"), dao.idsPendientes())
        assertEquals(1, dao.pendientes().size)

        dao.marcarSincronizado("t-1", FECHA_BASE.plusHours(1))
        assertTrue(dao.obtenerPorId("t-1")!!.pendienteSync)

        dao.marcarSincronizado("t-1", FECHA_BASE)
        assertFalse(dao.obtenerPorId("t-1")!!.pendienteSync)
        assertEquals(0, dao.contarPendientes().first())
    }

    @Test
    fun actualizaYRecibeLoQueLlegaDelServidor() = runBlocking<Unit> {
        dao.insertar(ticket())
        dao.actualizar(dao.obtenerPorId("t-1")!!.copy(estado = EstadoTicket.CERRADO, solucion = "Cambio de capacitor"))
        assertEquals(EstadoTicket.CERRADO, dao.obtenerPorId("t-1")?.estado)
        assertEquals("Cambio de capacitor", dao.obtenerPorId("t-1")?.solucion)

        dao.upsertTodos(listOf(ticket(id = "t-1", titulo = "Titulo del servidor"), ticket(id = "t-3", numero = 3)))
        assertEquals("Titulo del servidor", dao.obtenerPorId("t-1")?.titulo)
        assertEquals(2, dao.observarTodos().first().size)
    }
}
