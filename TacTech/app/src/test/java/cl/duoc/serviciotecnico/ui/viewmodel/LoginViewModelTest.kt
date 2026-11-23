package cl.duoc.serviciotecnico.ui.viewmodel

import cl.duoc.serviciotecnico.domain.model.Rol
import cl.duoc.serviciotecnico.domain.validation.CampoLogin
import cl.duoc.serviciotecnico.testutil.ADMIN
import cl.duoc.serviciotecnico.testutil.EntornoDePrueba
import cl.duoc.serviciotecnico.testutil.MainDispatcherRule
import cl.duoc.serviciotecnico.testutil.usuarioDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Capa VIEWMODEL: formulario de login y navegacion segun el rol. */
class LoginViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun formularioVacioNoLlamaAlServidorYMuestraErrores() = runTest {
        val e = EntornoDePrueba(backgroundScope)
        val vm = LoginViewModel(e.sesion)
        assertFalse(vm.estado.value.formulario.esValido)
        assertNull(vm.estado.value.formulario.error(CampoLogin.CORREO))

        vm.ingresar()

        assertTrue(e.api.llamadas.isEmpty())
        assertNotNull(vm.estado.value.formulario.error(CampoLogin.CORREO))
        assertNotNull(vm.estado.value.formulario.error(CampoLogin.CLAVE))
    }

    @Test
    fun credencialesCorrectasEntreganElRol() = runTest {
        val e = EntornoDePrueba(backgroundScope)
        e.api.usuarioLogin = usuarioDto(nombre = ADMIN.nombre, correo = ADMIN.correo, rol = Rol.ADMINISTRADOR)
        val vm = LoginViewModel(e.sesion)

        vm.cambiarCorreo(" admin@tactech.cl ")
        vm.cambiarClave("admin123")
        vm.ingresar()

        assertEquals("admin@tactech.cl", vm.estado.value.correo)
        assertEquals(Rol.ADMINISTRADOR, vm.estado.value.rolIngresado)
        assertFalse(vm.estado.value.ingresando)
    }

    @Test
    fun credencialesIncorrectasMuestranMensajeQueSeLimpiaAlEscribir() = runTest {
        val e = EntornoDePrueba(backgroundScope)
        e.api.usuarioLogin = null // el servidor responde 401
        val vm = LoginViewModel(e.sesion)
        vm.cambiarCorreo("tecnico@tactech.cl")
        vm.cambiarClave("incorrecta")

        vm.ingresar()

        assertNull(vm.estado.value.rolIngresado)
        assertEquals("Correo o contraseña incorrectos", vm.estado.value.mensaje)

        vm.cambiarClave("tecnico123")
        assertNull(vm.estado.value.mensaje)
    }
}
