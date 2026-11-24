package cl.duoc.serviciotecnico.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Boton principal de un formulario. Mientras se guarda, muestra un indicador animado. */
@Composable
fun BotonGuardar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    texto: String = "Guardar",
    icono: ImageVector = Icons.Filled.Save,
    habilitado: Boolean = true,
    guardando: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = habilitado && !guardando,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        AnimatedContent(
            targetState = guardando,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "estadoDelBotonGuardar"
        ) { estaGuardando ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (estaGuardando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Guardando...")
                } else {
                    Icon(icono, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(texto)
                }
            }
        }
    }
}
