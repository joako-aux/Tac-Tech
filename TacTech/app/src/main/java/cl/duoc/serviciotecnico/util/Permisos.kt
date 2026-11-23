package cl.duoc.serviciotecnico.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat

object Permisos {

    fun concedido(context: Context, permiso: String): Boolean =
        ContextCompat.checkSelfPermission(context, permiso) == PackageManager.PERMISSION_GRANTED

    fun abrirAjustesDeLaApp(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
