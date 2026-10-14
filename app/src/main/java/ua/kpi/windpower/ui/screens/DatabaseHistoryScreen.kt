package ua.kpi.windpower.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import ua.kpi.windpower.data.db.entities.WindStation
import ua.kpi.windpower.ui.components.AppTextField
import ua.kpi.windpower.ui.components.ConfirmDialog
import ua.kpi.windpower.ui.components.EmptyState
import ua.kpi.windpower.ui.components.InfoRow
import ua.kpi.windpower.ui.components.ScreenTitle
import ua.kpi.windpower.ui.components.SectionHeader
import ua.kpi.windpower.ui.components.StatusChip
import ua.kpi.windpower.ui.theme.EmeraldLight
import ua.kpi.windpower.ui.theme.StormAlert
import ua.kpi.windpower.ui.viewmodels.AddStationResult
import ua.kpi.windpower.ui.viewmodels.WindDatabaseViewModel
import java.util.Locale

/**
 * Локальний режим сортування списку станцій.
 */
private enum class StationSort(val label: String) {
    BY_NAME("За назвою"),
    BY_CAPACITY("За потужністю")
}

/**
 * Екран локального реляційного сховища Room Database.
 *
 * Багатотаблична структура з зовнішніми ключами та каскадним видаленням,
 * транзакційне збереження телеметрії, реактивне відображення даних через
 * Room Flow, аналітичні агрегати AVG, пошук і сортування станцій,
 * підтвердження видалення та повідомлення про результати операцій.
 */
