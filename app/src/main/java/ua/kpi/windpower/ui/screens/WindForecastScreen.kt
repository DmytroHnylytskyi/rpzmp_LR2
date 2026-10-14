package ua.kpi.windpower.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
 * Екран оперативного прогнозу генерації вітропарку.
 *
 * Демонструє передачу та отримання параметрів через Jetpack Navigation Component
 * (аргумент [userName]) та адаптивний інтерфейс для різних розмірів екранів.
 */
@Composable
fun WindForecastScreen(
    navController: NavHostController,
    userName: String
) {
    var turbinesCountInput by rememberSaveable { mutableStateOf("6") }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLargeScreen = maxWidth > 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScreenTitle("Оперативний сеанс: $userName")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Метеорологічний прогноз для вітропарку",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow("Сьогодні:", "вітер 9.2 м/с • очікувано 14.5 МВт")
                    InfoRow("Завтра:", "вітер 12.0 м/с • очікувано 20.4 МВт")
                    InfoRow("Післязавтра:", "вітер 6.5 м/с • очікувано 7.8 МВт")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = turbinesCountInput,
                onValueChange = { turbinesCountInput = it },
                label = { Text("Кількість діючих турбін ВЕУ в строю") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (isLargeScreen) {
                // Адаптивне розміщення кнопок у рядок для широких екранів
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MenuButton(
                        text = "Розрахувати інженерні деталі",
                        modifier = Modifier.weight(1f)
                    ) {
                        val count = turbinesCountInput.toIntOrNull() ?: 6
                        navController.navigate(Screen.WindDetails.createRoute(count))
                    }
                    MenuButton(
                        text = "До моніторингу",
                        modifier = Modifier.weight(1f)
                    ) {
                        navController.navigate(Screen.Dashboard.route)
                    }
                }
            } else {
                // Вертикальне розташування для смартфонів
                MenuButton("Розрахувати інженерні деталі") {
                    val count = turbinesCountInput.toIntOrNull() ?: 6
                    navController.navigate(Screen.WindDetails.createRoute(count))
                }
                MenuButton("До моніторингу") {
                    navController.navigate(Screen.Dashboard.route)
                }
            }
        }
    }
}
