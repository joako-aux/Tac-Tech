package cl.duoc.serviciotecnico.ui.screens

import cl.duoc.serviciotecnico.testutil.PruebaDePantalla
import cl.duoc.serviciotecnico.testutil.TECNICO
import cl.duoc.serviciotecnico.testutil.equipo
import cl.duoc.serviciotecnico.testutil.ticket
import cl.duoc.serviciotecnico.ui.viewmodel.FormularioGuiaViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

/** Reporte de visita del tecnico: campos, foto obligatoria, GPS y direccion de OpenStreetMap. */
@RunWith(RobolectricTestRunner::class)
class FormularioGuiaScreenTest : PruebaDePantalla() {

    private var volvio = false
    private lateinit var vm: FormularioGuiaViewModel

    private fun abrir(id: String = "t-1", antes: (FormularioGuiaViewModel) -> Unit = {}) {
        sesion(TECNICO)
        vm = FormularioGuiaViewModel(id, e.ticketRepository, e.guiaRepository, e.geocodificacion)
        antes(vm)
        mostrar { FormularioGuiaScreen(ticketId = id, onVolver = { volvio = true }, vm = vm) }
    }

    private fun conTicket() {
        e.equipos.guardar(equipo())
        e.tickets.guardar(ticket())
    }

    @Test
    fun completaElReporteConFotoYUbicacionYLoEnvia() {
        conTicket()
        abrir {
            it.registrarUbicacion(-33.4172, -70.6044)
            it.agregarFoto("content://foto1")
        }
        esperarTexto("Datos de la visita")
        esperarTexto("Direccion segun GPS")
        tocar("Usar esta direccion en el reporte")
        existe("Actualizar ubicacion")

        escribir("Nombre del tecnico", "Carlos Lobos")
        escribir("Recibido por", "Ana Rojas")
        escribir("Detalle del trabajo", "Cambio de capacitor y limpieza de filtros")
        escribir("Repuestos utilizados", "Capacitor 35uF")
        escribir("Observaciones", "Equipo operativo")
        tocar("Enviar reporte")

        esperar { volvio }
        assertEquals(1, e.guias.todas.size)
    }

    @Test
    fun enviarIncompletoMarcaLosErrores() {
        conTicket()
        abrir()
        esperarTexto("Datos de la visita")
        tocar("Enviar reporte")
        existeIcono("con error", parcial = true)
        assertEquals(0, e.guias.todas.size)
    }

    @Test
    fun quitarUnaFotoDeLaGaleria() {
        conTicket()
        abrir { it.agregarFoto("content://foto1") }
        esperarTexto("1 de 15 fotografias", parcial = true)
        tocarIcono("Quitar fotografia")
        esperarTexto("0 de 15 fotografias", parcial = true)
    }

    @Test
    fun siElServicioDeMapasFallaSePuedeReintentar() {
        conTicket()
        e.nominatim.falla = IOException("sin internet")
        abrir { it.registrarUbicacion(-33.4172, -70.6044) }
        esperarTexto("Reintentar")

        e.nominatim.falla = null
        tocar("Reintentar")
        esperarTexto("Direccion segun GPS")
        assertTrue(e.nominatim.consultas.size >= 2)
    }

    @Test
    fun sinTicketSeMuestraCargando() {
        abrir(id = "no-existe")
        tocarIcono("Volver")
        assertTrue(volvio)
    }
}
