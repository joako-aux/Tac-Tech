package cl.duoc.serviciotecnico.ui.viewmodel

import cl.duoc.serviciotecnico.data.remote.Resultado
import cl.duoc.serviciotecnico.domain.model.DireccionGps

/** Estado de la busqueda de la direccion de la visita en la API externa (OpenStreetMap). */
data class DireccionGpsUi(
    val direccion: DireccionGps? = null,
    val buscando: Boolean = false,
    val error: String? = null
) {
    companion object {
        val Buscando = DireccionGpsUi(buscando = true)

        fun desde(resultado: Resultado<DireccionGps>): DireccionGpsUi = when (resultado) {
            is Resultado.Exito -> DireccionGpsUi(direccion = resultado.dato)
            is Resultado.Error -> DireccionGpsUi(error = resultado.mensaje)
        }
    }
}
