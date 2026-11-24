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
import cl.duoc.serviciotecnico.data.repository.GeocodificacionRepository
import cl.duoc.serviciotecnico.data.repository.GuiaRepository
import cl.duoc.serviciotecnico.data.repository.TicketRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetalleGuiaUiState(
    val cargando: Boolean = true,
    val guia: Guia? = null,
    val ticket: Ticket? = null,
    val equipo: Equipo? = null,
    val fotos: List<String> = emptyList(),
    /** Direccion de la ubicacion registrada segun la API externa (OpenStreetMap). */
    val gps: DireccionGpsUi = DireccionGpsUi()
)

@OptIn(ExperimentalCoroutinesApi::class)
class DetalleGuiaViewModel(
    guiaId: String,
    guiaRepository: GuiaRepository,
    ticketRepository: TicketRepository,
    equipoRepository: EquipoRepository,
    private val geocodificacion: GeocodificacionRepository
) : ViewModel() {

    private val guiaFlow = guiaRepository.observarGuia(guiaId)
    private val gps = MutableStateFlow(DireccionGpsUi())

    val estado: StateFlow<DetalleGuiaUiState> = combine(
        guiaFlow,
        guiaFlow.flatMapLatest { g -> if (g == null) flowOf(null) else ticketRepository.observarTicket(g.ticketId) },
        guiaFlow.flatMapLatest { g -> if (g == null) flowOf(null) else equipoRepository.observarEquipo(g.equipoId) },
        guiaRepository.observarFotos(guiaId),
        gps
    ) { guia, ticket, equipo, fotos, direccionGps ->
        DetalleGuiaUiState(
            cargando = false,
            guia = guia,
            ticket = ticket,
            equipo = equipo,
            fotos = fotos.map { it.uri },
            gps = direccionGps
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetalleGuiaUiState()
    )

    init {
        // Cuando la guia carga y tiene coordenadas, se pide su direccion a la API externa
        viewModelScope.launch {
            val guia = guiaFlow.filterNotNull().first()
            val lat = guia.latitud
            val lon = guia.longitud
            if (lat != null && lon != null) {
                gps.value = DireccionGpsUi.Buscando
                gps.value = DireccionGpsUi.desde(geocodificacion.direccionDe(lat, lon))
            }
        }
    }

    companion object {
        fun factory(guiaId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ServicioTecnicoApp
                DetalleGuiaViewModel(
                    guiaId, app.guiaRepository, app.ticketRepository, app.equipoRepository, app.geocodificacionRepository
                )
            }
        }
    }
}
