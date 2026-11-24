package cl.duoc.serviciotecnico.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.serviciotecnico.ServicioTecnicoApp
import cl.duoc.serviciotecnico.data.repository.SesionRepository
import cl.duoc.serviciotecnico.data.repository.SolicitudRepository
import cl.duoc.serviciotecnico.domain.model.Propiedad
import cl.duoc.serviciotecnico.domain.validation.CampoSolicitud
import cl.duoc.serviciotecnico.domain.validation.DatosSolicitud
import cl.duoc.serviciotecnico.domain.validation.ValidadorSolicitud
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FormularioSolicitudUiState(
    val titulo: String = "",
    val descripcion: String = "",
    val direccion: String = "",
    val nombreEquipo: String = "",
    val modelo: String = "",
    val serie: String = "",
    val propiedad: Propiedad = Propiedad.PROPIO_DEL_CLIENTE,
    val empresa: String? = null,
    val guardando: Boolean = false,
    val guardado: Boolean = false,
    val mensaje: String? = null,
    val formulario: EstadoFormulario<CampoSolicitud> = EstadoFormulario()
) {
    fun error(campo: CampoSolicitud): String? = formulario.error(campo)
    fun valido(campo: CampoSolicitud): Boolean = formulario.correcto(campo)

    val completos: Int get() = CampoSolicitud.entries.size - formulario.errores.size
    val progreso: Float get() = completos.toFloat() / CampoSolicitud.entries.size

    fun datos() = DatosSolicitud(titulo, descripcion, direccion, nombreEquipo, modelo, serie)
}

/** El cliente crea una solicitud para su equipo de frio. Funciona sin conexion. */
class FormularioSolicitudViewModel(
    private val solicitudRepository: SolicitudRepository,
    private val sesionRepository: SesionRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(FormularioSolicitudUiState())
    val estado: StateFlow<FormularioSolicitudUiState> = _estado.asStateFlow()

    init {
        validar()
        viewModelScope.launch {
            val usuario = sesionRepository.usuarioActual()
            _estado.update { it.copy(empresa = usuario?.empresaNombre) }
        }
    }

    fun cambiarCampo(campo: CampoSolicitud, valor: String) {
        _estado.update {
            val s = when (campo) {
                CampoSolicitud.TITULO -> it.copy(titulo = valor)
                CampoSolicitud.DESCRIPCION -> it.copy(descripcion = valor)
                CampoSolicitud.DIRECCION -> it.copy(direccion = valor)
                CampoSolicitud.NOMBRE_EQUIPO -> it.copy(nombreEquipo = valor)
                CampoSolicitud.MODELO -> it.copy(modelo = valor)
                CampoSolicitud.SERIE -> it.copy(serie = valor.uppercase())
            }
            s.copy(formulario = s.formulario.tocar(campo))
        }
        validar()
    }

    fun cambiarPropiedad(propiedad: Propiedad) = _estado.update { it.copy(propiedad = propiedad) }

    fun mensajeMostrado() = _estado.update { it.copy(mensaje = null) }

    private fun validar() {
        _estado.update { it.copy(formulario = it.formulario.conErrores(ValidadorSolicitud.validar(it.datos()))) }
    }

    fun enviar() {
        _estado.update { it.copy(formulario = it.formulario.enviar()) }
        val s = _estado.value
        if (s.guardando) return
        if (!s.formulario.esValido) {
            _estado.update { it.copy(mensaje = "Revisa los campos marcados en rojo") }
            return
        }
        _estado.update { it.copy(guardando = true) }
        viewModelScope.launch {
            val cliente = sesionRepository.usuarioActual()
            if (cliente == null) {
                _estado.update { it.copy(guardando = false, mensaje = "Tu sesion expiro, vuelve a ingresar") }
                return@launch
            }
            solicitudRepository.crear(
                cliente = cliente,
                titulo = s.titulo.trim(),
                descripcion = s.descripcion.trim(),
                direccion = s.direccion.trim(),
                equipoNombre = s.nombreEquipo.trim(),
                equipoModelo = s.modelo.trim(),
                equipoSerie = s.serie.trim(),
                propiedad = s.propiedad
            )
            _estado.update { it.copy(guardando = false, guardado = true) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ServicioTecnicoApp
                FormularioSolicitudViewModel(app.solicitudRepository, app.sesionRepository)
            }
        }
    }
}
