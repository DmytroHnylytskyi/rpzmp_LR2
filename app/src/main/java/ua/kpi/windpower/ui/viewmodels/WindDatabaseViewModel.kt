package ua.kpi.windpower.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.kpi.windpower.data.db.WindStationDatabase
import ua.kpi.windpower.data.db.entities.WindPowerRecord
import ua.kpi.windpower.data.db.entities.WindSpeedRecord
import ua.kpi.windpower.data.db.entities.WindStation
import ua.kpi.windpower.data.db.entities.WindVoltageRecord
import ua.kpi.windpower.data.db.repository.WindStationRepository

/**
 * Результат транзакційного збереження нової станції.
 */
sealed interface AddStationResult {
    /** Станцію та телеметрію успішно записано у Room БД. */
    data object Success : AddStationResult

    /** Валідація не пройдена: [message] пояснює причину. */
    data class Error(val message: String) : AddStationResult
}

/**
 * Агрегований зріз телеметрії обраної станції (реактивно оновлюється з Room).
 */
data class StationAnalytics(
    val powers: List<WindPowerRecord> = emptyList(),
    val speeds: List<WindSpeedRecord> = emptyList(),
    val voltages: List<WindVoltageRecord> = emptyList(),
    val averagePower: Double? = null,
    val averageSpeed: Double? = null
)

/**
 * ViewModel локального реляційного сховища Room.
 *
 * Забезпечує реляційні зв'язки, аналітичні запити (AVG), транзакційну вставку
 * та реактивне оновлення UI через StateFlow/Room Flow.
 *
 * Отримує [Application] через AndroidViewModel, а екземпляр бази — через
 * thread-safe singleton [WindStationDatabase.getInstance]: постійне з'єднання
 * переживає рекомпозиції та зміни конфігурації Activity.
 */
class WindDatabaseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WindStationRepository(
        WindStationDatabase.getInstance(application).windStationDao()
    )

    /** Реактивний список станцій: миттєво синхронізований з таблицею wind_stations. */
    val stations: StateFlow<List<WindStation>> = repository.observeAllStations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedStationId = MutableStateFlow<Int?>(null)
    val selectedStationId: StateFlow<Int?> = _selectedStationId.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    /** Реактивний зріз аналітики для обраної станції (історії + SQL-агрегати AVG). */
    @OptIn(ExperimentalCoroutinesApi::class)
    val analytics: StateFlow<StationAnalytics> = _selectedStationId
        .flatMapLatest { stationId ->
            if (stationId == null) {
                flowOf(StationAnalytics())
            } else {
                combine(
                    repository.observePowersForStation(stationId),
                    repository.observeSpeedsForStation(stationId),
                    repository.observeVoltagesForStation(stationId),
                    repository.observeAveragePower(stationId),
                    repository.observeAverageSpeed(stationId)
                ) { powers, speeds, voltages, avgPower, avgSpeed ->
                    StationAnalytics(
                        powers = powers,
                        speeds = speeds,
                        voltages = voltages,
                        averagePower = avgPower,
                        averageSpeed = avgSpeed
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StationAnalytics())

    init {
        // За потреби одразу обираємо першу станцію, щоб аналітика була видна без дій користувача
        viewModelScope.launch {
            val firstStation = stations.first { it.isNotEmpty() }.firstOrNull()
            if (_selectedStationId.value == null && firstStation != null) {
                selectStation(firstStation.id)
            }
        }
    }

    /**
     * Обирає станцію для аналізу. Вся її телеметрія та агрегати підтягнуться
     * реактивно через [analytics].
     */
    fun selectStation(stationId: Int) {
        _selectedStationId.value = stationId
    }

    /**
     * Транзакційно зберігає нову станцію разом із масивами телеметрії.
     *
     * Перед записом виконує перевірку унікальності назви — повторна
     * реєстрація тієї ж ВЕС блокується з явним поясненням користувачу.
     *
     * @param onResult Callback результату операції для показу Snackbar.
     */
    fun addStationWithTelemetry(
        name: String,
        location: String,
        capacity: Double,
        powers: List<Double>,
        speeds: List<Double>,
        voltages: List<Double>,
        onResult: (AddStationResult) -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val trimmedName = name.trim()
                if (repository.isStationNameTaken(trimmedName)) {
                    onResult(AddStationResult.Error("ВЕС з назвою «$trimmedName» вже зареєстрована у базі"))
                    return@launch
                }
                repository.insertStationWithTelemetry(
                    station = WindStation(
                        name = trimmedName,
                        location = location.trim().ifBlank { "Україна" },
                        installedCapacityMw = capacity
                    ),
                    powers = powers,
                    speeds = speeds,
                    voltages = voltages
                )
                onResult(AddStationResult.Success)
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Видаляє станцію каскадно разом з усіма записами телеметрії.
     *
     * @param onDeleted Callback після завершення видалення (для Snackbar).
     */
    fun deleteStation(station: WindStation, onDeleted: (WindStation) -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteStation(station)
            if (_selectedStationId.value == station.id) {
                _selectedStationId.value = null
            }
            onDeleted(station)
        }
    }
}
