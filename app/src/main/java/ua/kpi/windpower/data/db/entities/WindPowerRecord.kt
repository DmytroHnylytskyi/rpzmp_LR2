package ua.kpi.windpower.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сутність запису виробленої потужності ВЕС.
 * Пов'язана зовнішнім ключем із батьківською таблицею wind_stations.
 */
@Entity(
    tableName = "wind_power_records",
    foreignKeys = [
        ForeignKey(
            entity = WindStation::class,
            parentColumns = ["id"],
            childColumns = ["stationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("stationId")]
)
data class WindPowerRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val stationId: Int,
    val value: Double, // Потужність у МВт
    val timestamp: Long = System.currentTimeMillis()
)
