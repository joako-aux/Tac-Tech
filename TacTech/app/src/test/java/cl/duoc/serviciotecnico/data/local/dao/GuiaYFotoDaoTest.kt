package cl.duoc.serviciotecnico.data.local.dao

import cl.duoc.serviciotecnico.data.local.AppDatabase
import cl.duoc.serviciotecnico.data.local.entity.FotoGuia
import cl.duoc.serviciotecnico.testutil.BaseEnMemoria
import cl.duoc.serviciotecnico.testutil.FECHA_BASE
import cl.duoc.serviciotecnico.testutil.equipo
import cl.duoc.serviciotecnico.testutil.guia
import cl.duoc.serviciotecnico.testutil.ticket
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

/** Capa DATA (Room): guias de visita y sus fotografias. */
@RunWith(RobolectricTestRunner::class)
class GuiaYFotoDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var guias: GuiaDao
    private lateinit var fotos: FotoGuiaDao

    @Before
    fun abrir() {
        db = BaseEnMemoria.crear()
        guias = db.guiaDao()
        fotos = db.fotoGuiaDao()
        // Claves foraneas: equipo -> ticket -> guia -> foto
        runBlocking {
            db.equipoDao().insertar(equipo())
            db.ticketDao().insertar(ticket())
        }
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun guiasPorTicketYNumeracion() = runBlocking<Unit> {
        assertNull(guias.ultimoNumero())
        guias.insertar(guia(id = "g-1", numero = 1))
        guias.insertar(guia(id = "g-2", numero = 2))

        assertEquals(listOf("g-2", "g-1"), guias.observarPorTicket("t-1").first().map { it.id })
        assertEquals("g-1", guias.observarPorId("g-1").first()?.id)
        assertEquals(2, guias.contarPorTicket("t-1"))
        assertEquals(2, guias.ultimoNumero())
    }

    @Test
    fun sincronizacionDeGuias() = runBlocking<Unit> {
        guias.insertar(guia(pendienteSync = true))
        assertEquals(1, guias.contarPendientes().first())
        assertEquals(listOf("g-1"), guias.idsPendientes())
        assertEquals(1, guias.pendientes().size)

        guias.marcarSincronizado("g-1")
        assertEquals(0, guias.contarPendientes().first())

        guias.upsertTodos(listOf(guia(id = "g-1", numero = 1).copy(recibidoPor = "Pedro Soto"), guia(id = "g-3", numero = 3)))
        assertEquals("Pedro Soto", guias.observarPorId("g-1").first()?.recibidoPor)
        assertFalse(guias.observarPorId("g-3").first()!!.pendienteSync)
    }

    @Test
    fun fotosDeUnaGuiaEnOrden() = runBlocking<Unit> {
        guias.insertar(guia())
        fotos.insertarTodas(
            listOf(
                FotoGuia(id = "f-2", guiaId = "g-1", uri = "content://f2", fechaCaptura = FECHA_BASE.plusMinutes(5)),
                FotoGuia(id = "f-1", guiaId = "g-1", uri = "content://f1", fechaCaptura = FECHA_BASE)
            )
        )
        assertEquals(listOf("f-1", "f-2"), fotos.obtenerPorGuia("g-1").map { it.id })
        assertEquals(2, fotos.observarPorGuia("g-1").first().size)

        fotos.upsertTodas(listOf(FotoGuia(id = "f-1", guiaId = "g-1", uri = "content://nueva", fechaCaptura = FECHA_BASE)))
        assertEquals("content://nueva", fotos.obtenerPorGuia("g-1").first().uri)
    }
}
