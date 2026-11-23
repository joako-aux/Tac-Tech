package cl.duoc.serviciotecnico.ui.viewmodel

import cl.duoc.serviciotecnico.domain.validation.CampoLogin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regla de presentacion: un error solo se muestra si el campo se toco o se intento enviar. */
class EstadoFormularioTest {

    private val conError = EstadoFormulario<CampoLogin>().conErrores(mapOf(CampoLogin.CORREO to "Correo invalido"))

    @Test
    fun formularioRecienAbiertoNoMuestraErrores() {
        assertFalse(conError.esValido)
        assertNull(conError.error(CampoLogin.CORREO))
        assertFalse(conError.correcto(CampoLogin.CLAVE))
    }

    @Test
    fun alTocarUnCampoSeVeSuError() {
        val tocado = conError.tocar(CampoLogin.CORREO)
        assertEquals("Correo invalido", tocado.error(CampoLogin.CORREO))
        assertNull(tocado.error(CampoLogin.CLAVE))
    }

    @Test
    fun alEnviarSeVenTodosLosErroresYLosCorrectos() {
        val enviado = conError.enviar()
        assertEquals("Correo invalido", enviado.error(CampoLogin.CORREO))
        assertTrue(enviado.correcto(CampoLogin.CLAVE))
        assertFalse(enviado.correcto(CampoLogin.CORREO))
    }

    @Test
    fun sinErroresEsValido() {
        assertTrue(EstadoFormulario<CampoLogin>().esValido)
    }
}
