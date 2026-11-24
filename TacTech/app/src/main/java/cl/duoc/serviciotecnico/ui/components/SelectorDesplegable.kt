package cl.duoc.serviciotecnico.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cl.duoc.serviciotecnico.ui.theme.Exito

/**
 * Campo de seleccion de una lista (tecnico a asignar, empresa, rol...).
 * Igual que CampoTextoValidado, no valida: recibe el error ya calculado por el ViewModel.
 */
@Composable
fun <T> SelectorDesplegable(
    etiqueta: String,
    opciones: List<T>,
    seleccionado: T?,
    textoDe: (T) -> String,
    onSeleccionar: (T) -> Unit,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    error: String? = null,
    habilitado: Boolean = true,
    textoVacio: String = "No hay opciones disponibles"
) {
    var abierto by remember { mutableStateOf(false) }
    val colorBorde = when {
        error != null -> MaterialTheme.colorScheme.error
        seleccionado != null -> Exito
        else -> MaterialTheme.colorScheme.outline
    }

    Column(modifier.fillMaxWidth()) {
        Box {
            Surface(
                onClick = { abierto = true },
                enabled = habilitado,
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, colorBorde),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icono, contentDescription = null, tint = colorBorde)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            seleccionado?.let(textoDe) ?: "Selecciona una opcion",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (seleccionado == null) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (seleccionado != null && error == null) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Seleccion correcta", tint = Exito)
                    }
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "Abrir opciones")
                }
            }
            DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                if (opciones.isEmpty()) {
                    DropdownMenuItem(text = { Text(textoVacio) }, onClick = { abierto = false }, enabled = false)
                }
                opciones.forEach { opcion ->
                    DropdownMenuItem(
                        text = { Text(textoDe(opcion)) },
                        onClick = {
                            abierto = false
                            onSeleccionar(opcion)
                        }
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = error != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(Modifier.padding(start = 12.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
