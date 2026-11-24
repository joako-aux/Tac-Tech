package cl.duoc.serviciotecnico.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.ui.theme.colorDeEstado

/** Etiqueta del estado del ticket. Al cambiar de estado, el color transiciona y el texto se cruza. */
@Composable
fun EtiquetaEstado(
    estado: EstadoTicket,
    modifier: Modifier = Modifier
) {
    val color by animateColorAsState(
        targetValue = colorDeEstado(estado),
        animationSpec = tween(durationMillis = 450),
        label = "colorDelEstado"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(color = color.copy(alpha = 0.14f), shape = RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .semantics { contentDescription = "Estado del ticket: ${estado.etiqueta}" }
    ) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        AnimatedContent(
            targetState = estado,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(150)) },
            label = "textoDelEstado"
        ) { actual ->
            Text(text = actual.etiqueta, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}
