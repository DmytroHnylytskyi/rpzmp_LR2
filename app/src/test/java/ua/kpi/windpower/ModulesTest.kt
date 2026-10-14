package ua.kpi.windpower

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.kpi.windpower.data.WindParkModel
import ua.kpi.windpower.data.Validators
import ua.kpi.windpower.data.db.dao.WindStationDao
import ua.kpi.windpower.data.db.entities.WindPowerRecord
import ua.kpi.windpower.data.db.entities.WindSpeedRecord
import ua.kpi.windpower.data.db.entities.WindStation
import ua.kpi.windpower.data.db.entities.WindVoltageRecord
import ua.kpi.windpower.data.db.repository.WindStationRepository
import ua.kpi.windpower.ui.viewmodels.WindTelemetryViewModel

/**
 * Автоматизовані модульні тести бізнес-логіки лабораторної роботи №2
 * (Варіант 2: ВЕС з моніторингом швидкості вітру, максимальний рівень).
 *
 * Охоплюють:
 * - валідацію форм (email, стійкість пароля, потужність);
 * - фізичну модель вітропарку (Cut-in / MPPT / Pitch / Cut-out) та початковий стан ViewModel;
 * - транзакційну вставку, видалення, унікальність назв та SQL-агрегати AVG (через стаб DAO).
 */

// --- Тестування валідації форм ---

class FormValidationTest {

    @Test
    fun testEmailValidation() {
        assertTrue(Validators.isValidEmail("engineer@windpower.kpi.ua"))
        assertTrue(Validators.isValidEmail("scada_admin@ves.energy.gov.ua"))
        assertFalse(Validators.isValidEmail("invalid-email"))
        assertFalse(Validators.isValidEmail("engineer@"))
        assertFalse(Validators.isValidEmail("@kpi.ua"))
    }

    @Test
    fun testPasswordComplexityValidation() {
        assertTrue(Validators.isStrongPassword("WindPower2026"))
        assertTrue(Validators.isStrongPassword("ScadaPass1!"))
        assertFalse(Validators.isStrongPassword("short1A")) // < 8 символів
        assertFalse(Validators.isStrongPassword("alllowercase123")) // немає великої літери
        assertFalse(Validators.isStrongPassword("NoDigitsPassword")) // немає цифри
    }

    @Test
    fun testCapacityValidation() {
        assertTrue(Validators.isValidCapacityMw(34.0))
        assertFalse(Validators.isValidCapacityMw(0.0))
        assertFalse(Validators.isValidCapacityMw(null))
    }
}

// --- Фізична модель вітропарку та реактивна ViewModel ---

class WindParkModelTest {

    @Test
    fun testCalmWindBelowCutIn_returnsZeroPower() {
        val state = WindParkModel.resolveTelemetry(2.4)
        assertEquals(0.0, state.powerMw, 0.001)
        assertTrue(state.status.contains("Штиль"))
    }

    @Test
    fun testOptimalWind_followsBetzCubicLaw() {
        // Мощність пропорційна v³: при подвоєнні швидкості в межах MPPT-діапазону — зростає у 8 разів
        val low = WindParkModel.resolveTelemetry(4.0).powerMw
        val high = WindParkModel.resolveTelemetry(8.0).powerMw
        assertEquals(low * 8.0, high, 0.05)
        assertTrue(low > 0.0)
    }

    @Test
    fun testNominalWind_pitchStabilization_holdsRatedPower() {
        val state = WindParkModel.resolveTelemetry(18.0)
        assertEquals(5 * WindParkModel.TURBINE_RATED_MW, state.powerMw, 0.001)
        assertTrue(state.status.contains("Pitch"))
    }

