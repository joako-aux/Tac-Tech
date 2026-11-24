package cl.duoc.serviciotecnico.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cl.duoc.serviciotecnico.ui.theme.Exito

/** Estado visual de un campo, decidido por el ViewModel. */
private enum class Retro { NINGUNA, ERROR, VALIDO }

/**
 * Campo de formulario compartido por toda la app.
 *
 * No valida nada: recibe [error] y [valido] ya calculados por el ViewModel y solo
 * los pinta. Muestra un icono que describe el dato, un icono de estado (error o
 * correcto) y el mensaje de error animado bajo el campo.
 */
@Composable
fun CampoTextoValidado(
    valor: String,
    onValorCambia: (String) -> Unit,
    etiqueta: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    error: String? = null,
    valido: Boolean = false,
    obligatorio: Boolean = false,
    ayuda: String? = null,
    largoMaximo: Int? = null,
    lineasMaximas: Int = 1,
    tipoDeTeclado: KeyboardType = KeyboardType.Text,
    accionDelTeclado: ImeAction = ImeAction.Next,
    capitalizacion: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    habilitado: Boolean = true,
    esClave: Boolean = false
) {
    val retro = when {
        error != null -> Retro.ERROR
        valido -> Retro.VALIDO
        else -> Retro.NINGUNA
    }
    var claveVisible by rememberSaveable { mutableStateOf(false) }

    val colorIcono by animateColorAsState(
        targetValue = when (retro) {
            Retro.ERROR -> MaterialTheme.colorScheme.error
            Retro.VALIDO -> Exito
            Retro.NINGUNA -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(250),
        label = "colorIconoCampo"
    )

    OutlinedTextField(
        value = valor,
        onValueChange = onValorCambia,
        modifier = modifier
            .fillMaxWidth()
            .semantics { if (error != null) contentDescription = "$etiqueta, con error: $error" },
        enabled = habilitado,
        label = { Text(if (obligatorio) "$etiqueta *" else etiqueta) },
        leadingIcon = { Icon(icono, contentDescription = null, tint = colorIcono) },
        trailingIcon = {
            if (esClave) {
                IconButton(onClick = { claveVisible = !claveVisible }) {
                    Icon(
                        imageVector = if (claveVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (claveVisible) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            } else {
                AnimatedContent(
                    targetState = retro,
                    transitionSpec = {
                        (scaleIn(tween(220)) + fadeIn(tween(220))) togetherWith
                            (scaleOut(tween(120)) + fadeOut(tween(120)))
                    },
                    label = "iconoEstadoCampo"
                ) { estado ->
                    when (estado) {
                        Retro.ERROR -> Icon(
                            Icons.Filled.Error,
                            contentDescription = "Campo con error",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Retro.VALIDO -> Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Campo correcto",
                            tint = Exito
                        )
                        Retro.NINGUNA -> Spacer(Modifier.size(24.dp))
                    }
                }
            }
        },
        supportingText = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = error,
                        transitionSpec = {
                            (slideInVertically(tween(200)) { -it / 2 } + fadeIn(tween(200))) togetherWith
                                fadeOut(tween(100))
                        },
                        label = "mensajeCampo"
                    ) { mensaje ->
                        if (mensaje != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.ErrorOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(mensaje)
                            }
                        } else {
                            Text(ayuda.orEmpty())
                        }
                    }
                }
                if (largoMaximo != null) {
                    Text("${valor.length}/$largoMaximo")
                }
            }
        },
        isError = error != null,
        visualTransformation = if (esClave && !claveVisible) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = lineasMaximas == 1,
        minLines = if (lineasMaximas > 1) 2 else 1,
        maxLines = lineasMaximas,
        keyboardOptions = KeyboardOptions(
            capitalization = if (esClave || tipoDeTeclado == KeyboardType.Email) KeyboardCapitalization.None else capitalizacion,
            keyboardType = tipoDeTeclado,
            imeAction = accionDelTeclado
        ),
        shape = MaterialTheme.shapes.small,
        colors = OutlinedTextFieldDefaults.colors(
            focusedLeadingIconColor = colorIcono,
            unfocusedLeadingIconColor = colorIcono
        )
    )
}
