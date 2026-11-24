package cl.duoc.serviciotecnico.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.serviciotecnico.ServicioTecnicoApp
import cl.duoc.serviciotecnico.data.local.entity.Equipo
import cl.duoc.serviciotecnico.data.local.entity.Guia
import cl.duoc.serviciotecnico.data.local.entity.Ticket
import cl.duoc.serviciotecnico.data.repository.EquipoRepository
import cl.duoc.serviciotecnico.data.repository.GuiaRepository
import cl.duoc.serviciotecnico.data.repository.SesionRepository
import cl.duoc.serviciotecnico.data.repository.TicketRepository
import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.domain.validation.CampoCambioEstado
import cl.duoc.serviciotecnico.domain.validation.ValidadorCambioEstado
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Formulario de cambio de estado: lo que escribe el tecnico. */
data class CambioEstadoUi(
    val seleccionado: EstadoTicket? = null,
    val comentario: String = "",
    val tocados: Set<CampoCambioEstado> = emptySet(),
    val intentoEnviar: Boolean = false,
    val enviando: Boolean = false
)

data class DetalleTicketUiState(
    val cargando: Boolean = true,
    val ticket: Ticket? = null,
    val equipo: Equipo? = null,
    val guias: List<Guia> = emptyList(),
    val cambio: CambioEstadoUi = CambioEstadoUi(),
    val formulario: EstadoFormulario<CampoCambioEstado> = EstadoFormulario(),
    val mensaje: String? = null,
    val esAdministrador: Boolean = false
) {
    val editable: Boolean get() = ticket?.editable == true

    /** Emitir el reporte tecnico (guia) es trabajo del tecnico asignado, no del administrador. */
    val puedeEmitirReporte: Boolean get() = editable && !esAdministrador

    val estadosDisponibles: List<EstadoTicket>
        get() = ticket?.let { ValidadorCambioEstado.estadosDisponibles(it.estado, esAdministrador) }.orEmpty()

    /** Al elegir Cerrado el comentario es la solucion; al Cancelar, el motivo. */
    val etiquetaComentario: String
        get() = when (cambio.seleccionado) {
            EstadoTicket.CERRADO -> "Solucion aplicada"
            EstadoTicket.CANCELADO -> "Motivo de la cancelacion"
            else -> "Comentario del cambio"
        }
}

private data class Datos(val ticket: Ticket?, val equipo: Equipo?, val guias: List<Guia>, val esAdmin: Boolean)

@OptIn(ExperimentalCoroutinesApi::class)
class DetalleTicketViewModel(
    private val ticketId: String,
    private val ticketRepository: TicketRepository,
    guiaRepository: GuiaRepository,
    equipoRepository: EquipoRepository,
    sesionRepository: SesionRepository
) : ViewModel() {

    private val cambio = MutableStateFlow(CambioEstadoUi())
    private val mensaje = MutableStateFlow<String?>(null)

    private val ticketFlow = ticketRepository.observarTicket(ticketId)

    private val datosFlow = combine(
        ticketFlow,
        ticketFlow.flatMapLatest { t -> if (t == null) flowOf(null) else equipoRepository.observarEquipo(t.equipoId) },
        guiaRepository.observarGuiasDeTicket(ticketId),
        sesionRepository.sesion
    ) { ticket, equipo, guias, usuario -> Datos(ticket, equipo, guias, usuario?.esAdministrador == true) }

    /**
     * Estado de la pantalla. Los errores se recalculan cada vez que cambia lo escrito
     * o la cantidad de guias, asi la regla "no cerrar sin guias" reacciona sola.
     */
    val estado: StateFlow<DetalleTicketUiState> = combine(datosFlow, cambio, mensaje) { d, c, m ->
        val errores = d.ticket?.let {
            ValidadorCambioEstado.validar(it.estado, c.seleccionado, c.comentario, d.guias.size, d.esAdmin)
        }.orEmpty()
        DetalleTicketUiState(
            esAdministrador = d.esAdmin,
            cargando = false,
            ticket = d.ticket,
            equipo = d.equipo,
            guias = d.guias,
            cambio = c,
            formulario = EstadoFormulario(errores, c.tocados, c.intentoEnviar),
            mensaje = m
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetalleTicketUiState()
    )

    fun seleccionarEstado(nuevo: EstadoTicket) = cambio.update {
        it.copy(seleccionado = nuevo, tocados = it.tocados + CampoCambioEstado.ESTADO)
    }

    fun cambiarComentario(texto: String) = cambio.update {
        it.copy(comentario = texto, tocados = it.tocados + CampoCambioEstado.COMENTARIO)
    }

    fun enviarCambioDeEstado() {
        cambio.update { it.copy(intentoEnviar = true) }
        val s = estado.value
        val c = cambio.value
        val nuevo = c.seleccionado
        if (c.enviando || !s.editable) return
        // Se vuelve a validar con los datos actuales, sin depender de la pantalla
        val errores = s.ticket?.let {
            ValidadorCambioEstado.validar(it.estado, nuevo, c.comentario, s.guias.size, s.esAdministrador)
        }.orEmpty()
        if (errores.isNotEmpty() || nuevo == null) {
            mensaje.value = "Revisa el formulario de cambio de estado"
            return
        }

        cambio.update { it.copy(enviando = true) }
        viewModelScope.launch {
            val aplicado = ticketRepository.cambiarEstado(ticketId, nuevo, c.comentario.trim())
            if (aplicado) {
                cambio.value = CambioEstadoUi() // formulario limpio para el siguiente cambio
                mensaje.value = "Estado actualizado a ${nuevo.etiqueta}"
            } else {
                cambio.update { it.copy(enviando = false) }
                mensaje.value = "No se pudo cambiar el estado"
            }
        }
    }

    fun mensajeMostrado() {
        mensaje.value = null
    }

    companion object {
        fun factory(ticketId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ServicioTecnicoApp
                DetalleTicketViewModel(
                    ticketId, app.ticketRepository, app.guiaRepository, app.equipoRepository, app.sesionRepository
                )
            }
        }
    }
}
