package ua.kpi.windpower.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.kpi.windpower.data.WindPowerCalculator
import ua.kpi.windpower.ui.components.AppTextField
import ua.kpi.windpower.ui.theme.CyanAccent
import ua.kpi.windpower.ui.theme.EmeraldLight
import java.util.Locale
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Екран інженерного калькулятора аеродинамічного моделювання ВЕС.
 *
 * Дозволяє інженеру або студенту розрахувати прогнозовану електричну та механічну
 * потужність вітрогенератора за вхідними фізичними параметрами:
 * - швидкістю вітрового потоку v (м/с);
 * - довжиною лопаті / радіусом ротора R (м);
 * - аеродинамічним коефіцієнтом потужності Cp (до межі Беца 59.3%).
 *
 * Бізнес-логіка розрахунків повністю винесена у модуль [WindPowerCalculator].
 */
@Composable
fun CalculatorScreen(
    onNavigateBack: (() -> Unit)? = null
) {
    var windSpeed by remember { mutableStateOf("11.5") }
    var bladeRadius by remember { mutableStateOf("54.0") }
    var cpEfficiency by remember { mutableStateOf("43.5") }

    val calculator = remember { WindPowerCalculator() }

    val speedVal = windSpeed.toDoubleOrNull()
    val radiusVal = bladeRadius.toDoubleOrNull()
    val cpVal = cpEfficiency.toDoubleOrNull()

    // Валідація вхідних параметрів інженерної моделі
    val speedError = when {
        speedVal == null -> "Введіть числове значення швидкості вітру"
        speedVal < WindPowerCalculator.CUT_IN_MPS || speedVal > WindPowerCalculator.CUT_OUT_MPS ->
            "Поза робочим діапазоном (Cut-in 3.0 — Cut-out 25.0 м/с)"
        else -> null
    }
    val radiusError = when {
        radiusVal == null -> "Введіть числове значення радіуса ротора"
        radiusVal <= 0.0 -> "Радіус має бути більшим за 0"
        else -> null
    }
    val cpError = when {
        cpVal == null -> "Введіть числове значення коефіцієнта Cp, %"
        cpVal <= 0.0 || cpVal > 59.3 -> "Допустимий діапазон: 0 < Cp ≤ 59.3% (теоретична межа Беца)"
        else -> null
    }
    val hasErrors = speedError != null || radiusError != null || cpError != null

    // Розрахунок площі ометання ротора: S = π * R² (м²)
    val sweptArea = if (radiusVal != null && radiusVal > 0) PI * radiusVal.pow(2) else 0.0

    // Розрахунок виробленої потужності (кВт) за законом Беца
    val powerKw = if (hasErrors) 0.0 else calculator.calculatePowerAdvanced(windSpeed, bladeRadius, cpEfficiency)
    val powerMw = (powerKw / 1000.0 * 100).roundToInt() / 100.0

    // Прогноз добового виробітку енергії: E = P * 24 год (МВт·год)
    val dailyEnergyMwh = ((powerMw * 24.0) * 10).roundToInt() / 10.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (onNavigateBack != null) {
            Button(
                onClick = onNavigateBack,
                colors = ButtonDefaults.filledTonalButtonColors(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("← Повернутися до моніторингу")
            }
        }

        // Картка фізичної моделі та формули Беца
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ФІЗИЧНА МОДЕЛЬ БЕЦА",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "P = ½ • ρ • S • v³ • Cp",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ρ = 1.225 кг/м³ (густина повітря) • S = πR² (площа ометання) • v — швидкість вітру • Cp — аеродинамічний коефіцієнт (макс. 59.3%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // Швидкі пресети типових промислових турбін
        Column {
            Text(
                text = "Типові конфігурації промислових ВЕУ",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresetChip(label = "2.0 МВт\nR=45 м", modifier = Modifier.weight(1f)) {
                    bladeRadius = "45.0"; cpEfficiency = "42.0"
                }
                PresetChip(label = "3.4 МВт\nR=54 м", modifier = Modifier.weight(1f)) {
                    bladeRadius = "54.0"; cpEfficiency = "43.5"
                }
                PresetChip(label = "5.0 МВт\nR=68 м", modifier = Modifier.weight(1f)) {
                    bladeRadius = "68.0"; cpEfficiency = "46.0"
                }
            }
        }

        // Поля введення інженерних параметрів
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AppTextField(
                    value = windSpeed,
                    onValueChange = { windSpeed = it },
                    label = "Швидкість вітрового потоку v (м/с)",
                    helperText = "Робочий діапазон: 3.0 — 25.0 м/с",
                    errorMessage = speedError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                AppTextField(
                    value = bladeRadius,
                    onValueChange = { bladeRadius = it },
                    label = "Радіус вітрового колеса R (м)",
                    helperText = "Довжина лопаті ротора, метри",
                    errorMessage = radiusError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                AppTextField(
                    value = cpEfficiency,
                    onValueChange = { cpEfficiency = it },
                    label = "Коефіцієнт потужності Cp (%)",
                    helperText = "Максимум — теоретична межа Беца 59.3%",
                    errorMessage = cpError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        }

        // Картка результатів моделювання
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (hasErrors) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surface
            ),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "РЕЗУЛЬТАТ РОЗРАХУНКУ",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.sp,
                    color = CyanAccent
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (hasErrors) {
                    Text(
                        text = "Виправте параметри введення, щоб виконати розрахунок",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ResultRowItem(label = "Генерація", value = "$powerKw кВт", highlight = true)
                        ResultRowItem(label = "У мегаватах", value = String.format(Locale.US, "%.2f МВт", powerMw), highlight = true)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ResultRowItem(
                            label = "Площа ометання S",
                            value = "${(sweptArea * 10).roundToInt() / 10.0} м²"
                        )
                        ResultRowItem(
                            label = "Добовий виробіток",
                            value = String.format(Locale.US, "%.1f МВт·год", dailyEnergyMwh)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = calculator.getOperatingStatus(windSpeed),
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldLight
                    )
                }
            }
        }
    }
}

/**
 * Чіп попередньо визначеної конфігурації генератора.
 */
@Composable
private fun PresetChip(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

/**
 * Рядок результату розрахунку з назвою показника та значенням.
 */
@Composable
private fun ResultRowItem(label: String, value: String, highlight: Boolean = false) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (highlight) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleSmall,
            color = if (highlight) EmeraldLight else MaterialTheme.colorScheme.onSurface
        )
    }
}
