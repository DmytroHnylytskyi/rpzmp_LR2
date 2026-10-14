package ua.kpi.windpower.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Темна фірмова кольорова схема — базовий режим диспетчерського пульту SCADA.
 * Кожна роль Material 3 виражена через брендові токени [Color.kt].
 */
private val DarkColorScheme = darkColorScheme(
    primary = EmeraldLight,
    onPrimary = InkOnAccent,
    primaryContainer = DarkContainer,
    onPrimaryContainer = EmeraldLight,
    secondary = CyanAccent,
    onSecondary = Color(0xFF00252C),
    secondaryContainer = DarkCyanContainer,
    onSecondaryContainer = CyanLight,
    tertiary = AmberWarning,
    onTertiary = Color(0xFF261800),
    tertiaryContainer = DarkAmberContainer,
    onTertiaryContainer = AmberWarning,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainerLowest = Color(0xFF0A0E12),
    surfaceContainerLow = DarkBackground,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceVariant,
    surfaceContainerHighest = DarkSurfaceHigh,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = StormAlert,
    onError = Color.White,
    errorContainer = Color(0xFF3D0F0F),
    onErrorContainer = Color(0xFFFCA5A5),
    inverseSurface = LightSurface,
    inverseOnSurface = LightTextPrimary,
    inversePrimary = EmeraldDark
)

/**
 * Світла схема залишена для повноти Material 3 (використовується лише
 * при явному запиті, за замовчуванням застосунок працює у фірмовій темній темі).
 */
private val LightColorScheme = lightColorScheme(
    primary = EmeraldDark,
    onPrimary = Color.White,
    primaryContainer = LightContainer,
    onPrimaryContainer = EmeraldDark,
    secondary = CyanAccent,
    onSecondary = Color.White,
    secondaryContainer = LightCyanContainer,
    onSecondaryContainer = Color(0xFF00363F),
    tertiary = AmberWarning,
    onTertiary = Color(0xFF261800),
    tertiaryContainer = LightAmberContainer,
    onTertiaryContainer = Color(0xFF3F2E04),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFFCBD5E1),
    error = StormAlert,
    onError = Color.White
)

/**
 * Єдина система скруглень (8dp сітка): чіпи 10dp, картки 16dp, hero-блоки 20dp.
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

/**
 * Головна тема інтерфейсу застосунку AeroGrid.
 *
 * Застосунок виконується у фірмовій темній темі Eco-Tech: [darkTheme]
 * за замовчуванням увімкнена незалежно від системного налаштування —
 * диспетчерський інтерфейс SCADA завжди працює у темному режимі.
 *
 * @param darkTheme Прапорець активації темної брендової теми.
 * @param content Вміст інтерфейсу користувача.
 */
@Composable
fun WindPowerPlantTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
