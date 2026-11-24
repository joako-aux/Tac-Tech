package cl.duoc.serviciotecnico.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.serviciotecnico.ServicioTecnicoApp
import cl.duoc.serviciotecnico.data.local.entity.Solicitud
import cl.duoc.serviciotecnico.data.repository.EstadoSync
import cl.duoc.serviciotecnico.data.repository.SincronizacionRepository
import cl.duoc.serviciotecnico.data.repository.SolicitudRepository
import cl.duoc.serviciotecnico.domain.model.EstadoSolicitud
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SolicitudesAdminUiState(
    val cargando: Boolean = true,
    val solicitudes: List<Solicitud> = emptyList(),
    /** null = todas. Por defecto se muestran las que esperan al administrador. */
    val filtro: EstadoSolicitud? = EstadoSolicitud.POR_CONFIRMAR,
    val sync: EstadoSync = EstadoSync()
) {
    val filtradas: List<Solicitud> get() = solicitudes.filter { filtro == null || it.estado == filtro }
    fun cantidad(estado: EstadoSolicitud?): Int = solicitudes.count { estado == null || it.estado == estado }
}

/** Bandeja de solicitudes del administrador. */
class SolicitudesAdminViewModel(
    solicitudRepository: SolicitudRepository,
    private val sincronizacion: SincronizacionRepository
) : ViewModel() {

    private val filtro = MutableStateFlow<EstadoSolicitud?>(EstadoSolicitud.POR_CONFIRMAR)

    val estado: StateFlow<SolicitudesAdminUiState> = combine(
        solicitudRepository.observarTodas(), filtro, sincronizacion.estado
    ) { lista, f, sync ->
        SolicitudesAdminUiState(cargando = false, solicitudes = lista, filtro = f, sync = sync)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SolicitudesAdminUiState())

    init {
        sincronizarAhora()
    }

    fun cambiarFiltro(nuevo: EstadoSolicitud?) {
        filtro.value = nuevo
    }

    fun sincronizarAhora() {
        viewModelScope.launch { sincronizacion.sincronizar() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ServicioTecnicoApp
                SolicitudesAdminViewModel(app.solicitudRepository, app.sincronizacionRepository)
            }
        }
    }
}
