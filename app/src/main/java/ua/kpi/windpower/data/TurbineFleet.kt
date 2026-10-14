package ua.kpi.windpower.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlin.math.roundToInt

/**
 * Диспетчерський парк вітроустановок (ВЕС-01 — ВЕС-05+) — єдине джерело даних
 * для екранів Dashboard та Fleet Management з повним життєвим циклом парку.
 *
 * Список турбін є активним (Compose SnapshotStateList): додавання, редагування
 * та видалення установок диспетчером миттєво оновлюють всі екрани, що читають
 * парк, — телеметрія, сумарна потужність та кількість агрегатів «у мережі»
 * щоразу перераховуються за актуальним складом.
 *
 * Телеметрія обчислюється детерміновано від середньої швидкості вітру парку:
 * - кожна установка має власний зсув анемометра (імітація рельєфу майданчика);
 * - генерація розраховується за законом Беца через [WindPowerCalculator]
 *   з Pitch-обмеженням паспортним номіналом установки;
 * - статуси визначаються порогами Cut-in/Cut-out, режимом Pitch-стабілізації та ознакою
 *   перебування установки в технічному обслуговуванні.
 */
object TurbineFleet {

    /** Середній аеродинамічний коефіцієнт потужності моделей парку, %. */
    private const val FLEET_CP_PERCENT = 43.0

    /** Детерміновані зсуви показань анемометрів установок відносно парку, м/с. */
    private val SENSOR_OFFSETS_MPS = listOf(-0.6, 0.4, 0.8, -0.9, 0.3, -0.4, 0.7)

    /** Активний склад парку (реактивний: зміни оновлюють UI). */
    private val _turbines: SnapshotStateList<TurbineModel> = mutableStateListOf(
        TurbineModel(
            id = "WTG-01",
            name = "ВЕС-01 «Борей»",
            model = "Vestas V112-3.0MW",
            ratedPowerKw = 3000.0,
            bladeLengthMeters = 56.0,
            currentSpeedMps = 0.0,
            currentPowerKw = 0.0,
            rpm = 0.0,
            pitchAngleDeg = 3.2,
            status = TurbineStatus.OPERATING
        ),
        TurbineModel(
            id = "WTG-02",
            name = "ВЕС-02 «Зефір»",
            model = "Nordex N131-3.3MW",
            ratedPowerKw = 3300.0,
            bladeLengthMeters = 65.5,
            currentSpeedMps = 0.0,
            currentPowerKw = 0.0,
            rpm = 0.0,
            pitchAngleDeg = 3.2,
            status = TurbineStatus.OPERATING
        ),
        TurbineModel(
            id = "WTG-03",
            name = "ВЕС-03 «Стрибог»",
            model = "Enercon E-126-3.5MW",
            ratedPowerKw = 3500.0,
            bladeLengthMeters = 63.0,
            currentSpeedMps = 0.0,
            currentPowerKw = 0.0,
            rpm = 0.0,
            pitchAngleDeg = 3.2,
            status = TurbineStatus.OPERATING
        ),
        TurbineModel(
            id = "WTG-04",
            name = "ВЕС-04 «Еол»",
            model = "Siemens Gamesa 3.4MW",
            ratedPowerKw = 3400.0,
            bladeLengthMeters = 66.0,
            currentSpeedMps = 0.0,
            currentPowerKw = 0.0,
            rpm = 0.0,
            pitchAngleDeg = 85.0,
            status = TurbineStatus.STANDBY
        ),
        TurbineModel(
            id = "WTG-05",
            name = "ВЕС-05 «Нот»",
            model = "Vestas V126-3.45MW",
            ratedPowerKw = 3450.0,
            bladeLengthMeters = 63.0,
            currentSpeedMps = 0.0,
            currentPowerKw = 0.0,
            rpm = 0.0,
            pitchAngleDeg = 90.0,
            status = TurbineStatus.MAINTENANCE,
            isInMaintenance = true
        )
    )

    /** Поточний паспортний склад парку (спостережуваний у Compose). */
    val turbines: List<TurbineModel> get() = _turbines

    /** Сумарна паспортна потужність парку (МВт). */
    fun totalRatedMw(): Double = _turbines.sumOf { it.ratedPowerKw } / 1000.0

