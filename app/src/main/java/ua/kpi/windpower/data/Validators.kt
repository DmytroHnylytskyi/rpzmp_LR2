package ua.kpi.windpower.data

/**
 * Утиліти клієнтської валідації вхідних даних: email, пароль, потужність ВЕС.
 */
object Validators {

    /** Шаблон формату корпоративної електронної пошти. */
    val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

    /** Мінімальна довжина паролю доступу до SCADA. */
    const val MIN_PASSWORD_LENGTH = 8

    /** Перевіряє відповідність формату email (ім'я@домен.kpi / .ua тощо). */
    fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    /**
     * Перевіряє стійкість пароля: довжина не менше [MIN_PASSWORD_LENGTH] символів,
     * обов'язкова наявність як мінімум однієї великої літери та однієї цифри.
     */
    fun isStrongPassword(password: String): Boolean =
        password.length >= MIN_PASSWORD_LENGTH &&
            password.any { it.isUpperCase() } &&
            password.any { it.isDigit() }

    /** Перевіряє коректність встановленої потужності ВЕС (строго додатнє число). */
    fun isValidCapacityMw(value: Double?): Boolean = value != null && value > 0.0

    /**
     * Обчислює SHA-256 хеш пароля (безпечне зберігання: у реєстрі диспетчерів
     * зберігається лише хеш, а не відкритий пароль).
     */
    fun sha256(value: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
