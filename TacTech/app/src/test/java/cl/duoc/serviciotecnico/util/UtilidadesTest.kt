package cl.duoc.serviciotecnico.util

import android.Manifest
import android.app.Application
import android.net.Uri
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** Utilidades de Android: archivos de las fotos y permisos en tiempo de ejecucion. */
@RunWith(RobolectricTestRunner::class)
class UtilidadesTest {

    private val app = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun creaYEliminaElArchivoDeUnaFoto() {
        val archivo = ArchivosDeFoto.crearArchivo(app)
        assertTrue(archivo.name.startsWith("guia_"))
        archivo.writeText("foto")

        val uri = Uri.fromFile(archivo)
        assertTrue(ArchivosDeFoto.eliminar(app, uri))
        assertFalse("Ya no existe", ArchivosDeFoto.eliminar(app, uri))
    }

    @Test
    fun variasFotosVanALaMismaCarpetaYUnaUriSinArchivoNoSeBorra() {
        val primera = ArchivosDeFoto.crearArchivo(app)
        val segunda = ArchivosDeFoto.crearArchivo(app)
        assertTrue(primera.parentFile!!.exists())
        assertEquals(primera.parentFile, segunda.parentFile)

        // Una URI sin ruta (sin nombre de archivo) no borra nada
        assertFalse(ArchivosDeFoto.eliminar(app, Uri.parse("content://${app.packageName}.fileprovider")))
    }

    @Test
    fun permisosYAjustesDeLaApp() {
        assertFalse(Permisos.concedido(app, Manifest.permission.CAMERA))
        shadowOf(app).grantPermissions(Manifest.permission.CAMERA)
        assertTrue(Permisos.concedido(app, Manifest.permission.CAMERA))

        Permisos.abrirAjustesDeLaApp(app)
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, shadowOf(app).nextStartedActivity.action)
    }
}
