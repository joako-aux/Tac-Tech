package cl.duoc.serviciotecnico.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cl.duoc.serviciotecnico.data.repository.EstadoSync
import cl.duoc.serviciotecnico.ui.theme.EstadoPendienteCliente
import cl.duoc.serviciotecnico.ui.theme.Exito
import java.time.format.DateTimeFormatter

private enum class Situacion { SIN_CONEXION, SINCRONIZANDO, PENDIENTES, ERROR, AL_DIA }

private val formatoHora = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Banner que muestra el estado de la sincronizacion con el backend.
 * Cambia de color y mensaje con animacion; mientras sincroniza, el icono gira.
 */
@Composable
fun BannerConexion(
    sync: EstadoSync,
    onSincronizar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val situacion = when {
        !sync.enLinea -> Situacion.SIN_CONEXION
        sync.sincronizando -> Situacion.SINCRONIZANDO
        sync.error != null -> Situacion.ERROR
        sync.pendientes > 0 -> Situacion.PENDIENTES
        else -> Situacion.AL_DIA
    }
    val color by animateColorAsState(
        targetValue = when (situacion) {
            Situacion.SIN_CONEXION -> MaterialTheme.colorScheme.onSurfaceVariant
            Situacion.SINCRONIZANDO -> MaterialTheme.colorScheme.primary
            Situacion.PENDIENTES -> EstadoPendienteCliente
            Situacion.ERROR -> MaterialTheme.colorScheme.error
            Situacion.AL_DIA -> Exito
        },
        animationSpec = tween(400),
        label = "colorBanner"
    )
    val giro = rememberInfiniteTransition(label = "giroSync")
    val angulo by giro.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "anguloSync"
    )

    Surface(
        color = color.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            AnimatedContent(
                targetState = situacion,
                transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith fadeOut() },
                label = "contenidoBanner",
                modifier = Modifier.weight(1f)
            ) { s ->
                val icono: ImageVector = when (s) {
                    Situacion.SIN_CONEXION -> Icons.Outlined.CloudOff
                    Situacion.SINCRONIZANDO -> Icons.Outlined.Sync
                    Situacion.PENDIENTES -> Icons.Outlined.CloudUpload
                    Situacion.ERROR -> Icons.Outlined.SyncProblem
                    Situacion.AL_DIA -> Icons.Outlined.CloudDone
                }
                val texto: String = when (s) {
                    Situacion.SIN_CONEXION ->
                        if (sync.pendientes > 0) "Sin conexion · ${sync.pendientes} cambios guardados en el telefono"
                        else "Sin conexion · trabajando sin internet"
                    Situacion.SINCRONIZANDO -> "Sincronizando con el servidor..."
                    Situacion.PENDIENTES -> "${sync.pendientes} cambios por enviar"
                    Situacion.ERROR -> sync.error ?: "Error al sincronizar"
                    Situacion.AL_DIA -> "Todo sincronizado" + (sync.ultimaSync?.let { " · ${it.format(formatoHora)}" } ?: "")
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    Icon(
                        icono,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(if (s == Situacion.SINCRONIZANDO) angulo else 0f)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            if (sync.enLinea && !sync.sincronizando) {
                TextButton(onClick = onSincronizar) { Text("Sincronizar", color = color) }
            }
        }
    }
}

