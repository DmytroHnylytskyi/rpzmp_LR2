package ua.kpi.windpower.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Обліковий запис оперативного персоналу (диспетчера) вітроелектростанції.
 *
 * Доступ на зміну має лише зареєстрований персонал: форма входу перевіряє
 * пару логін/email + пароль проти цієї таблиці. Пароль зберігається у вигляді
 * SHA-256 хеша ([ua.kpi.windpower.data.Validators.sha256]), а не відкритим текстом.
 *
 * @property id Автогенерований ідентифікатор запису.
 * @property login Позивний або корпоративний email диспетчера (унікальний).
 * @property fullName ПІБ інженера (відображається у кабінеті).
 * @property email Корпоративна електронна пошта.
 * @property shift Номер та тип зміни, за якою закріплений інженер.
 * @property smsAlerts Ознака активації аварійних SMS-сповіщень при вітрі > 25 м/с.
 * @property passwordHash SHA-256 хеш пароля доступу до SCADA.
 */
@Entity(
    tableName = "wind_dispatchers",
    indices = [Index(value = ["login"], unique = true)]
)
data class DispatcherAccount(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val login: String,
    val fullName: String,
    val email: String,
    val shift: String,
    @ColumnInfo(name = "sms_alerts")
    val smsAlerts: Boolean,
    @ColumnInfo(name = "password_hash")
    val passwordHash: String
)
