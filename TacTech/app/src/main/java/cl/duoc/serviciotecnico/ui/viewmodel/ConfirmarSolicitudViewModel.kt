package cl.duoc.serviciotecnico.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.serviciotecnico.ServicioTecnicoApp
import cl.duoc.serviciotecnico.data.local.entity.Solicitud
import cl.duoc.serviciotecnico.data.remote.Resultado
import cl.duoc.serviciotecnico.data.remote.dto.UsuarioDto
import cl.duoc.serviciotecnico.data.repository.SincronizacionRepository
import cl.duoc.serviciotecnico.data.repository.SolicitudRepository
import cl.duoc.serviciotecnico.data.repository.UsuarioRepository
import cl.duoc.serviciotecnico.domain.model.Prioridad
import cl.duoc.serviciotecnico.domain.model.Rol
import cl.duoc.serviciotecnico.domain.validation.ValidadorConfirmacion
import cl.duoc.serviciotecnico.domain.validation.ValidadorSolicitud
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Lo que el administrador elige para crear el ticket. */
data class AsignacionUi(
    val tecnicos: List<UsuarioDto> = emptyList(),
    val cargandoTecnicos: Boolean = true,
    val errorTecnicos: String? = null,
    val tecnico: UsuarioDto? = null,
    val prioridad: Prioridad = Prioridad.MEDIA,
    val intentoEnviar: Boolean = false,
    val procesando: Boolean = false,
    /** Dialogo de rechazo abierto (motivo escrito) o null. */
    val motivoRechazo: String? = null,
    val intentoRechazar: Boolean = false,
    val mensaje: String? = null,
    val terminado: Boolean = false
) {
    val errorTecnico: String? get() = if (intentoEnviar) ValidadorConfirmacion.validar(tecnico?.correo) else null
    val errorMotivo: String? get() = if (intentoRechazar) motivoRechazo?.let(ValidadorSolicitud::validarMotivo) else null
}

data class ConfirmarSolicitudUiState(
    val solicitud: Solicitud? = null,
    val asignacion: AsignacionUi = AsignacionUi(),
    val enLinea: Boolean = true
)

/**
 * El administrador revisa una solicitud "Por confirmar" y crea el ticket formal,
 * asignandolo a un soporte tecnico, o la rechaza con un motivo.
 */
class ConfirmarSolicitudViewModel(
    private val solicitudId: String,
    private val solicitudRepository: SolicitudRepository,
    private val usuarioRepository: UsuarioRepository,
    private val sincronizacion: SincronizacionRepository
) : ViewModel() {

    private val asignacion = MutableStateFlow(AsignacionUi())

    val estado: StateFlow<ConfirmarSolicitudUiState> = combine(
        solicitudRepository.observarSolicitud(solicitudId), asignacion, sincronizacion.estado
    ) { s, a, sync ->
        ConfirmarSolicitudUiState(solicitud = s, asignacion = a, enLinea = sync.enLinea)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConfirmarSolicitudUiState())

    init {
        cargarTecnicos()
    }

    /** Lista de soportes tecnicos desde el servidor (incluye los recien creados por el admin). */
    fun cargarTecnicos() {
        asignacion.update { it.copy(cargandoTecnicos = true, errorTecnicos = null) }
        viewModelScope.launch {
            when (val r = usuarioRepository.listar(Rol.TECNICO)) {
                is Resultado.Exito -> asignacion.update {
                    it.copy(tecnicos = r.dato.filter { u -> u.activo }, cargandoTecnicos = false)
                }
                is Resultado.Error -> asignacion.update { it.copy(cargandoTecnicos = false, errorTecnicos = r.mensaje) }
            }
        }
    }

    fun elegirTecnico(tecnico: UsuarioDto) = asignacion.update { it.copy(tecnico = tecnico) }

    fun elegirPrioridad(prioridad: Prioridad) = asignacion.update { it.copy(prioridad = prioridad) }

    fun confirmar() {
        asignacion.update { it.copy(intentoEnviar = true) }
        val a = asignacion.value
        val tecnico = a.tecnico
        if (a.procesando || tecnico == null || ValidadorConfirmacion.validar(tecnico.correo) != null) return
        asignacion.update { it.copy(procesando = true) }
        viewModelScope.launch {
            when (val r = solicitudRepository.confirmar(solicitudId, tecnico.correo, a.prioridad)) {
                is Resultado.Exito -> {
                    // Se descargan el ticket y el equipo creados en el servidor
                    sincronizacion.sincronizar()
                    asignacion.update {
                        it.copy(procesando = false, terminado = true, mensaje = "Ticket N° ${r.dato} creado y asignado a ${tecnico.nombre}")
                    }
                }
                is Resultado.Error -> asignacion.update { it.copy(procesando = false, mensaje = r.mensaje) }
            }
        }
    }

    // ---------- Rechazo ----------

    fun abrirRechazo() = asignacion.update { it.copy(motivoRechazo = "", intentoRechazar = false) }

    fun cambiarMotivo(texto: String) = asignacion.update { it.copy(motivoRechazo = texto) }

    fun cerrarRechazo() = asignacion.update { it.copy(motivoRechazo = null) }

    fun rechazar() {
        asignacion.update { it.copy(intentoRechazar = true) }
        val motivo = asignacion.value.motivoRechazo ?: return
        if (ValidadorSolicitud.validarMotivo(motivo) != null) return
        asignacion.update { it.copy(procesando = true) }
        viewModelScope.launch {
            when (val r = solicitudRepository.rechazar(solicitudId, motivo.trim())) {
                is Resultado.Exito -> asignacion.update {
                    it.copy(procesando = false, motivoRechazo = null, terminado = true, mensaje = "Solicitud rechazada")
                }
                is Resultado.Error -> asignacion.update { it.copy(procesando = false, mensaje = r.mensaje) }
            }
        }
    }

    fun mensajeMostrado() = asignacion.update { it.copy(mensaje = null) }

    companion object {
        fun factory(solicitudId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ServicioTecnicoApp
                ConfirmarSolicitudViewModel(solicitudId, app.solicitudRepository, app.usuarioRepository, app.sincronizacionRepository)
            }
        }
    }
}
