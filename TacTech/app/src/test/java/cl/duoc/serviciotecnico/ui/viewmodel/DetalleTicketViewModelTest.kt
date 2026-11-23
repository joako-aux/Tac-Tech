package cl.duoc.serviciotecnico.ui.viewmodel

import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.domain.validation.CampoCambioEstado
import cl.duoc.serviciotecnico.testutil.ADMIN
import cl.duoc.serviciotecnico.testutil.EntornoDePrueba
import cl.duoc.serviciotecnico.testutil.MainDispatcherRule
import cl.duoc.serviciotecnico.testutil.TECNICO
import cl.duoc.serviciotecnico.testutil.equipo
import cl.duoc.serviciotecnico.testutil.guia
import cl.duoc.serviciotecnico.testutil.observar
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

/** Capa VIEWMODEL: detalle del ticket y formulario de cambio de estado. */
class DetalleTicketViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private suspend fun TestScope.crear(
        estado: EstadoTicket = EstadoTicket.ABIERTO,
        conGuia: Boolean = false,
        comoAdmin: Boolean = false
    ): Pair<EntornoDePrueba, DetalleTicketViewModel> {
        val e = EntornoDePrueba(backgroundScope)
        e.iniciarComo(if (comoAdmin) ADMIN else TECNICO)
        e.tickets.guardar(ticket(estado = estado))
        e.equipos.guardar(equipo())
        if (conGuia) e.guias.guardar(guia())
        val vm = DetalleTicketViewModel("t-1", e.ticketRepository, e.guiaRepository, e.equipoRepository, e.sesion)
        observar(vm.estado)
        return e to vm
    }

    @Test
    fun cargaTicketEquipoYGuias() = runTest {
        val (_, vm) = crear(conGuia = true)
        val s = vm.estado.value

        assertFalse(s.cargando)
        assertEquals("t-1", s.ticket?.id)
        assertEquals("MID-12345", s.equipo?.serie)
        assertEquals(1, s.guias.size)
        assertTrue(s.editable)
        assertTrue(s.puedeEmitirReporte)
        assertFalse(EstadoTicket.CANCELADO in s.estadosDisponibles)
        assertEquals("Comentario del cambio", s.etiquetaComentario)
    }

    @Test
    fun elAdministradorPuedeCancelarPeroNoEmitirReporte() = runTest {
        val (_, vm) = crear(comoAdmin = true)
        val s = vm.estado.value

        assertTrue(s.esAdministrador)
        assertFalse(s.puedeEmitirReporte)
        assertTrue(EstadoTicket.CANCELADO in s.estadosDisponibles)

        vm.seleccionarEstado(EstadoTicket.CANCELADO)
        assertEquals("Motivo de la cancelacion", vm.estado.value.etiquetaComentario)
    }

    @Test
    fun cambioValidoActualizaElTicketYLimpiaElFormulario() = runTest {
        val (e, vm) = crear()

        vm.seleccionarEstado(EstadoTicket.COORDINADO)
        vm.cambiarComentario("Visita coordinada para el lunes")
        vm.enviarCambioDeEstado()

        assertEquals(EstadoTicket.COORDINADO, e.tickets.buscar("t-1")?.estado)
        assertEquals("Estado actualizado a Coordinado", vm.estado.value.mensaje)
        assertNull(vm.estado.value.cambio.seleccionado)
        assertEquals("", vm.estado.value.cambio.comentario)

        vm.mensajeMostrado()
        assertNull(vm.estado.value.mensaje)
    }

    @Test
    fun formularioIncompletoNoCambiaElEstado() = runTest {
        val (e, vm) = crear()

        vm.cambiarComentario("ok")
        assertNotNull(vm.estado.value.formulario.error(CampoCambioEstado.COMENTARIO))
        vm.enviarCambioDeEstado()

        assertEquals("Revisa el formulario de cambio de estado", vm.estado.value.mensaje)
        assertEquals(EstadoTicket.ABIERTO, e.tickets.buscar("t-1")?.estado)
        assertNotNull(vm.estado.value.formulario.error(CampoCambioEstado.ESTADO))
    }

    @Test
    fun noSeCierraSinGuiaYSiConGuia() = runTest {
        val (e, vm) = crear()
        vm.seleccionarEstado(EstadoTicket.CERRADO)
        vm.cambiarComentario("Se cambio el capacitor")
        assertEquals("Solucion aplicada", vm.estado.value.etiquetaComentario)
        vm.enviarCambioDeEstado()
        assertEquals(EstadoTicket.ABIERTO, e.tickets.buscar("t-1")?.estado)

        // Al emitirse una guia, la regla reacciona sola y el cierre se permite
        e.guias.guardar(guia())
        vm.enviarCambioDeEstado()
        assertEquals(EstadoTicket.CERRADO, e.tickets.buscar("t-1")?.estado)
        assertEquals("Se cambio el capacitor", e.tickets.buscar("t-1")?.solucion)
    }

    @Test
    fun unTicketCerradoEsSoloLectura() = runTest {
        val (e, vm) = crear(estado = EstadoTicket.CERRADO)
        assertFalse(vm.estado.value.editable)

        vm.seleccionarEstado(EstadoTicket.TRANSITO)
        vm.cambiarComentario("Intento reabrir")
        vm.enviarCambioDeEstado()

        assertEquals(EstadoTicket.CERRADO, e.tickets.buscar("t-1")?.estado)
        assertNull(vm.estado.value.mensaje)
    }
}
