package app.siphon.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.siphon.data.settings.ThemeMode

/*
 * Siphon design tokens — Kotlin mirror of web/src/styles/tokens.css.
 */
object SiphonColors {
    val Ink0 = Color(0xFF08080C)
    val Ink1 = Color(0xFF0E0E15)
    val Ink2 = Color(0xFF14141D)
    val Ink3 = Color(0xFF1B1B27)
    val Ink4 = Color(0xFF232333)

    val Iris300 = Color(0xFFA99CFF)
    val Iris400 = Color(0xFF948BFF)
    val Iris500 = Color(0xFF7C6BFF)
    val Iris600 = Color(0xFF6753E8)
    val Cyan400 = Color(0xFF46C8FF)

    val TextPrimary = Color(0xFFF2F2F8)
    val TextSecondary = Color(0xFFA9A9BC)
    val TextTertiary = Color(0xFF6D6D82)

    val Success = Color(0xFF3ADFA5)
    val Warning = Color(0xFFFFC553)
    val Danger = Color(0xFFFF6373)

    val BorderSubtle = Color(0x12FFFFFF)

    val BrandGradient = Brush.linearGradient(
        listOf(Color(0xFF9D8CFF), Color(0xFF7C6BFF), Color(0xFF46C8FF)),
    )
}

private val DarkColors = darkColorScheme(
    primary = SiphonColors.Iris500,
    onPrimary = Color.White,
    primaryContainer = SiphonColors.Iris600,
    onPrimaryContainer = Color.White,
    secondary = SiphonColors.Cyan400,
    onSecondary = SiphonColors.Ink0,
    background = SiphonColors.Ink0,
    onBackground = SiphonColors.TextPrimary,
    surface = SiphonColors.Ink1,
    onSurface = SiphonColors.TextPrimary,
    surfaceVariant = SiphonColors.Ink3,
    onSurfaceVariant = SiphonColors.TextSecondary,
    surfaceContainer = SiphonColors.Ink2,
    surfaceContainerHigh = SiphonColors.Ink3,
    surfaceContainerHighest = SiphonColors.Ink4,
    outline = SiphonColors.TextTertiary,
    outlineVariant = SiphonColors.Ink4,
    error = SiphonColors.Danger,
    onError = Color.White,
)

private val LightColors = lightColorScheme(
    primary = SiphonColors.Iris600,
    onPrimary = Color.White,
    secondary = Color(0xFF0E7490),
    background = Color(0xFFF7F7FB),
    onBackground = Color(0xFF14141D),
    surface = Color.White,
    onSurface = Color(0xFF14141D),
    surfaceVariant = Color(0xFFECECF4),
    onSurfaceVariant = Color(0xFF56566B),
    error = Color(0xFFD73B4C),
)

val SiphonTypography = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, letterSpacing = (-1.2).sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.6).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 19.sp, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = (-0.2).sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.6.sp),
)

val SiphonShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun SiphonTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = SiphonTypography,
        shapes = SiphonShapes,
        content = content,
    )
}
