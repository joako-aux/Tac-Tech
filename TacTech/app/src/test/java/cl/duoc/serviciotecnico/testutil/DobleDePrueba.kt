package cl.duoc.serviciotecnico.testutil

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import cl.duoc.serviciotecnico.data.remote.EstadoRed
import cl.duoc.serviciotecnico.data.remote.geocodificacion.NominatimApi
import cl.duoc.serviciotecnico.data.remote.geocodificacion.NominatimDireccion
import cl.duoc.serviciotecnico.data.remote.geocodificacion.NominatimRespuesta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** DataStore en memoria: reemplaza al archivo de preferencias de Android. */
class DataStoreEnMemoria : DataStore<Preferences> {
    private val estado = MutableStateFlow(emptyPreferences())
    override val data: Flow<Preferences> = estado

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val nuevo = transform(estado.value)
        estado.value = nuevo
        return nuevo
    }
}

/** Red controlada por el test: enLinea.value = false simula modo avion. */
class RedFalsa(conectado: Boolean = true) : EstadoRed {
    override val enLinea = MutableStateFlow(conectado)
}

/** API externa (OpenStreetMap) falsa: devuelve [respuesta] o lanza [falla]. */
class NominatimFalsa : NominatimApi {
    var respuesta: NominatimRespuesta = RESPUESTA_APOQUINDO
    var falla: Exception? = null
    val consultas = mutableListOf<Pair<Double, Double>>()

    override suspend fun direccionDe(
        latitud: Double,
        longitud: Double,
        formato: String,
        idioma: String,
        conDetalle: Int
    ): NominatimRespuesta {
        consultas += latitud to longitud
        falla?.let { throw it }
        return respuesta
    }

    companion object {
        val RESPUESTA_APOQUINDO = NominatimRespuesta(
            nombreCompleto = "1234, Avenida Apoquindo, Las Condes, Region Metropolitana de Santiago, Chile",
            direccion = NominatimDireccion(
                calle = "Avenida Apoquindo",
                numero = "1234",
                ciudad = "Las Condes",
                region = "Region Metropolitana de Santiago",
                pais = "Chile"
            )
        )
    }
}
