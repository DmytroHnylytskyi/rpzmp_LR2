package ua.kpi.windpower.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ua.kpi.windpower.ui.components.InfoRow
import ua.kpi.windpower.ui.components.ScreenTitle
import ua.kpi.windpower.ui.theme.EmeraldLight
import ua.kpi.windpower.ui.theme.StormAlert
import ua.kpi.windpower.ui.viewmodels.WindTelemetryViewModel
import java.util.Locale

/**
 * Екран оперативного моніторингу ВЕС на базі архітектури MVVM.
 *
 * Демонструє реактивну взаємодію ViewModel зі StateFlow:
 * - асинхронне опитування давачів через viewModelScope (стан isLoading);
 * - стан помилки телеметричного каналу з повторним запитом (стан error);
 * - підписка інтерфейсу на зміни через collectAsState().
 */
@Composable
fun TelemetryMvvmScreen(
    viewModel: WindTelemetryViewModel = viewModel()
) {
    val power by viewModel.power.collectAsState()
    val windSpeed by viewModel.windSpeed.collectAsState()
    val status by viewModel.status.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top
    ) {
        ScreenTitle("SCADA Телеметрія ВЕС")

        // Головна картка телеметрії (стан завантаження / помилка / дані)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                when {
                    isLoading -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Опитування польових давачів вітропарку...",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    error != null -> {
                        // Стан помилки: канал зв'язку з давачем не відповів
                        Text(
                            text = "⚠ ПОМИЛКА ТЕЛЕМЕТРІЇ",
                            style = MaterialTheme.typography.labelSmall,
                            color = StormAlert
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "На екрані відображаються останні достовірні показники (SCADA утримує останий зріз).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        Text(
                            text = "Поточна швидкість вітру:",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f м/с", windSpeed),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Сумарна активна потужність 5-ти ВЕУ:",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f МВт", power),
                            style = MaterialTheme.typography.headlineLarge,
                            color = EmeraldLight
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Експлуатаційний статус SCADA:",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = status,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Фізичні параметри аеродинамічного контуру:",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(6.dp))
                InfoRow("Пускова швидкість (Cut-in):", "3.0 м/с")
                InfoRow("Номінальна швидкість:", "12.0 м/с")
                InfoRow("Штормовий захист (Cut-out):", "25.0 м/с")
                InfoRow("Теоретична межа Беца:", "59.3 %")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { viewModel.refreshTelemetry() },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = if (error != null) {
                // Кнопка retry у стані помилки — акцентована на аварійний колір
                ButtonDefaults.buttonColors(
                    containerColor = StormAlert,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            } else {
                ButtonDefaults.buttonColors()
            }
        ) {
            Text(
                when {
                    isLoading -> "Йде зчитування..."
                    error != null -> "Повторити опитування давачів"
                    else -> "Опитати польові контролери SCADA"
                }
            )
        }
    }
}
