package ua.kpi.windpower

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.kpi.windpower.data.Validators
import ua.kpi.windpower.data.db.dao.WindDispatcherDao
import ua.kpi.windpower.data.db.entities.DispatcherAccount
import ua.kpi.windpower.data.db.repository.AuthOutcome
import ua.kpi.windpower.data.db.repository.DispatcherAuthRepository
import ua.kpi.windpower.data.db.repository.RegistrationOutcome

/**
 * In-memory стаб DAO диспетчерів (імітат таблиці wind_dispatchers Room)
 * для чистого JVM-тестування логіки доступа без Android.
 */
private class InMemoryWindDispatcherDao : WindDispatcherDao {

    private val accounts = mutableListOf<DispatcherAccount>()

    override suspend fun insertDispatcher(account: DispatcherAccount): Long {
        require(accounts.none { it.login.equals(account.login, ignoreCase = true) }) {
            "UNIQUE constraint failed: wind_dispatchers.login"
        }
        val newId = (accounts.maxOfOrNull { it.id } ?: 0) + 1
        accounts.add(account.copy(id = newId))
        return newId.toLong()
    }

    override suspend fun findByLogin(login: String): DispatcherAccount? =
        accounts.firstOrNull { it.login.equals(login, ignoreCase = true) }

    override suspend fun countByLogin(login: String): Int =
        accounts.count { it.login.equals(login, ignoreCase = true) }

    override suspend fun countAll(): Int = accounts.size
}

/**
 * Модульні тести автентифікації диспетчерів у локальній БД.
 * Незареєстрований користувач не може увійти; пароль перевіряється за SHA-256.
 */
class DispatcherAuthRepositoryTest {

    private val repository = DispatcherAuthRepository(InMemoryWindDispatcherDao())

    @Test
    fun testAuth6_Register_thenAuthenticate() = runBlocking {
        val registration = repository.register(
            fullName = "Олена Ковальчук",
            email = "olena@windpower.kpi.ua",
            password = "ScadaPass1",
            shift = "Зміна №2 (Нічна)",
            smsAlerts = false
        )
        assertTrue(registration is RegistrationOutcome.Success)

        val login = repository.authenticate("olena@windpower.kpi.ua", "ScadaPass1")
        assertTrue(login is AuthOutcome.Success)
        assertEquals(
            "Олена Ковальчук",
            (login as AuthOutcome.Success).account.fullName
        )
        assertEquals("Зміна №2 (Нічна)", login.account.shift)
    }

    @Test
    fun testAuth6_UnknownLogin_cannotEnterShift() = runBlocking {
        assertEquals(
            AuthOutcome.AccountNotFound,
            repository.authenticate("ghost@windpower.kpi.ua", "NoMatter123")
        )
    }

    @Test
    fun testAuth6_WrongPassword_cannotEnterShift() = runBlocking {
        repository.register(
            fullName = "Олена Ковальчук",
            email = "olena@windpower.kpi.ua",
            password = "ScadaPass1",
            shift = "Зміна №1 (Денна)",
            smsAlerts = true
        )
        assertEquals(
            AuthOutcome.WrongPassword,
            repository.authenticate("olena@windpower.kpi.ua", "DifferentPass1")
        )
    }

    @Test
    fun testAuth6_DuplicateLogin_isRejected() = runBlocking {
        repository.register(
            fullName = "Перший Інженер",
            email = "same@windpower.kpi.ua",
            password = "Abcdefgh1",
            shift = "Зміна №1 (Денна)",
            smsAlerts = true
        )
        assertEquals(
            RegistrationOutcome.Duplicate,
            repository.register(
                fullName = "Другий Інженер",
                email = "SAME@windpower.kpi.ua", // без урахування регістру
                password = "Xyz98765",
                shift = "Зміна №3 (Резервна)",
                smsAlerts = false
            )
        )
    }
}
