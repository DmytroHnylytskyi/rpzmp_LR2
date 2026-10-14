package ua.kpi.windpower.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Діалог підтвердження ризикованої дії (примірок — видалення станції з БД).
 * Гарантує, що руйнівні операції не виконуються з випадкового дотику.
 *
 * @param title Заголовок діалога.
 * @param text Пояснення наслідків дії.
 * @param confirmLabel Текст підтверджуючої кнопки (за замовчуванням червоний).
 * @param onConfirm Виконати дію.
 * @param onDismiss Скасувати дію.
 */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmColor: Color = MaterialTheme.colorScheme.error
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmLabel, color = confirmColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
