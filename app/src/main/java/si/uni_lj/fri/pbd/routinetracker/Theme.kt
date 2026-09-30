package si.uni_lj.fri.pbd.routinetracker

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val ExpressiveColorScheme = lightColorScheme(
    primary = Color(0xFF874B6D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD8E9),
    onPrimaryContainer = Color(0xFF6C3454),
    secondary = Color(0xFF715764),
    onSecondary = Color(0xFFFFFFFF),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFFFF8F8),
    surface = Color(0xFFFFF8F8),
    surfaceVariant = Color(0xFFF0DEE4),
    onSurfaceVariant = Color(0xFF504349)
)


val ExpressiveTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp
    )
)

@Composable
fun RoutineExpressiveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ExpressiveColorScheme,
        typography = ExpressiveTypography,
        content = content
    )
}