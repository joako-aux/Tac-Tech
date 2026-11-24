package cl.duoc.serviciotecnico.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.serviciotecnico.ServicioTecnicoApp
import cl.duoc.serviciotecnico.data.local.entity.Ticket
import cl.duoc.serviciotecnico.data.repository.GeocodificacionRepository
import cl.duoc.serviciotecnico.data.repository.GuiaRepository
import cl.duoc.serviciotecnico.data.repository.TicketRepository
import cl.duoc.serviciotecnico.domain.validation.CampoGuia
import cl.duoc.serviciotecnico.domain.validation.DatosGuia
import cl.duoc.serviciotecnico.domain.validation.ValidadorGuia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FormularioGuiaUiState(
    val ticket: Ticket? = null,
    val direccion: String = "",
    val tecnico: String = "",
    val recibidoPor: String = "",
    val trabajoRealizado: String = "",
    val repuestos: String = "",
    val observaciones: String = "",
    val latitud: Double? = null,
    val longitud: Double? = null,
    val fotos: List<String> = emptyList(),
    /** Direccion de las coordenadas segun la API externa (OpenStreetMap). */
    val gps: DireccionGpsUi = DireccionGpsUi(),
    val guardando: Boolean = false,
    val guardado: Boolean = false,
    val mensaje: String? = null,
    val formulario: EstadoFormulario<CampoGuia> = EstadoFormulario()
) {
    fun error(campo: CampoGuia): String? = formulario.error(campo)

    /** Check verde solo en campos con contenido (los opcionales vacios no se marcan). */
    fun valido(campo: CampoGuia): Boolean = formulario.correcto(campo) && valor(campo).isNotBlank()

    private fun valor(campo: CampoGuia): String = when (campo) {
        CampoGuia.DIRECCION -> direccion
        CampoGuia.TECNICO -> tecnico
        CampoGuia.RECIBIDO_POR -> recibidoPor
        CampoGuia.TRABAJO -> trabajoRealizado
        CampoGuia.REPUESTOS -> repuestos
        CampoGuia.OBSERVACIONES -> observaciones
        CampoGuia.FOTOS -> if (fotos.isEmpty()) "" else "ok"
        CampoGuia.UBICACION -> if (tieneUbicacion) "ok" else ""
    }

    val tieneUbicacion: Boolean get() = latitud != null && longitud != null
    val puedeAgregarFotos: Boolean get() = fotos.size < ValidadorGuia.MAX_FOTOS

    /** Se ofrece usar la direccion del GPS solo si es distinta a la que ya esta escrita. */
    val puedeUsarDireccionGps: Boolean
        get() = gps.direccion != null && gps.direccion.corta.take(MAX_DIRECCION) != direccion

    /** Incluye la evidencia: el validador exige al menos una foto y la ubicacion. */
    fun datos() = DatosGuia(
        direccion, tecnico, recibidoPor, trabajoRealizado, repuestos, observaciones,
        cantidadFotos = fotos.size,
        tieneUbicacion = tieneUbicacion
    )
}

/** Largo maximo del campo direccion (ver ValidadorGuia). */
const val MAX_DIRECCION = 120

