package cl.duoc.serviciotecnico.data.remote

import cl.duoc.serviciotecnico.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP unico de la app (Singleton).
 *
 * `object` garantiza una sola instancia: se comparte el pool de conexiones de OkHttp
 * en vez de abrir uno nuevo por cada llamada. `by lazy` lo crea recien la primera vez
 * que se usa.
 */
object RetrofitClient {

    private val cliente: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // Tiempos cortos: si el backend no responde, la app sigue trabajando offline
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    // Muestra en Logcat cada peticion y su codigo de respuesta (solo en debug)
                    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                    else HttpLoggingInterceptor.Level.NONE
                }
            )
            .build()
    }

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_URL)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