    @Test
    fun testBoundarySpeeds_areHandledOnceAt12Mps() {
        // 11.0 м/с — ще MPPT-гілка (кубічний закон нижче номіналу), вище 12.0 — Pitch-плато 17.0 МВт
        val mppt = WindParkModel.resolveTelemetry(11.0)
        val pitch = WindParkModel.resolveTelemetry(12.1)
        assertTrue(mppt.powerMw < WindParkModel.TURBINE_COUNT * WindParkModel.TURBINE_RATED_MW)
        assertEquals(WindParkModel.TURBINE_COUNT * WindParkModel.TURBINE_RATED_MW, pitch.powerMw, 0.001)
        // На межі 12.0 м/с MPPT-потужність не перевищує паспортний номінал парку
        assertTrue(WindParkModel.resolveTelemetry(12.0).powerMw <= WindParkModel.TURBINE_COUNT * WindParkModel.TURBINE_RATED_MW)
    }

    @Test
    fun testStormAboveCutOut_emergencyBrake() {
        val state = WindParkModel.resolveTelemetry(27.5)
        assertEquals(0.0, state.powerMw, 0.001)
        assertTrue(state.status.contains("Cut-out"))
    }

    @Test
    fun testViewModel_initialStateMatchesPhysicalModel() {
        val viewModel = WindTelemetryViewModel()
        val expected = WindParkModel.resolveTelemetry(WindParkModel.DEFAULT_WIND_SPEED)
        // Початкові показники обчислені моделлю, а не задані довільним числом
        assertEquals(expected.powerMw, viewModel.power.value, 0.001)
        assertEquals(expected.status, viewModel.status.value)
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.error.value)
    }
}

// --- Транзакції, видалення та аналітика Room-репозиторію ---

/**
 * Стаб DAO без Android/Room: імітує таблиці у пам'яті для чистого JVM-тестування
 * логіки репозиторію (транзакційна вставка, каскадне видалення, реактивні запити).
 */
private class InMemoryWindStationDao : WindStationDao {

    private val stations = MutableStateFlow<List<WindStation>>(emptyList())
    private val powers = MutableStateFlow<List<WindPowerRecord>>(emptyList())
    private val speeds = MutableStateFlow<List<WindSpeedRecord>>(emptyList())
    private val voltages = MutableStateFlow<List<WindVoltageRecord>>(emptyList())

    override suspend fun insertStation(station: WindStation): Long {
        val newId = (stations.value.maxOfOrNull { it.id } ?: 0) + 1
        stations.value = stations.value + station.copy(id = newId)
        return newId.toLong()
    }

    override suspend fun updateStation(station: WindStation) {
        stations.value = stations.value.map { if (it.id == station.id) station else it }
    }

    override suspend fun deleteStation(station: WindStation) {
        stations.value = stations.value.filter { it.id != station.id }
        powers.value = powers.value.filter { it.stationId != station.id }
        speeds.value = speeds.value.filter { it.stationId != station.id }
        voltages.value = voltages.value.filter { it.stationId != station.id }
    }

    override fun observeAllStations(): Flow<List<WindStation>> =
        stations.map { list -> list.sortedBy { it.name } }

    override suspend fun getStationById(stationId: Int): WindStation? =
        stations.value.firstOrNull { it.id == stationId }

    override suspend fun countStationsByName(name: String): Int =
        stations.value.count { it.name.equals(name, ignoreCase = true) }

    override suspend fun insertPower(power: WindPowerRecord) {
        val newId = (powers.value.maxOfOrNull { it.id } ?: 0) + 1
        powers.value = powers.value + power.copy(id = newId)
    }

    override suspend fun insertSpeed(speed: WindSpeedRecord) {
        val newId = (speeds.value.maxOfOrNull { it.id } ?: 0) + 1
        speeds.value = speeds.value + speed.copy(id = newId)
    }

    override suspend fun insertVoltage(voltage: WindVoltageRecord) {
        val newId = (voltages.value.maxOfOrNull { it.id } ?: 0) + 1
        voltages.value = voltages.value + voltage.copy(id = newId)
    }

