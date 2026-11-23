package cl.duoc.serviciotecnico.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val EsquemaClaro = lightColorScheme(
    primary = Petroleo,
    onPrimary = Color.White,
    primaryContainer = PetroleoClaro,
    onPrimaryContainer = PetroleoTexto,
    secondary = Naranja,
    onSecondary = Color.White,
    secondaryContainer = NaranjaClaro,
    onSecondaryContainer = NaranjaTexto,
    tertiary = Exito,
    onTertiary = Color.White,
    error = RojoError,
    onError = Color.White,
    errorContainer = RojoErrorClaro,
    onErrorContainer = RojoError,
    background = Fondo,
    onBackground = TextoPrincipal,
    surface = Superficie,
    onSurface = TextoPrincipal,
    surfaceVariant = SuperficieVariante,
    onSurfaceVariant = TextoSecundario,
    surfaceContainerLowest = Superficie,
    surfaceContainerLow = Superficie,
    surfaceContainer = Superficie,
    surfaceContainerHigh = Superficie,
    surfaceContainerHighest = SuperficieVariante,
    outline = Borde,
    outlineVariant = SuperficieVariante
)

private val EsquemaOscuro = darkColorScheme(
    primary = PetroleoNoche,
    onPrimary = PetroleoTexto,
    primaryContainer = PetroleoNocheContenedor,
    onPrimaryContainer = PetroleoClaro,
    secondary = NaranjaNoche,
    onSecondary = NaranjaTexto,
    secondaryContainer = Color(0xFF5C3300),
    onSecondaryContainer = NaranjaClaro,
    tertiary = Color(0xFF6FD8A2),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = RojoErrorClaro,
    background = FondoNoche,
    onBackground = TextoNoche,
    surface = SuperficieNoche,
    onSurface = TextoNoche,
    surfaceVariant = SuperficieVarianteNoche,
    onSurfaceVariant = TextoSecundarioNoche,
    surfaceContainerLowest = SuperficieNoche,
    surfaceContainerLow = SuperficieNoche,
    surfaceContainer = SuperficieNoche,
    surfaceContainerHigh = SuperficieNoche,
    surfaceContainerHighest = SuperficieVarianteNoche,
    outline = BordeNoche,
    outlineVariant = SuperficieVarianteNoche
)

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ServicioTecnicoTheme(
    enOscuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (enOscuro) EsquemaOscuro else EsquemaClaro,
        typography = Tipografia,
        shapes = Formas,
        content = content
    )
}
