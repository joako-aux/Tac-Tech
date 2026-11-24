package cl.duoc.serviciotecnico.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.serviciotecnico.ServicioTecnicoApp
import cl.duoc.serviciotecnico.data.remote.Resultado
import cl.duoc.serviciotecnico.data.remote.dto.UsuarioDto
import cl.duoc.serviciotecnico.data.repository.UsuarioRepository
import cl.duoc.serviciotecnico.domain.model.Rol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UsuariosUiState(
    val cargando: Boolean = true,
    val usuarios: List<UsuarioDto> = emptyList(),
    /** null = todos los roles. */
    val filtro: Rol? = null,
    val error: String? = null,
    val porEliminar: UsuarioDto? = null,
    val mensaje: String? = null
) {
    val filtrados: List<UsuarioDto> get() = usuarios.filter { filtro == null || it.rol == filtro.name }
    fun cantidad(rol: Rol?): Int = usuarios.count { rol == null || it.rol == rol.name }
}

/** Listado de usuarios para el administrador: tecnicos, clientes y administradores. */
class UsuariosViewModel(private val usuarioRepository: UsuarioRepository) : ViewModel() {

    private val _estado = MutableStateFlow(UsuariosUiState())
    val estado: StateFlow<UsuariosUiState> = _estado.asStateFlow()

    /** Se llama al entrar a la pantalla y al volver de crear/editar. */
    fun recargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            when (val r = usuarioRepository.listar()) {
                is Resultado.Exito -> _estado.update { it.copy(cargando = false, usuarios = r.dato) }
                is Resultado.Error -> _estado.update { it.copy(cargando = false, error = r.mensaje) }
            }
        }
    }

    fun cambiarFiltro(rol: Rol?) = _estado.update { it.copy(filtro = rol) }

    fun pedirEliminar(usuario: UsuarioDto) = _estado.update { it.copy(porEliminar = usuario) }

    fun cancelarEliminar() = _estado.update { it.copy(porEliminar = null) }

    fun eliminar() {
        val usuario = _estado.value.porEliminar ?: return
        val id = usuario.id ?: return
        _estado.update { it.copy(porEliminar = null) }
        viewModelScope.launch {
            when (val r = usuarioRepository.eliminar(id)) {
                is Resultado.Exito -> {
                    _estado.update { it.copy(mensaje = "Usuario ${usuario.nombre} eliminado") }
                    recargar()
                }
                is Resultado.Error -> _estado.update { it.copy(mensaje = r.mensaje) }
            }
        }
    }

    fun mensajeMostrado() = _estado.update { it.copy(mensaje = null) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ServicioTecnicoApp
                UsuariosViewModel(app.usuarioRepository)
            }
        }
    }
}
