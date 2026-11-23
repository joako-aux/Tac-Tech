package cl.duoc.serviciotecnico.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import cl.duoc.serviciotecnico.data.remote.ApiService
import cl.duoc.serviciotecnico.data.remote.dto.LoginRequest
import cl.duoc.serviciotecnico.data.remote.dto.aUsuario
import cl.duoc.serviciotecnico.domain.model.Rol
import cl.duoc.serviciotecnico.domain.model.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException

sealed interface ResultadoLogin {
    /** [sinConexion] = se valido contra los usuarios locales porque el backend no respondio. */
    data class Exito(val usuario: Usuario, val sinConexion: Boolean) : ResultadoLogin
    data object CredencialesInvalidas : ResultadoLogin
}

/**
 * Sesion del usuario guardada con DataStore Preferences.
 *
 * 1. Primero intenta autenticar contra tactech-backend (POST /api/auth/login).
 * 2. Si no hay red o el servidor no responde, valida contra los usuarios de prueba locales,
 *    para que el tecnico pueda trabajar offline.
 * 3. Guarda correo, nombre y rol (nunca la contraseña) y expone la sesion como Flow.
 *
 * El DataStore se recibe por constructor (lo crea ServicioTecnicoApp): asi en las pruebas
 * unitarias se usa uno en memoria, sin Context de Android.
 */
class SesionRepository(
    private val almacen: DataStore<Preferences>,
    private val api: ApiService
) {
    private object Claves {
        val CORREO = stringPreferencesKey("correo")
        val NOMBRE = stringPreferencesKey("nombre")
        val ROL = stringPreferencesKey("rol")
        val EMPRESA = stringPreferencesKey("empresa")
    }

    /** null = sin sesion. Se vuelve a emitir cada vez que la sesion cambia. */
    val sesion: Flow<Usuario?> = almacen.data.map { p ->
        val correo = p[Claves.CORREO] ?: return@map null
        Usuario(
            correo = correo,
            nombre = p[Claves.NOMBRE].orEmpty(),
            rol = p[Claves.ROL]?.let { runCatching { Rol.valueOf(it) }.getOrNull() } ?: Rol.TECNICO,
            empresaNombre = p[Claves.EMPRESA]
        )
    }

    suspend fun usuarioActual(): Usuario? = sesion.first()

    suspend fun iniciarSesion(correo: String, clave: String): ResultadoLogin {
        val correoLimpio = correo.trim().lowercase()
        val resultado = try {
            val usuario = api.login(LoginRequest(correoLimpio, clave)).aUsuario()
            ResultadoLogin.Exito(usuario, sinConexion = false)
        } catch (e: HttpException) {
            // 401 / 403 / 400: el servidor respondio y rechazo las credenciales
            ResultadoLogin.CredencialesInvalidas
        } catch (e: IOException) {
            // Sin red o backend apagado: respaldo local
            val local = USUARIOS_LOCALES[correoLimpio]
            if (local != null && local.first == clave) ResultadoLogin.Exito(local.second, sinConexion = true)
            else ResultadoLogin.CredencialesInvalidas
        }
        if (resultado is ResultadoLogin.Exito) guardar(resultado.usuario)
        return resultado
    }

    private suspend fun guardar(usuario: Usuario) {
        almacen.edit { p ->
            p[Claves.CORREO] = usuario.correo
            p[Claves.NOMBRE] = usuario.nombre
            p[Claves.ROL] = usuario.rol.name
            val empresa = usuario.empresaNombre
            if (empresa != null) p[Claves.EMPRESA] = empresa else p.remove(Claves.EMPRESA)
        }
    }

    suspend fun cerrarSesion() {
        almacen.edit { it.clear() }
    }

    companion object {
        const val USUARIO_DEMO = "tecnico@tactech.cl"
        const val CLAVE_DEMO = "tecnico123"
        const val ADMIN_DEMO = "admin@tactech.cl"
        const val CLAVE_ADMIN = "admin123"
        const val CLIENTE_DEMO = "cliente@tactech.cl"
        const val CLAVE_CLIENTE = "cliente123"

        /** Respaldo offline: los mismos usuarios que el backend crea al iniciar. */
        private val USUARIOS_LOCALES: Map<String, Pair<String, Usuario>> = mapOf(
            USUARIO_DEMO to (CLAVE_DEMO to Usuario(USUARIO_DEMO, "Tecnico Demo", Rol.TECNICO)),
            "tecnico2@tactech.cl" to ("tecnico123" to Usuario("tecnico2@tactech.cl", "Tecnico Dos", Rol.TECNICO)),
            ADMIN_DEMO to (CLAVE_ADMIN to Usuario(ADMIN_DEMO, "Administrador", Rol.ADMINISTRADOR)),
            CLIENTE_DEMO to (CLAVE_CLIENTE to Usuario(CLIENTE_DEMO, "Cliente Demo", Rol.CLIENTE, "Clinica Las Condes"))
        )
    }
}
