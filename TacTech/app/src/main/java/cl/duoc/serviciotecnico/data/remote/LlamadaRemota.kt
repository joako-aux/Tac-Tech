package cl.duoc.serviciotecnico.data.remote

import cl.duoc.serviciotecnico.data.remote.dto.ErrorDto
import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

/** Resultado de una operacion que solo se puede hacer con conexion (gestion del administrador). */
sealed interface Resultado<out T> {
    data class Exito<T>(val dato: T) : Resultado<T>
    data class Error(val mensaje: String) : Resultado<Nothing>
}

private val gson = Gson()

/**
 * Ejecuta una llamada a tactech-backend y traduce los errores a un mensaje para la interfaz:
 * - sin red -> "Sin conexion con el servidor"
 * - 400/401/404/409 -> el mensaje que manda el backend en su ErrorDto
 */
suspend fun <T> llamar(bloque: suspend () -> T): Resultado<T> = try {
    Resultado.Exito(bloque())
} catch (e: IOException) {
    Resultado.Error("Sin conexion con el servidor. Esta accion necesita internet.")
} catch (e: HttpException) {
    Resultado.Error(mensajeDe(e))
}

fun mensajeDe(e: HttpException): String {
    val cuerpo = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
    val error = cuerpo?.let { runCatching { gson.fromJson(it, ErrorDto::class.java) }.getOrNull() }
    val porCampo = error?.errores?.values?.firstOrNull()
    return porCampo ?: error?.mensaje ?: "El servidor respondio ${e.code()}"
}
