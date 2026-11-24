package cl.duoc.serviciotecnico.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Miniaturas de las fotografias. Si se pasa [onQuitarFoto], cada una muestra el
 * boton de quitar; sin el, la galeria es de solo lectura (detalle de la guia).
 * Cada miniatura entra y sale con animacion.
 */
@Composable
fun GaleriaDeFotos(
    fotos: List<Uri>,
    modifier: Modifier = Modifier,
    onQuitarFoto: ((Uri) -> Unit)? = null
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = fotos, key = { it.toString() }) { foto ->
            Box(Modifier.animateItem()) {
                AsyncImage(
                    model = foto,
                    contentDescription = "Fotografia del trabajo realizado",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(108.dp)
                        .clip(MaterialTheme.shapes.small)
                )
                if (onQuitarFoto != null) {
                    FilledIconButton(
                        onClick = { onQuitarFoto(foto) },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(28.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Quitar fotografia", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
