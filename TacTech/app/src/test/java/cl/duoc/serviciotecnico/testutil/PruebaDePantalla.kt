package cl.duoc.serviciotecnico.testutil

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cl.duoc.serviciotecnico.data.local.AppDatabase
import cl.duoc.serviciotecnico.domain.model.Usuario
import cl.duoc.serviciotecnico.ui.theme.ServicioTecnicoTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule

/**
 * Base de las pruebas de pantallas y componentes Compose (Robolectric, sin emulador).
 *
 * Cada pantalla recibe su ViewModel REAL armado sobre [EntornoDePrueba] (DAO en memoria,
 * API falsa, red falsa): se prueba la pantalla + ViewModel + Repository juntos.
 * Los metodos auxiliares buscan los elementos por el texto que ve el usuario.
 */
abstract class PruebaDePantalla {

    @get:Rule
    val compose = createComposeRule()

    protected val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    protected val e = EntornoDePrueba(alcance)

    @After
    fun cerrarAlcance() {
        alcance.cancel()
    }

    /** Dibuja el contenido dentro del tema de la app. */
    protected fun mostrar(contenido: @Composable () -> Unit) {
        compose.setContent { ServicioTecnicoTheme(enOscuro = false) { contenido() } }
        compose.waitForIdle()
    }

    /** Deja una sesion iniciada con el rol indicado. */
    protected fun sesion(usuario: Usuario) = runBlocking { e.iniciarComo(usuario) }

    protected fun existe(texto: String, parcial: Boolean = false) {
        compose.onAllNodesWithText(texto, substring = parcial).onFirst().assertExists()
    }

    protected fun noExiste(texto: String, parcial: Boolean = false) {
        compose.waitForIdle()
        check(compose.onAllNodesWithText(texto, substring = parcial).fetchSemanticsNodes().isEmpty()) {
            "No deberia mostrarse: $texto"
        }
    }

    /** Espera (hasta 10 s) a que aparezca un texto: los datos llegan de forma asincrona. */
    protected fun esperarTexto(texto: String, parcial: Boolean = false) {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText(texto, substring = parcial).fetchSemanticsNodes().isNotEmpty()
        }
    }

    protected fun esperar(condicion: () -> Boolean) {
        compose.waitUntil(10_000, condicion)
    }

    protected fun tocar(texto: String, parcial: Boolean = false) {
        clic(compose.onAllNodesWithText(texto, substring = parcial).onFirst())
    }

    /**
     * Ejecuta la accion de clic del elemento. No depende de su posicion en la pantalla simulada
     * (los botones flotantes quedan al fondo y un toque por coordenadas podria caer afuera).
     */
    private fun clic(nodo: SemanticsNodeInteraction) {
        if (SemanticsActions.OnClick in nodo.fetchSemanticsNode().config) {
            nodo.performSemanticsAction(SemanticsActions.OnClick)
        } else {
            nodo.performClick()
        }
        compose.waitForIdle()
    }

    /** Toca un icono por su descripcion de accesibilidad (contentDescription). */
    protected fun tocarIcono(descripcion: String) {
        clic(compose.onAllNodesWithContentDescription(descripcion).onFirst())
    }

    protected fun existeIcono(descripcion: String, parcial: Boolean = false) {
        compose.onAllNodesWithContentDescription(descripcion, substring = parcial).onFirst().assertExists()
    }

    /** Toca un boton que esta dentro de un dialogo (evita confundirlo con uno de la pantalla). */
    protected fun tocarEnDialogo(texto: String) {
        clic(compose.onAllNodes(hasText(texto) and hasClickAction() and hasAnyAncestor(isDialog())).onFirst())
    }

    /** Escribe en el campo de texto cuya etiqueta contiene [etiqueta]. */
    protected fun escribir(etiqueta: String, valor: String) {
        compose.onAllNodes(hasSetTextAction() and hasText(etiqueta, substring = true)).onFirst().performTextReplacement(valor)
        compose.waitForIdle()
    }

    protected fun hayDialogo(): Boolean = compose.onAllNodes(isDialog()).fetchSemanticsNodes().isNotEmpty()
}

/** Base de datos Room real, pero en memoria: se borra al cerrar cada prueba. */
object BaseEnMemoria {
    fun crear(): AppDatabase = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(),
        AppDatabase::class.java
    ).allowMainThreadQueries().build()
}
