package cl.duoc.serviciotecnico.data.repository

import cl.duoc.serviciotecnico.data.remote.ApiService
import cl.duoc.serviciotecnico.data.remote.Resultado
import cl.duoc.serviciotecnico.data.remote.dto.UsuarioDto
import cl.duoc.serviciotecnico.data.remote.dto.UsuarioRequest
import cl.duoc.serviciotecnico.data.remote.llamar
import cl.duoc.serviciotecnico.domain.model.Rol

/**
 * Gestion de usuarios del administrador. Va siempre contra el servidor: las cuentas
 * no se crean offline porque el correo debe ser unico y la clave se encripta (BCrypt) alla.
 */
class UsuarioRepository(private val api: ApiService) {

    suspend fun listar(rol: Rol? = null): Resultado<List<UsuarioDto>> = llamar { api.usuarios(rol?.name) }

    suspend fun guardar(id: Long?, datos: UsuarioRequest): Resultado<UsuarioDto> = llamar {
        if (id == null) api.crearUsuario(datos) else api.actualizarUsuario(id, datos)
    }

    suspend fun eliminar(id: Long): Resultado<Unit> = llamar {
        val respuesta = api.eliminarUsuario(id)
        if (!respuesta.isSuccessful) throw retrofit2.HttpException(respuesta)
    }
}
