package ua.kpi.windpower.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Головна сутність вітроелектростанції (ВЕС) у Room БД.
 * Виступає батьківською таблицею у реляційній схемі.
 *
 * @property id Унікальний числовий ідентифікатор станції (автогенерація).
 * @property name Офіційна назва ВЕС (наприклад, «Тилігульська ВЕС»).
 * @property location Географічне розташування або область.
 * @property installedCapacityMw Встановлена електрична потужність вітропарку (МВт).
 */
@Entity(tableName = "wind_stations")
data class WindStation(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val location: String = "Україна",
    val installedCapacityMw: Double = 0.0
)
