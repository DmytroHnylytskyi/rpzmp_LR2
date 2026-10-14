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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ua.kpi.windpower.data.Validators
import ua.kpi.windpower.ui.components.AppTextField
import ua.kpi.windpower.ui.components.LoadingIndicator
import ua.kpi.windpower.ui.components.ScreenTitle
import java.util.Locale

/**
 * Екран реєстрації та диспетчерського налаштування ВЕС.
 *
 * Клієнтська валідація полів через модуль [Validators] (формат email,
 * стійкість пароля з індикатором надійності, діапазон потужності),
 * підказки та повідомлення про помилки під кожним полем, асинхронна
 * відправка форми через корутини з індикатором завантаження.
 */
@Composable
fun StationRegistrationScreen(
    onNavigateBack: (() -> Unit)? = null
) {
    var stationName by remember { mutableStateOf("") }
    var operatorEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var capacityInput by remember { mutableStateOf("") }

    var attempted by remember { mutableStateOf(false) }
    var isSubmitted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // --- Клієнтська валідація  ---
    val capacityValue = capacityInput.toDoubleOrNull()
    val nameError = if (attempted && stationName.isBlank()) "Назва станції не може бути порожньою" else null
    val emailError = when {
        !attempted || operatorEmail.isBlank() -> null
        !Validators.isValidEmail(operatorEmail) -> "Некоректний формат email (очікується ім'я@домен)"
        else -> null
    }
    val passwordError = when {
        !attempted || password.isBlank() -> null
        !Validators.isStrongPassword(password) ->
            "Пароль має містити ≥${Validators.MIN_PASSWORD_LENGTH} символів, велику літеру та цифру"
        else -> null
    }
    val capacityError = when {
        !attempted || capacityInput.isBlank() -> null
        !Validators.isValidCapacityMw(capacityValue) -> "Введіть додатнє число (наприклад 34.0)"
        else -> null
    }
    val hasErrors = nameError != null || emailError != null || passwordError != null || capacityError != null

    // Динамічний індикатор надійності пароля: 0..1
    val passwordStrength = cipherStrength(password)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top
    ) {
        if (onNavigateBack != null) {
            Button(
                onClick = onNavigateBack,
                colors = ButtonDefaults.filledTonalButtonColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text("← Повернутися до бази даних")
            }
        }

        ScreenTitle("Реєстрація нового об'єкта ВЕС")

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Назва ВЕС
                AppTextField(
                    value = stationName,
                    onValueChange = { stationName = it },
                    label = "Назва вітропарку (ВЕС)",
                    helperText = "Офіційна назва (напр. «Овідіопольська ВЕС»)",
                    errorMessage = nameError
                )

                // 2. Email чергового інженера
                AppTextField(
                    value = operatorEmail,
                    onValueChange = { operatorEmail = it },
                    label = "Email чергового інженера",
                    helperText = "Формат: engineer@windpower.kpi.ua",
                    errorMessage = emailError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                // 3. Пароль SCADA з індикатором надійності
                AppTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Пароль доступу до SCADA",
                    helperText = if (password.isBlank()) {
                        "Мін. ${Validators.MIN_PASSWORD_LENGTH} символів, велика літера та цифра"
                    } else null,
                    errorMessage = passwordError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation()
                )
                if (password.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = { passwordStrength },
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp),
                            color = when {
                                passwordStrength >= 1f -> MaterialTheme.colorScheme.primary
                                passwordStrength >= 0.66f -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.error
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                        Text(
                            text = when {
                                passwordStrength >= 1f -> "Надійний"
                                passwordStrength >= 0.66f -> "Середній"
                                else -> "Слабкий"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 4. Номінальна потужність
                AppTextField(
                    value = capacityInput,
                    onValueChange = { capacityInput = it },
                    label = "Встановлена потужність (МВт)",
                    helperText = "Сумарна номінальна потужність генераторів (напр. 34.0)",
                    errorMessage = capacityError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                attempted = true
                if (!hasErrors) {
                    // Асинхронна імітація верифікації та реєстрації об'єкта в системі
                    scope.launch {
                        isLoading = true
                        delay(1200)
                        isLoading = false
                        isSubmitted = true
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(if (isLoading) "Йде верифікація..." else "Зареєструвати ВЕС")
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            LoadingIndicator()
        }

        if (isSubmitted) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "✓ ВЕС успішно зареєстрована!",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Станція «${stationName.trim()}» (${
                            String.format(Locale.US, "%.1f", capacityValue ?: 0.0)
                        } МВт) підключена до диспетчерського контуру.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

/**
 * Оцінка надійності пароля 0..1 за критеріями Validators:
 * довжина >= 8, наявність великої літери та цифри.
 */
private fun cipherStrength(password: String): Float {
    if (password.isEmpty()) return 0f
    var criteria = 0
    if (password.length >= Validators.MIN_PASSWORD_LENGTH) criteria++
    if (password.any { it.isUpperCase() }) criteria++
    if (password.any { it.isDigit() }) criteria++
    // Частковий зачіт довжини для проміжних значень
    val lengthProgress = (password.length.toFloat() / Validators.MIN_PASSWORD_LENGTH).coerceAtMost(1f)
    return ((criteria / 3f) * 0.75f + lengthProgress * 0.25f).coerceIn(0f, 1f)
}