class FormularioGuiaViewModel(
    private val ticketId: String,
    private val ticketRepository: TicketRepository,
    private val guiaRepository: GuiaRepository,
    private val geocodificacion: GeocodificacionRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(FormularioGuiaUiState())
    val estado: StateFlow<FormularioGuiaUiState> = _estado.asStateFlow()

    init {
        validar()
        viewModelScope.launch {
            val ticket = ticketRepository.obtenerTicket(ticketId)
            // La guia hereda la direccion del ticket; el tecnico puede corregirla
            _estado.update { it.copy(ticket = ticket, direccion = ticket?.direccion.orEmpty()) }
            validar()
        }
    }

    fun cambiarCampo(campo: CampoGuia, valor: String) {
        _estado.update {
            val s = when (campo) {
                CampoGuia.DIRECCION -> it.copy(direccion = valor)
                CampoGuia.TECNICO -> it.copy(tecnico = valor)
                CampoGuia.RECIBIDO_POR -> it.copy(recibidoPor = valor)
                CampoGuia.TRABAJO -> it.copy(trabajoRealizado = valor)
                CampoGuia.REPUESTOS -> it.copy(repuestos = valor)
                CampoGuia.OBSERVACIONES -> it.copy(observaciones = valor)
                CampoGuia.FOTOS, CampoGuia.UBICACION -> it // no son de texto
            }
            s.copy(formulario = s.formulario.tocar(campo))
        }
        validar()
    }

    private fun validar() {
        _estado.update { it.copy(formulario = it.formulario.conErrores(ValidadorGuia.validar(it.datos()))) }
    }

    fun registrarUbicacion(latitud: Double, longitud: Double) {
        _estado.update {
            it.copy(
                latitud = latitud,
                longitud = longitud,
                mensaje = "Ubicacion registrada",
                formulario = it.formulario.tocar(CampoGuia.UBICACION)
            )
        }
        validar()
        buscarDireccion(latitud, longitud)
    }

    /** API externa: traduce las coordenadas a una direccion legible. */
    private fun buscarDireccion(latitud: Double, longitud: Double) {
        _estado.update { it.copy(gps = DireccionGpsUi.Buscando) }
        viewModelScope.launch {
            val resultado = geocodificacion.direccionDe(latitud, longitud)
            _estado.update { it.copy(gps = DireccionGpsUi.desde(resultado)) }
        }
    }

    /** Vuelve a consultar la direccion (por ejemplo, si no habia internet). */
    fun reintentarDireccion() {
        val s = _estado.value
        val lat = s.latitud ?: return
        val lon = s.longitud ?: return
        buscarDireccion(lat, lon)
    }

    /** El tecnico acepta la direccion del GPS como direccion del reporte. */
    fun usarDireccionGps() {
        val direccion = _estado.value.gps.direccion ?: return
        _estado.update {
            it.copy(
                direccion = direccion.corta.take(MAX_DIRECCION),
                mensaje = "Direccion actualizada con la ubicacion GPS",
                formulario = it.formulario.tocar(CampoGuia.DIRECCION)
            )
        }
        validar()
    }

    fun agregarFoto(uri: String) {
        if (!_estado.value.puedeAgregarFotos) {
            _estado.update { it.copy(mensaje = "Maximo ${ValidadorGuia.MAX_FOTOS} fotografias por guia") }
            return
        }
        _estado.update { it.copy(fotos = it.fotos + uri, formulario = it.formulario.tocar(CampoGuia.FOTOS)) }
        validar()
    }

    fun quitarFoto(uri: String) {
        _estado.update { it.copy(fotos = it.fotos - uri, formulario = it.formulario.tocar(CampoGuia.FOTOS)) }
        validar()
    }

    fun mensajeMostrado() {
        _estado.update { it.copy(mensaje = null) }
    }

    fun guardar() {
        _estado.update { it.copy(formulario = it.formulario.enviar()) }
        val s = _estado.value
        if (s.guardando) return
        if (!s.formulario.esValido) {
            val falta = s.formulario.errores.keys
            val texto = when {
                CampoGuia.FOTOS in falta && CampoGuia.UBICACION in falta -> "Falta la foto de evidencia y la ubicacion"
                CampoGuia.FOTOS in falta -> "Falta la fotografia de evidencia"
                CampoGuia.UBICACION in falta -> "Falta registrar la ubicacion de la visita"
                else -> "Revisa los campos marcados en rojo"
            }
            _estado.update { it.copy(mensaje = texto) }
            return
        }
        val ticket = s.ticket
        if (ticket == null || !ticket.editable) {
            _estado.update { it.copy(mensaje = "No se puede emitir una guia para este ticket") }
            return
        }

        _estado.update { it.copy(guardando = true) }
        viewModelScope.launch {
            try {
                guiaRepository.crear(
                    ticketId = ticketId,
                    equipoId = ticket.equipoId,
                    direccion = s.direccion.trim(),
                    tecnico = s.tecnico.trim(),
                    trabajoRealizado = s.trabajoRealizado.trim(),
                    repuestos = s.repuestos.trim(),
                    observaciones = s.observaciones.trim(),
                    recibidoPor = s.recibidoPor.trim(),
                    latitud = s.latitud,
                    longitud = s.longitud,
                    urisDeFotos = s.fotos
                )
                _estado.update { it.copy(guardando = false, guardado = true) }
            } catch (e: Exception) {
                _estado.update { it.copy(guardando = false, mensaje = "No se pudo guardar la guia") }
            }
        }
    }

    companion object {
        fun factory(ticketId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ServicioTecnicoApp
                FormularioGuiaViewModel(ticketId, app.ticketRepository, app.guiaRepository, app.geocodificacionRepository)
            }
        }
    }
}
