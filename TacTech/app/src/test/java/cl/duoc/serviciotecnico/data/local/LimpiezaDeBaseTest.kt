package cl.duoc.serviciotecnico.data.local

import cl.duoc.serviciotecnico.testutil.BaseEnMemoria
import cl.duoc.serviciotecnico.testutil.equipo
import cl.duoc.serviciotecnico.testutil.guia
import cl.duoc.serviciotecnico.testutil.solicitud
import cl.duoc.serviciotecnico.testutil.ticket
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Al vaciar la base (por ejemplo, al cambiar de cuenta) se borran todas las tablas, respetando las claves foraneas. */
@RunWith(RobolectricTestRunner::class)
class LimpiezaDeBaseTest {

    @Test
    fun vaciarLaBaseBorraTodasLasTablas() = runBlocking<Unit> {
        val db = BaseEnMemoria.crear()
        db.equipoDao().insertar(equipo())
        db.ticketDao().insertar(ticket())
        db.guiaDao().insertar(guia())
        db.solicitudDao().insertar(solicitud())
        assertEquals(1, db.ticketDao().observarTodos().first().size)

        db.clearAllTables()

        assertTrue(db.ticketDao().observarTodos().first().isEmpty())
        assertTrue(db.solicitudDao().observarTodas().first().isEmpty())
        assertTrue(db.guiaDao().observarPorTicket("t-1").first().isEmpty())
        db.close()
    }
}