    override fun observePowersForStation(stationId: Int): Flow<List<WindPowerRecord>> =
        powers.map { list ->
            list.filter { it.stationId == stationId }.sortedByDescending { it.value }
        }

    override fun observeSpeedsForStation(stationId: Int): Flow<List<WindSpeedRecord>> =
        speeds.map { list ->
            list.filter { it.stationId == stationId }.sortedByDescending { it.value }
        }

    override fun observeVoltagesForStation(stationId: Int): Flow<List<WindVoltageRecord>> =
        voltages.map { list ->
            list.filter { it.stationId == stationId }.sortedByDescending { it.value }
        }

    override fun observeAveragePower(stationId: Int): Flow<Double?> =
        powers.map { list ->
            list.filter { it.stationId == stationId }
                .map { it.value }
                .takeIf { it.isNotEmpty() }
                ?.average()
        }

    override fun observeAverageSpeed(stationId: Int): Flow<Double?> =
        speeds.map { list ->
            list.filter { it.stationId == stationId }
                .map { it.value }
                .takeIf { it.isNotEmpty() }
                ?.average()
        }
}

class WindStationRepositoryTest {

    private val repository = WindStationRepository(InMemoryWindStationDao())

    private fun insertTiligulska(): Int = runBlocking {
        repository.insertStationWithTelemetry(
            station = WindStation(name = "Тилігульська ВЕС", location = "Миколаївська обл.", installedCapacityMw = 500.0),
            powers = listOf(450.0, 420.5, 380.0, 490.2),
            speeds = listOf(11.8, 10.5, 9.2, 12.4),
            voltages = listOf(690.0, 690.0, 690.0, 690.0)
        ).toInt()
    }

    @Test
    fun testTransactionInsert_persistsStationWithTelemetry() = runBlocking {
        val stationId = insertTiligulska()
        assertTrue(stationId > 0)
        val stations = repository.observeAllStations().first()
        assertEquals(1, stations.size)
        assertEquals("Тилігульська ВЕС", stations.first().name)

        // Групи телеметрії прив'язані до станції зовнішнім ключем
        assertEquals(4, repository.observePowersForStation(stationId).first().size)
        assertEquals(4, repository.observeSpeedsForStation(stationId).first().size)
        assertEquals(4, repository.observeVoltagesForStation(stationId).first().size)
    }

    @Test
    fun testAggregatedAverages_matchSeededMeasurements() = runBlocking {
        val stationId = insertTiligulska()

        // AVG по записах потужності: (450.0 + 420.5 + 380.0 + 490.2) / 4
        assertEquals(435.175, repository.observeAveragePower(stationId).first()!!, 0.001)
        // AVG по записах швидкості: (11.8 + 10.5 + 9.2 + 12.4) / 4
        assertEquals(10.975, repository.observeAverageSpeed(stationId).first()!!, 0.001)
    }

    @Test
    fun testDuplicateStationName_isRejectedByUniquenessCheck() = runBlocking {
        insertTiligulska()
        assertTrue(repository.isStationNameTaken("тилігульська вес")) // без урахування регістру
        assertFalse(repository.isStationNameTaken("Ботієвська ВЕС"))
    }

    @Test
    fun testDeleteStation_cascadesTelemetryRecords() = runBlocking {
        val stationId = insertTiligulska()
        val station = repository.observeAllStations().first().first()

        repository.deleteStation(station)

        assertTrue(repository.observeAllStations().first().isEmpty())
        // Каскадне видалення: дитинні записи зникли разом із батьківським
        assertTrue(repository.observePowersForStation(stationId).first().isEmpty())
        assertNull(repository.observeAveragePower(stationId).first())
    }

    @Test
    fun testSortedHistory_isReturnedInDescendingOrder() = runBlocking {
        val stationId = insertTiligulska()
        val sortedPowers = repository.observePowersForStation(stationId).first()
        assertEquals(listOf(490.2, 450.0, 420.5, 380.0), sortedPowers.map { it.value })
    }
}
