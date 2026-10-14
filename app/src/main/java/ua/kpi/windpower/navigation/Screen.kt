package ua.kpi.windpower.navigation

/**
 * Маршрути навігаційного графа Jetpack Navigation Component
 * для системи «AeroGrid • ВЕС Моніторинг».
 */
sealed class Screen(val route: String) {

    /** Екран оперативного моніторингу ВЕС з інтерактивною Canvas-анімацією ротора турбіни */
    object Dashboard : Screen("dashboard")

    /** Екран SCADA телеметрії на базі архітектури MVVM та StateFlow */
    object ScadaMvvm : Screen("scada_mvvm")

    /** Екран оперативного прогнозу вітрогенерації з параметром імені диспетчера */
    object WindForecast : Screen("wind_forecast/{userName}") {
        fun createRoute(userName: String): String = "wind_forecast/$userName"
    }

    /** Екран інженерної деталізації з параметром кількості вітроагрегатів */
    object WindDetails : Screen("wind_details/{turbinesCount}") {
        fun createRoute(turbinesCount: Int): String = "wind_details/$turbinesCount"
    }

    /** Екран профілю диспетчера, авторизації та реєстрації оперативного персоналу ВЕС */
    object AuthProfile : Screen("auth_profile")

    /** Екран реєстрації та конфігурації нового об'єкта ВЕС із валідацією полів */
    object StationRegistration : Screen("station_registration")

    /** Екран локальної реляційної бази даних Room та аналітики телеметрії */
    object DatabaseHistory : Screen("database_history")

    /** Екран контролю парку турбін ВЕС-01..ВЕС-05 */
    object Fleet : Screen("fleet")

    /** Екран аеродинамічного калькулятора за законом Беца */
    object Calculator : Screen("calculator")
}
