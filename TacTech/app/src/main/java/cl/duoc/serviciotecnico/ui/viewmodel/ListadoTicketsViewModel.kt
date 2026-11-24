package cl.duoc.serviciotecnico.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.serviciotecnico.ServicioTecnicoApp
import cl.duoc.serviciotecnico.data.local.entity.Ticket
import cl.duoc.serviciotecnico.data.repository.EstadoSync
import cl.duoc.serviciotecnico.data.repository.SesionRepository
import cl.duoc.serviciotecnico.data.repository.SincronizacionRepository
import cl.duoc.serviciotecnico.data.repository.TicketRepository
import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.domain.model.Usuario
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ListadoTicketsUiState(
    val cargando: Boolean = true,
    val usuario: Usuario? = null,
    val tickets: List<Ticket> = emptyList(),
    val sync: EstadoSync = EstadoSync()
) {
    val total: Int get() = tickets.size
    val activos: Int get() = tickets.count { !it.estado.esFinal }
    val cerrados: Int get() = tickets.count { it.estado == EstadoTicket.CERRADO }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ListadoTicketsViewModel(
    repository: TicketRepository,
    private val sesionRepository: SesionRepository,
    private val sincronizacion: SincronizacionRepository
) : ViewModel() {

    // El tecnico solo ve sus propios tickets. Si cambia la sesion, la consulta cambia sola.
    private val datos = sesionRepository.sesion.flatMapLatest { usuario ->
        if (usuario == null) flowOf<Pair<Usuario?, List<Ticket>>>(null to emptyList())
        else repository.observarTicketsDe(usuario.correo).map { usuario to it }
    }

    val estado: StateFlow<ListadoTicketsUiState> = combine(datos, sincronizacion.estado) { (usuario, lista), sync ->
        ListadoTicketsUiState(cargando = false, usuario = usuario, tickets = lista, sync = sync)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ListadoTicketsUiState()
    )

    init {
        // Al entrar se descargan los tickets que el administrador asigno a este tecnico
        sincronizarAhora()
    }

    fun sincronizarAhora() {
        viewModelScope.launch { sincronizacion.sincronizar() }
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
                ListadoTicketsViewModel(app.ticketRepository, app.sesionRepository, app.sincronizacionRepository)
            }
        }
    }
}
