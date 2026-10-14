package ua.kpi.windpower.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сутність запису вихідної напруги генератора (В) з прив'язкою до станції.
 */
@Entity(
    tableName = "wind_voltage_records",
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
data class WindVoltageRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val stationId: Int,
    val value: Double, // Напруга у Вольтах
    val timestamp: Long = System.currentTimeMillis()
)
