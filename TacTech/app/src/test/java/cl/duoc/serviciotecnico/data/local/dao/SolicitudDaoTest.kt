package cl.duoc.serviciotecnico.data.local.dao

import cl.duoc.serviciotecnico.data.local.AppDatabase
import cl.duoc.serviciotecnico.domain.model.EstadoSolicitud
import cl.duoc.serviciotecnico.testutil.BaseEnMemoria
import cl.duoc.serviciotecnico.testutil.CLIENTE
import cl.duoc.serviciotecnico.testutil.FECHA_BASE
import cl.duoc.serviciotecnico.testutil.solicitud
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

/** Capa DATA (Room): solicitudes de los clientes. */
@RunWith(RobolectricTestRunner::class)
class SolicitudDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: SolicitudDao

    @Before
    fun abrir() {
        db = BaseEnMemoria.crear()
        dao = db.solicitudDao()
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun cadaClienteVeLasSuyasYElAdministradorTodas() = runBlocking<Unit> {
        dao.insertar(solicitud(id = "s-1"))
        dao.insertar(solicitud(id = "s-2", clienteCorreo = "otro@cliente.cl", fecha = FECHA_BASE.plusDays(1)))

        assertEquals(listOf("s-1"), dao.observarDeCliente(CLIENTE.correo).first().map { it.id })
        assertEquals(listOf("s-2", "s-1"), dao.observarTodas().first().map { it.id })
        assertEquals("s-1", dao.observarPorId("s-1").first()?.id)
        assertEquals("Equipo no enfria", dao.obtenerPorId("s-1")?.titulo)
        assertNull(dao.obtenerPorId("no-existe"))

        dao.actualizar(solicitud(id = "s-1", estado = EstadoSolicitud.CANCELADA))
        assertEquals(EstadoSolicitud.CANCELADA, dao.obtenerPorId("s-1")?.estado)
    }

    @Test
    fun sincronizacionDeSolicitudes() = runBlocking<Unit> {
        dao.insertar(solicitud(pendienteSync = true))
        assertEquals(1, dao.contarPendientes().first())
        assertEquals(listOf("s-1"), dao.idsPendientes())
        assertEquals(1, dao.pendientes().size)

        dao.marcarSincronizado("s-1", FECHA_BASE.plusMinutes(1))
        assertTrue(dao.obtenerPorId("s-1")!!.pendienteSync)
        dao.marcarSincronizado("s-1", FECHA_BASE)
        assertFalse(dao.obtenerPorId("s-1")!!.pendienteSync)

        dao.upsertTodas(listOf(solicitud(id = "s-1", estado = EstadoSolicitud.CONFIRMADA, ticketId = "t-1"), solicitud(id = "s-9")))
        assertEquals("t-1", dao.obtenerPorId("s-1")?.ticketId)
        assertEquals(2, dao.observarTodas().first().size)
    }
}
