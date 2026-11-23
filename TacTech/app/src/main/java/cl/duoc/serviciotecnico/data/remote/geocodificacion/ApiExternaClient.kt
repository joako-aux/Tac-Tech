package cl.duoc.serviciotecnico.data.remote.geocodificacion

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente Retrofit de la API externa (Nominatim / OpenStreetMap).
 *
 * Es un cliente distinto al del microservicio propio (RetrofitClient): otra URL base (HTTPS)
 * y otras cabeceras, asi la API externa no interfiere con tactech-backend.
 */
object ApiExternaClient {

    private const val URL_NOMINATIM = "https://nominatim.openstreetmap.org/"

    /** La politica de uso de Nominatim pide identificar la aplicacion en cada peticion. */
    private const val USER_AGENT = "TacTech/1.0 (DSY1105 Duoc UC - proyecto academico)"

    private val cliente: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(Interceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
            })
            .build()
    }

    val nominatim: NominatimApi by lazy {
        Retrofit.Builder()
            .baseUrl(URL_NOMINATIM)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NominatimApi::class.java)
    }
}