    /**
     * Обчислює миттєву телеметрію парку для середньої швидкості вітру [parkWindMps].
     *
     * @return Оновлені копії моделей турбін з актуальними показниками SCADA.
     */
    fun resolveTelemetry(
        parkWindMps: Float,
        calculator: WindPowerCalculator = WindPowerCalculator()
    ): List<TurbineModel> = _turbines.mapIndexed { index, passport ->
        val wind = (parkWindMps + SENSOR_OFFSETS_MPS[index % SENSOR_OFFSETS_MPS.size]).coerceIn(0.0, 35.0)

        val status = when {
            passport.isInMaintenance -> TurbineStatus.MAINTENANCE
            wind < WindPowerCalculator.CUT_IN_MPS -> TurbineStatus.STANDBY
            wind > WindPowerCalculator.CUT_OUT_MPS -> TurbineStatus.EMERGENCY_BRAKE
            wind > WindPowerCalculator.RATED_MPS -> TurbineStatus.OPTIMIZING
            else -> TurbineStatus.OPERATING
        }

        val isGenerating = status == TurbineStatus.OPERATING || status == TurbineStatus.OPTIMIZING

        val powerKw = if (!isGenerating) {
            0.0
        } else {
            calculator.calculatePowerAdvanced(
                windSpeedInput = wind.toString(),
                bladeRadiusInput = passport.bladeLengthMeters.toString(),
                cpInput = FLEET_CP_PERCENT.toString()
            ).coerceAtMost(passport.ratedPowerKw) // Pitch-контроль утримує паспортний номінал
        }

        val rpm = if (!isGenerating) 0.0 else (wind * 1.35).coerceAtMost(18.0)

        val pitchDeg = when (status) {
            TurbineStatus.MAINTENANCE -> 90.0
            TurbineStatus.EMERGENCY_BRAKE -> 89.0 // флюгування лопатей
            TurbineStatus.STANDBY -> 85.0         // підготовка до пуску
            TurbineStatus.OPTIMIZING -> ((wind - WindPowerCalculator.RATED_MPS) * 2.4 + 3.2)
                .coerceIn(3.2, 35.0)               // ріст кута установки із зростанням вітру
            TurbineStatus.OPERATING -> 3.2
        }

        passport.copy(
            currentSpeedMps = (wind * 10).roundToInt() / 10.0,
            currentPowerKw = (powerKw * 10).roundToInt() / 10.0,
            rpm = (rpm * 10).roundToInt() / 10.0,
            pitchAngleDeg = (pitchDeg * 10).roundToInt() / 10.0,
            status = status
        )
    }

    /**
     * Додає нову вітроустановку до парку з автогенерацією диспетчерського id.
     *
     * @return false, якщо установка з такою назвою вже є в парку.
     */
    fun addTurbine(
        name: String,
        model: String,
        ratedPowerKw: Double,
        bladeLengthMeters: Double,
        isInMaintenance: Boolean = false
    ): Boolean {
        val trimmedName = name.trim()
        if (_turbines.any { it.name.equals(trimmedName, ignoreCase = true) }) return false

        val nextNumber = (_turbines.mapNotNull { t -> t.id.removePrefix("WTG-").toIntOrNull() }.maxOrNull() ?: 0) + 1
        _turbines.add(
            TurbineModel(
                id = "WTG-" + nextNumber.toString().padStart(2, '0'),
                name = trimmedName,
                model = model.trim(),
                ratedPowerKw = ratedPowerKw,
                bladeLengthMeters = bladeLengthMeters,
                currentSpeedMps = 0.0,
                currentPowerKw = 0.0,
                rpm = 0.0,
                pitchAngleDeg = if (isInMaintenance) 90.0 else 3.2,
                status = if (isInMaintenance) TurbineStatus.MAINTENANCE else TurbineStatus.OPERATING,
                isInMaintenance = isInMaintenance
            )
        )
        return true
    }

    /**
     * Оновлює паспортні дані існуючої установки (крім телеметрії та id).
     *
     * @return false, якщо інша установка вже використовує нову назву.
     */
    fun updateTurbine(updated: TurbineModel): Boolean {
        val duplicate = _turbines.any { it.id != updated.id && it.name.equals(updated.name.trim(), ignoreCase = true) }
        if (duplicate) return false
        val index = _turbines.indexOfFirst { it.id == updated.id }
        if (index < 0) return false
        _turbines[index] = updated.copy(name = updated.name.trim(), model = updated.model.trim())
        return true
    }

    /** Видаляє установку з парку за id. */
    fun deleteTurbine(id: String) {
        _turbines.removeAll { it.id == id }
    }

    /** Сумарна активна потужність парку за телеметрією (МВт). */
    fun totalPowerMw(telemetry: List<TurbineModel>): Double =
        telemetry.sumOf { it.currentPowerKw } / 1000.0

    /** Кількість установок, що генерують та віддають потужність у мережу. */
    fun onlineCount(telemetry: List<TurbineModel>): Int =
        telemetry.count { it.currentPowerKw > 0.0 }
}
