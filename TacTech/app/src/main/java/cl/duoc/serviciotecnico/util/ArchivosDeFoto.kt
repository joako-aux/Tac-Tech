package cl.duoc.serviciotecnico.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ArchivosDeFoto {

    private const val CARPETA = "fotos"
    private val formato = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS")

    fun crearArchivo(context: Context): File {
        val carpeta = File(context.filesDir, CARPETA)
        if (!carpeta.exists()) {
            carpeta.mkdirs()
        }
        val nombre = "guia_${LocalDateTime.now().format(formato)}.jpg"
        return File(carpeta, nombre)
    }

    fun uriDeArchivo(context: Context, archivo: File): Uri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            archivo
        )

    fun eliminar(context: Context, uri: Uri): Boolean {
        val nombre = uri.lastPathSegment?.substringAfterLast('/') ?: return false
        val archivo = File(File(context.filesDir, CARPETA), nombre)
        return archivo.exists() && archivo.delete()
    }
}
