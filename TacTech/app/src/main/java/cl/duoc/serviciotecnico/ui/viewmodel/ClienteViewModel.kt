package cl.duoc.serviciotecnico.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.serviciotecnico.ServicioTecnicoApp
import cl.duoc.serviciotecnico.data.local.entity.Solicitud
import cl.duoc.serviciotecnico.data.local.entity.Ticket
import cl.duoc.serviciotecnico.data.repository.EstadoSync
import cl.duoc.serviciotecnico.data.repository.SesionRepository
import cl.duoc.serviciotecnico.data.repository.SincronizacionRepository
import cl.duoc.serviciotecnico.data.repository.SolicitudRepository
import cl.duoc.serviciotecnico.data.repository.TicketRepository
import cl.duoc.serviciotecnico.domain.model.EstadoSolicitud
import cl.duoc.serviciotecnico.domain.model.Usuario
import cl.duoc.serviciotecnico.domain.validation.ValidadorSolicitud
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

/** Una solicitud junto al ticket que se creo desde ella (si ya fue confirmada). */
data class SolicitudConTicket(val solicitud: Solicitud, val ticket: Ticket?)

/** Dialogo de cancelacion: que solicitud, el motivo escrito y si ya se intento confirmar. */
data class CancelacionUi(
    val solicitudId: String,
    val motivo: String = "",
    val intentoEnviar: Boolean = false
) {
    val error: String? get() = if (intentoEnviar) ValidadorSolicitud.validarMotivo(motivo) else null
}

data class ClienteUiState(
    val cargando: Boolean = true,
    val usuario: Usuario? = null,
    val solicitudes: List<SolicitudConTicket> = emptyList(),
    val sync: EstadoSync = EstadoSync(),
    val cancelacion: CancelacionUi? = null,
    val mensaje: String? = null
) {
    val porConfirmar: Int get() = solicitudes.count { it.solicitud.estado == EstadoSolicitud.POR_CONFIRMAR }
    val enAtencion: Int get() = solicitudes.count { it.ticket != null && !it.ticket.estado.esFinal }
    val atendidas: Int get() = solicitudes.count { it.ticket?.estado?.esFinal == true }
}

/**
 * Panel del cliente: sus solicitudes, el estado del ticket que genero cada una
 * (sincronizacion multipanel) y la opcion de cancelar mientras esten por confirmar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClienteViewModel(
    private val solicitudRepository: SolicitudRepository,
    ticketRepository: TicketRepository,
    private val sesionRepository: SesionRepository,
    private val sincronizacion: SincronizacionRepository
) : ViewModel() {

    private val cancelacion = MutableStateFlow<CancelacionUi?>(null)
    private val mensaje = MutableStateFlow<String?>(null)

    private val datos = sesionRepository.sesion.flatMapLatest { usuario ->
        if (usuario == null) {
            flowOf<Pair<Usuario?, List<SolicitudConTicket>>>(null to emptyList())
        } else {
            combine(
                solicitudRepository.observarDeCliente(usuario.correo),
                ticketRepository.observarTickets()
            ) { solicitudes, tickets ->
                val porId = tickets.associateBy { it.id }
                usuario to solicitudes.map { s -> SolicitudConTicket(s, s.ticketId?.let(porId::get)) }
            }
        }
    }

    val estado: StateFlow<ClienteUiState> = combine(datos, sincronizacion.estado, cancelacion, mensaje) { d, sync, c, m ->
        ClienteUiState(cargando = false, usuario = d.first, solicitudes = d.second, sync = sync, cancelacion = c, mensaje = m)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClienteUiState())

    init {
        sincronizarAhora()
    }

    fun sincronizarAhora() {
        viewModelScope.launch { sincronizacion.sincronizar() }
    }

    // ---------- Cancelar (anular por error) ----------

    fun pedirCancelacion(solicitudId: String) {
        cancelacion.value = CancelacionUi(solicitudId)
    }

    fun cambiarMotivo(texto: String) = cancelacion.update { it?.copy(motivo = texto) }

    fun cerrarCancelacion() {
        cancelacion.value = null
    }

    fun confirmarCancelacion() {
        val c = cancelacion.value ?: return
        cancelacion.value = c.copy(intentoEnviar = true)
        if (ValidadorSolicitud.validarMotivo(c.motivo) != null) return
        viewModelScope.launch {
            val ok = solicitudRepository.cancelar(c.solicitudId, c.motivo.trim())
            cancelacion.value = null
            mensaje.value = if (ok) "Solicitud cancelada" else "La solicitud ya fue procesada y no se puede cancelar"
        }
    }

    fun mensajeMostrado() {
        mensaje.value = null
    }

    fun cerrarSesion(alTerminar: () -> Unit) {
        viewModelScope.launch {
            sesionRepository.cerrarSesion()
            alTerminar()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ServicioTecnicoApp
                ClienteViewModel(app.solicitudRepository, app.ticketRepository, app.sesionRepository, app.sincronizacionRepository)
            }
        }
    }
}
