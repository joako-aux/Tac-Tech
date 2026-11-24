package cl.duoc.serviciotecnico

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * La app completa: Application (inyeccion manual de dependencias), MainActivity, tema y
 * navegacion. Sin sesion guardada, la navegacion debe partir en el login.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = ServicioTecnicoApp::class)
class AplicacionTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun sinSesionGuardadaLaAppAbreEnElLogin() {
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("Iniciar sesion").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
