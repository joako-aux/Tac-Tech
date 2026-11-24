package cl.duoc.serviciotecnico.ui.components

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cl.duoc.serviciotecnico.util.Permisos
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnFailureListener

/**
 * Recurso nativo 2: ubicacion (GPS).
 *
 * Pide los permisos de ubicacion al usarse, consulta la posicion actual con el
 * FusedLocationProvider y, si no hay una nueva, recurre a la ultima conocida.
 * Entrega latitud y longitud hacia afuera.
 */
@SuppressLint("MissingPermission")
@Composable
fun BotonDeUbicacion(
    onUbicacionObtenida: (Double, Double) -> Unit,
    modifier: Modifier = Modifier,
    etiqueta: String = "Registrar mi ubicacion"
) {
    val context = LocalContext.current
    var obteniendo by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var permisoDenegado by rememberSaveable { mutableStateOf(false) }

    fun obtenerUbicacion() {
        obteniendo = true
        mensaje = null
        val cliente = LocationServices.getFusedLocationProviderClient(context)
        val falla = OnFailureListener {
            obteniendo = false
            mensaje = "No se pudo obtener la ubicacion."
        }
        cliente.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { ubicacion ->
                if (ubicacion != null) {
                    obteniendo = false
                    onUbicacionObtenida(ubicacion.latitude, ubicacion.longitude)
                } else {
                    cliente.lastLocation
                        .addOnSuccessListener { ultima ->
                            obteniendo = false
                            if (ultima != null) {
                                onUbicacionObtenida(ultima.latitude, ultima.longitude)
                            } else {
                                mensaje = "No se pudo obtener la ubicacion. Revisa que el GPS este activado."
                            }
                        }
                        .addOnFailureListener(falla)
                }
            }
            .addOnFailureListener(falla)
    }

    val solicitarPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        if (resultados.values.any { it }) obtenerUbicacion() else permisoDenegado = true
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FilledTonalButton(
            onClick = {
                val tienePermiso =
                    Permisos.concedido(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
                        Permisos.concedido(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                if (tienePermiso) {
                    obtenerUbicacion()
                } else {
                    solicitarPermiso.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            enabled = !obteniendo,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (obteniendo) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Outlined.MyLocation, contentDescription = null)
            }
            Spacer(Modifier.width(8.dp))
            Text(if (obteniendo) "Obteniendo ubicacion..." else etiqueta)
        }

        AnimatedVisibility(
            visible = mensaje != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Text(
                text = mensaje.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    if (permisoDenegado) {
        DialogoPermisoDenegado(
            titulo = "Permiso de ubicacion",
            explicacion = "La ubicacion se usa para registrar donde se realizo el servicio. " +
                "Puedes guardar la guia sin coordenadas.",
            onCerrar = { permisoDenegado = false }
        )
    }
}
