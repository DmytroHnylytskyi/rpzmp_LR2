package ua.kpi.windpower.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ua.kpi.windpower.data.TurbineFleet
import ua.kpi.windpower.data.WindPowerCalculator
import ua.kpi.windpower.ui.components.StatCard
import ua.kpi.windpower.ui.components.StatusChip
import ua.kpi.windpower.ui.components.StatusDot
import ua.kpi.windpower.ui.components.TelemetryMetricCard
import ua.kpi.windpower.ui.components.WindTurbineVisualizer
import ua.kpi.windpower.ui.theme.AmberWarning
import ua.kpi.windpower.ui.theme.CyanAccent
import ua.kpi.windpower.ui.theme.EmeraldLight
import ua.kpi.windpower.ui.theme.StormAlert
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Екран оперативного моніторингу та телеметрії ВЕС.
 *
 * Відображає:
 * - сумарну миттєву потужність вітропарку з 5 установок (МВт) — розрахунок
 *   за фактичними паспортними даними кожної турбіни з [TurbineFleet];
 * - анімовану векторну модель головної турбіни з синхронізованою швидкістю обертання ротора;
 * - ключові телеметричні показники (швидкість вітру, оберти ротора, кут атаки);
 * - інтерактивну панель симуляції вітру та швидкі метеорологічні пресети;
 * - автоматичний інженерний статус режиму роботи обладнання.
 */
@Composable
fun DashboardScreen(
    onNavigateToFleet: (() -> Unit)? = null,
    onNavigateToCalculator: (() -> Unit)? = null
) {
    var windSpeed by remember { mutableFloatStateOf(9.5f) }
    val calculator = remember { WindPowerCalculator() }

    // Телеметрія усього парку з фактичних паспортних даних установок (ВЕС-01..05+).
    // Читання активного складу TurbineFleet.turbines підписує композицію на зміни:
    // додавання/редагування/видалення турбін миттєво перераховує телеметрію.
    val telemetry = TurbineFleet.resolveTelemetry(windSpeed, calculator)
    val totalParkPowerMw = TurbineFleet.totalPowerMw(telemetry)
    val onlineCount = TurbineFleet.onlineCount(telemetry)
    val leadTurbine = telemetry.first()

    // Плавна анімація числового значення потужності парку
    val animatedParkPower by animateFloatAsState(
        targetValue = totalParkPowerMw.toFloat(),
        animationSpec = tween(durationMillis = 500),
        label = "ParkPowerAnimation"
    )

    // Динамічний колір системного статусу
    val statusColor by animateColorAsState(
        targetValue = when {
            windSpeed < WindPowerCalculator.CUT_IN_MPS -> AmberWarning
            windSpeed > WindPowerCalculator.CUT_OUT_MPS -> StormAlert
            else -> EmeraldLight
        },
        label = "OperatingStatusColorAnimation"
    )

    val statusLabel = when {
        windSpeed < WindPowerCalculator.CUT_IN_MPS -> "ШТИЛЬ"
        windSpeed > WindPowerCalculator.CUT_OUT_MPS -> "ШТОРМ"
        else -> "ГЕНЕРАЦІЯ"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Героїчна картка сумарної потужності вітропарку
        StatCard(
            title = "Сумарна потужність парку",
            value = String.format(Locale.US, "%.2f", animatedParkPower),
            unit = "МВт",
            accent = EmeraldLight,
            badge = { StatusChip(text = statusLabel, color = statusColor) },
            caption = buildString {
                append("В мережі: ")
                append(onlineCount)
                append(" з ")
                append(TurbineFleet.turbines.size)
                append(" установок • номінал парку ")
                append(String.format(Locale.US, "%.2f", TurbineFleet.totalRatedMw()))
                append(" МВт")
            }
        )

        // 2. Векторна модель головної турбіни з живим обертанням ротора
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${leadTurbine.name} (${leadTurbine.model.replace("-3.0MW", " / 3.0 MW")})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                WindTurbineVisualizer(
                    windSpeed = windSpeed,
                    modifier = Modifier.size(200.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Сітка ключових телеметричних параметрів головної установки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    TelemetryMetricCard(
                        label = "Швидкість вітру",
                        value = String.format(Locale.US, "%.1f м/с", leadTurbine.currentSpeedMps),
                        accentColor = CyanAccent
                    )
                    TelemetryMetricCard(
                        label = "Оберти ротора",
                        value = String.format(Locale.US, "%.1f об/хв", leadTurbine.rpm),
                        accentColor = EmeraldLight
                    )
                    TelemetryMetricCard(
                        label = "Кут атаки",
                        value = String.format(Locale.US, "%.1f°", leadTurbine.pitchAngleDeg),
                        accentColor = MaterialTheme.colorScheme.tertiary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = leadTurbine.status.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 3. Інтерактивне регулювання вітрового потоку
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Симуляція поривів вітру",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f м/с", windSpeed),
                        style = MaterialTheme.typography.titleMedium,
                        color = CyanAccent
                    )
                }
                Slider(
                    value = windSpeed,
                    onValueChange = { windSpeed = it },
                    valueRange = 0f..30f,
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldLight,
                        activeTrackColor = EmeraldLight,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WeatherPresetButton(
                        title = "Штиль (1.5)",
                        isSelected = windSpeed == 1.5f,
                        onClick = { windSpeed = 1.5f },
                        modifier = Modifier.weight(1f)
                    )
                    WeatherPresetButton(
                        title = "Бриз (7.0)",
                        isSelected = windSpeed == 7.0f,
                        onClick = { windSpeed = 7.0f },
                        modifier = Modifier.weight(1f)
                    )
                    WeatherPresetButton(
                        title = "Номінал (12.5)",
                        isSelected = windSpeed == 12.5f,
                        onClick = { windSpeed = 12.5f },
                        modifier = Modifier.weight(1f)
                    )
                    WeatherPresetButton(
                        title = "Шторм (27.0)",
                        isSelected = windSpeed == 27.0f,
                        onClick = { windSpeed = 27.0f },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. Експлуатаційний стан систем авторегулювання та захисту
        Card(
            colors = CardDefaults.cardColors(
                containerColor = statusColor.copy(alpha = 0.12f)
            ),
            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusDot(color = statusColor)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = calculator.getOperatingStatus(windSpeed.toString()),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 5. Швидкі переходи до модулів парку турбін та калькулятора
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onNavigateToFleet?.invoke() },
                enabled = onNavigateToFleet != null,
                colors = ButtonDefaults.filledTonalButtonColors(),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.weight(1f)
            ) {
                Text("🌀 Парк турбін")
            }
            Button(
                onClick = { onNavigateToCalculator?.invoke() },
                enabled = onNavigateToCalculator != null,
                colors = ButtonDefaults.filledTonalButtonColors(),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.weight(1f)
            ) {
                Text("📐 Закон Беца")
            }
        }
    }
}

/**
 * Кнопка швидкого вибору погодного пресету вітрового потоку.
 */
@Composable
private fun WeatherPresetButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}