@Composable
fun DatabaseHistoryScreen(
    onNavigateToRegisterStation: (() -> Unit)? = null,
    viewModel: WindDatabaseViewModel = viewModel()
) {
    val stations by viewModel.stations.collectAsState()
    val selectedStationId by viewModel.selectedStationId.collectAsState()
    val analytics by viewModel.analytics.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    // Поля форми додавання станції з масивами телеметрії
    var stationName by remember { mutableStateOf("") }
    var stationLocation by remember { mutableStateOf("") }
    var capacityInput by remember { mutableStateOf("") }
    var powerInput by remember { mutableStateOf("") }
    var windSpeedInput by remember { mutableStateOf("") }
    var voltageInput by remember { mutableStateOf("") }
    var attempted by remember { mutableStateOf(false) }

    // Фільтр/сортування списку станцій
    var searchQuery by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(StationSort.BY_NAME) }

    // Підтвердження видалення та повідомлення про результат операцій
    var stationToDelete by remember { mutableStateOf<WindStation?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun showSnackbar(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    // --- Валідація форми додавання  ---
    val capacityValue = capacityInput.toDoubleOrNull()
    val nameError = when {
        !attempted || stationName.isBlank() -> null
        stationName.trim().length < 3 -> "Назва має містити щонайменше 3 символи"
        else -> null
    }
    val capacityError = if (attempted && capacityInput.isNotBlank() && (capacityValue == null || capacityValue <= 0.0)) {
        "Введіть додатнє число"
    } else null

    fun parseArrayInput(raw: String): Pair<List<Double>, String?> {
        val values = raw.split(",").mapNotNull { it.trim().toDoubleOrNull() }
        val totalTokens = raw.split(",").count { it.isNotBlank() }
        return if (totalTokens > 0 && values.size != totalTokens) {
            values to "Частина значень не розпізнана як число"
        } else values to null
    }

    val (powersArray, powersError) = parseArrayInput(powerInput)
    val (speedsArray, speedsError) = parseArrayInput(windSpeedInput)
    val (voltagesArray, voltagesError) = parseArrayInput(voltageInput)
    val hasFormErrors =
        nameError != null || capacityError != null || powersError != null ||
            speedsError != null || voltagesError != null

    // --- Фільтрування та сортування списку ---
    val filteredStations = remember(stations, searchQuery, sort) {
        val matched = if (searchQuery.isBlank()) stations
        else stations.filter {
            it.name.contains(searchQuery.trim(), ignoreCase = true) ||
                it.location.contains(searchQuery.trim(), ignoreCase = true)
        }
        when (sort) {
            StationSort.BY_NAME -> matched.sortedBy { it.name }
            StationSort.BY_CAPACITY -> matched.sortedByDescending { it.installedCapacityMw }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ScreenTitle("Реляційна база даних ВЕС")
            }

            onNavigateToRegisterStation?.let { navigate ->
                item {
                    Button(
                        onClick = navigate,
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📝 Розширена реєстрація об'єкта ВЕС →")
                    }
                }
            }

            // --- Форма транзакційного додавання станції ---
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Додати нову ВЕС з масивом телеметрії",
                            style = MaterialTheme.typography.titleSmall,
                            color = EmeraldLight
                        )
                        AppTextField(
                            value = stationName,
                            onValueChange = { stationName = it },
                            label = "Назва станції",
                            helperText = "Офіційна назва (напр. «Тилігульська ВЕС»); має бути унікальною",
                            errorMessage = nameError
                        )
                        AppTextField(
                            value = stationLocation,
                            onValueChange = { stationLocation = it },
                            label = "Локація (область)",
                            helperText = "Напр. «Миколаївська обл.»"
                        )
                        AppTextField(
                            value = capacityInput,
                            onValueChange = { capacityInput = it },
                            label = "Встановлена потужність (МВт)",
                            helperText = "Додатнє число, напр. 500.0",
                            errorMessage = capacityError,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            )
                        )
                        AppTextField(
                            value = powerInput,
                            onValueChange = { powerInput = it },
                            label = "Потужності через кому (МВт)",
                            helperText = "Масив вимірювань, напр. 450.0, 420.5, 380.0",
                            errorMessage = powersError?.let { msg -> "Масив: $msg" } ?: if (attempted && powerInput.isBlank()) "Потрібен хоча б один запис" else null,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            )
                        )
                        AppTextField(
                            value = windSpeedInput,
                            onValueChange = { windSpeedInput = it },
                            label = "Швидкості вітру через кому (м/с)",
                            helperText = "Масив вимірювань, напр. 11.8, 10.5, 9.2",
                            errorMessage = speedsError?.let { msg -> "Масив: $msg" } ?: if (attempted && windSpeedInput.isBlank()) "Потрібен хоча б один запис" else null,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            )
                        )
                        AppTextField(
                            value = voltageInput,
                            onValueChange = { voltageInput = it },
                            label = "Напруги генераторів через кому (В)",
                            helperText = "Масив вимірювань, напр. 690.0, 690.0",
                            errorMessage = voltagesError?.let { msg -> "Масив: $msg" },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            )
                        )

                        Button(
                            onClick = {
                                attempted = true
                                if (!hasFormErrors && stationName.isNotBlank() &&
                                    capacityValue != null && powersArray.isNotEmpty() && speedsArray.isNotEmpty()
                                ) {
                                    viewModel.addStationWithTelemetry(
                                        name = stationName,
                                        location = stationLocation,
                                        capacity = capacityValue,
                                        powers = powersArray,
                                        speeds = speedsArray,
                                        voltages = voltagesArray
                                    ) { result ->
                                        when (result) {
                                            is AddStationResult.Success -> {
                                                // Очистка форми після успішного запису у БД
                                                stationName = ""
                                                stationLocation = ""
                                                capacityInput = ""
                                                powerInput = ""
                                                windSpeedInput = ""
                                                voltageInput = ""
                                                attempted = false
                                                showSnackbar("✓ ВЕС транзакційно записана у Room БД")
                                            }
                                            is AddStationResult.Error -> showSnackbar("✗ ${result.message}")
                                        }
                                    }
                                }
                            },
                            enabled = !isSaving,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Text(if (isSaving) "Збереження у SQLite..." else "Транзакційно записати в Room БД")
                        }
                    }
                }
            }

            // --- Фільтр та сортування ---
            item {
                SectionHeader(
                    title = "Зареєстровані станції",
                    trailing = "${filteredStations.size} з ${stations.size}"
                )
                Spacer(modifier = Modifier.height(4.dp))
                AppTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = "Пошук станції (назва або область)",
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Text
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StationSort.entries.forEach { option ->
                        FilterSortChip(
                            label = option.label,
                            isSelected = sort == option,
                            modifier = Modifier.weight(1f)
                        ) { sort = option }
                    }
                }
            }

            // --- Список станцій або Empty State (чиста база) ---
            if (filteredStations.isEmpty()) {
                item {
                    EmptyState(
                        title = if (searchQuery.isBlank() && stations.isEmpty()) {
                            "База даних порожня"
                        } else {
                            "Нічого не знайдено"
                        },
                        subtitle = if (searchQuery.isBlank() && stations.isEmpty()) {
                            "Тут зберігаються ваші вітроелектростанції. Додайте першу станцію через форму вище — дані запишуться у SQLite разом з телеметрією."
                        } else {
                            "Спробуйте змінити пошуковий запит або скинути фільтр."
                        },
                        iconText = if (searchQuery.isBlank()) "🗄️" else "🔍"
                    )
                }
            } else {
                items(filteredStations) { station ->
                    StationCard(
                        station = station,
                        isSelected = station.id == selectedStationId,
                        onClick = { viewModel.selectStation(station.id) },
                        onDeleteRequest = { stationToDelete = station }
                    )
                }

                // --- Аналітика обраної станції (SQL-агрегати) ---
                selectedStationId?.let { stationId ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Аналітика вимірювань ВЕС #$stationId (SQL-агрегація)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = EmeraldLight
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                analytics.averagePower?.let {
                                    InfoRow(
                                        "Середня вироблена потужність (AVG):",
                                        String.format(Locale.US, "%.2f МВт", it)
                                    )
                                } ?: InfoRow("Середня вироблена потужність (AVG):", "—")
                                analytics.averageSpeed?.let {
                                    InfoRow(
                                        "Середня швидкість вітру (AVG):",
                                        String.format(Locale.US, "%.2f м/с", it)
                                    )
                                } ?: InfoRow("Середня швидкість вітру (AVG):", "—")

                                Spacer(modifier = Modifier.height(8.dp))
                                HistorySection("Історія потужностей (DESC):", analytics.powers.map { it.value }, "МВт")
                                HistorySection("Історія швидкостей вітру (DESC):", analytics.speeds.map { it.value }, "м/с")
                                HistorySection("Вихідні напруги генераторів:", analytics.voltages.map { it.value }, "В")
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // Діалог підтвердження каскадного видалення станції
    stationToDelete?.let { station ->
        ConfirmDialog(
            title = "Видалити ${station.name}?",
            text = "Разом зі станцією буде каскадно видалено всі її записи телеметрії (потужності, швидкості вітру, напруги). Дію неможливо скасувати.",
            confirmLabel = "Видалити",
            onConfirm = {
                viewModel.deleteStation(station) { deleted ->
                    showSnackbar("✗ ${deleted.name} та її телеметрія видалені з БД")
                }
                stationToDelete = null
            },
            onDismiss = { stationToDelete = null }
        )
    }
}

/** Чіп вибору режиму сортування списку. */
@Composable
private fun FilterSortChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surface,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

/**
 * Картка станції зі статусом вибору та кнопкою каскадного видалення.
 */
@Composable
private fun StationCard(
    station: WindStation,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "⚡ ВЕС #${station.id}: ${station.name}",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${station.location} • ${String.format(Locale.US, "%.1f", station.installedCapacityMw)} МВт",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (isSelected) {
                    StatusChip("ОБРАНО ДЛЯ АНАЛІТИКИ", EmeraldLight)
                }
            }
            Button(
                onClick = onDeleteRequest,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = MaterialTheme.shapes.small
            ) {
                Text("Видалити", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/**
 * Секція історії вимірювань у картці аналітики.
 */
@Composable
private fun HistorySection(title: String, values: List<Double>, unit: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface
    )
    if (values.isEmpty()) {
        Text(
            text = "  • немає записів",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        values.forEach { value ->
            Text(
                text = "  • ${String.format(Locale.US, "%.2f", value)} $unit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
}
