package cl.duoc.serviciotecnico.ui.viewmodel

import cl.duoc.serviciotecnico.data.local.dao.GuiaDao
import cl.duoc.serviciotecnico.data.local.entity.Guia
import cl.duoc.serviciotecnico.data.repository.GeocodificacionRepository
import cl.duoc.serviciotecnico.data.repository.GuiaRepository
import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.domain.validation.CampoGuia
import cl.duoc.serviciotecnico.domain.validation.ValidadorGuia
import cl.duoc.serviciotecnico.testutil.EntornoDePrueba
import cl.duoc.serviciotecnico.testutil.GuiaDaoEnMemoria
import cl.duoc.serviciotecnico.testutil.MainDispatcherRule
import cl.duoc.serviciotecnico.testutil.ticket
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * Capa VIEWMODEL: reporte de visita del tecnico.
 * Incluye la integracion con la API EXTERNA: al registrar la ubicacion GPS se consulta la
 * direccion en OpenStreetMap y el tecnico puede usarla en el reporte.
 */
class FormularioGuiaViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private fun TestScope.crear(
        estado: EstadoTicket = EstadoTicket.TRABAJO_EN_PROCESO,
        guiaRepository: GuiaRepository? = null
    ): Pair<EntornoDePrueba, FormularioGuiaViewModel> {
        val e = EntornoDePrueba(backgroundScope)
        e.tickets.guardar(ticket(estado = estado))
        val vm = FormularioGuiaViewModel("t-1", e.ticketRepository, guiaRepository ?: e.guiaRepository, e.geocodificacion)
        return e to vm
    }

    private fun FormularioGuiaViewModel.llenarTextos() {
        cambiarCampo(CampoGuia.TECNICO, "Carlos Lobos")
        cambiarCampo(CampoGuia.RECIBIDO_POR, "Ana Rojas")
        cambiarCampo(CampoGuia.TRABAJO, "Cambio de capacitor y limpieza de filtros")
        cambiarCampo(CampoGuia.REPUESTOS, "Capacitor 35uF")
        cambiarCampo(CampoGuia.OBSERVACIONES, "Equipo operativo")
    }

    @Test
    fun laDireccionSeCopiaDelTicket() = runTest {
        val (_, vm) = crear()
        assertEquals("Av. Apoquindo 1234, Las Condes", vm.estado.value.direccion)
        assertEquals("t-1", vm.estado.value.ticket?.id)
        assertFalse(vm.estado.value.formulario.esValido)
    }

    @Test
    fun cambiarCamposMarcaLosTocados() = runTest {
        val (_, vm) = crear()
        vm.llenarTextos()
        vm.cambiarCampo(CampoGuia.DIRECCION, "Av. Kennedy 5000")
        vm.cambiarCampo(CampoGuia.FOTOS, "ignorado")

        val s = vm.estado.value
        assertEquals("Av. Kennedy 5000", s.direccion)
        assertTrue(s.valido(CampoGuia.TECNICO))
        assertTrue(s.valido(CampoGuia.REPUESTOS))
        assertTrue(s.valido(CampoGuia.OBSERVACIONES))
        assertFalse(s.valido(CampoGuia.FOTOS))
        assertNull(s.error(CampoGuia.UBICACION))
    }

    @Test
    fun alRegistrarLaUbicacionSeBuscaLaDireccionEnLaApiExterna() = runTest {
        val (e, vm) = crear()

        vm.registrarUbicacion(-33.4172, -70.6044)

        val s = vm.estado.value
        assertTrue(s.tieneUbicacion)
        assertTrue(s.valido(CampoGuia.UBICACION))
        assertEquals("Ubicacion registrada", s.mensaje)
        assertEquals(listOf(-33.4172 to -70.6044), e.nominatim.consultas)
        assertEquals("Avenida Apoquindo 1234, Las Condes", s.gps.direccion?.corta)
        assertFalse(s.gps.buscando)
        assertTrue(s.puedeUsarDireccionGps)
    }

    @Test
    fun usarLaDireccionDelGpsEnElReporte() = runTest {
        val (_, vm) = crear()
        vm.registrarUbicacion(-33.4172, -70.6044)

        vm.usarDireccionGps()

        val s = vm.estado.value
        assertEquals("Avenida Apoquindo 1234, Las Condes", s.direccion)
        assertEquals("Direccion actualizada con la ubicacion GPS", s.mensaje)
        assertFalse("Ya no se ofrece porque es la misma", s.puedeUsarDireccionGps)
        assertTrue(s.valido(CampoGuia.DIRECCION))
    }

    @Test
    fun sinInternetLaUbicacionQuedaYSePuedeReintentar() = runTest {
        val (e, vm) = crear()
        e.nominatim.falla = IOException("sin red")

        vm.registrarUbicacion(-33.4172, -70.6044)

        assertTrue(vm.estado.value.tieneUbicacion)
        assertEquals(GeocodificacionRepository.SIN_INTERNET, vm.estado.value.gps.error)
        assertFalse(vm.estado.value.puedeUsarDireccionGps)

        e.nominatim.falla = null
        vm.reintentarDireccion()

        assertNull(vm.estado.value.gps.error)
        assertNotNull(vm.estado.value.gps.direccion)
        assertEquals(2, e.nominatim.consultas.size)
    }

    @Test
    fun sinUbicacionNoHayDireccionQueBuscarNiUsar() = runTest {
        val (e, vm) = crear()

        vm.reintentarDireccion()
        vm.usarDireccionGps()

        assertTrue(e.nominatim.consultas.isEmpty())
        assertEquals("Av. Apoquindo 1234, Las Condes", vm.estado.value.direccion)
    }

    @Test
    fun fotosSeAgreganYQuitanHastaElMaximo() = runTest {
        val (_, vm) = crear()
        repeat(ValidadorGuia.MAX_FOTOS) { vm.agregarFoto("content://foto$it") }
        assertFalse(vm.estado.value.puedeAgregarFotos)

        vm.agregarFoto("content://sobra")
        assertEquals(ValidadorGuia.MAX_FOTOS, vm.estado.value.fotos.size)
        assertEquals("Maximo 15 fotografias por guia", vm.estado.value.mensaje)

        vm.quitarFoto("content://foto0")
        assertEquals(ValidadorGuia.MAX_FOTOS - 1, vm.estado.value.fotos.size)
        assertTrue(vm.estado.value.valido(CampoGuia.FOTOS))

        vm.mensajeMostrado()
        assertNull(vm.estado.value.mensaje)
    }

    @Test
    fun guardarAvisaLaEvidenciaQueFalta() = runTest {
        val (_, vm) = crear()
        vm.llenarTextos()

        vm.guardar()
        assertEquals("Falta la foto de evidencia y la ubicacion", vm.estado.value.mensaje)

        vm.agregarFoto("content://foto1")
        vm.guardar()
        assertEquals("Falta registrar la ubicacion de la visita", vm.estado.value.mensaje)

        vm.quitarFoto("content://foto1")
        vm.registrarUbicacion(-33.4, -70.6)
        vm.guardar()
        assertEquals("Falta la fotografia de evidencia", vm.estado.value.mensaje)

        vm.agregarFoto("content://foto1")
        vm.cambiarCampo(CampoGuia.TRABAJO, "corto")
        vm.guardar()
        assertEquals("Revisa los campos marcados en rojo", vm.estado.value.mensaje)
        assertFalse(vm.estado.value.guardado)
    }

    @Test
    fun guardarReporteCompletoConFotoYUbicacion() = runTest {
        val (e, vm) = crear()
        vm.llenarTextos()
        vm.agregarFoto("content://foto1")
        vm.registrarUbicacion(-33.4172, -70.6044)

        vm.guardar()

        assertTrue(vm.estado.value.guardado)
        val guia = e.guias.todas.single()
        assertEquals("t-1", guia.ticketId)
        assertEquals("Carlos Lobos", guia.tecnico)
        assertEquals(-70.6044, guia.longitud!!, 0.0)
        assertTrue(guia.pendienteSync)
        assertEquals(listOf("content://foto1"), e.fotos.todas.map { it.uri })
    }

    @Test
    fun noSeEmiteGuiaParaUnTicketCerrado() = runTest {
        val (e, vm) = crear(estado = EstadoTicket.CERRADO)
        vm.llenarTextos()
        vm.agregarFoto("content://foto1")
        vm.registrarUbicacion(-33.4, -70.6)

        vm.guardar()

        assertEquals("No se puede emitir una guia para este ticket", vm.estado.value.mensaje)
        assertTrue(e.guias.todas.isEmpty())
    }

    @Test
    fun siFallaElGuardadoSeAvisa() = runTest {
        val falla = object : GuiaDao by GuiaDaoEnMemoria() {
            override suspend fun insertar(guia: Guia) = throw IllegalStateException("disco lleno")
        }
        val e0 = EntornoDePrueba(backgroundScope)
        val (_, vm) = crear(guiaRepository = GuiaRepository(falla, e0.fotos))
        vm.llenarTextos()
        vm.agregarFoto("content://foto1")
        vm.registrarUbicacion(-33.4, -70.6)

        vm.guardar()

        assertEquals("No se pudo guardar la guia", vm.estado.value.mensaje)
        assertFalse(vm.estado.value.guardando)
        assertFalse(vm.estado.value.guardado)
    }
}
