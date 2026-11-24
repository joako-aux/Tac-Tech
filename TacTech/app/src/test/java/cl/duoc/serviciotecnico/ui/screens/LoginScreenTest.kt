package cl.duoc.serviciotecnico.ui.screens

import cl.duoc.serviciotecnico.domain.model.Rol
import cl.duoc.serviciotecnico.testutil.PruebaDePantalla
import cl.duoc.serviciotecnico.testutil.usuarioDto
import cl.duoc.serviciotecnico.ui.viewmodel.LoginViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Pantalla de login: formulario, validaciones en vivo y navegacion segun el rol. */
@RunWith(RobolectricTestRunner::class)
class LoginScreenTest : PruebaDePantalla() {

    private var rolIngresado: Rol? = null

    private fun abrir() {
        val vm = LoginViewModel(e.sesion)
        mostrar { LoginScreen(onIngresado = { rolIngresado = it }, vm = vm) }
    }

    @Test
    fun muestraElFormularioYLasCuentasDeDemostracion() {
        abrir()
        existe("TacTech")
        existe("Iniciar sesion")
        existe("Administrador:", parcial = true)
    }

    @Test
    fun formularioVacioMarcaLosErroresSinIngresar() {
        abrir()
        tocar("Ingresar")
        existeIcono("con error", parcial = true)
        assertNull(rolIngresado)
    }

    @Test
    fun credencialesCorrectasLlevanAlPanelDelRol() {
        e.api.usuarioLogin = usuarioDto(nombre = "Administrador", correo = "admin@tactech.cl", rol = Rol.ADMINISTRADOR)
        abrir()

        escribir("Correo", "admin@tactech.cl")
        escribir("Contraseña", "admin123")
        tocarIcono("Mostrar contraseña")
        tocar("Ingresar")

        esperar { rolIngresado != null }
        assertEquals(Rol.ADMINISTRADOR, rolIngresado)
    }

    @Test
    fun credencialesIncorrectasMuestranUnAviso() {
        e.api.usuarioLogin = null
        abrir()

        escribir("Correo", "tecnico@tactech.cl")
        escribir("Contraseña", "incorrecta")
        tocar("Ingresar")

        esperarTexto("Correo o contraseña incorrectos")
        assertNull(rolIngresado)
    }
}
