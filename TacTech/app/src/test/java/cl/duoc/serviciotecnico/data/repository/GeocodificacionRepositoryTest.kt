package cl.duoc.serviciotecnico.data.repository

import cl.duoc.serviciotecnico.data.remote.Resultado
import cl.duoc.serviciotecnico.data.remote.geocodificacion.NominatimDireccion
import cl.duoc.serviciotecnico.data.remote.geocodificacion.NominatimRespuesta
import cl.duoc.serviciotecnico.testutil.NominatimFalsa
import cl.duoc.serviciotecnico.testutil.errorHttp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Capa REPOSITORY de la API EXTERNA (OpenStreetMap / Nominatim): coordenadas -> direccion.
 */
class GeocodificacionRepositoryTest {

    private val api = NominatimFalsa()
    private val repo = GeocodificacionRepository(api)

    @Test
    fun convierteLasCoordenadasEnDireccion() = runTest {
        val r = repo.direccionDe(-33.4172, -70.6044)

        assertTrue(r is Resultado.Exito)
        val d = (r as Resultado.Exito).dato
        assertEquals("Avenida Apoquindo 1234, Las Condes", d.corta)
        assertEquals("Region Metropolitana de Santiago", d.region)
        assertEquals(listOf(-33.4172 to -70.6044), api.consultas)
    }

    @Test
    fun laComunaPuedeVenirComoPuebloVillaMunicipioOSector() = runTest {
        fun respuesta(d: NominatimDireccion) = NominatimRespuesta(nombreCompleto = "texto", direccion = d)

        api.respuesta = respuesta(NominatimDireccion(calle = "Calle 1", pueblo = "Pirque"))
        assertEquals("Calle 1, Pirque", (repo.direccionDe(0.0, 0.0) as Resultado.Exito).dato.corta)
        api.respuesta = respuesta(NominatimDireccion(villa = "Cajon del Maipo"))
        assertEquals("Cajon del Maipo", (repo.direccionDe(0.0, 0.0) as Resultado.Exito).dato.corta)
        api.respuesta = respuesta(NominatimDireccion(municipio = "Colina"))
        assertEquals("Colina", (repo.direccionDe(0.0, 0.0) as Resultado.Exito).dato.corta)
        api.respuesta = respuesta(NominatimDireccion(sector = "Lo Barnechea"))
        assertEquals("Lo Barnechea", (repo.direccionDe(0.0, 0.0) as Resultado.Exito).dato.corta)
        api.respuesta = NominatimRespuesta(nombreCompleto = "Solo texto")
        assertEquals("Solo texto", (repo.direccionDe(0.0, 0.0) as Resultado.Exito).dato.corta)
    }

    @Test
    fun puntoSinDireccion() = runTest {
        api.respuesta = NominatimRespuesta(error = "Unable to geocode")
        assertEquals(Resultado.Error(GeocodificacionRepository.SIN_RESULTADO), repo.direccionDe(-60.0, -80.0))

        api.respuesta = NominatimRespuesta(nombreCompleto = " ")
        assertEquals(Resultado.Error(GeocodificacionRepository.SIN_RESULTADO), repo.direccionDe(-60.0, -80.0))
    }

    @Test
    fun sinInternet() = runTest {
        api.falla = IOException("sin red")
        assertEquals(Resultado.Error(GeocodificacionRepository.SIN_INTERNET), repo.direccionDe(-33.4, -70.6))
    }

    @Test
    fun servicioDeMapasConError() = runTest {
        api.falla = errorHttp(429)
        assertEquals(Resultado.Error("El servicio de mapas respondio 429"), repo.direccionDe(-33.4, -70.6))
    }
}
