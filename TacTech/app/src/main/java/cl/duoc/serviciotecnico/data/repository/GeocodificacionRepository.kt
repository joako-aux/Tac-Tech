package cl.duoc.serviciotecnico.data.repository

import cl.duoc.serviciotecnico.data.remote.Resultado
import cl.duoc.serviciotecnico.data.remote.geocodificacion.NominatimApi
import cl.duoc.serviciotecnico.data.remote.geocodificacion.NominatimRespuesta
import cl.duoc.serviciotecnico.domain.model.DireccionGps
import retrofit2.HttpException
import java.io.IOException

/**
 * Convierte las coordenadas GPS de una visita en una direccion legible usando la API externa.
 *
 * Solo consulta: no escribe en Room ni en tactech-backend. El tecnico decide si usa la
 * direccion sugerida en su reporte.
 */
class GeocodificacionRepository(private val api: NominatimApi) {

    suspend fun direccionDe(latitud: Double, longitud: Double): Resultado<DireccionGps> = try {
        val direccion = api.direccionDe(latitud, longitud).aDireccionGps()
        if (direccion != null) Resultado.Exito(direccion) else Resultado.Error(SIN_RESULTADO)
    } catch (e: IOException) {
        Resultado.Error(SIN_INTERNET)
    } catch (e: HttpException) {
        Resultado.Error("El servicio de mapas respondio ${e.code()}")
    }

    companion object {
        const val SIN_RESULTADO = "No se encontro una direccion para esta ubicacion"
        const val SIN_INTERNET = "Sin internet: las coordenadas quedan registradas, pero no se pudo buscar la direccion"
    }
}

/** null si el servicio no encontro una direccion para el punto. */
fun NominatimRespuesta.aDireccionGps(): DireccionGps? {
    val nombre = nombreCompleto
    if (error != null || nombre.isNullOrBlank()) return null
    val d = direccion
    return DireccionGps(
        calle = d?.calle,
        numero = d?.numero,
        comuna = d?.ciudad ?: d?.pueblo ?: d?.villa ?: d?.municipio ?: d?.sector,
        region = d?.region,
        completa = nombre
    )
}
