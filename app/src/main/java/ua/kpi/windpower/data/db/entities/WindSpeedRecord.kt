package ua.kpi.windpower.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сутність запису швидкості вітру (м/с) на метеовежі або анемометрі ВЕС.
 */
@Entity(
    tableName = "wind_speed_records",
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
data class WindSpeedRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val stationId: Int,
    val value: Double, // Швидкість вітру в м/с
    val timestamp: Long = System.currentTimeMillis()
)
