package cl.duoc.serviciotecnico

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.withTransaction
import cl.duoc.serviciotecnico.data.local.AppDatabase
import cl.duoc.serviciotecnico.data.remote.MonitorConexion
import cl.duoc.serviciotecnico.data.remote.RetrofitClient
import cl.duoc.serviciotecnico.data.remote.geocodificacion.ApiExternaClient
import cl.duoc.serviciotecnico.data.repository.EmpresaRepository
import cl.duoc.serviciotecnico.data.repository.EquipoRepository
import cl.duoc.serviciotecnico.data.repository.GeocodificacionRepository
import cl.duoc.serviciotecnico.data.repository.GuiaRepository
import cl.duoc.serviciotecnico.data.repository.SesionRepository
import cl.duoc.serviciotecnico.data.repository.SincronizacionRepository
import cl.duoc.serviciotecnico.data.repository.SolicitudRepository
import cl.duoc.serviciotecnico.data.repository.TicketRepository
import cl.duoc.serviciotecnico.data.repository.UsuarioRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Un unico DataStore por proceso para la sesion, asociado al Context de la aplicacion. */
private val Context.almacenSesion: DataStore<Preferences> by preferencesDataStore(name = "sesion")

/**
 * Punto unico donde se arman las dependencias (inyeccion manual).
 * Los ViewModel las obtienen desde aqui con su factory.
 */
class ServicioTecnicoApp : Application() {

    /** Alcance de corrutinas que vive mientras viva la app (sincronizacion en segundo plano). */
    val alcanceApp = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val baseDeDatos by lazy { AppDatabase.obtener(this) }
    private val api by lazy { RetrofitClient.api }
    private val monitorConexion by lazy { MonitorConexion(this, alcanceApp) }

    val ticketRepository by lazy { TicketRepository(baseDeDatos.ticketDao(), baseDeDatos.guiaDao()) }
    val equipoRepository by lazy { EquipoRepository(baseDeDatos.equipoDao()) }
    val guiaRepository by lazy { GuiaRepository(baseDeDatos.guiaDao(), baseDeDatos.fotoGuiaDao()) }
    val sesionRepository by lazy { SesionRepository(almacenSesion, api) }
    val solicitudRepository by lazy { SolicitudRepository(baseDeDatos.solicitudDao(), api) }
    val usuarioRepository by lazy { UsuarioRepository(api) }
    val empresaRepository by lazy { EmpresaRepository(api) }

    /** API externa (OpenStreetMap): direccion a partir de las coordenadas de la visita. */
    val geocodificacionRepository by lazy { GeocodificacionRepository(ApiExternaClient.nominatim) }

    val sincronizacionRepository by lazy {
        SincronizacionRepository(
            ticketDao = baseDeDatos.ticketDao(),
            equipoDao = baseDeDatos.equipoDao(),
            guiaDao = baseDeDatos.guiaDao(),
            fotoDao = baseDeDatos.fotoGuiaDao(),
            solicitudDao = baseDeDatos.solicitudDao(),
            api = api,
            monitor = monitorConexion,
            sesionRepository = sesionRepository,
            scope = alcanceApp,
            enTransaccion = { bloque -> baseDeDatos.withTransaction { bloque() } }
        )
    }

    override fun onCreate() {
        super.onCreate()
        sincronizacionRepository.iniciar()
    }
}
