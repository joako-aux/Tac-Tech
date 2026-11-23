package cl.duoc.serviciotecnico.testutil

import cl.duoc.serviciotecnico.data.remote.dto.UsuarioDto
import cl.duoc.serviciotecnico.data.repository.EmpresaRepository
import cl.duoc.serviciotecnico.data.repository.EquipoRepository
import cl.duoc.serviciotecnico.data.repository.GeocodificacionRepository
import cl.duoc.serviciotecnico.data.repository.GuiaRepository
import cl.duoc.serviciotecnico.data.repository.SesionRepository
import cl.duoc.serviciotecnico.data.repository.SincronizacionRepository
import cl.duoc.serviciotecnico.data.repository.SolicitudRepository
import cl.duoc.serviciotecnico.data.repository.TicketRepository
import cl.duoc.serviciotecnico.data.repository.UsuarioRepository
import cl.duoc.serviciotecnico.domain.model.Usuario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/**
 * Arma los repositorios REALES de la app sobre dobles de prueba (DAO en memoria, API falsa,
 * DataStore en memoria y red falsa). Asi las pruebas de ViewModel ejercitan ViewModel +
 * Repository juntos, igual que en la app, pero sin Android, sin red y sin base de datos.
 */
class EntornoDePrueba(scope: CoroutineScope) {
    val tickets = TicketDaoEnMemoria()
    val equipos = EquipoDaoEnMemoria()
    val guias = GuiaDaoEnMemoria()
    val fotos = FotoGuiaDaoEnMemoria()
    val solicitudes = SolicitudDaoEnMemoria()
    val api = ApiServiceFalsa()
    val red = RedFalsa()
    val nominatim = NominatimFalsa()

    val sesion = SesionRepository(DataStoreEnMemoria(), api)
    val ticketRepository = TicketRepository(tickets, guias)
    val guiaRepository = GuiaRepository(guias, fotos)
    val equipoRepository = EquipoRepository(equipos)
    val solicitudRepository = SolicitudRepository(solicitudes, api)
    val usuarioRepository = UsuarioRepository(api)
    val empresaRepository = EmpresaRepository(api)
    val geocodificacion = GeocodificacionRepository(nominatim)
    val sincronizacion = SincronizacionRepository(tickets, equipos, guias, fotos, solicitudes, api, red, sesion, scope)

    /** Deja una sesion iniciada (como si el usuario hubiera hecho login) y limpia el registro de llamadas. */
    suspend fun iniciarComo(usuario: Usuario) {
        api.usuarioLogin = UsuarioDto(1, usuario.nombre, usuario.correo, usuario.rol.name, empresaNombre = usuario.empresaNombre)
        sesion.iniciarSesion(usuario.correo, "clave123")
        api.llamadas.clear()
    }
}

/**
 * Los estados con stateIn(WhileSubscribed) solo se calculan si alguien los observa (en la app,
 * la pantalla). En el test se deja un colector activo durante toda la prueba.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> TestScope.observar(estado: StateFlow<T>) {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { estado.collect {} }
}
