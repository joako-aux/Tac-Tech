package cl.duoc.serviciotecnico.data.remote

import android.content.Context
import android.net.ConnectivityManager
import androidx.test.core.app.ApplicationProvider
import cl.duoc.serviciotecnico.data.remote.geocodificacion.ApiExternaClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkCapabilities

/** Clientes HTTP (Singleton) y monitor de conexion a internet. */
@RunWith(RobolectricTestRunner::class)
class ClientesYMonitorTest {

    @Test
    fun losClientesHttpSonUnicos() {
        // Microservicio propio y API externa: cada uno con su propio cliente Retrofit
        assertSame(RetrofitClient.api, RetrofitClient.api)
        assertSame(ApiExternaClient.nominatim, ApiExternaClient.nominatim)
    }

    @Test
    fun elMonitorReaccionaCuandoLaRedApareceYDesaparece() {
        val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val monitor = MonitorConexion(context, alcance)
        assertNotNull(monitor.enLinea.value)

        val conectividad = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val red = ShadowNetwork.newInstance(1)
        val callbacks = shadowOf(conectividad).networkCallbacks

        callbacks.forEach { it.onAvailable(red) }
        assertTrue(monitor.enLinea.value)

        val sinInternet = ShadowNetworkCapabilities.newInstance()
        callbacks.forEach { it.onCapabilitiesChanged(red, sinInternet) }
        assertFalse(monitor.enLinea.value)

        callbacks.forEach { it.onLost(red) }
        alcance.cancel()
    }
}
