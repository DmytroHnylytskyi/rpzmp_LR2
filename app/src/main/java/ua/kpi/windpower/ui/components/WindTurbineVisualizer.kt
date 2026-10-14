package ua.kpi.windpower.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Графічний Composable-компонент динамічної візуалізації вітрової турбіни.
 *
 * Малювання об'єкта виконується повністю декларативно через [Canvas] без використання
 * растрових зображень. Компонент забезпечує плавне обертання трилопатевого ротора,
 * при цьому кутова швидкість обертання безпосередньо залежить від поточної швидкості вітру:
 * - При вітрі < 3.0 м/с (Cut-in) обертання припиняється (режим очікування).
 * - При робочому вітрі 3.0..25.0 м/с частота обертів динамічно зростає.
 * - При вітрі > 25.0 м/с (Cut-out) спрацьовує аварійна зупинка (флюгування).
 *
 * @param windSpeed Поточна швидкість вітрового потоку у м/с.
 * @param modifier Модифікатор компонування для налаштування розмірів та відступів.
 */
@Composable
fun WindTurbineVisualizer(
    windSpeed: Float,
    modifier: Modifier = Modifier.size(180.dp)
) {
    val isOperating = windSpeed in 3.0f..25.0f

    // Період одного повного оберту (мс): зворотна залежність від швидкості вітру
    val rotationDurationMs = when {
        !isOperating -> 100000 // практична нерухомість при штилі або аварійному стопі
        windSpeed < 6f -> 3000
        windSpeed < 12f -> 1500
        windSpeed < 18f -> 900
        else -> 650
    }

    val infiniteTransition = rememberInfiniteTransition(label = "TurbineRotationTransition")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isOperating) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = rotationDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "BladeAngleDegrees"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val hubCenterX = canvasWidth / 2f
            val hubCenterY = canvasHeight * 0.42f
            val groundY = canvasHeight * 0.95f

            // 1. Конічна вежа вітроустановки (Tower)
            val towerPath = Path().apply {
                moveTo(hubCenterX - 5f, hubCenterY)
                lineTo(hubCenterX - 16f, groundY)
                lineTo(hubCenterX + 16f, groundY)
                lineTo(hubCenterX + 5f, hubCenterY)
                close()
            }
            drawPath(
                path = towerPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF94A3B8), Color(0xFF475569))
                )
            )

            // 2. Гондола з редуктором та генератором (Nacelle)
            drawRoundRect(
                color = Color(0xFF334155),
                topLeft = Offset(hubCenterX - 14f, hubCenterY - 8f),
                size = androidx.compose.ui.geometry.Size(28f, 16f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )

            // 3. Аеродинамічні лопаті ротора (3 лопаті з фазовим зсувом 120°)
            rotate(
                degrees = if (isOperating) rotationAngle else 15f,
                pivot = Offset(hubCenterX, hubCenterY)
            ) {
                val bladeLength = canvasHeight * 0.38f

                for (bladeIndex in 0..2) {
                    val angleDegrees = bladeIndex * 120.0
                    val angleRadians = Math.toRadians(angleDegrees)
                    val cosAlpha = cos(angleRadians).toFloat()
                    val sinAlpha = sin(angleRadians).toFloat()

                    // Нормальний вектор для формування профілю лопаті
                    val normalX = -sinAlpha
                    val normalY = cosAlpha

                    val tipX = hubCenterX + bladeLength * cosAlpha
                    val tipY = hubCenterY + bladeLength * sinAlpha

                    val chordDist = bladeLength * 0.45f
                    val halfChordWidth = 9f

                    val chordX1 = hubCenterX + chordDist * cosAlpha + halfChordWidth * normalX
                    val chordY1 = hubCenterY + chordDist * sinAlpha + halfChordWidth * normalY
                    val chordX2 = hubCenterX + chordDist * cosAlpha - halfChordWidth * normalX
                    val chordY2 = hubCenterY + chordDist * sinAlpha - halfChordWidth * normalY

                    val bladePath = Path().apply {
                        moveTo(hubCenterX, hubCenterY)
                        lineTo(chordX1, chordY1)
                        lineTo(tipX, tipY)
                        lineTo(chordX2, chordY2)
                        close()
                    }

                    drawPath(
                        path = bladePath,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFFE2E8F0)),
                            start = Offset(hubCenterX, hubCenterY),
                            end = Offset(tipX, tipY)
                        )
                    )
                }
            }

            // 4. Захисний обтічник маточини (Hub Spinner)
            drawCircle(
                color = Color(0xFF0F172A),
                radius = 10f,
                center = Offset(hubCenterX, hubCenterY)
            )
            drawCircle(
                color = Color(0xFF34D399),
                radius = 5f,
                center = Offset(hubCenterX, hubCenterY)
            )

            // 5. Основа фундаментної плити
            drawLine(
                color = Color(0xFF334155),
                start = Offset(hubCenterX - 35f, groundY),
                end = Offset(hubCenterX + 35f, groundY),
                strokeWidth = 4f
            )
        }
    }
}
