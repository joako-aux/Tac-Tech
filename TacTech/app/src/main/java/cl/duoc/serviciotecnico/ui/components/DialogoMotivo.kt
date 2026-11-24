package cl.duoc.serviciotecnico.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction

/**
 * Dialogo para pedir un motivo (cancelar o rechazar una solicitud).
 * El texto y su error los maneja el ViewModel.
 */
@Composable
fun DialogoMotivo(
    titulo: String,
    explicacion: String,
    icono: ImageVector,
    motivo: String,
    error: String?,
    textoConfirmar: String,
    onMotivoCambia: (String) -> Unit,
    onConfirmar: () -> Unit,
    onCerrar: () -> Unit,
    procesando: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        icon = { Icon(icono, contentDescription = null) },
        title = { Text(titulo) },
        text = {
            Column {
                Text(explicacion)
                CampoTextoValidado(
                    valor = motivo,
                    onValorCambia = onMotivoCambia,
                    etiqueta = "Motivo",
                    icono = Icons.Outlined.ChatBubbleOutline,
                    obligatorio = true,
                    lineasMaximas = 3,
                    largoMaximo = 300,
                    accionDelTeclado = ImeAction.Done,
                    error = error,
                    habilitado = !procesando
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirmar, enabled = !procesando) { Text(textoConfirmar) }
        },
        dismissButton = {
            TextButton(onClick = onCerrar, enabled = !procesando) { Text("Volver") }
        }
    )
}
