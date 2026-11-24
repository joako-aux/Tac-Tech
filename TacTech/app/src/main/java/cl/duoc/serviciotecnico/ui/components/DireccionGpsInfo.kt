package cl.duoc.serviciotecnico.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.serviciotecnico.ui.viewmodel.DireccionGpsUi

/**
 * API externa en la interfaz: direccion de las coordenadas de la visita (OpenStreetMap / Nominatim).
 *
 * @param onUsarDireccion si no es null, muestra el boton para copiar la direccion al reporte.
 * @param onReintentar si no es null y la busqueda fallo, muestra el boton para reintentar.
 */
@Composable
fun DireccionGpsInfo(
    gps: DireccionGpsUi,
    modifier: Modifier = Modifier,
    onUsarDireccion: (() -> Unit)? = null,
    onReintentar: (() -> Unit)? = null
) {
    AnimatedContent(
        targetState = gps,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "direccionGps",
        modifier = modifier.fillMaxWidth()
    ) { estado ->
        val direccion = estado.direccion
        val error = estado.error
        when {
            estado.buscando -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Buscando la direccion en OpenStreetMap...", style = MaterialTheme.typography.bodySmall)
            }

            direccion != null -> Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                shape = MaterialTheme.shapes.small
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Direccion segun GPS",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(direccion.corta, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    direccion.region?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (onUsarDireccion != null) {
                        TextButton(onClick = onUsarDireccion) { Text("Usar esta direccion en el reporte") }
                    }
                    Text(
                        "Datos de direccion © colaboradores de OpenStreetMap",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            error != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                if (onReintentar != null) {
                    TextButton(onClick = onReintentar) { Text("Reintentar") }
                }
            }

            else -> Spacer(Modifier.size(0.dp))
        }
    }
}
