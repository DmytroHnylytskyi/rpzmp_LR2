package ua.kpi.windpower.data

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Розрахунковий модуль інженерної бізнес-логіки вітроелектростанції.
 *
 * Виконує моделювання аеродинамічних характеристик вітрових турбін
 * на основі класичного фізичного закону Беца:
 * P = 0.5 * rho * S * v^3 * Cp
 *
 * Модуль інкапсулює фізичні константи атмосфери та обмеження режимів
 * експлуатації промислового вітроенергетичного обладнання.
 */
class WindPowerCalculator {

    /**
     * Стандартна густина сухого повітря на рівні моря при температурі 15°C (кг/м³).
     */
    private val standardAirDensityKgM3: Double = 1.225

    /**
     * Теоретична межа коефіцієнта використання енергії вітру (межа Беца).
     */
    val betzLimit: Double = 0.593

    /**
     * Виконує експрес-оцінку виробітку потужності за одним вхідним параметром — швидкістю вітру.
     *
     * @param windSpeedInput Швидкість набігаючого вітрового потоку (м/с).
     * @return Прогнозна вироблена потужність у кіловатах (кВт).
     */
    fun calculatePowerBasic(windSpeedInput: String): Double {
        val speed = windSpeedInput.toDoubleOrNull() ?: 0.0
        if (speed < CUT_IN_MPS || speed > CUT_OUT_MPS) return 0.0

        val estimatedKw = speed * 45.0
        return (estimatedKw * 10.0).roundToInt() / 10.0
    }

    /**
     * Виконує розрахунок потужності з урахуванням швидкості вітру та ККД перетворення енергії.
     *
     * @param windSpeedInput Швидкість вітру (м/с).
     * @param efficiencyInput Відсоток корисної дії енергетичної системи (0..100%).
     * @return Розрахована потужність у кіловатах (кВт).
     */
    fun calculatePowerMedium(windSpeedInput: String, efficiencyInput: String): Double {
        val speed = windSpeedInput.toDoubleOrNull() ?: 0.0
        val effPercent = efficiencyInput.toDoubleOrNull() ?: 0.0
        val efficiency = (effPercent / 100.0).coerceIn(0.0, 1.0)

        if (speed < CUT_IN_MPS || speed > CUT_OUT_MPS) return 0.0

        val powerKw = speed.pow(2.5) * efficiency * 18.0
        return (powerKw * 100.0).roundToInt() / 100.0
    }

    /**
     * Виконує повний інженерний розрахунок за аеродинамічною формулою потужності вітрового колеса:
     * P = 0.5 * rho * S * v^3 * Cp
     *
     * @param windSpeedInput Швидкість вітрового потоку v (м/с).
     * @param bladeRadiusInput Довжина лопаті / радіус ротора R (м).
     * @param cpInput Аеродинамічний коефіцієнт потужності Cp у відсотках (0..59.3%).
     * @return Електрична потужність турбіни у кіловатах (кВт).
     */
    fun calculatePowerAdvanced(
        windSpeedInput: String,
        bladeRadiusInput: String,
        cpInput: String
    ): Double {
        val speed = windSpeedInput.toDoubleOrNull() ?: 0.0
        val radius = bladeRadiusInput.toDoubleOrNull() ?: 0.0
        val cpPercent = cpInput.toDoubleOrNull() ?: 0.0
        val cp = (cpPercent / 100.0).coerceIn(0.0, betzLimit)

        // Перевірка допустимого робочого діапазону швидкостей (Cut-in та Cut-out)
        if (speed < CUT_IN_MPS || speed > CUT_OUT_MPS || radius <= 0.0 || cp <= 0.0) {
            return 0.0
        }

        // Площа ометання вітрового колеса: S = π * R²
        val sweptAreaM2 = PI * radius.pow(2)

        // Кінетична потужність повітряного потоку, перетворена в електричну (Вт)
        val powerWatts = 0.5 * standardAirDensityKgM3 * sweptAreaM2 * speed.pow(3) * cp

        // Конвертація у кіловати з округленням до сотих
        val powerKilowatts = powerWatts / 1000.0
        return (powerKilowatts * 100.0).roundToInt() / 100.0
    }

    /**
     * Повертає інженерний опис експлуатаційного режиму турбіни за поточною швидкістю вітру.
     *
     * @param windSpeedInput Швидкість вітру (м/с).
     * @return Текстовий статус роботи систем авторегулювання та захисту.
     */
    fun getOperatingStatus(windSpeedInput: String): String {
        val speed = windSpeedInput.toDoubleOrNull() ?: 0.0
        return when {
            speed <= 0.0 -> "Очікування достовірних даних анемометра"
            speed < CUT_IN_MPS -> "Штиль (< 3.0 м/с): Нижче швидкості включення (Cut-in). Турбіна в режимі готовності."
            speed < RATED_MPS -> "Робочий вітер (3.0 - 12.0 м/с): Часткове навантаження з регулюванням за швидкістю ротора."
            speed <= CUT_OUT_MPS -> "Номінальний вітер (12.0 - 25.0 м/с): Номінальна потужність. Активний Pitch-контроль кута атаки."
            else -> "Ураганний вітер (> 25.0 м/с): Перевищення швидкості зупинки (Cut-out)! Аварійне флюгування та дискове гальмування."
        }
    }

    companion object {
        /** Пускова швидкість турбіни (Cut-in), м/с. */
        const val CUT_IN_MPS = 3.0

        /** Номінальна швидкість виходу на Pitch-плато, м/с. */
        const val RATED_MPS = 12.0

        /** Швидкість аварійного відключення (Cut-out), м/с. */
        const val CUT_OUT_MPS = 25.0
    }
}
