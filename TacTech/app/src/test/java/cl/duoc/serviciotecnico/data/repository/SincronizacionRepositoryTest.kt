package cl.duoc.serviciotecnico.data.repository

import cl.duoc.serviciotecnico.data.remote.dto.aDto
import cl.duoc.serviciotecnico.data.remote.dto.FotoGuiaDto
import cl.duoc.serviciotecnico.domain.model.EstadoSolicitud
import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.domain.model.Rol
import cl.duoc.serviciotecnico.testutil.ApiServiceFalsa
import cl.duoc.serviciotecnico.testutil.DataStoreEnMemoria
import cl.duoc.serviciotecnico.testutil.EquipoDaoEnMemoria
import cl.duoc.serviciotecnico.testutil.FotoGuiaDaoEnMemoria
import cl.duoc.serviciotecnico.testutil.GuiaDaoEnMemoria
import cl.duoc.serviciotecnico.testutil.RedFalsa
import cl.duoc.serviciotecnico.testutil.SolicitudDaoEnMemoria
import cl.duoc.serviciotecnico.testutil.TicketDaoEnMemoria
import cl.duoc.serviciotecnico.testutil.equipo
import cl.duoc.serviciotecnico.testutil.errorHttp
import cl.duoc.serviciotecnico.testutil.guia
import cl.duoc.serviciotecnico.testutil.solicitud
import cl.duoc.serviciotecnico.testutil.ticket
import cl.duoc.serviciotecnico.testutil.usuarioDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Capa REPOSITORY: sincronizacion offline-first, el punto mas critico de la app.
 *
 * Se prueba con DAO en memoria (Room), un microservicio falso (Retrofit) y una red falsa
 * (ConnectivityManager). Reglas verificadas:
 * - sube lo pendiente en orden y lo marca como enviado;
 * - ante un 409 gana el servidor; ante otro error, el cambio sigue pendiente;
 * - descarga solo lo que corresponde a cada rol, sin pisar cambios locales sin enviar;
 * - no sincroniza sin sesion ni dos veces al mismo tiempo.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SincronizacionRepositoryTest {

    private val tickets = TicketDaoEnMemoria()
    private val equipos = EquipoDaoEnMemoria()
    private val guias = GuiaDaoEnMemoria()
    private val fotos = FotoGuiaDaoEnMemoria()
    private val solicitudes = SolicitudDaoEnMemoria()
    private val api = ApiServiceFalsa()
    private val red = RedFalsa(conectado = true)
    private val sesion = SesionRepository(DataStoreEnMemoria(), api)

    private fun crear(
        scope: CoroutineScope,
        enTransaccion: suspend (suspend () -> Unit) -> Unit = { it() }
    ) = SincronizacionRepository(tickets, equipos, guias, fotos, solicitudes, api, red, sesion, scope, enTransaccion)

    /** Inicia sesion contra el servidor falso y limpia el registro de llamadas. */
    private suspend fun iniciarComo(rol: Rol, correo: String) {
        api.usuarioLogin = usuarioDto(correo = correo, rol = rol)
        sesion.iniciarSesion(correo, "clave123")
        api.llamadas.clear()
    }

    @Test
    fun sinSesionNoSincroniza() = runTest {
        val repo = crear(backgroundScope)
        assertFalse(repo.sincronizar())
        assertTrue(api.llamadas.isEmpty())
    }

    @Test
    fun subeLoPendienteEnOrdenYLoMarcaEnviado() = runTest {
        iniciarComo(Rol.TECNICO, "tecnico@tactech.cl")
        solicitudes.guardar(solicitud(pendienteSync = true))
        equipos.guardar(equipo(pendienteSync = true))
        tickets.guardar(ticket(pendienteSync = true))
        guias.guardar(guia(pendienteSync = true))
        val repo = crear(backgroundScope)

        assertTrue(repo.sincronizar())

        val subidas = api.llamadas.filter { it.startsWith("guardar") }
        assertEquals(listOf("guardarSolicitud(s-1)", "guardarEquipo(eq-1)", "guardarTicket(t-1)", "guardarGuia(g-1)"), subidas)
        assertFalse(solicitudes.buscar("s-1")!!.pendienteSync)
        assertFalse(equipos.buscar("eq-1")!!.pendienteSync)
        assertFalse(tickets.buscar("t-1")!!.pendienteSync)
        assertFalse(guias.buscar("g-1")!!.pendienteSync)
    }

    @Test
    fun conflicto409GanaElServidor() = runTest {
        iniciarComo(Rol.CLIENTE, "cliente@tactech.cl")
        // El cliente cancelo offline, pero el administrador ya la habia confirmado en el servidor
        solicitudes.guardar(solicitud(estado = EstadoSolicitud.CANCELADA, pendienteSync = true))
        api.solicitudes += solicitud(estado = EstadoSolicitud.CONFIRMADA, ticketId = "t-1").aDto()
        api.conflictos = setOf("s-1")
        val repo = crear(backgroundScope)

        assertTrue(repo.sincronizar())

        val local = solicitudes.buscar("s-1")!!
        assertEquals(EstadoSolicitud.CONFIRMADA, local.estado)
        assertFalse(local.pendienteSync)
    }

    @Test
    fun errorDelServidorDejaElCambioPendiente() = runTest {
        iniciarComo(Rol.TECNICO, "tecnico@tactech.cl")
        tickets.guardar(ticket(pendienteSync = true))
        val repo = crear(backgroundScope)
        api.fallaRed = errorHttp(500)

        assertFalse(repo.sincronizar())
        advanceUntilIdle()

        assertTrue(tickets.buscar("t-1")!!.pendienteSync)
        assertEquals("El servidor respondio 500", repo.estado.value.error)
    }

    @Test
    fun sinConexionConElServidorEsErrorDeRed() = runTest {
        iniciarComo(Rol.TECNICO, "tecnico@tactech.cl")
        val repo = crear(backgroundScope)
        api.fallaRed = IOException("timeout")

        assertFalse(repo.sincronizar())
        advanceUntilIdle()

        assertEquals("Sin conexion con el servidor", repo.estado.value.error)
    }

    @Test
    fun datoInvalidoDelServidorNoRompeLaApp() = runTest {
        iniciarComo(Rol.TECNICO, "tecnico@tactech.cl")
        // Un enum desconocido (ej: version nueva del backend) hace fallar el mapeo
        api.tickets += ticket().aDto().copy(prioridad = "URGENTE")
        val repo = crear(backgroundScope)

        assertFalse(repo.sincronizar())
        advanceUntilIdle()

        assertEquals("No se pudo sincronizar", repo.estado.value.error)
    }

    @Test
    fun elTecnicoDescargaSusTicketsYGuias() = runTest {
        iniciarComo(Rol.TECNICO, "tecnico@tactech.cl")
        api.tickets += ticket(id = "t-1", tecnicoCorreo = "tecnico@tactech.cl").aDto()
        api.tickets += ticket(id = "t-2", tecnicoCorreo = "otro@tactech.cl").aDto()
        api.equipos += equipo().aDto()
        api.guias += guia().aDto(emptyList()).copy(fotos = listOf(FotoGuiaDto("f-1", "content://f1", "2025-11-02T10:30")))
        val repo = crear(backgroundScope)

        assertTrue(repo.sincronizar())

        assertTrue("tickets(tecnico=tecnico@tactech.cl,cliente=null)" in api.llamadas)
        assertTrue("guias(tecnico=tecnico@tactech.cl)" in api.llamadas)
        assertTrue(api.llamadas.none { it.startsWith("solicitudes") })
        assertEquals(listOf("t-1"), tickets.todas.map { it.id })
        assertNotNull(equipos.buscar("eq-1"))
        assertEquals(1, fotos.todas.size)
    }

    @Test
    fun elClienteDescargaSusSolicitudesYTicketsSinGuias() = runTest {
        iniciarComo(Rol.CLIENTE, "cliente@tactech.cl")
        api.solicitudes += solicitud(id = "s-1", ticketId = "t-1").aDto()
        api.solicitudes += solicitud(id = "s-2", clienteCorreo = "otro@cliente.cl").aDto()
        api.tickets += ticket(id = "t-1", solicitudId = "s-1").aDto()
        val repo = crear(backgroundScope)

        assertTrue(repo.sincronizar())

        assertTrue("solicitudes(cliente=cliente@tactech.cl)" in api.llamadas)
        assertTrue("tickets(tecnico=null,cliente=cliente@tactech.cl)" in api.llamadas)
        assertTrue(api.llamadas.none { it.startsWith("guias") })
        assertEquals(listOf("s-1"), solicitudes.todas.map { it.id })
        assertEquals(listOf("t-1"), tickets.todas.map { it.id })
    }

    @Test
    fun elAdministradorDescargaTodo() = runTest {
        iniciarComo(Rol.ADMINISTRADOR, "admin@tactech.cl")
        api.solicitudes += solicitud(id = "s-1").aDto()
        api.solicitudes += solicitud(id = "s-2", clienteCorreo = "otro@cliente.cl").aDto()
        api.guias += guia().aDto(emptyList())
        val repo = crear(backgroundScope)

        assertTrue(repo.sincronizar())

        assertTrue("solicitudes(cliente=null)" in api.llamadas)
        assertTrue("guias(tecnico=null)" in api.llamadas)
        assertEquals(2, solicitudes.todas.size)
        assertEquals(1, guias.todas.size)
    }

    @Test
    fun laDescargaNoPisaCambiosLocalesSinEnviar() = runTest {
        iniciarComo(Rol.TECNICO, "tecnico@tactech.cl")
        tickets.guardar(ticket(estado = EstadoTicket.TRANSITO, pendienteSync = true))
        api.tickets += ticket(estado = EstadoTicket.ABIERTO).aDto()
        val repo = crear(backgroundScope, enTransaccion = { bloque ->
            // Justo antes de guardar lo descargado, el tecnico vuelve a modificar el ticket
            tickets.guardar(ticket(estado = EstadoTicket.TRABAJO_EN_PROCESO, pendienteSync = true))
            bloque()
        })

        assertTrue(repo.sincronizar())

        val local = tickets.buscar("t-1")!!
        assertEquals(EstadoTicket.TRABAJO_EN_PROCESO, local.estado)
        assertTrue(local.pendienteSync)
    }

    @Test
    fun noSincronizaDosVecesAlMismoTiempo() = runTest {
        iniciarComo(Rol.TECNICO, "tecnico@tactech.cl")
        var segunda: Boolean? = null
        lateinit var repo: SincronizacionRepository
        repo = crear(backgroundScope, enTransaccion = { bloque ->
            segunda = repo.sincronizar() // ocurre mientras la primera tiene el candado
            bloque()
        })

        assertTrue(repo.sincronizar())
        assertEquals(false, segunda)
    }

    @Test
    fun elEstadoInformaRedPendientesYUltimaSincronizacion() = runTest {
        iniciarComo(Rol.TECNICO, "tecnico@tactech.cl")
        tickets.guardar(ticket(pendienteSync = true))
        guias.guardar(guia(pendienteSync = true))
        val repo = crear(backgroundScope)
        advanceUntilIdle()

        assertTrue(repo.estado.value.enLinea)
        assertEquals(2, repo.estado.value.pendientes)
        assertNull(repo.estado.value.ultimaSync)

        repo.sincronizar()
        advanceUntilIdle()

        assertEquals(0, repo.estado.value.pendientes)
        assertNotNull(repo.estado.value.ultimaSync)
        assertNull(repo.estado.value.error)
        assertFalse(repo.estado.value.sincronizando)
    }

    @Test
    fun alVolverLaConexionSincronizaSolo() = runTest {
        iniciarComo(Rol.TECNICO, "tecnico@tactech.cl")
        red.enLinea.value = false
        tickets.guardar(ticket(pendienteSync = true))
        val repo = crear(backgroundScope)

        repo.iniciar()
        esperarDebounce()
        assertTrue("Sin internet no debe intentar subir", api.llamadas.isEmpty())

        red.enLinea.value = true
        esperarDebounce()

        assertTrue("guardarTicket(t-1)" in api.llamadas)
        assertFalse(tickets.buscar("t-1")!!.pendienteSync)
    }

    /** iniciar() agrupa los cambios con debounce(1 s): se avanza el reloj virtual del test. */
    private fun TestScope.esperarDebounce() {
        advanceTimeBy(1_100)
        runCurrent()
    }
}
