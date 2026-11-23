package cl.duoc.serviciotecnico.data.repository

import cl.duoc.serviciotecnico.data.local.dao.EquipoDao
import cl.duoc.serviciotecnico.data.local.dao.FotoGuiaDao
import cl.duoc.serviciotecnico.data.local.dao.GuiaDao
import cl.duoc.serviciotecnico.data.local.dao.SolicitudDao
import cl.duoc.serviciotecnico.data.local.dao.TicketDao
import cl.duoc.serviciotecnico.data.remote.ApiService
import cl.duoc.serviciotecnico.data.remote.EstadoRed
import cl.duoc.serviciotecnico.data.remote.dto.aDto
import cl.duoc.serviciotecnico.data.remote.dto.aEntidad
import cl.duoc.serviciotecnico.data.remote.dto.fotosAEntidad
import cl.duoc.serviciotecnico.domain.model.Rol
import cl.duoc.serviciotecnico.domain.model.Usuario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDateTime

data class EstadoSync(
    val enLinea: Boolean = false,
    val pendientes: Int = 0,
    val sincronizando: Boolean = false,
    val ultimaSync: LocalDateTime? = null,
    val error: String? = null
)

/**
 * Repositorio central de sincronizacion (estrategia offline-first).
 *
 * - La app SIEMPRE escribe primero en Room y marca el registro con pendienteSync = true.
 *   Asi el tecnico trabaja igual sin internet.
 * - Cuando hay conexion y existen pendientes, se envian al backend (subir) y luego se
 *   descargan los cambios de otros usuarios, por ejemplo el administrador (descargar).
 * - Un Mutex evita dos sincronizaciones al mismo tiempo.
 *
 * Recibe los DAO y no la base completa, y la transaccion como funcion ([enTransaccion]):
 * en la app es `db.withTransaction { }` de Room; en las pruebas unitarias, ejecutar el bloque.
 */
