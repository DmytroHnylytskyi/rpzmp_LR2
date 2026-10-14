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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ua.kpi.windpower.data.TurbineFleet
import ua.kpi.windpower.data.TurbineModel
import ua.kpi.windpower.data.TurbineStatus
import ua.kpi.windpower.ui.components.AppTextField
import ua.kpi.windpower.ui.components.ConfirmDialog
import ua.kpi.windpower.ui.components.ScreenTitle
import ua.kpi.windpower.ui.components.StatCard
import ua.kpi.windpower.ui.components.StatusChip
import ua.kpi.windpower.ui.theme.AmberWarning
import ua.kpi.windpower.ui.theme.CyanAccent
import ua.kpi.windpower.ui.theme.EmeraldLight
import ua.kpi.windpower.ui.theme.MaintenanceSlate
import ua.kpi.windpower.ui.theme.StormAlert
import java.util.Locale

/**
 * Екран диспетчерського керування парком вітрогенераторів.
 *
 * Надає деталізовану інформацію щодо кожного агрегату вітропарку та повний
 * життєвий цикл установок: додавання нових турбін, редагування паспортних
 * даних (назва, модель, номінал, довжина лопаті, режим ТО) та каскадне
 * видалення з підтвердженням.
 *
 * Телеметрія береться зі спільного джерела [TurbineFleet] — того самого,
 * що використовує оперативний дашборд, тому зміна складу парку миттєво
 * відображається на всіх екранах застосунку.
 */
