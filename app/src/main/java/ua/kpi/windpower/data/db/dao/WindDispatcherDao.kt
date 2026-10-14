package ua.kpi.windpower.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ua.kpi.windpower.data.db.entities.DispatcherAccount

/**
 * DAO облікових записів диспетчерів (таблиця wind_dispatchers).
 *
 * Забезпечує автентифікацію: вхід на зміну можливий лише для
 * зареєстрованих інженерів. Логін унікальний (унікальний індекс у БД).
 */
@Dao
interface WindDispatcherDao {

    /** Вставляє обліковий запис; повертає rowId або кидає виключення при дублікаті логіна. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDispatcher(account: DispatcherAccount): Long

    /** Шукає обліковий запис за логіном (без урахування регістру). */
    @Query("SELECT * FROM wind_dispatchers WHERE login = :login COLLATE NOCASE LIMIT 1")
    suspend fun findByLogin(login: String): DispatcherAccount?

    /** Кількість записів з таким логіном (перевірка унікальності при реєстрації). */
    @Query("SELECT COUNT(id) FROM wind_dispatchers WHERE login = :login COLLATE NOCASE")
    suspend fun countByLogin(login: String): Int

    /** Загальна кількість зареєстрованих диспетчерів. */
    @Query("SELECT COUNT(id) FROM wind_dispatchers")
    suspend fun countAll(): Int
}
