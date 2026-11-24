package cl.duoc.serviciotecnico.ui.screens

import cl.duoc.serviciotecnico.data.local.entity.FotoGuia
import cl.duoc.serviciotecnico.testutil.FECHA_BASE
import cl.duoc.serviciotecnico.testutil.PruebaDePantalla
import cl.duoc.serviciotecnico.testutil.TECNICO
import cl.duoc.serviciotecnico.testutil.equipo
import cl.duoc.serviciotecnico.testutil.guia
import cl.duoc.serviciotecnico.testutil.ticket
import cl.duoc.serviciotecnico.ui.viewmodel.DetalleGuiaViewModel
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

/** Reporte de visita emitido: datos, fotos, coordenadas y direccion de OpenStreetMap. */
@RunWith(RobolectricTestRunner::class)
class DetalleGuiaScreenTest : PruebaDePantalla() {

    private var volvio = false

    private fun abrir(id: String = "g-1") {
        sesion(TECNICO)
        val vm = DetalleGuiaViewModel(id, e.guiaRepository, e.ticketRepository, e.equipoRepository, e.geocodificacion)
        mostrar { DetalleGuiaScreen(guiaId = id, onVolver = { volvio = true }, vm = vm) }
    }

    @Test
    fun muestraElReporteConFotosYDireccionGps() {
        e.equipos.guardar(equipo())
        e.tickets.guardar(ticket())
        e.guias.guardar(guia())
        e.fotos.guardar(FotoGuia(id = "f-1", guiaId = "g-1", uri = "content://foto1", fechaCaptura = FECHA_BASE))
        abrir()

        esperarTexto("Reporte N° 1")
        existe("Datos de la visita")
        existe("Ana Rojas")
        existe("Capacitor 35uF")
        existe("1 fotografias")
        esperarTexto("Direccion segun GPS")

        tocarIcono("Volver")
        assertTrue(volvio)
    }

    @Test
    fun reporteSinCoordenadasNiFotos() {
        e.tickets.guardar(ticket())
        e.guias.guardar(guia(latitud = null, longitud = null).copy(observaciones = "Equipo operativo"))
        abrir()
        esperarTexto("La guia se emitio sin coordenadas")
        existe("Sin fotografias")
        existe("Equipo operativo")
    }

    @Test
    fun siFallaElServicioDeMapasIgualMuestraElReporte() {
        e.nominatim.falla = IOException("sin internet")
        e.guias.guardar(guia())
        abrir()
        esperarTexto("Reporte N° 1")
        existe("Lat", parcial = true)
    }

    @Test
    fun guiaInexistente() {
        abrir("no-existe")
        esperarTexto("No se encontro la guia")
    }
}
