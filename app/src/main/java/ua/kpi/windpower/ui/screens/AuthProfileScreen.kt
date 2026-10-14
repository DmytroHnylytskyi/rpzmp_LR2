package ua.kpi.windpower.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.kpi.windpower.data.Validators
import androidx.lifecycle.viewmodel.compose.viewModel
import ua.kpi.windpower.data.db.repository.AuthOutcome
import ua.kpi.windpower.data.db.repository.RegistrationOutcome
import ua.kpi.windpower.ui.components.AppTextField
import ua.kpi.windpower.ui.components.LoadingIndicator
import ua.kpi.windpower.ui.components.ScreenTitle
import ua.kpi.windpower.ui.viewmodels.DispatcherAuthViewModel
import ua.kpi.windpower.ui.theme.CyanAccent
import ua.kpi.windpower.ui.theme.EmeraldLight
import ua.kpi.windpower.ui.theme.StormAlert

/**
 * Екран диспетчера: авторизація, реєстрація оперативного персоналу ВЕС та профіль інженера.
 *
 * Форми введення даних з валідацією, вибором зміни та доступом до SCADA:
 * - Текстові поля AppTextField з валідацією полів та підтримкою повідомлень про помилки;
 * - Комплексна перевірка валідності email (Regex) та складності пароля (довжина, великі літери, цифри);
 * - Селективні компоненти: Checkbox (згода з інструкцією безпеки), Switch (SMS-сповіщення), RadioButton (чергова зміна);
 * - Асинхронна корутинна обробка подання форми з відображенням LoadingIndicator;
 * - Відображення профілю чергового інженера зміни з параметрами захищеної сесії SCADA.
 */
@Composable
fun AuthProfileScreen() {
    // Сесія стартує з екрану входу: демонструється повний цикл авторизації
    var isLoggedIn by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf("Олександр К. (Диспетчер №1)") }
    var userEmail by remember { mutableStateOf("engineer@windpower.kpi.ua") }
    var userRole by remember { mutableStateOf("Головний інженер зміни ВЕС") }
    var selectedShift by remember { mutableStateOf("Зміна №1 (Денна)") }
    var isEmergencySmsEnabled by remember { mutableStateOf(true) }

    if (isLoggedIn) {
        DispatcherProfileView(
            name = userName,
            email = userEmail,
            role = userRole,
            shift = selectedShift,
            isEmergencySmsEnabled = isEmergencySmsEnabled,
            onLogout = { isLoggedIn = false }
        )
    } else {
        DispatcherAuthView(
            onAuthSuccess = { name, email, shift, smsEnabled ->
                userName = name
                userEmail = email
                selectedShift = shift
                isEmergencySmsEnabled = smsEnabled
                isLoggedIn = true
            }
        )
    }
}

/**
 * Профіль активного чергового диспетчера вітроелектростанції.
 */
@Composable
private fun DispatcherProfileView(
    name: String,
    email: String,
    role: String,
    shift: String,
    isEmergencySmsEnabled: Boolean,
    onLogout: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenTitle("Профіль чергового диспетчера")

        // Картка активної сесії інженера
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(EmeraldLight.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "⚡", fontSize = 32.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = name,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = role,
                    fontSize = 13.sp,
                    color = EmeraldLight
                )

                Text(
                    text = email,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ProfileBadge(label = "Статус сесії", value = "На зміні", color = EmeraldLight)
                    ProfileBadge(label = "Рівень SCADA", value = "Рівень 3 (Адмін)", color = CyanAccent)
                    ProfileBadge(label = "Чергування", value = shift, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        // Параметри безпеки каналів зв'язку SCADA
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ПАРАМЕТРИ ПІДКЛЮЧЕННЯ SCADA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    letterSpacing = 1.sp
                )

                ProfileInfoRow(label = "Сервер телеметрії", value = "scada.grid.internal (Онлайн)")
                ProfileInfoRow(label = "Протокол шифрування", value = "TLS 1.3 / AES-256-GCM")
                ProfileInfoRow(label = "Опитування датчиків", value = "Щойно (затримка 12 мс)")
                ProfileInfoRow(
                    label = "Аварійні сповіщення SMS",
                    value = if (isEmergencySmsEnabled) "Увімкнено (> 25 м/с)" else "Вимкнено"
                )
            }
        }

        OutlinedButton(
            onClick = onLogout,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = StormAlert)
        ) {
            Text("Завершити зміну (Вийти з облікового запису)")
        }
    }
}