@Composable
fun TurbineFleetScreen(
    onNavigateBack: (() -> Unit)? = null
) {
    // Телеметрія парку при номінальній швидкості вітру дашборда (9.5 м/с).
    // Без remember: читання активного складу [TurbineFleet.turbines] оновлює
    // композицію після кожної CRUD-операції диспетчера.
    val turbines = TurbineFleet.resolveTelemetry(9.5f)
    val online = TurbineFleet.onlineCount(turbines)
    val totalOnlineMw = TurbineFleet.totalPowerMw(turbines)

    // Діалоги CRUD-операцій парку
    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<TurbineModel?>(null) }
    var deleteTarget by remember { mutableStateOf<TurbineModel?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenTitle("Парк вітроустановок")
        }
        item {
            // Зведений статус вітропарку
            StatCard(
                title = "Диспетчерський профіль парку",
                value = String.format(Locale.US, "%.2f", totalOnlineMw),
                unit = "МВт зараз",
                caption = "${turbines.size} турбін встановлено • " +
                    "номінал парку ${String.format(Locale.US, "%.2f", TurbineFleet.totalRatedMw())} МВт",
                badge = { StatusChip(text = "В МЕРЕЖІ: $online / ${turbines.size}", color = CyanAccent) }
            )
        }
        item {
            Button(
                onClick = { showAddDialog = true },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("＋ Додати вітроустановку до парку")
            }
        }

        items(turbines) { turbine ->
            TurbineCard(
                turbine = turbine,
                onEdit = { editTarget = turbine },
                onDelete = { deleteTarget = turbine }
            )
        }
    }

    if (showAddDialog) {
        TurbineEditDialog(
            initial = null,
            onDismiss = { showAddDialog = false }
        )
    }

    editTarget?.let { target ->
        TurbineEditDialog(
            initial = target,
            onDismiss = { editTarget = null }
        )
    }

    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "Видалити ${target.name}?",
            text = "Вітроустановку буде вилучено з диспетчерського парку. Її телеметрію буде зупинено, а паспортні дані — втрачено. Дію неможливо скасувати.",
            confirmLabel = "Видалити",
            onConfirm = {
                TurbineFleet.deleteTurbine(target.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

/**
 * Картка індивідуального вітрогенератора зі шкалою навантаження, телеметрією
 * та кнопками редагування/видалення паспортних даних.
 */
@Composable
private fun TurbineCard(
    turbine: TurbineModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = (turbine.currentPowerKw / turbine.ratedPowerKw).toFloat().coerceIn(0f, 1f)
    val statusColor = when (turbine.status) {
        TurbineStatus.OPERATING -> EmeraldLight
        TurbineStatus.STANDBY -> AmberWarning
        TurbineStatus.EMERGENCY_BRAKE -> StormAlert
        TurbineStatus.MAINTENANCE -> MaintenanceSlate
        TurbineStatus.OPTIMIZING -> CyanAccent
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = turbine.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${turbine.model} • Лопать: ${turbine.bladeLengthMeters} м",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                StatusChip(text = turbine.status.title.uppercase(), color = statusColor)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Шкала відносної генерації від паспортного номіналу
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${turbine.currentPowerKw.toInt()} кВт",
                    style = MaterialTheme.typography.titleSmall,
                    color = EmeraldLight
                )
                Text(
                    text = "Номінал: ${turbine.ratedPowerKw.toInt()} кВт",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = EmeraldLight,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    DetailParam("Вітер", String.format(Locale.US, "%.1f м/с", turbine.currentSpeedMps))
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    DetailParam("Оберти", String.format(Locale.US, "%.1f об/хв", turbine.rpm))
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    DetailParam("Pitch-кут", String.format(Locale.US, "%.1f°", turbine.pitchAngleDeg))
                }
                Row {
                    OutlinedButton(
                        onClick = onEdit,
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.secondary
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("✎", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = onDelete,
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("🗑", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

/**
 * Діалог додавання або редагування паспортних даних вітроустановки (CRUD).
 *
 * Валідація: назва не порожня та унікальна у парку, модель не порожня,
 * номінал і довжина лопаті — додатні числа. Перемикач переводить установку
 * в режим технічного обслуговування (ротор зупиняється, лопаті флюгуються).
 */
@Composable
private fun TurbineEditDialog(
    initial: TurbineModel?,
    onDismiss: () -> Unit
) {
    var nameInput by remember { mutableStateOf(initial?.name ?: "") }
    var modelInput by remember { mutableStateOf(initial?.model ?: "") }
    var ratedInput by remember {
        mutableStateOf(initial?.ratedPowerKw?.let { String.format(Locale.US, "%.1f", it) } ?: "")
    }
    var bladeInput by remember {
        mutableStateOf(initial?.bladeLengthMeters?.let { String.format(Locale.US, "%.1f", it) } ?: "")
    }
    var inMaintenance by remember { mutableStateOf(initial?.isInMaintenance ?: false) }
    var attempted by remember { mutableStateOf(false) }

    fun parsePositive(raw: String): Double? = raw.toDoubleOrNull()?.takeIf { it > 0.0 }

    val nameError = when {
        !attempted || nameInput.isNotBlank() -> null
        else -> "Вкажіть назву установки"
    }
    val uniqueNameError = if (attempted && nameInput.isNotBlank()) {
        when {
            initial != null && nameInput.trim().equals(initial.name, ignoreCase = true) -> null
            TurbineFleet.turbines.any { it.name.equals(nameInput.trim(), ignoreCase = true) } ->
                "Установка з такою назвою вже є в парку"
            else -> null
        }
    } else null
    val modelError = if (attempted && modelInput.isBlank()) "Вкажіть модель турбіни" else null
    val ratedError = if (attempted && parsePositive(ratedInput) == null) "Номінал має бути додатнім числом" else null
    val bladeError = if (attempted && parsePositive(bladeInput) == null) "Довжина лопаті має бути додатнім числом" else null
    val hasErrors = nameError != null || uniqueNameError != null || modelError != null ||
        ratedError != null || bladeError != null

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = if (initial == null) "Нова вітроустановка парку" else "Редагування ${initial.name}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AppTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = "Диспетчерська назва (напр. ВЕС-06 «Слог»)",
                    errorMessage = nameError ?: uniqueNameError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                AppTextField(
                    value = modelInput,
                    onValueChange = { modelInput = it },
                    label = "Марка та модель турбіни",
                    helperText = "Напр. Vestas V150-4.2MW",
                    errorMessage = modelError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                AppTextField(
                    value = ratedInput,
                    onValueChange = { ratedInput = it },
                    label = "Номінальна потужність (кВт)",
                    helperText = "Паспортна потужність генератора, напр. 4200.0",
                    errorMessage = ratedError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                AppTextField(
                    value = bladeInput,
                    onValueChange = { bladeInput = it },
                    label = "Довжина лопаті / радіус ротора (м)",
                    helperText = "Напр. 73.0",
                    errorMessage = bladeError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Технічне обслуговування",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Вивести установку з генерації (флюгування лопатей)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = inMaintenance,
                        onCheckedChange = { inMaintenance = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldLight)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    attempted = true
                    val rated = parsePositive(ratedInput)
                    val blade = parsePositive(bladeInput)
                    if (!hasErrors && rated != null && blade != null && nameInput.isNotBlank() && modelInput.isNotBlank()) {
                        val saved = if (initial == null) {
                            TurbineFleet.addTurbine(
                                name = nameInput,
                                model = modelInput,
                                ratedPowerKw = rated,
                                bladeLengthMeters = blade,
                                isInMaintenance = inMaintenance
                            )
                        } else {
                            TurbineFleet.updateTurbine(
                                initial.copy(
                                    name = nameInput,
                                    model = modelInput,
                                    ratedPowerKw = rated,
                                    bladeLengthMeters = blade,
                                    isInMaintenance = inMaintenance
                                )
                            )
                        }
                        if (saved) onDismiss()
                    }
                }
            ) {
                Text(if (initial == null) "Додати" else "Зберегти")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

/**
 * Елемент відображення технічного параметра турбіни.
 */
@Composable
private fun DetailParam(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