@OptIn(FlowPreview::class)
class SincronizacionRepository(
    private val ticketDao: TicketDao,
    private val equipoDao: EquipoDao,
    private val guiaDao: GuiaDao,
    private val fotoDao: FotoGuiaDao,
    private val solicitudDao: SolicitudDao,
    private val api: ApiService,
    private val monitor: EstadoRed,
    private val sesionRepository: SesionRepository,
    private val scope: CoroutineScope,
    private val enTransaccion: suspend (suspend () -> Unit) -> Unit = { bloque -> bloque() }
) {

    private val mutex = Mutex()
    private val sincronizando = MutableStateFlow(false)
    private val ultimaSync = MutableStateFlow<LocalDateTime?>(null)
    private val error = MutableStateFlow<String?>(null)

    private val pendientes = combine(
        ticketDao.contarPendientes(),
        equipoDao.contarPendientes(),
        guiaDao.contarPendientes(),
        solicitudDao.contarPendientes()
    ) { t, e, g, s -> t + e + g + s }

    /** Estado que muestra el banner de conexion en la interfaz. */
    val estado: StateFlow<EstadoSync> = combine(
        monitor.enLinea, pendientes, sincronizando, ultimaSync, error
    ) { enLinea, pend, sinc, ultima, err ->
        EstadoSync(enLinea, pend, sinc, ultima, err)
    }.stateIn(scope, SharingStarted.Eagerly, EstadoSync())

    /** Se llama una vez al abrir la app (ServicioTecnicoApp). */
    fun iniciar() {
        scope.launch {
            // Reacciona cuando vuelve la conexion o aparecen registros nuevos sin enviar.
            // debounce agrupa varios cambios seguidos en un solo envio.
            combine(monitor.enLinea, pendientes) { enLinea, pend -> enLinea to pend }
                .distinctUntilChanged()
                .debounce(1_000)
                .collect { (enLinea, _) -> if (enLinea) sincronizar() }
        }
    }

    /** Sube lo pendiente y descarga lo nuevo. Devuelve true si termino sin errores. */
    suspend fun sincronizar(): Boolean {
        // Sin sesion no hay a quien sincronizar
        val usuario = sesionRepository.usuarioActual() ?: return false
        if (!mutex.tryLock()) return false
        sincronizando.value = true
        return try {
            subir()
            descargar(usuario)
            ultimaSync.value = LocalDateTime.now()
            error.value = null
            true
        } catch (e: IOException) {
            error.value = "Sin conexion con el servidor"
            false
        } catch (e: HttpException) {
            error.value = "El servidor respondio ${e.code()}"
            false
        } catch (e: Exception) {
            error.value = "No se pudo sincronizar"
            false
        } finally {
            sincronizando.value = false
            mutex.unlock()
        }
    }

    /**
     * Envia un registro. Si el servidor responde 409 (por ejemplo, el cliente cancelo una
     * solicitud que el administrador ya confirmo), gana el servidor: se descarta el cambio
     * local y la descarga trae la version correcta.
     */
    private suspend fun enviar(envio: suspend () -> Unit, alTerminar: suspend () -> Unit) {
        try {
            envio()
            alTerminar()
        } catch (e: HttpException) {
            if (e.code() == 409) alTerminar() else throw e
        }
    }

    /** Orden: solicitudes, equipos -> tickets -> guias (cada uno depende del anterior en MySQL). */
    private suspend fun subir() {
        solicitudDao.pendientes().forEach { s ->
            enviar({ api.guardarSolicitud(s.id, s.aDto()) }) { solicitudDao.marcarSincronizado(s.id, s.fechaActualizacion) }
        }
        equipoDao.pendientes().forEach { equipo ->
            enviar({ api.guardarEquipo(equipo.id, equipo.aDto()) }) { equipoDao.marcarSincronizado(equipo.id) }
        }
        ticketDao.pendientes().forEach { ticket ->
            enviar({ api.guardarTicket(ticket.id, ticket.aDto()) }) {
                ticketDao.marcarSincronizado(ticket.id, ticket.fechaActualizacion)
            }
        }
        guiaDao.pendientes().forEach { guia ->
            enviar({ api.guardarGuia(guia.id, guia.aDto(fotoDao.obtenerPorGuia(guia.id))) }) {
                guiaDao.marcarSincronizado(guia.id)
            }
        }
    }

    /**
     * Descarga solo lo que corresponde a cada rol:
     * - Cliente: sus solicitudes y los tickets nacidos de ellas (para ver el avance).
     * - Tecnico: los tickets que le asignaron y sus guias.
     * - Administrador: todo.
     * Los registros que siguen pendientes localmente no se pisan.
     */
    private suspend fun descargar(usuario: Usuario) {
        val correo = usuario.correo
        val equipos = api.equipos()
        val solicitudes = when (usuario.rol) {
            Rol.CLIENTE -> api.solicitudes(cliente = correo)
            Rol.ADMINISTRADOR -> api.solicitudes()
            Rol.TECNICO -> emptyList()
        }
        val tickets = when (usuario.rol) {
            Rol.CLIENTE -> api.tickets(cliente = correo)
            Rol.TECNICO -> api.tickets(tecnico = correo)
            Rol.ADMINISTRADOR -> api.tickets()
        }
        val guias = when (usuario.rol) {
            Rol.CLIENTE -> emptyList()
            Rol.TECNICO -> api.guias(tecnico = correo)
            Rol.ADMINISTRADOR -> api.guias()
        }

        enTransaccion {
            val solicitudesPendientes = solicitudDao.idsPendientes().toSet()
            solicitudDao.upsertTodas(solicitudes.filter { it.id !in solicitudesPendientes }.map { it.aEntidad() })

            val equiposPendientes = equipoDao.idsPendientes().toSet()
            equipoDao.upsertTodos(equipos.filter { it.id !in equiposPendientes }.map { it.aEntidad() })

            val ticketsPendientes = ticketDao.idsPendientes().toSet()
            ticketDao.upsertTodos(tickets.filter { it.id !in ticketsPendientes }.map { it.aEntidad() })

            val guiasPendientes = guiaDao.idsPendientes().toSet()
            val guiasNuevas = guias.filter { it.id !in guiasPendientes }
            guiaDao.upsertTodos(guiasNuevas.map { it.aEntidad() })
            fotoDao.upsertTodas(guiasNuevas.flatMap { it.fotosAEntidad() })
        }
    }
}
