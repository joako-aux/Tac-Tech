package cl.duoc.serviciotecnico.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import cl.duoc.serviciotecnico.util.Permisos

@Composable
fun DialogoPermisoDenegado(
    titulo: String,
    explicacion: String,
    onCerrar: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onCerrar,
        icon = {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null
            )
        },
        title = { Text(titulo) },
        text = { Text(explicacion) },
        confirmButton = {
            TextButton(
                onClick = {
                    Permisos.abrirAjustesDeLaApp(context)
                    onCerrar()
                }
            ) {
                Text("Abrir ajustes")
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) {
                Text("Continuar sin esto")
            }
        }
    )
}
