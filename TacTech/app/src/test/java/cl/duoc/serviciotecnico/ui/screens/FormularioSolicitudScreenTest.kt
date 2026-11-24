package cl.duoc.serviciotecnico.ui.screens

import cl.duoc.serviciotecnico.testutil.CLIENTE
import cl.duoc.serviciotecnico.testutil.PruebaDePantalla
import cl.duoc.serviciotecnico.ui.viewmodel.FormularioSolicitudViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Formulario de nueva solicitud del cliente (funciona sin internet). */
@RunWith(RobolectricTestRunner::class)
class FormularioSolicitudScreenTest : PruebaDePantalla() {

    private var volvio = false

    private fun abrir() {
        sesion(CLIENTE)
        val vm = FormularioSolicitudViewModel(e.solicitudRepository, e.sesion)
        mostrar { FormularioSolicitudScreen(onVolver = { volvio = true }, vm = vm) }
        esperarTexto("¿Que esta pasando?")
    }

    @Test
    fun completaYEnviaLaSolicitud() {
        abrir()
        esperarTexto("Clinica Las Condes")

        escribir("Titulo", "Equipo no enfria")
        escribir("Descripcion del problema", "El split del segundo piso no baja de 26 grados")
        escribir("Direccion del equipo", "Av. Apoquindo 1234, Las Condes")
        escribir("Nombre del equipo", "Split muro")
        escribir("Modelo", "MSAFB-12")
        escribir("Numero de serie", "MID-12345")
        tocar("Propio del cliente")
        tocar("Arrendado")
        tocar("Enviar solicitud")

        esperar { volvio }
        assertEquals(1, e.solicitudes.todas.size)
    }

    @Test
    fun enviarVacioMarcaLosErrores() {
        abrir()
        tocar("Enviar solicitud")
        existeIcono("con error", parcial = true)
        assertEquals(0, e.solicitudes.todas.size)

        tocarIcono("Volver")
        assertTrue(volvio)
    }
}
