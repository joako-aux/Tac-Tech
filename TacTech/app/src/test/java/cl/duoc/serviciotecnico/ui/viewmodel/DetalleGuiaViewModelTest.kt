package cl.duoc.serviciotecnico.ui.viewmodel

import cl.duoc.serviciotecnico.data.local.entity.FotoGuia
import cl.duoc.serviciotecnico.testutil.EntornoDePrueba
import cl.duoc.serviciotecnico.testutil.FECHA_BASE
import cl.duoc.serviciotecnico.testutil.MainDispatcherRule
import cl.duoc.serviciotecnico.testutil.equipo
import cl.duoc.serviciotecnico.testutil.errorHttp
import cl.duoc.serviciotecnico.testutil.guia
import cl.duoc.serviciotecnico.testutil.observar
import cl.duoc.serviciotecnico.testutil.ticket
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Capa VIEWMODEL: detalle del reporte, con la direccion GPS desde la API externa. */
class DetalleGuiaViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun muestraLaGuiaConTicketEquipoFotosYDireccionGps() = runTest {
        val e = EntornoDePrueba(backgroundScope)
        e.tickets.guardar(ticket())
        e.equipos.guardar(equipo())
        e.guias.guardar(guia())
        e.fotos.guardar(FotoGuia(id = "f-1", guiaId = "g-1", uri = "content://foto1", fechaCaptura = FECHA_BASE))

        val vm = DetalleGuiaViewModel("g-1", e.guiaRepository, e.ticketRepository, e.equipoRepository, e.geocodificacion)
        observar(vm.estado)

        val s = vm.estado.value
        assertFalse(s.cargando)
        assertEquals("g-1", s.guia?.id)
        assertEquals("t-1", s.ticket?.id)
        assertEquals("eq-1", s.equipo?.id)
        assertEquals(listOf("content://foto1"), s.fotos)
        assertEquals("Avenida Apoquindo 1234, Las Condes", s.gps.direccion?.corta)
        assertEquals(listOf(-33.4172 to -70.6044), e.nominatim.consultas)
    }

    @Test
    fun guiaSinCoordenadasNoConsultaLaApiExterna() = runTest {
        val e = EntornoDePrueba(backgroundScope)
        e.guias.guardar(guia(latitud = null, longitud = null))

        val vm = DetalleGuiaViewModel("g-1", e.guiaRepository, e.ticketRepository, e.equipoRepository, e.geocodificacion)
        observar(vm.estado)

        assertTrue(e.nominatim.consultas.isEmpty())
        assertNull(vm.estado.value.gps.direccion)
        assertNull(vm.estado.value.ticket)
    }

    @Test
    fun errorDeLaApiExternaSeMuestraSinRomperElDetalle() = runTest {
        val e = EntornoDePrueba(backgroundScope)
        e.guias.guardar(guia())
        e.nominatim.falla = errorHttp(503)

        val vm = DetalleGuiaViewModel("g-1", e.guiaRepository, e.ticketRepository, e.equipoRepository, e.geocodificacion)
        observar(vm.estado)

        assertEquals("g-1", vm.estado.value.guia?.id)
        assertEquals("El servicio de mapas respondio 503", vm.estado.value.gps.error)
    }

    @Test
    fun guiaInexistente() = runTest {
        val e = EntornoDePrueba(backgroundScope)

        val vm = DetalleGuiaViewModel("no-existe", e.guiaRepository, e.ticketRepository, e.equipoRepository, e.geocodificacion)
        observar(vm.estado)

        assertFalse(vm.estado.value.cargando)
        assertNull(vm.estado.value.guia)
        assertNull(vm.estado.value.equipo)
    }
}
