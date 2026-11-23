package cl.duoc.serviciotecnico.ui.viewmodel

import cl.duoc.serviciotecnico.data.remote.dto.aDto
import cl.duoc.serviciotecnico.domain.model.EstadoSolicitud
import cl.duoc.serviciotecnico.domain.model.Prioridad
import cl.duoc.serviciotecnico.domain.model.Rol
import cl.duoc.serviciotecnico.testutil.ADMIN
import cl.duoc.serviciotecnico.testutil.EntornoDePrueba
import cl.duoc.serviciotecnico.testutil.MainDispatcherRule
import cl.duoc.serviciotecnico.testutil.observar
import cl.duoc.serviciotecnico.testutil.solicitud
import cl.duoc.serviciotecnico.testutil.ticket
import cl.duoc.serviciotecnico.testutil.usuarioDto
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/** Capa VIEWMODEL: el administrador convierte una solicitud en ticket o la rechaza. */
class ConfirmarSolicitudViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val activo = usuarioDto(id = 1, nombre = "Tecnico Demo", correo = "tecnico@tactech.cl")
    private val inactivo = usuarioDto(id = 2, nombre = "Tecnico Baja", correo = "baja@tactech.cl", activo = false)

    private suspend fun TestScope.crear(): Pair<EntornoDePrueba, ConfirmarSolicitudViewModel> {
        val e = EntornoDePrueba(backgroundScope)
        e.iniciarComo(ADMIN)
        e.solicitudes.guardar(solicitud())
        e.api.solicitudes += solicitud().aDto()
        e.api.usuarios += listOf(activo, inactivo, usuarioDto(id = 3, correo = "c@t.cl", rol = Rol.CLIENTE))
        val vm = ConfirmarSolicitudViewModel("s-1", e.solicitudRepository, e.usuarioRepository, e.sincronizacion)
        observar(vm.estado)
        return e to vm
    }

    @Test
    fun cargaLaSolicitudYSoloTecnicosActivos() = runTest {
        val (_, vm) = crear()
        val s = vm.estado.value
        assertEquals("s-1", s.solicitud?.id)
        assertEquals(listOf(activo), s.asignacion.tecnicos)
        assertFalse(s.asignacion.cargandoTecnicos)
        assertEquals(Prioridad.MEDIA, s.asignacion.prioridad)
    }

    @Test
    fun sinInternetNoCargaTecnicos() = runTest {
        val e = EntornoDePrueba(backgroundScope)
        e.api.fallaRed = IOException("sin red")
        val vm = ConfirmarSolicitudViewModel("s-1", e.solicitudRepository, e.usuarioRepository, e.sincronizacion)
        observar(vm.estado)

        assertEquals("Sin conexion con el servidor. Esta accion necesita internet.", vm.estado.value.asignacion.errorTecnicos)
        assertFalse(vm.estado.value.asignacion.cargandoTecnicos)

        e.api.fallaRed = null
        e.api.usuarios += activo
        vm.cargarTecnicos()
        assertEquals(listOf(activo), vm.estado.value.asignacion.tecnicos)
    }

    @Test
    fun confirmarSinTecnicoMuestraError() = runTest {
        val (e, vm) = crear()
        assertNull(vm.estado.value.asignacion.errorTecnico)

        vm.confirmar()

        assertEquals("Debes asignar un tecnico al ticket", vm.estado.value.asignacion.errorTecnico)
        assertTrue(e.api.llamadas.none { it.startsWith("confirmarSolicitud") })
    }

    @Test
    fun confirmarCreaElTicketAsignado() = runTest {
        val (e, vm) = crear()
        e.api.ticketConfirmado = ticket(id = "t-55", numero = 55, solicitudId = "s-1").aDto()

        vm.elegirTecnico(activo)
        vm.elegirPrioridad(Prioridad.ALTA)
        vm.confirmar()

        val a = vm.estado.value.asignacion
        assertTrue(a.terminado)
        assertFalse(a.procesando)
        assertEquals("Ticket N° 55 creado y asignado a Tecnico Demo", a.mensaje)
        assertTrue("confirmarSolicitud(s-1,tecnico@tactech.cl,ALTA)" in e.api.llamadas)
        assertEquals(EstadoSolicitud.CONFIRMADA, e.solicitudes.buscar("s-1")?.estado)

        vm.mensajeMostrado()
        assertNull(vm.estado.value.asignacion.mensaje)
    }

    @Test
    fun confirmarConErrorDelServidor() = runTest {
        val (_, vm) = crear()
        vm.elegirTecnico(activo)

        vm.confirmar() // ticketConfirmado = null: el servidor responde 404

        assertEquals("Tecnico no encontrado", vm.estado.value.asignacion.mensaje)
        assertFalse(vm.estado.value.asignacion.terminado)
    }

    @Test
    fun rechazarConMotivo() = runTest {
        val (e, vm) = crear()

        vm.abrirRechazo()
        assertEquals("", vm.estado.value.asignacion.motivoRechazo)
        vm.cambiarMotivo("no")
        vm.rechazar()
        assertEquals("El motivo debe tener al menos 5 caracteres", vm.estado.value.asignacion.errorMotivo)

        vm.cambiarMotivo("Equipo fuera de cobertura")
        vm.rechazar()

        val a = vm.estado.value.asignacion
        assertTrue(a.terminado)
        assertEquals("Solicitud rechazada", a.mensaje)
        assertNull(a.motivoRechazo)
        assertEquals(EstadoSolicitud.RECHAZADA, e.solicitudes.buscar("s-1")?.estado)
    }

    @Test
    fun rechazarSinInternetMuestraError() = runTest {
        val (e, vm) = crear()
        vm.abrirRechazo()
        vm.cambiarMotivo("Equipo fuera de cobertura")
        e.api.fallaRed = IOException("sin red")

        vm.rechazar()

        assertEquals("Sin conexion con el servidor. Esta accion necesita internet.", vm.estado.value.asignacion.mensaje)
        assertFalse(vm.estado.value.asignacion.procesando)
    }

    @Test
    fun rechazarSinDialogoNoHaceNadaYCerrarLoDescarta() = runTest {
        val (e, vm) = crear()
        vm.rechazar()
        assertTrue(e.api.llamadas.none { it.startsWith("rechazar") })

        vm.abrirRechazo()
        vm.cerrarRechazo()
        assertNull(vm.estado.value.asignacion.motivoRechazo)
        assertNull(vm.estado.value.asignacion.errorMotivo)
    }
}
