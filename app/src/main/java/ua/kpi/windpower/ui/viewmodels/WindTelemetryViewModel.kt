package ua.kpi.windpower.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ua.kpi.windpower.data.WindParkModel

/**
 * ViewModel для оперативного моніторингу ВЕС.
 *
 * Керує реактивними потоками StateFlow, що відображають стан вітропарку:
 * - швидкість вітру;
 * - згенерована потужність за фізичною моделлю [WindParkModel];
 * - експлуатаційний статус SCADA (Cut-in, MPPT, Pitch, Cut-out);
 * - стан асинхронного опитування давачів (isLoading);
 * - стан помилки телеметричного зв'язку (error) з повторним опитуванням.
 */
class WindTelemetryViewModel : ViewModel() {

    private val _power = MutableStateFlow(WindParkModel.initialTelemetry().powerMw)
    val power: StateFlow<Double> = _power.asStateFlow()

    private val _windSpeed = MutableStateFlow(WindParkModel.DEFAULT_WIND_SPEED)
    val windSpeed: StateFlow<Double> = _windSpeed.asStateFlow()

    private val _status = MutableStateFlow(WindParkModel.initialTelemetry().status)
    val status: StateFlow<String> = _status.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Активна помилка опитування давачів; null — зв'язок справний. */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Асинхронно опитує промислові давачі вітропарку через корутину з імітацією
     * затримки мережі. Періодичний збій каналу зв'язку (~10% опитувань) перекладає
     * UI у стан помилки зі збереженням останніх достовірних показників (стійкість
     * SCADA); повторний запит після збою гарантовано відновлює телеметрію.
     */
    fun refreshTelemetry() {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true

            // Повторний запит після збою імітує відновлення каналу зв'язку
            val isRecoveryAttempt = _error.value != null
            _error.value = null
            delay(1000) // Затримка телеметричного опитування

            if (!isRecoveryAttempt && (1..100).random() <= SCADA_FAILURE_CHANCE_PERCENT) {
                _error.value =
                    "Тайм-аут опитування давача анемометра (помилка $SCADA_TIMEOUT_CODE). Повторіть запит."
                _isLoading.value = false
                return@launch
            }

            // Випадкова зміна швидкості вітру від 1.0 до 28.0 м/с
            val newSpeed = ((10..280).random() / 10.0)

            // Розрахунок робочого режиму ВЕС за фізичною моделлю парку
            val state = WindParkModel.resolveTelemetry(newSpeed)
            _windSpeed.value = state.windSpeed
            _power.value = state.powerMw
            _status.value = state.status

            _isLoading.value = false
        }
    }

    private companion object {
        /** Код типового телеметричного збою (тайм-аут шлюзу SCADA). */
        const val SCADA_TIMEOUT_CODE = 503

        /** Ймовірність збою каналу опитування, %. */
        const val SCADA_FAILURE_CHANCE_PERCENT = 10
    }
}