/**
 * Форма авторизації та реєстрації оператора в системі SCADA.
 */
@Composable
private fun DispatcherAuthView(
    onAuthSuccess: (name: String, email: String, shift: String, smsEnabled: Boolean) -> Unit,
    viewModel: DispatcherAuthViewModel = viewModel()
) {
    var isRegisterMode by remember { mutableStateOf(false) }

    // Поля вводу
    var loginInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }

    // Компоненти вибору
    val shiftOptions = listOf("Зміна №1 (Денна)", "Зміна №2 (Нічна)", "Зміна №3 (Резервна)")
    var selectedShift by remember { mutableStateOf(shiftOptions[0]) }
    var isEmergencySmsEnabled by remember { mutableStateOf(true) }
    var isSafetyConfirmed by remember { mutableStateOf(false) }

    // Стани валідації
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var safetyError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Email-валідація виконується спільним модулем Validators 

    fun validateForm(): Boolean {
        var isValid = true
        nameError = null
        emailError = null
        passwordError = null
        confirmPasswordError = null
        safetyError = null
        generalError = null

        if (isRegisterMode) {
            if (nameInput.trim().length < 3) {
                nameError = "Вкажіть коректне ПІБ (щонайменше 3 символи)"
                isValid = false
            }

            if (!Validators.isValidEmail(emailInput)) {
                emailError = "Введіть коректну адресу корпоративного email"
                isValid = false
            }

            if (!Validators.isStrongPassword(passwordInput)) {
                passwordError = "Пароль має містити від 8 символів, велику літеру та цифру"
                isValid = false
            }

            if (passwordInput != confirmPasswordInput) {
                confirmPasswordError = "Паролі не співпадають"
                isValid = false
            }

            if (!isSafetyConfirmed) {
                safetyError = "Необхідно підтвердити ознайомлення з регламентом безпеки"
                isValid = false
            }
        } else {
            if (loginInput.isBlank()) {
                generalError = "Введіть позивний або корпоративний email"
                isValid = false
            }
            if (passwordInput.isBlank()) {
                passwordError = "Введіть пароль"
                isValid = false
            }
        }

        return isValid
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenTitle(if (isRegisterMode) "Реєстрація інженера ВЕС" else "Авторизація диспетчера")

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (isRegisterMode) "Створення облікового запису оператора" else "Вхід до робочого сеансу SCADA",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldLight
                )

                if (isRegisterMode) {
                    // ПІБ
                    AppTextField(
                        value = nameInput,
                        onValueChange = {
                            nameInput = it
                            if (nameError != null) nameError = null
                        },
                        label = "ПІБ інженера",
                        errorMessage = nameError
                    )

                    // Email (виступає логіном облікового запису)
                    AppTextField(
                        value = emailInput,
                        onValueChange = {
                            emailInput = it
                            if (emailError != null) emailError = null
                        },
                        label = "Корпоративний Email (він же логін доступу)",
                        helperText = "Формат: engineer@windpower.kpi.ua",
                        errorMessage = emailError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    // Вибір зміни (RadioButton)
                    Text(
                        text = "Чергова зміна диспетчера:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    shiftOptions.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (selectedShift == option),
                                    onClick = { selectedShift = option }
                                )
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = (selectedShift == option),
                                onClick = { selectedShift = option },
                                colors = RadioButtonDefaults.colors(selectedColor = EmeraldLight)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = option, fontSize = 13.sp)
                        }
                    }

                    // Пароль
                    AppTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            if (passwordError != null) passwordError = null
                        },
                        label = "Пароль (від 8 символів)",
                        errorMessage = passwordError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation()
                    )

                    // Підтвердження пароля
                    AppTextField(
                        value = confirmPasswordInput,
                        onValueChange = {
                            confirmPasswordInput = it
                            if (confirmPasswordError != null) confirmPasswordError = null
                        },
                        label = "Підтвердження пароля",
                        errorMessage = confirmPasswordError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation()
                    )

                    // Switch для сповіщень
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Аварійні SMS-сповіщення", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Отримувати сповіщення при штормовому вітрі > 25 м/с", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isEmergencySmsEnabled,
                            onCheckedChange = { isEmergencySmsEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldLight)
                        )
                    }

                    // Checkbox регламенту безпеки
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isSafetyConfirmed = !isSafetyConfirmed
                                if (safetyError != null) safetyError = null
                            }
                    ) {
                        Checkbox(
                            checked = isSafetyConfirmed,
                            onCheckedChange = {
                                isSafetyConfirmed = it
                                if (safetyError != null) safetyError = null
                            },
                            colors = CheckboxDefaults.colors(checkedColor = EmeraldLight)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Підтверджую проходження інструктажу з безпеки та допуску до SCADA ВЕС",
                            fontSize = 12.sp,
                            color = if (safetyError != null) StormAlert else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (safetyError != null) {
                        Text(text = safetyError!!, color = StormAlert, fontSize = 11.sp)
                    }

                } else {
                    // Логін (позивний або корпоративний email зареєстрованого інженера)
                    AppTextField(
                        value = loginInput,
                        onValueChange = {
                            loginInput = it
                            if (generalError != null) generalError = null
                        },
                        label = "Позивний або Email диспетчера"
                    )

                    // Пароль
                    AppTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            if (passwordError != null) passwordError = null
                        },
                        label = "Пароль доступу",
                        errorMessage = passwordError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation()
                    )
                }

                if (generalError != null) {
                    Text(text = generalError!!, color = StormAlert, fontSize = 12.sp)
                }

                if (isLoading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LoadingIndicator()
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isRegisterMode) "Реєстрація оператора в SCADA..." else "Авторизація сесії...",
                            fontSize = 12.sp,
                            color = EmeraldLight
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            if (validateForm()) {
                                isLoading = true
                                if (isRegisterMode) {
                                    // Реєстрація у таблиці wind_dispatchers (Room) з автоматичним входом
                                    viewModel.register(
                                        fullName = nameInput.trim(),
                                        email = emailInput.trim(),
                                        password = passwordInput,
                                        shift = selectedShift,
                                        smsAlerts = isEmergencySmsEnabled
                                    ) { outcome ->
                                        isLoading = false
                                        when (outcome) {
                                            is RegistrationOutcome.Success -> onAuthSuccess(
                                                outcome.account.fullName,
                                                outcome.account.email,
                                                outcome.account.shift,
                                                outcome.account.smsAlerts
                                            )
                                            RegistrationOutcome.Duplicate ->
                                                generalError = "Інженер з таким Email вже зареєстрований у системі"
                                            RegistrationOutcome.InvalidInput ->
                                                generalError = "Заповніть обов'язкові поля реєстрації"
                                        }
                                    }
                                } else {
                                    // Вхід лише для зареєстрованих диспетчерів: логін/email + SHA-256 пароля
                                    viewModel.authenticate(loginInput.trim(), passwordInput) { outcome ->
                                        isLoading = false
                                        when (outcome) {
                                            is AuthOutcome.Success -> onAuthSuccess(
                                                outcome.account.fullName,
                                                outcome.account.email,
                                                outcome.account.shift,
                                                outcome.account.smsAlerts
                                            )
                                            AuthOutcome.AccountNotFound ->
                                                generalError = "Диспетчера з таким позивним/email не знайдено — зареєструйтеся"
                                            AuthOutcome.WrongPassword -> generalError = "Невірний пароль доступу"
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = if (isRegisterMode) "Зареєструвати інженера" else "Увійти на зміну",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Доступ на зміну має лише зареєстрований персонал ВЕС
                Text(
                    text = "Доступ на зміну мають лише зареєстровані інженери станції. Створіть обліковий запис через реєстрацію.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                // Перемикання режимів
                TextButton(
                    onClick = {
                        isRegisterMode = !isRegisterMode
                        nameError = null
                        emailError = null
                        passwordError = null
                        confirmPasswordError = null
                        safetyError = null
                        generalError = null
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = if (isRegisterMode) "Вже маєте обліковий запис? Увійти" else "Новий інженер? Зареєструватися",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileBadge(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
