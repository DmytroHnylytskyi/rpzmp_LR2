package ua.kpi.windpower.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Кольорова палітра AeroGrid у стилі Eco-Tech / Renewable Energy.
 *
 * Використовує природні смарагдові та м'ятні тони для генерації чистої енергії,
 * контрастні графітові відтінки для SCADA інтерфейсу, а також сигнальні кольори
 * для попередження та аварійного відключення турбін.
 *
 * Це ЄДИНИЙ джерело кольорів бренду: екрани та компоненти звертаються
 * до цих токенів (переважно через MaterialTheme.colorScheme) і не містять
 * жодних локальних hex-значень.
 */

// Основні акцентні кольори вітроенергетики
val EmeraldPrimary = Color(0xFF10B981)
val EmeraldLight = Color(0xFF34D399)
val EmeraldDark = Color(0xFF047857)

// Допоміжні інженерні акценти
val CyanAccent = Color(0xFF06B6D4)
val CyanLight = Color(0xFF67E8F9)
val AmberWarning = Color(0xFFF59E0B)
val StormAlert = Color(0xFFEF4444)

// Службові статуси обладнання
val MaintenanceSlate = Color(0xFF64748B)

// Тонкий текст на смарагдових ціях (глибокий морський, без синеви)
val InkOnAccent = Color(0xFF06251A)

// Кольорові контейнери темної теми (графітовий / темно-вугільний)
val DarkBackground = Color(0xFF0C1014)
val DarkSurface = Color(0xFF151D24)
val DarkSurfaceVariant = Color(0xFF1E2832)
val DarkSurfaceHigh = Color(0xFF28343F)
val DarkContainer = Color(0xFF12302A)
val DarkCyanContainer = Color(0xFF0E3A44)
val DarkAmberContainer = Color(0xFF3F2E04)

// Текстові токени темної теми
val DarkTextPrimary = Color(0xFFF1F5F9)
val DarkTextSecondary = Color(0xFF94A3B8)

// Розділювачі та рамки темної теми
val DarkOutline = Color(0xFF3B4A5A)
val DarkOutlineVariant = Color(0xFF27323D)

// Світла тема інтерфейсу (чистий сланець / м'ята)
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF64748B)
val LightContainer = Color(0xFFD1FAE5)
val LightCyanContainer = Color(0xFFCFF5FA)
val LightAmberContainer = Color(0xFFFDF0C2)
