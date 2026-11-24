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
import cl.duoc.serviciotecnico.data.repository.EmpresaRepository
import cl.duoc.serviciotecnico.domain.validation.CampoEmpresa
import cl.duoc.serviciotecnico.domain.validation.ValidadorEmpresa
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FormularioEmpresaUiState(
    val esNueva: Boolean = true,
    val nombre: String = "",
    val rut: String = "",
    val direccion: String = "",
    val telefono: String = "",
    val guardando: Boolean = false,
    val guardado: Boolean = false,
    val mensaje: String? = null,
    val formulario: EstadoFormulario<CampoEmpresa> = EstadoFormulario()
) {
    fun error(campo: CampoEmpresa): String? = formulario.error(campo)
    fun valido(campo: CampoEmpresa): Boolean = formulario.correcto(campo) && when (campo) {
        CampoEmpresa.TELEFONO -> telefono.isNotBlank()
        else -> true
    }
}

class FormularioEmpresaViewModel(
    private val empresaId: Long?,
    private val empresaRepository: EmpresaRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(FormularioEmpresaUiState(esNueva = empresaId == null))
    val estado: StateFlow<FormularioEmpresaUiState> = _estado.asStateFlow()

    init {
        validar()
        if (empresaId != null) {
            viewModelScope.launch {
                when (val r = empresaRepository.listar()) {
                    is Resultado.Exito -> r.dato.firstOrNull { it.id == empresaId }?.let { e ->
                        _estado.update {
                            it.copy(nombre = e.nombre, rut = e.rut, direccion = e.direccion, telefono = e.telefono.orEmpty())
                        }
                        validar()
                    }
                    is Resultado.Error -> _estado.update { it.copy(mensaje = r.mensaje) }
                }
            }
        }
    }

    fun cambiarCampo(campo: CampoEmpresa, valor: String) {
        _estado.update {
            val s = when (campo) {
                CampoEmpresa.NOMBRE -> it.copy(nombre = valor)
                CampoEmpresa.RUT -> it.copy(rut = valor.uppercase())
                CampoEmpresa.DIRECCION -> it.copy(direccion = valor)
                CampoEmpresa.TELEFONO -> it.copy(telefono = valor)
            }
            s.copy(formulario = s.formulario.tocar(campo))
        }
        validar()
    }

    fun mensajeMostrado() = _estado.update { it.copy(mensaje = null) }

    private fun validar() {
        _estado.update {
            it.copy(formulario = it.formulario.conErrores(ValidadorEmpresa.validar(it.nombre, it.rut, it.direccion, it.telefono)))
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
            val datos = EmpresaDto(
                id = empresaId,
                nombre = s.nombre.trim(),
                rut = s.rut.trim(),
                direccion = s.direccion.trim(),
                telefono = s.telefono.replace(" ", "").ifBlank { null }
            )
            when (val r = empresaRepository.guardar(datos)) {
                is Resultado.Exito -> _estado.update { it.copy(guardando = false, guardado = true) }
                is Resultado.Error -> _estado.update { it.copy(guardando = false, mensaje = r.mensaje) }
            }
        }
    }

    companion object {
        fun factory(empresaId: Long?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ServicioTecnicoApp
                FormularioEmpresaViewModel(empresaId, app.empresaRepository)
            }
        }
    }
}
