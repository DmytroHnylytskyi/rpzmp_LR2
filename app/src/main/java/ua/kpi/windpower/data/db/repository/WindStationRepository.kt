package ua.kpi.windpower.data.db.repository

import kotlinx.coroutines.flow.Flow
import ua.kpi.windpower.data.db.dao.WindStationDao
import ua.kpi.windpower.data.db.entities.WindPowerRecord
import ua.kpi.windpower.data.db.entities.WindSpeedRecord
import ua.kpi.windpower.data.db.entities.WindStation
import ua.kpi.windpower.data.db.entities.WindVoltageRecord

/**
 * Репозиторій для роботи з базою даних вітропарку Room.
 * Забезпечує шар абстракції над DAO відповідно до архітектури MVVM.
 *
 * Запити читання повертають реактивні [Flow] — UI отримує свіжі дані
 * автоматично після кожної зміни в таблицях.
 */
class WindStationRepository(private val dao: WindStationDao) {

    // --- Реактивні запити читання ---

    /** Реактивний список усіх станцій (сортування за назвою). */
    fun observeAllStations(): Flow<List<WindStation>> = dao.observeAllStations()

    /** Реактивна історія виробітку потужності обраної станції. */
    fun observePowersForStation(stationId: Int): Flow<List<WindPowerRecord>> =
        dao.observePowersForStation(stationId)

    /** Реактивна історія швидкостей вітру обраної станції. */
    fun observeSpeedsForStation(stationId: Int): Flow<List<WindSpeedRecord>> =
        dao.observeSpeedsForStation(stationId)

    /** Реактивна історія вихідних напруг обраної станції. */
    fun observeVoltagesForStation(stationId: Int): Flow<List<WindVoltageRecord>> =
        dao.observeVoltagesForStation(stationId)

    /** Реактивна аналітика: середнє виробіток потужності станції (SQL AVG). */
    fun observeAveragePower(stationId: Int): Flow<Double?> =
        dao.observeAveragePower(stationId)

    /** Реактивна аналітика: середня швидкість вітру станції (SQL AVG). */
    fun observeAverageSpeed(stationId: Int): Flow<Double?> =
        dao.observeAverageSpeed(stationId)

    // --- Разові запити ---

    suspend fun getStationById(id: Int): WindStation? = dao.getStationById(id)

    /**
     * Перевіряє існування станції із зазначеною назвою (без урахування регістру).
     * Використовується для валідації унікальності при додаванні нової ВЕС.
     */
    suspend fun isStationNameTaken(name: String): Boolean =
        dao.countStationsByName(name.trim()) > 0

    // --- Операції запису ---

    /**
     * Транзакційно вставляє станцію разом з усіма масивами телеметрії.
     * Повертає ідентифікатор створеного запису станції.
     */
    suspend fun insertStationWithTelemetry(
        station: WindStation,
        powers: List<Double>,
        speeds: List<Double>,
        voltages: List<Double>
    ): Long = dao.insertStationWithTelemetry(station, powers, speeds, voltages)

    /** Видаляє станцію разом з усією телеметрією (каскадне видалення через ForeignKey). */
    suspend fun deleteStation(station: WindStation) = dao.deleteStation(station)
}
