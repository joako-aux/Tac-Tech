package cl.duoc.serviciotecnico.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.serviciotecnico.ServicioTecnicoApp
import cl.duoc.serviciotecnico.data.remote.Resultado
import cl.duoc.serviciotecnico.data.remote.dto.EmpresaDto
import cl.duoc.serviciotecnico.data.repository.EmpresaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EmpresasUiState(
    val cargando: Boolean = true,
    val empresas: List<EmpresaDto> = emptyList(),
    val error: String? = null,
    val porEliminar: EmpresaDto? = null,
    val mensaje: String? = null
)

class EmpresasViewModel(private val empresaRepository: EmpresaRepository) : ViewModel() {

    private val _estado = MutableStateFlow(EmpresasUiState())
    val estado: StateFlow<EmpresasUiState> = _estado.asStateFlow()

    fun recargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            when (val r = empresaRepository.listar()) {
                is Resultado.Exito -> _estado.update { it.copy(cargando = false, empresas = r.dato) }
                is Resultado.Error -> _estado.update { it.copy(cargando = false, error = r.mensaje) }
            }
        }
    }

    fun pedirEliminar(empresa: EmpresaDto) = _estado.update { it.copy(porEliminar = empresa) }

    fun cancelarEliminar() = _estado.update { it.copy(porEliminar = null) }

    fun eliminar() {
        val empresa = _estado.value.porEliminar ?: return
        val id = empresa.id ?: return
        _estado.update { it.copy(porEliminar = null) }
        viewModelScope.launch {
            when (val r = empresaRepository.eliminar(id)) {
                is Resultado.Exito -> {
                    _estado.update { it.copy(mensaje = "Empresa ${empresa.nombre} eliminada") }
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
                EmpresasViewModel(app.empresaRepository)
            }
        }
    }
}
