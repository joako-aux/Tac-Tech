package cl.duoc.serviciotecnico.data.remote

import cl.duoc.serviciotecnico.testutil.errorHttp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/**
 * Capa de DATOS (red): `llamar` traduce las respuestas del microservicio a [Resultado]
 * y los errores HTTP al mensaje que manda tactech-backend en su ErrorDto.
 */
class LlamadaRemotaTest {

    @Test
    fun respuestaCorrectaEsExito() = runTest {
        assertEquals(Resultado.Exito(42), llamar { 42 })
    }

    @Test
    fun sinRedEsErrorDeConexion() = runTest {
        val r = llamar<Int> { throw IOException("timeout") }
        assertEquals(Resultado.Error("Sin conexion con el servidor. Esta accion necesita internet."), r)
    }

    @Test
    fun errorDeValidacionMuestraElPrimerCampo() = runTest {
        val json = """{"estado":400,"mensaje":"Datos invalidos","errores":{"rut":"El RUT no es valido"}}"""
        val r = llamar<Int> { throw errorHttp(400, json) }
        assertEquals(Resultado.Error("El RUT no es valido"), r)
    }

    @Test
    fun errorDeNegocioMuestraElMensaje() = runTest {
        val r = llamar<Int> { throw errorHttp(409, """{"estado":409,"mensaje":"El correo ya existe"}""") }
        assertEquals(Resultado.Error("El correo ya existe"), r)
    }

    @Test
    fun cuerpoQueNoEsJsonMuestraElCodigo() {
        assertEquals("El servidor respondio 500", mensajeDe(errorHttp(500, "<html>Error</html>")))
        assertEquals("El servidor respondio 404", mensajeDe(errorHttp(404, "{}")))
    }
}
