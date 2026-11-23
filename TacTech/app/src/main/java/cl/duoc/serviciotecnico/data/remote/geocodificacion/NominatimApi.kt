package cl.duoc.serviciotecnico.data.remote.geocodificacion

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * API externa: Nominatim de OpenStreetMap (https://nominatim.openstreetmap.org).
 *
 * Geocodificacion inversa: recibe latitud y longitud y devuelve la direccion de ese punto.
 * Se usa para mostrar la direccion real donde el tecnico registro la visita.
 * Es gratuita y no usa API key; su politica exige identificar la app (User-Agent) y
 * no superar 1 peticion por segundo (la app solo consulta al registrar una ubicacion).
 *
 * Ejemplo: GET /reverse?lat=-33.4172&lon=-70.6044&format=jsonv2&accept-language=es&addressdetails=1
 */
interface NominatimApi {

    @GET("reverse")
    suspend fun direccionDe(
        @Query("lat") latitud: Double,
        @Query("lon") longitud: Double,
        @Query("format") formato: String = "jsonv2",
        @Query("accept-language") idioma: String = "es",
        @Query("addressdetails") conDetalle: Int = 1
    ): NominatimRespuesta
}

/** Respuesta JSON de /reverse. Si no hay direccion para el punto, solo viene [error]. */
data class NominatimRespuesta(
    @SerializedName("display_name") val nombreCompleto: String? = null,
    @SerializedName("address") val direccion: NominatimDireccion? = null,
    @SerializedName("error") val error: String? = null
)

/** Partes de la direccion. Segun la zona, la comuna llega como city, town, village o municipality. */
data class NominatimDireccion(
    @SerializedName("road") val calle: String? = null,
    @SerializedName("house_number") val numero: String? = null,
    @SerializedName("city") val ciudad: String? = null,
    @SerializedName("town") val pueblo: String? = null,
    @SerializedName("village") val villa: String? = null,
    @SerializedName("municipality") val municipio: String? = null,
    @SerializedName("suburb") val sector: String? = null,
    @SerializedName("state") val region: String? = null,
    @SerializedName("country") val pais: String? = null
)
