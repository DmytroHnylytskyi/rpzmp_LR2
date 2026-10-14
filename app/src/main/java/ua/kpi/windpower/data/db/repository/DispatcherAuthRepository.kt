package ua.kpi.windpower.data.db.repository

import ua.kpi.windpower.data.Validators
import ua.kpi.windpower.data.db.dao.WindDispatcherDao
import ua.kpi.windpower.data.db.entities.DispatcherAccount

/**
 * Результат автентифікації диспетчера (вхід на зміну).
 */
sealed interface AuthOutcome {
    /** Вхід виконано — повертає обліковий запис інженера. */
    data class Success(val account: DispatcherAccount) : AuthOutcome

    /** Облікового запису з таким логіном/email не існує. */
    data object AccountNotFound : AuthOutcome

    /** Пароль не відповідає обліковому запису. */
    data object WrongPassword : AuthOutcome
}

/**
 * Результат реєстрації нового диспетчера.
 */
sealed interface RegistrationOutcome {
    /** Реєстрація успішна — повертає створений обліковий запис. */
    data class Success(val account: DispatcherAccount) : RegistrationOutcome

    /** Логін/email уже зайнятий іншим інженером. */
    data object Duplicate : RegistrationOutcome

    /** Порожні обов'язкові поля. */
    data object InvalidInput : RegistrationOutcome
}

/**
 * Репозиторій облікових записів диспетчерів.
 * Автентифікація виконується за таблицею wind_dispatchers локальної БД Room;
 * пароль перевіряється за SHA-256 хешем.
 */
class DispatcherAuthRepository(private val dao: WindDispatcherDao) {

    /**
     * Автентифікація за логіном (позивним або email) та паролем.
     */
    suspend fun authenticate(loginInput: String, password: String): AuthOutcome {
        if (loginInput.isBlank() || password.isBlank()) return AuthOutcome.AccountNotFound
        val account = dao.findByLogin(loginInput.trim()) ?: return AuthOutcome.AccountNotFound
        return if (account.passwordHash == Validators.sha256(password)) {
            AuthOutcome.Success(account)
        } else {
            AuthOutcome.WrongPassword
        }
    }

    /**
     * Реєструє нового інженера зміни.
     *
     * Логіном виступає корпоративний email; пароль зберігається лише як SHA-256 хеш.
     */
    suspend fun register(
        fullName: String,
        email: String,
        password: String,
        shift: String,
        smsAlerts: Boolean
    ): RegistrationOutcome {
        val trimmedEmail = email.trim()
        if (fullName.isBlank() || trimmedEmail.isBlank() || password.isBlank()) {
            return RegistrationOutcome.InvalidInput
        }
        if (dao.countByLogin(trimmedEmail) > 0) {
            return RegistrationOutcome.Duplicate
        }
        val account = DispatcherAccount(
            login = trimmedEmail,
            fullName = fullName.trim(),
            email = trimmedEmail,
            shift = shift,
            smsAlerts = smsAlerts,
            passwordHash = Validators.sha256(password)
        )
        dao.insertDispatcher(account)
        return RegistrationOutcome.Success(account)
    }

    /** Кількість зареєстрованих диспетчерів у базі. */
    suspend fun registeredCount(): Int = dao.countAll()
}
