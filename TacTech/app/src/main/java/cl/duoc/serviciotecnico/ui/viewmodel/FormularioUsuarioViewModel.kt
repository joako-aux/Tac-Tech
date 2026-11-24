package cl.duoc.serviciotecnico.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.serviciotecnico.ServicioTecnicoApp
import cl.duoc.serviciotecnico.data.remote.Resultado
import cl.duoc.serviciotecnico.data.remote.dto.EmpresaDto
import cl.duoc.serviciotecnico.data.remote.dto.UsuarioRequest
import cl.duoc.serviciotecnico.data.repository.EmpresaRepository
import cl.duoc.serviciotecnico.data.repository.UsuarioRepository
import cl.duoc.serviciotecnico.domain.model.Rol
import cl.duoc.serviciotecnico.domain.validation.CampoUsuario
import cl.duoc.serviciotecnico.domain.validation.ValidadorUsuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FormularioUsuarioUiState(
    val esNuevo: Boolean = true,
    val nombre: String = "",
    val correo: String = "",
    val clave: String = "",
    val rol: Rol = Rol.TECNICO,
    val activo: Boolean = true,
    val empresaId: Long? = null,
    val empresas: List<EmpresaDto> = emptyList(),
    val cargando: Boolean = false,
    val guardando: Boolean = false,
    val guardado: Boolean = false,
    val mensaje: String? = null,
    val formulario: EstadoFormulario<CampoUsuario> = EstadoFormulario()
) {
    fun error(campo: CampoUsuario): String? = formulario.error(campo)
    fun valido(campo: CampoUsuario): Boolean = formulario.correcto(campo)
    val empresa: EmpresaDto? get() = empresas.firstOrNull { it.id == empresaId }
    val pideEmpresa: Boolean get() = rol == Rol.CLIENTE
}

/** Crear o editar un usuario (incluye dar de alta nuevos soportes tecnicos). */
class FormularioUsuarioViewModel(
    private val usuarioId: Long?,
    private val usuarioRepository: UsuarioRepository,
    private val empresaRepository: EmpresaRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(FormularioUsuarioUiState(esNuevo = usuarioId == null))
    val estado: StateFlow<FormularioUsuarioUiState> = _estado.asStateFlow()

    init {
        validar()
        cargar()
    }

    private fun cargar() {
        _estado.update { it.copy(cargando = true) }
        viewModelScope.launch {
            when (val r = empresaRepository.listar()) {
                is Resultado.Exito -> _estado.update { it.copy(empresas = r.dato) }
                is Resultado.Error -> _estado.update { it.copy(mensaje = r.mensaje) }
            }
            if (usuarioId != null) {
                when (val r = usuarioRepository.listar()) {
                    is Resultado.Exito -> r.dato.firstOrNull { it.id == usuarioId }?.let { u ->
                        _estado.update {
                            it.copy(
                                nombre = u.nombre,
                                correo = u.correo,
                                rol = Rol.valueOf(u.rol),
                                activo = u.activo,
                                empresaId = u.empresaId
                            )
                        }
                    }
                    is Resultado.Error -> _estado.update { it.copy(mensaje = r.mensaje) }
                }
            }
            _estado.update { it.copy(cargando = false) }
            validar()
        }
    }

    fun cambiarCampo(campo: CampoUsuario, valor: String) {
        _estado.update {
            val s = when (campo) {
                CampoUsuario.NOMBRE -> it.copy(nombre = valor)
                CampoUsuario.CORREO -> it.copy(correo = valor.trim())
                CampoUsuario.CLAVE -> it.copy(clave = valor)
                CampoUsuario.EMPRESA -> it
            }
            s.copy(formulario = s.formulario.tocar(campo))
        }
        validar()
    }

    fun cambiarRol(rol: Rol) {
        _estado.update { it.copy(rol = rol, empresaId = if (rol == Rol.CLIENTE) it.empresaId else null) }
        validar()
    }

    fun cambiarEmpresa(empresa: EmpresaDto) {
        _estado.update { it.copy(empresaId = empresa.id, formulario = it.formulario.tocar(CampoUsuario.EMPRESA)) }
        validar()
    }

    fun cambiarActivo(activo: Boolean) = _estado.update { it.copy(activo = activo) }

    fun mensajeMostrado() = _estado.update { it.copy(mensaje = null) }

    private fun validar() {
        _estado.update {
            it.copy(
                formulario = it.formulario.conErrores(
                    ValidadorUsuario.validar(it.nombre, it.correo, it.clave, it.rol, it.empresaId, it.esNuevo)
                )
            )
        }
    }

    fun guardar() {
        _estado.update { it.copy(formulario = it.formulario.enviar()) }
        val s = _estado.value
        if (s.guardando) return
        if (!s.formulario.esValido) {
            _estado.update { it.copy(mensaje = "Revisa los campos marcados en rojo") }
            return
        }
        _estado.update { it.copy(guardando = true) }
        viewModelScope.launch {
            val datos = UsuarioRequest(
                nombre = s.nombre.trim(),
                correo = s.correo.trim().lowercase(),
                clave = s.clave.ifEmpty { null },
                rol = s.rol.name,
                activo = s.activo,
                empresaId = s.empresaId
            )
            when (val r = usuarioRepository.guardar(usuarioId, datos)) {
                is Resultado.Exito -> _estado.update { it.copy(guardando = false, guardado = true) }
                is Resultado.Error -> _estado.update { it.copy(guardando = false, mensaje = r.mensaje) }
            }
        }
    }

    companion object {
        fun factory(usuarioId: Long?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ServicioTecnicoApp
                FormularioUsuarioViewModel(usuarioId, app.usuarioRepository, app.empresaRepository)
            }
        }
    }
}
