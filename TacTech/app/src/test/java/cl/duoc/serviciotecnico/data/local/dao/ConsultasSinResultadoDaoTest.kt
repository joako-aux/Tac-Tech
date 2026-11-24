package cl.duoc.serviciotecnico.data.local.dao

import cl.duoc.serviciotecnico.data.local.AppDatabase
import cl.duoc.serviciotecnico.testutil.BaseEnMemoria
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Capa DATA (Room): las consultas sobre una base vacia no fallan, devuelven null, 0 o lista vacia. */
@RunWith(RobolectricTestRunner::class)
class ConsultasSinResultadoDaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun abrir() {
        db = BaseEnMemoria.crear()
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun buscarLoQueNoExiste() = runBlocking<Unit> {
        assertNull(db.ticketDao().observarPorId("no-existe").first())
        assertNull(db.equipoDao().observarPorId("no-existe").first())
        assertNull(db.equipoDao().obtenerPorId("no-existe"))
        assertNull(db.solicitudDao().observarPorId("no-existe").first())
        assertNull(db.guiaDao().observarPorId("no-existe").first())
        assertNull(db.guiaDao().ultimoNumero())
        assertNull(db.ticketDao().ultimoNumero())
    }

    @Test
    fun listasYContadoresVacios() = runBlocking<Unit> {
        assertTrue(db.ticketDao().observarTodos().first().isEmpty())
        assertTrue(db.ticketDao().pendientes().isEmpty())
        assertTrue(db.equipoDao().pendientes().isEmpty())
        assertTrue(db.guiaDao().observarPorTicket("t-1").first().isEmpty())
        assertTrue(db.fotoGuiaDao().obtenerPorGuia("g-1").isEmpty())
        assertTrue(db.solicitudDao().observarTodas().first().isEmpty())
        assertEquals(0, db.guiaDao().contarPorTicket("t-1"))
        assertEquals(0, db.solicitudDao().contarPendientes().first())
        assertEquals(0, db.equipoDao().contarPendientes().first())
        assertEquals(0, db.guiaDao().contarPendientes().first())
    }
}
