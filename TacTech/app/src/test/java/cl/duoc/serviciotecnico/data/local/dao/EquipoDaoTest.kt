package cl.duoc.serviciotecnico.data.local.dao

import cl.duoc.serviciotecnico.data.local.AppDatabase
import cl.duoc.serviciotecnico.testutil.BaseEnMemoria
import cl.duoc.serviciotecnico.testutil.equipo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Capa DATA (Room): DAO de equipos de frio. */
@RunWith(RobolectricTestRunner::class)
class EquipoDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: EquipoDao

    @Before
    fun abrir() {
        db = BaseEnMemoria.crear()
        dao = db.equipoDao()
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun insertaBuscaPorSerieYActualiza() = runBlocking<Unit> {
        dao.insertar(equipo())

        assertEquals("Split muro", dao.obtenerPorId("eq-1")?.nombre)
        assertEquals("eq-1", dao.observarPorId("eq-1").first()?.id)
        assertEquals("eq-1", dao.buscarPorSerie("MID-12345")?.id)
        assertNull(dao.buscarPorSerie("NO-EXISTE"))

        dao.actualizar(equipo().copy(nombre = "Split cassette"))
        assertEquals("Split cassette", dao.obtenerPorId("eq-1")?.nombre)
    }

    @Test
    fun sincronizacionDeEquipos() = runBlocking<Unit> {
        dao.insertar(equipo(id = "eq-2", serie = "LG-777", pendienteSync = true))
        assertEquals(1, dao.contarPendientes().first())
        assertEquals(listOf("eq-2"), dao.idsPendientes())
        assertEquals(1, dao.pendientes().size)

        dao.marcarSincronizado("eq-2")
        assertFalse(dao.obtenerPorId("eq-2")!!.pendienteSync)

        dao.upsertTodos(listOf(equipo(id = "eq-2", serie = "LG-778"), equipo(id = "eq-3", serie = "LG-900")))
        assertEquals("LG-778", dao.obtenerPorId("eq-2")?.serie)
        assertEquals("eq-3", dao.obtenerPorId("eq-3")?.id)
    }
}
