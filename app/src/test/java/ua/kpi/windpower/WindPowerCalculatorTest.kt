package ua.kpi.windpower

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.kpi.windpower.data.WindPowerCalculator

/**
 * Набір модульних тестів (Unit Tests) для перевірки інженерних розрахунків аеродинамічного калькулятора.
 */
class WindPowerCalculatorTest {

    private val calculator = WindPowerCalculator()

    @Test
    fun testBasicPower_speedBelowCutIn_returnsZero() {
        // При швидкості менше 3 м/с турбіна зупинена
        val power = calculator.calculatePowerBasic("2.4")
        assertEquals(0.0, power, 0.001)
    }

    @Test
    fun testBasicPower_operationalSpeed_returnsPositivePower() {
        // При штатній швидкості 8 м/с має бути позитивна генерація
        val power = calculator.calculatePowerBasic("8.0")
        assertTrue("Генерація має бути більшою за 0", power > 0.0)
    }

    @Test
    fun testAdvancedPower_stormSpeed_triggersCutOut() {
        // При швидкості понад 25 м/с генерація вимикається (аварійне гальмування)
        val power = calculator.calculatePowerAdvanced("27.5", "54.0", "43.5")
        assertEquals(0.0, power, 0.001)
    }

    @Test
    fun testAdvancedPower_normalWind_calculatesAccurately() {
        // Номінальний вітер 11.5 м/с, радіус 54 м, Cp 43.5%
        val power = calculator.calculatePowerAdvanced("11.5", "54.0", "43.5")
        assertTrue("Розрахована електрична потужність має бути більшою за 1 МВт", power > 1000.0)
    }

    @Test
    fun testBetzLimit_respected() {
        assertTrue("Межа Беца не може перевищувати 59.3%", calculator.betzLimit <= 0.593)
    }
}
