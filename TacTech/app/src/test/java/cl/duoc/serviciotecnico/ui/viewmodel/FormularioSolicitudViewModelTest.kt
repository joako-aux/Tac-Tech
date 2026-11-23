package cl.duoc.serviciotecnico.ui.viewmodel

import cl.duoc.serviciotecnico.domain.model.EstadoSolicitud
import cl.duoc.serviciotecnico.domain.model.Propiedad
import cl.duoc.serviciotecnico.domain.validation.CampoSolicitud
import cl.duoc.serviciotecnico.testutil.CLIENTE
import cl.duoc.serviciotecnico.testutil.EntornoDePrueba
import cl.duoc.serviciotecnico.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Capa VIEWMODEL: el cliente crea una solicitud (funciona sin internet). */
class FormularioSolicitudViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private fun FormularioSolicitudViewModel.llenar() {
        cambiarCampo(CampoSolicitud.TITULO, " Equipo no enfria ")
        cambiarCampo(CampoSolicitud.DESCRIPCION, "El split del segundo piso no baja de 26 grados")
        cambiarCampo(CampoSolicitud.DIRECCION, "Av. Apoquindo 1234, Las Condes")
        cambiarCampo(CampoSolicitud.NOMBRE_EQUIPO, "Split muro")
        cambiarCampo(CampoSolicitud.MODELO, "MSAFB-12")
        cambiarCampo(CampoSolicitud.SERIE, "mid-12345")
    }

    @Test
    fun muestraLaEmpresaDelClienteYElProgreso() = runTest {
        val e = EntornoDePrueba(backgroundScope)
        e.iniciarComo(CLIENTE)
        val vm = FormularioSolicitudViewModel(e.solicitudRepository, e.sesion)
        assertEquals("Clinica Las Condes", vm.estado.value.empresa)
        assertEquals(0f, vm.estado.value.progreso, 0f)

        vm.llenar()

        val s = vm.estado.value
        assertEquals("MID-12345", s.serie)
        assertEquals(1f, s.progreso, 0f)
        assertEquals(CampoSolicitud.entries.size, s.completos)
        assertTrue(s.valido(CampoSolicitud.SERIE))
        assertNull(s.error(CampoSolicitud.TITULO))
    }

    @Test
    fun formularioIncompletoNoSeEnvia() = runTest {
        val e = EntornoDePrueba(backgroundScope)
        e.iniciarComo(CLIENTE)
        val vm = FormularioSolicitudViewModel(e.solicitudRepository, e.sesion)
        vm.cambiarCampo(CampoSolicitud.TITULO, "Hola")

        vm.enviar()

        assertEquals("Revisa los campos marcados en rojo", vm.estado.value.mensaje)
        assertNotNull(vm.estado.value.error(CampoSolicitud.SERIE))
        assertTrue(e.solicitudes.todas.isEmpty())

        vm.mensajeMostrado()
        assertNull(vm.estado.value.mensaje)
    }

    @Test
    fun enviarGuardaLaSolicitudLocalmente() = runTest {
        val e = EntornoDePrueba(backgroundScope)
        e.iniciarComo(CLIENTE)
        val vm = FormularioSolicitudViewModel(e.solicitudRepository, e.sesion)
        vm.llenar()
        vm.cambiarPropiedad(Propiedad.ARRENDADO)

        vm.enviar()

        assertTrue(vm.estado.value.guardado)
        val s = e.solicitudes.todas.single()
        assertEquals("Equipo no enfria", s.titulo)
        assertEquals(Propiedad.ARRENDADO, s.propiedad)
        assertEquals(EstadoSolicitud.POR_CONFIRMAR, s.estado)
        assertEquals(CLIENTE.correo, s.clienteCorreo)
        assertTrue("No necesita internet", e.api.llamadas.isEmpty())
    }

    @Test
    fun sinSesionNoSeGuarda() = runTest {
        val e = EntornoDePrueba(backgroundScope)
        val vm = FormularioSolicitudViewModel(e.solicitudRepository, e.sesion)
        vm.llenar()

        vm.enviar()

        assertEquals("Tu sesion expiro, vuelve a ingresar", vm.estado.value.mensaje)
        assertFalse(vm.estado.value.guardado)
        assertFalse(vm.estado.value.guardando)
    }
}
