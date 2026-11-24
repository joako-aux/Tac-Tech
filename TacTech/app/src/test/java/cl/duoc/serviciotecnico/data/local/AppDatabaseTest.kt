package cl.duoc.serviciotecnico.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import cl.duoc.serviciotecnico.testutil.equipo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** La base de datos de la app es un Singleton: siempre se obtiene la misma instancia. */
@RunWith(RobolectricTestRunner::class)
class AppDatabaseTest {

    @Test
    fun obtenerDevuelveSiempreLaMismaBaseConSusDao() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val base = AppDatabase.obtener(context)

        assertSame(base, AppDatabase.obtener(context))

        base.equipoDao().upsertTodos(listOf(equipo(id = "eq-singleton", serie = "SING-1")))
        assertNotNull(base.equipoDao().obtenerPorId("eq-singleton"))
        assertNotNull(base.ticketDao())
        assertNotNull(base.guiaDao())
        assertNotNull(base.fotoGuiaDao())
        assertNotNull(base.solicitudDao())
    }
}
