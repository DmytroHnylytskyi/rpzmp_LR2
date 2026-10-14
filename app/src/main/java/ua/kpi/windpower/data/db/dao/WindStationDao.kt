package ua.kpi.windpower.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ua.kpi.windpower.data.db.entities.WindPowerRecord
import ua.kpi.windpower.data.db.entities.WindSpeedRecord
import ua.kpi.windpower.data.db.entities.WindStation
import ua.kpi.windpower.data.db.entities.WindVoltageRecord

/**
 * Data Access Object (DAO) для керування базою даних вітропарків Room.
 * Забезпечує реляційні вибірки, фільтрацію, статистичні обчислення (AVG)
 * та атомарні транзакції вставки.
 *
 * Читання списків та аналітики реалізовано через реактивні [Flow]: будь-яка
 * операція вставки/видалення автоматически оновлює всі спостерігачі UI
 * у реальному часі без ручного перечитування.
 */
@Dao
interface WindStationDao {

    // --- Станції ВЕС ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station: WindStation): Long

    @Update
    suspend fun updateStation(station: WindStation)

    @Delete
    suspend fun deleteStation(station: WindStation)

    /** Реактивний список усіх станцій, відсортований за назвою. */
    @Query("SELECT * FROM wind_stations ORDER BY name ASC")
    fun observeAllStations(): Flow<List<WindStation>>

    @Query("SELECT * FROM wind_stations WHERE id = :stationId LIMIT 1")
    suspend fun getStationById(stationId: Int): WindStation?

    /** Кількість станцій із заданою назвою (без урахування регістру) — для перевірки унікальності. */
    @Query("SELECT COUNT(id) FROM wind_stations WHERE name = :name COLLATE NOCASE")
    suspend fun countStationsByName(name: String): Int

    // --- Записи потужності ---

    @Insert
    suspend fun insertPower(power: WindPowerRecord)

    /** Реактивна історія потужностей станції, відсортована за спаданням. */
    @Query("SELECT * FROM wind_power_records WHERE stationId = :stationId ORDER BY value DESC")
    fun observePowersForStation(stationId: Int): Flow<List<WindPowerRecord>>

    /** Реактивне середнє значення потужності (SQL-агрегат AVG). */
    @Query("SELECT AVG(value) FROM wind_power_records WHERE stationId = :stationId")
    fun observeAveragePower(stationId: Int): Flow<Double?>

    // --- Записи швидкості вітру ---

    @Insert
    suspend fun insertSpeed(speed: WindSpeedRecord)

    /** Реактивна історія швидкостей вітру станції, відсортована за спаданням. */
    @Query("SELECT * FROM wind_speed_records WHERE stationId = :stationId ORDER BY value DESC")
    fun observeSpeedsForStation(stationId: Int): Flow<List<WindSpeedRecord>>

    /** Реактивне середнє значення швидкості вітру (SQL-агрегат AVG). */
    @Query("SELECT AVG(value) FROM wind_speed_records WHERE stationId = :stationId")
    fun observeAverageSpeed(stationId: Int): Flow<Double?>

    // --- Записи напруги ---

    @Insert
    suspend fun insertVoltage(voltage: WindVoltageRecord)

    /** Реактивна історія вихідних напруг генераторів станції. */
    @Query("SELECT * FROM wind_voltage_records WHERE stationId = :stationId ORDER BY value DESC")
    fun observeVoltagesForStation(stationId: Int): Flow<List<WindVoltageRecord>>

    // --- Комплексна атомарна транзакція ---

    /**
     * Вставляє станцію та всі пов'язані масиви телеметрії атомарно:
     * або записуються всі дані, або жоден із них.
     *
     * @return Ідентифікатор (rowId) створеного запису станції.
     */
    @Transaction
    suspend fun insertStationWithTelemetry(
        station: WindStation,
        powers: List<Double>,
        speeds: List<Double>,
        voltages: List<Double>
    ): Long {
        val stationId = insertStation(station)
        powers.forEach { insertPower(WindPowerRecord(stationId = stationId.toInt(), value = it)) }
        speeds.forEach { insertSpeed(WindSpeedRecord(stationId = stationId.toInt(), value = it)) }
        voltages.forEach { insertVoltage(WindVoltageRecord(stationId = stationId.toInt(), value = it)) }
        return stationId
    }
}
