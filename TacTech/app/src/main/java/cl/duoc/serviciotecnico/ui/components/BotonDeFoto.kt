package cl.duoc.serviciotecnico.ui.components

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cl.duoc.serviciotecnico.util.ArchivosDeFoto
import cl.duoc.serviciotecnico.util.Permisos

/**
 * Recurso nativo 1: camara.
 *
 * Pide el permiso CAMERA solo al usarse, crea el archivo en el almacenamiento privado,
 * abre la camara del sistema mediante FileProvider y entrega la URI de la foto tomada.
 * Si el permiso se deniega, explica para que sirve y deja seguir trabajando.
 */
@Composable
fun BotonDeFoto(
    onFotoTomada: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    etiqueta: String = "Tomar fotografia",
    habilitado: Boolean = true
) {
    val context = LocalContext.current
    // Saveable: si la actividad se recrea mientras la camara esta abierta, la URI no se pierde
    var uriPendiente by rememberSaveable { mutableStateOf<Uri?>(null) }
    var permisoDenegado by rememberSaveable { mutableStateOf(false) }

    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        val uri = uriPendiente
        uriPendiente = null
        if (uri != null) {
            if (exito) onFotoTomada(uri) else ArchivosDeFoto.eliminar(context, uri)
        }
    }

    fun abrirCamara() {
        val uri = ArchivosDeFoto.uriDeArchivo(context, ArchivosDeFoto.crearArchivo(context))
        uriPendiente = uri
        camara.launch(uri)
    }

    val solicitarPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) abrirCamara() else permisoDenegado = true
    }

    FilledTonalButton(
        onClick = {
            if (Permisos.concedido(context, Manifest.permission.CAMERA)) {
                abrirCamara()
            } else {
                solicitarPermiso.launch(Manifest.permission.CAMERA)
            }
        },
        enabled = habilitado,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(etiqueta)
    }

    if (permisoDenegado) {
        DialogoPermisoDenegado(
            titulo = "Permiso de camara",
            explicacion = "La camara se usa para adjuntar fotografias del trabajo realizado " +
                "a la guia de servicio. Puedes guardar la guia sin fotografias.",
            onCerrar = { permisoDenegado = false }
        )
    }
}
