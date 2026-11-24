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
import cl.duoc.serviciotecnico.data.repository.SolicitudRepository
import cl.duoc.serviciotecnico.data.repository.TicketRepository
import cl.duoc.serviciotecnico.domain.model.EstadoSolicitud
import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.domain.model.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Filtro principal del panel de respaldo: el estado de las tareas. */
enum class FiltroAdmin(val etiqueta: String) {
    TODOS("Todos"),
    PENDIENTES("Pendientes"),
    EN_PROCESO("En proceso"),
    TERMINADOS("Terminados"),
    CANCELADOS("Cancelados");

    fun incluye(estado: EstadoTicket): Boolean = when (this) {
        TODOS -> true
        PENDIENTES -> !estado.esFinal && !estado.enProceso
        EN_PROCESO -> estado.enProceso
        TERMINADOS -> estado == EstadoTicket.CERRADO
        CANCELADOS -> estado == EstadoTicket.CANCELADO
    }
}

/** Avance de un tecnico: cuantas tareas tiene pendientes y cuantas termino. */
data class ResumenTecnico(val correo: String, val pendientes: Int, val terminados: Int) {
    val total: Int get() = pendientes + terminados
    val avance: Float get() = if (total == 0) 0f else terminados.toFloat() / total
}

data class AdminUiState(
    val cargando: Boolean = true,
    val usuario: Usuario? = null,
    val tickets: List<Ticket> = emptyList(),
    val filtro: FiltroAdmin = FiltroAdmin.PENDIENTES,
    val tecnico: String? = null,
    val busqueda: String = "",
    val sync: EstadoSync = EstadoSync(),
    val solicitudesPorConfirmar: Int = 0
) {
    fun cantidad(filtro: FiltroAdmin): Int = tickets.count { filtro.incluye(it.estado) }
    val pendientes: Int get() = cantidad(FiltroAdmin.PENDIENTES)
    val enProceso: Int get() = cantidad(FiltroAdmin.EN_PROCESO)
    val terminados: Int get() = cantidad(FiltroAdmin.TERMINADOS)
    val cancelados: Int get() = cantidad(FiltroAdmin.CANCELADOS)

    val tecnicos: List<String>
        get() = tickets.map { it.tecnicoCorreo }.filter { it.isNotBlank() }.distinct().sorted()

    val resumenPorTecnico: List<ResumenTecnico>
        get() = tickets.filter { it.tecnicoCorreo.isNotBlank() }
            .groupBy { it.tecnicoCorreo }
            .map { (correo, lista) ->
                ResumenTecnico(correo, lista.count { !it.estado.esFinal }, lista.count { it.estado.esFinal })
            }
            .sortedByDescending { it.pendientes }

    /** Lista que se muestra: aplica filtro de estado, tecnico y texto, en ese orden. */
    val filtrados: List<Ticket>
        get() = tickets
            .filter { filtro.incluye(it.estado) }
            .filter { tecnico == null || it.tecnicoCorreo == tecnico }
            .filter {
                busqueda.isBlank() ||
                    it.titulo.contains(busqueda, ignoreCase = true) ||
                    it.cliente.contains(busqueda, ignoreCase = true) ||
                    it.numero.toString() == busqueda.trim()
            }
}

private data class FiltrosAdmin(val filtro: FiltroAdmin, val tecnico: String?, val busqueda: String)

/**
 * ViewModel del rol Administrador: supervisa las tareas de todos los tecnicos.
 * Los datos salen de Room (siempre disponibles) y se refrescan desde el backend al sincronizar.
 */
class AdminViewModel(
    ticketRepository: TicketRepository,
    solicitudRepository: SolicitudRepository,
    private val sesionRepository: SesionRepository,
    private val sincronizacion: SincronizacionRepository,
    filtroInicial: FiltroAdmin = FiltroAdmin.PENDIENTES,
    tecnicoInicial: String? = null
) : ViewModel() {

    private val filtros = MutableStateFlow(FiltrosAdmin(filtroInicial, tecnicoInicial, ""))

    val estado: StateFlow<AdminUiState> = combine(
        sesionRepository.sesion,
        ticketRepository.observarTickets(),
        filtros,
        sincronizacion.estado,
        solicitudRepository.observarTodas()
    ) { usuario, tickets, f, sync, solicitudes ->
        AdminUiState(
            solicitudesPorConfirmar = solicitudes.count { it.estado == EstadoSolicitud.POR_CONFIRMAR },
            cargando = false,
            usuario = usuario,
            tickets = tickets,
            filtro = f.filtro,
            tecnico = f.tecnico,
            busqueda = f.busqueda,
            sync = sync
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AdminUiState(filtro = filtroInicial, tecnico = tecnicoInicial)
    )

    init {
        // Al abrir el panel se piden los datos mas recientes de todos los tecnicos
        sincronizarAhora()
    }

    fun cambiarFiltro(filtro: FiltroAdmin) {
        filtros.value = filtros.value.copy(filtro = filtro)
    }

    fun cambiarTecnico(correo: String?) {
        filtros.value = filtros.value.copy(tecnico = correo)
    }

    fun cambiarBusqueda(texto: String) {
        filtros.value = filtros.value.copy(busqueda = texto)
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
        /** filtro y tecnico llegan desde la navegacion (ej: tocar "Terminados" en el panel). */
        fun factory(filtro: FiltroAdmin = FiltroAdmin.PENDIENTES, tecnico: String? = null) = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ServicioTecnicoApp
                AdminViewModel(
                    app.ticketRepository, app.solicitudRepository, app.sesionRepository,
                    app.sincronizacionRepository, filtro, tecnico
                )
            }
        }
    }
}
