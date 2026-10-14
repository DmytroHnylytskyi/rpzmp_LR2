package ua.kpi.windpower.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ua.kpi.windpower.data.db.dao.WindDispatcherDao
import ua.kpi.windpower.data.db.dao.WindStationDao
import ua.kpi.windpower.data.db.entities.DispatcherAccount
import ua.kpi.windpower.data.db.entities.WindPowerRecord
import ua.kpi.windpower.data.db.entities.WindSpeedRecord
import ua.kpi.windpower.data.db.entities.WindStation
import ua.kpi.windpower.data.db.entities.WindVoltageRecord

/**
 * Головна база даних Room мобільного застосунку «AeroGrid».
 * Реляційне локальне сховище параметрів вітрової електростанції.
 *
 *
 * Доступ здійснюється через thread-safe singleton [getInstance], щоб усе
 * застосування працювало з єдиним підключенням до SQLite (без перестворення
 * при кожній рекомпозиції Compose UI чи зміні конфігурації Activity).
 */
@Database(
    entities = [
        WindStation::class,
        WindPowerRecord::class,
        WindSpeedRecord::class,
        WindVoltageRecord::class,
        DispatcherAccount::class
    ],
    version = 2,
    exportSchema = false
)
abstract class WindStationDatabase : RoomDatabase() {

    abstract fun windStationDao(): WindStationDao

    abstract fun windDispatcherDao(): WindDispatcherDao

    companion object {
        private const val DATABASE_NAME = "wind_stations_database"

        @Volatile
        private var INSTANCE: WindStationDatabase? = null

        /**
         * Міграція 1 → 2: додає таблицю облікових записів диспетчерів,
         * не чіпаючи дані станцій.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `wind_dispatchers` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`login` TEXT NOT NULL, " +
                        "`fullName` TEXT NOT NULL, " +
                        "`email` TEXT NOT NULL, " +
                        "`shift` TEXT NOT NULL, " +
                        "`smsAlerts` INTEGER NOT NULL, " +
                        "`passwordHash` TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_wind_dispatchers_login` " +
                        "ON `wind_dispatchers` (`login`)"
                )
            }
        }

        /**
         * Повертає єдиний екземпляр бази даних у межах процесу.
         *
         * @param context Будь-який контекст (використовується applicationContext).
         */
        fun getInstance(context: Context): WindStationDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WindStationDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
