package cl.duoc.serviciotecnico.data.repository

import cl.duoc.serviciotecnico.testutil.EquipoDaoEnMemoria
import cl.duoc.serviciotecnico.testutil.FotoGuiaDaoEnMemoria
import cl.duoc.serviciotecnico.testutil.GuiaDaoEnMemoria
import cl.duoc.serviciotecnico.testutil.equipo
import cl.duoc.serviciotecnico.testutil.guia
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Capa REPOSITORY: reporte de visita (guia) con fotos y ubicacion, y lectura de equipos. */
class GuiaYEquipoRepositoryTest {

    private val guiaDao = GuiaDaoEnMemoria()
    private val fotoDao = FotoGuiaDaoEnMemoria()
    private val guias = GuiaRepository(guiaDao, fotoDao)

    private suspend fun crearGuia(fotos: List<String>) = guias.crear(
        ticketId = "t-1",
        equipoId = "eq-1",
        direccion = "Av. Apoquindo 1234",
        tecnico = "Carlos Lobos",
        trabajoRealizado = "Cambio de capacitor",
        repuestos = "",
        observaciones = "",
        recibidoPor = "Ana Rojas",
        latitud = -33.4172,
        longitud = -70.6044,
        urisDeFotos = fotos
    )

    @Test
    fun laGuiaGuardaUbicacionFotosYQuedaPendiente() = runTest {
        val g = crearGuia(listOf("content://foto1", "content://foto2"))

        assertEquals(1, g.numero)
        assertEquals(-33.4172, g.latitud!!, 0.0)
        assertTrue(g.pendienteSync)
        assertEquals(2, guias.observarFotos(g.id).first().size)
        assertEquals(g, guias.observarGuia(g.id).first())
        assertEquals(listOf(g), guias.observarGuiasDeTicket("t-1").first())
    }

    @Test
    fun elNumeroDeGuiaEsCorrelativo() = runTest {
        guiaDao.guardar(guia(numero = 41))

        val g = crearGuia(listOf("content://foto1"))

        assertEquals(42, g.numero)
    }

    @Test
    fun sinFotosNoSeInsertanFotos() = runTest {
        val g = crearGuia(emptyList())
        assertTrue(fotoDao.todas.isEmpty())
        assertEquals(1, guiaDao.todas.size)
        assertEquals(g.id, guiaDao.todas.single().id)
    }

    @Test
    fun equiposSeLeenDeRoom() = runTest {
        val dao = EquipoDaoEnMemoria()
        dao.guardar(equipo())
        val repo = EquipoRepository(dao)

        assertEquals("MID-12345", repo.obtenerEquipo("eq-1")?.serie)
        assertEquals("eq-1", repo.observarEquipo("eq-1").first()?.id)
        assertNull(repo.obtenerEquipo("no-existe"))
    }
}
