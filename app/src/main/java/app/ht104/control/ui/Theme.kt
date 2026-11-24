package app.ht104.control.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark: Graphite / Antique Brass / Burnt Orange.  Light: Cream / Champagne / Copper Orange.
private val Dark = darkColorScheme(
    primary = Color(0xFFA85A30),
    onPrimary = Color(0xFFFFF4EA),
    primaryContainer = Color(0xFF7A4020),
    onPrimaryContainer = Color(0xFFFFE3D0),
    secondary = Color(0xFFC8873F),
    onSecondary = Color(0xFF2A1A08),
    tertiary = Color(0xFFD9A25F),
    background = Color(0xFF1B1F22),
    onBackground = Color(0xFFECE6DC),
    surface = Color(0xFF1B1F22),
    onSurface = Color(0xFFECE6DC),
    surfaceVariant = Color(0xFF262B2E),
    onSurfaceVariant = Color(0xFFA9A398),
    outline = Color(0xFF4A4F52),
    error = Color(0xFFE57A6A),
)

private val Light = lightColorScheme(
    primary = Color(0xFFC96B38),
    onPrimary = Color(0xFFFFF8F0),
    primaryContainer = Color(0xFFF0C9A8),
    onPrimaryContainer = Color(0xFF4A2410),
    secondary = Color(0xFFA8793A),
    onSecondary = Color(0xFFFFF8F0),
    tertiary = Color(0xFF8C6A33),
    background = Color(0xFFF3EADD),
    onBackground = Color(0xFF3B3128),
    surface = Color(0xFFF3EADD),
    onSurface = Color(0xFF3B3128),
    surfaceVariant = Color(0xFFE4D9C6),
    onSurfaceVariant = Color(0xFF6E6354),
    outline = Color(0xFFC9BBA2),
    error = Color(0xFFB3402E),
)

@Composable
fun HtTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = Typography(),
        content = content,
    )
}
