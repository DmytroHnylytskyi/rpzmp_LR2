package ua.kpi.windpower.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import ua.kpi.windpower.navigation.Screen
import ua.kpi.windpower.ui.components.InfoRow
import ua.kpi.windpower.ui.components.MenuButton
import ua.kpi.windpower.ui.components.ScreenTitle

/**
 * Екран інженерної деталізації виробітку вітропарку.
 *
 * Отримує аргумент [turbinesCount] через аргументи маршруту NavController
 * та розраховує енергетичні показники станції.
 */
@Composable
fun WindDetailsScreen(
    navController: NavHostController,
    turbinesCount: Int
) {
    val nominalPerTurbineMw = 3.4
    val totalPowerMw = turbinesCount * nominalPerTurbineMw
    val estimatedDailyMwh = totalPowerMw * 24.0 * 0.42 // 42% середній КВВП для ВЕС

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ScreenTitle("Інженерний розрахунок генерації ВЕС")

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Паспортні показники вітропарку",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))
                InfoRow("Кількість підключених ВЕУ:", "$turbinesCount од.")
                InfoRow("Тип вітротурбіни:", "3.4 МВт (діаметр ротора 112 м)")
                InfoRow("Сумарна встановлена потужність:", "%.1f МВт".format(totalPowerMw))
                InfoRow("Розрахункова добова генерація:", "%.1f МВт·год".format(estimatedDailyMwh))
                InfoRow("Коефіцієнт використання (КВВП):", "42.0 %")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        MenuButton("Повернутися до прогнозу") {
            navController.popBackStack()
        }

        Spacer(modifier = Modifier.height(8.dp))

        MenuButton("Перейти до SCADA Моніторингу") {
            navController.navigate(Screen.Dashboard.route) {
                popUpTo(Screen.Dashboard.route) { inclusive = true }
            }
        }
    }
}
