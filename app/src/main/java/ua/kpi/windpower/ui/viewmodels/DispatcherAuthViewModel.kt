package ua.kpi.windpower.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ua.kpi.windpower.data.db.WindStationDatabase
import ua.kpi.windpower.data.db.repository.AuthOutcome
import ua.kpi.windpower.data.db.repository.DispatcherAuthRepository
import ua.kpi.windpower.data.db.repository.RegistrationOutcome

/**
 * ViewModel облікових записів диспетчерів.
 * Автентифікація та реєстрація персоналу у таблиці wind_dispatchers
 * локальної БД Room.
 */
class DispatcherAuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DispatcherAuthRepository(
        WindStationDatabase.getInstance(application).windDispatcherDao()
    )

    /**
     * Асинхронна автентифікація диспетчера.
     *
     * @param onResult Callback з результатом (успіх з обліковим записом або причина відмови).
     */
    fun authenticate(login: String, password: String, onResult: (AuthOutcome) -> Unit) {
        viewModelScope.launch {
            onResult(repository.authenticate(login, password))
        }
    }

    /**
     * Асинхронна реєстрація нового інженера зміни з автотичним входом у кабінет.
     */
    fun register(
        fullName: String,
        email: String,
        password: String,
        shift: String,
        smsAlerts: Boolean,
        onResult: (RegistrationOutcome) -> Unit
    ) {
        viewModelScope.launch {
            onResult(repository.register(fullName, email, password, shift, smsAlerts))
        }
    }
}
