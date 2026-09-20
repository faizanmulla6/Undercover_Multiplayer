package com.faizan.undercover.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// A "field dossier" palette: paper and ink by day, briefing room by night.
private val CrimsonDark = Color(0xFFE2574C)
private val OliveLight = Color(0xFF8FA37A)

private val LightColors = lightColorScheme(
    primary = Color(0xFFC62828), // Warm Rich Red
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEBEE),
    onPrimaryContainer = Color(0xFFB71C1C),
    secondary = Color(0xFF1565C0), // Royal Blue
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3F2FD),
    onSecondaryContainer = Color(0xFF0D47A1),
    tertiary = Color(0xFF2E7D32), // Forest Green
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8F5E9),
    onTertiaryContainer = Color(0xFF1B5E20),
    background = Color(0xFFFDF4E7), // Warm Sunset Cream
    onBackground = Color(0xFF2C251D), // Darker Brown-Ink
    surface = Color(0xFFFFF9F0), // Ivory Surface
    onSurface = Color(0xFF2C251D),
    surfaceVariant = Color(0xFFF5EAD7), // Warm Sand
    onSurfaceVariant = Color(0xFF605242), // Coffee/Brown tone
    surfaceContainer = Color(0xFFF7EEDC), // Toasted Paper
    surfaceContainerHigh = Color(0xFFEFE2CB),
    outline = Color(0xFF8D7F70),
    outlineVariant = Color(0xFFDCCDBB),
    error = Color(0xFFB3261E)
)

private val DarkColors = darkColorScheme(
    primary = CrimsonDark,
    onPrimary = Color(0xFF1B1210),
    primaryContainer = Color(0xFF5C1710),
    onPrimaryContainer = Color(0xFFFFDAD5),
    secondary = OliveLight,
    onSecondary = Color(0xFF17200E),
    secondaryContainer = Color(0xFF2F3A25),
    onSecondaryContainer = Color(0xFFDDE8CC),
    tertiary = Color(0xFFD5C39B),
    onTertiary = Color(0xFF241B04),
    background = Color(0xFF101319),
    onBackground = Color(0xFFECE7DA),
    surface = Color(0xFF101319),
    onSurface = Color(0xFFECE7DA),
    surfaceVariant = Color(0xFF23262C),
    onSurfaceVariant = Color(0xFFA6A190),
    surfaceContainer = Color(0xFF1A1E24),
    surfaceContainerHigh = Color(0xFF22262D),
    outline = Color(0xFF6C7078),
    outlineVariant = Color(0xFF383C43),
    error = Color(0xFFFFB4AB)
)

/** Monospace headings give the stencilled, typewritten briefing feel. */
private val base = Typography()

val UndercoverTypography = base.copy(
    displaySmall = base.displaySmall.copy(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.sp
    ),
    headlineMedium = base.headlineMedium.copy(
        fontFamily = FontFamily.Monospace,
        letterSpacing = 0.5.sp
    ),
    headlineSmall = base.headlineSmall.copy(
        fontFamily = FontFamily.Monospace,
        letterSpacing = 0.5.sp
    ),
    titleLarge = base.titleLarge.copy(
        fontFamily = FontFamily.Monospace,
        letterSpacing = 2.sp
    ),
    labelSmall = base.labelSmall.copy(
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.2.sp
    )
)

val StencilLabel = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    letterSpacing = 1.6.sp
)

@Composable
fun UndercoverTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = UndercoverTypography,
        content = content
    )
}
