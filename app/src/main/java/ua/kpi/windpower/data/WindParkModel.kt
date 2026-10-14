package ua.kpi.windpower.data

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Чиста фізична модель роботи вітропарку.
 *
 * Відображає швидкість набігаючого вітру на сумарну активну потужність парку
 * та експлуатаційний статус SCADA за стандартною аеродинамічною моделлю:
 * - **Штиль** (v < 3.0 м/с): ротор зупинено — нижче пускової швидкості Cut-in;
 * - **MPPT** (3.0 ≤ v ≤ 12.0 м/с): кубічний закон Беца P ~ v³,
 *   режим пошуку точки максимальної потужності;
 * - **Pitch-стабілізація** (12.0 < v ≤ 25.0 м/с): поворот лопатей утримує
 *   потужність на номінальному рівні;
 * - **Cut-out** (v > 25.0 м/с): аварійне флюгування та гальмування.
 *
 * Модель не залежить від Android SDK та покривається модульними тестами.
 */
object WindParkModel {

    /** Пускова швидкість турбіни (Cut-in), м/с. */
    const val CUT_IN_SPEED = 3.0

    /** Номінальна швидкість виходу потужності на плато, м/с. */
    const val RATED_SPEED = 12.0

    /** Швидкість аварійного відключення (Cut-out), м/с. */
    const val CUT_OUT_SPEED = 25.0

    /** Кількість вітроустановок у парку. */
    const val TURBINE_COUNT = 5

    /** Номінальна потужність однієї ВЕУ (МВт) — 5 x 3.4 = 17.0 МВт на парк. */
    const val TURBINE_RATED_MW = 3.4

    /** Густина повітря на рівні моря (кг/м³). */
    private const val AIR_DENSITY = 1.225

    /** Площа ометання ротора однієї ВЕУ (м²) — R = 50 м. */
    private const val SWEPT_AREA_M2 = 7854.0

    /** Середній аеродинамічний коефіцієнт потужності Cp у робочому діапазоні. */
    private const val CP_EFFICIENCY = 0.46

    /** Результат моделювання одного циклу опитування телеметрії. */
    data class TelemetryState(
        val windSpeed: Double,
        val powerMw: Double,
        val status: String
    )

    /**
     * Розраховує стан вітропарку для заданої швидкості вітру.
     *
     * @param windSpeed Швидкість вітру за показаннями польового анемометра (м/с).
     * @return Потужність парку (МВт) та текстовий експлуатаційний статус SCADA.
     */
    fun resolveTelemetry(windSpeed: Double): TelemetryState {
        val status: String
        val powerMw: Double
        when {
            windSpeed < CUT_IN_SPEED -> {
                status = "Штиль: ротор зупинено (нижче пускової Cut-in: 3.0 м/с)"
                powerMw = 0.0
            }
            windSpeed <= RATED_SPEED -> {
                // Кубічний закон потужності Беца для парку з TURBINE_COUNT турбін
                // (з Pitch-обмеженням паспортним номіналом парку)
                val singleTurbineMw = (0.5 * AIR_DENSITY * SWEPT_AREA_M2 * windSpeed.pow(3) * CP_EFFICIENCY) / 1_000_000.0
                status = "Оптимальна генерація в мережу (MPPT)"
                powerMw = (singleTurbineMw * TURBINE_COUNT).coerceAtMost(TURBINE_COUNT * TURBINE_RATED_MW)
            }
            windSpeed <= CUT_OUT_SPEED -> {
                status = "Номінальна потужність (Pitch-стабілізація)"
                powerMw = TURBINE_COUNT * TURBINE_RATED_MW
            }
            else -> {
                status = "Аварійне гальмування при штормі (Cut-out > 25 м/с)"
                powerMw = 0.0
            }
        }
        return TelemetryState(
            windSpeed = windSpeed,
            powerMw = (powerMw * 100.0).roundToInt() / 100.0,
            status = status
        )
    }

    /**
     * Розраховує потужність парку для початкового (стовпчикового) стану телеметрії.
     * Використовується, щоб стартові показники ViewModel відповідали фізичній моделі.
     */
    fun initialTelemetry(): TelemetryState = resolveTelemetry(DEFAULT_WIND_SPEED)

    /** Початкова швидкість вітру при старті екрану (м/с). */
    const val DEFAULT_WIND_SPEED = 9.5
}
