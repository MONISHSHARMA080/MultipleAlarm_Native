package com.example.trying_native.dataBase

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(indices = [Index(value = ["first_value", "second_value"])])
data class AlarmData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "first_value") val first_value: Long,
    @ColumnInfo(name = "second_value") val second_value: Long,
    @ColumnInfo(name = "start_hour_for_display") val start_hour_for_display: Int,
    @ColumnInfo(name = "start_min_for_display") val start_min_for_display: Int,
    @ColumnInfo(name = "end_hour_for_display") val end_hour_for_display: Int,
    @ColumnInfo(name = "end_min_for_display") val end_min_for_display: Int,
    @ColumnInfo(name = "date_for_display") val date_for_display: String,
    @ColumnInfo(name = "freq_in_min") val freq_in_min: Long,
    @ColumnInfo(name = "is_ready_to_use") val isReadyToUse: Boolean
)

@Database(entities = [AlarmData::class], version = 12)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
}

@Dao
interface AlarmDao {
    @Insert
    suspend fun insert(alarmData: AlarmData): Long

    @Query("SELECT * FROM AlarmData ")
    fun getAll(): List<AlarmData>

    @Query("SELECT * FROM AlarmData")
    suspend fun getAllAlarms(): List<AlarmData>
}

//
//@Dao
//interface AlarmDataDAO {
//    @Query("SELECT * FROM AlarmData")
//    fun getAll(): Flow<List<AlarmData>>
//
//    @Query("SELECT * FROM AlarmData WHERE uid IN (:userIds)")
//    fun loadAllByIds(userIds: IntArray): Flow<List<AlarmData>>
//
//    @Query("SELECT * FROM AlarmData WHERE first_value = :first AND second_value = :last LIMIT 1")
//    fun findByValues(first: Long, last: Long): AlarmData?
//
//    @Insert
//     fun insertAll(alarms: AlarmData)
//
//    @Delete
//    fun delete(alarm: AlarmData)
//}
//
//@Database(entities = [AlarmData::class], version = 1)
//abstract class AppDatabase : RoomDatabase() {
//    abstract fun AlarmDataDAO(): AlarmDataDAO
//    companion object {
//        @Volatile
//        private var INSTANCE: AppDatabase? = null
//        fun getDatabase(context: Context):AppDatabase{
//            return INSTANCE?:synchronized(this) {
//                val instance = Room.databaseBuilder(
//                    context.applicationContext,
//                    AppDatabase::class.java,
//                    "word_database"
//                ).build()
//                INSTANCE = instance
//                // return instance
//                instance
//            }
//        }
//
//    }
//}
//
//
//class AlarmDataRepository(private val alarmDataDao: AlarmDataDAO) {
//    // Room executes all queries on a separate thread.
//    // Observed Flow will notify the observer when the data has changed.
//    val allAlarms: Flow<List<AlarmData>> = alarmDataDao.getAll()
//
//    // By default Room runs suspend queries off the main thread, therefore, we don't need to
//    // implement anything else to ensure we're not doing long running database work
//    // off the main thread.
//
//
////    fun getAllAlarms(): Flow<List<AlarmData>> {
////        return alarmDataDao.getAll()
////    }
////    @WorkerThread
//    suspend fun insert(alarm: AlarmData) {
//        alarmDataDao.insertAll(alarm)
//    }
//
//    @WorkerThread
//    suspend fun delete(alarm: AlarmData) {
//        alarmDataDao.delete(alarm)
//    }
//
//    @WorkerThread
//    suspend fun findByValues(first: Long, second: Long): AlarmData? {
//        return alarmDataDao.findByValues(first, second)
//    }
//
//    fun loadAllByIds(userIds: IntArray): Flow<List<AlarmData>> {
//        return alarmDataDao.loadAllByIds(userIds)
//    }
//}
//




//val db = Room.databaseBuilder(
//    applicationContext,
//    AppDatabase::class.java, "database-name"
//).build()

//
//object DatabaseManager {
//    private var instance: AppDatabase? = null
//
//    fun getInstance(context: Context): AppDatabase {
//        return instance ?: synchronized(this) {
//            instance ?: buildDatabase(context).also { instance = it }
//        }
//    }
//
//    private fun buildDatabase(context: Context): AppDatabase {
//        return Room.databaseBuilder(
//            context.applicationContext,
//            AppDatabase::class.java,
//            "alarm_database"
//        ).build()
//    }
//}
//
//val LocalAppDatabase = staticCompositionLocalOf<AppDatabase> { error("AppDatabase not provided") }
//
//@Composable
//fun ProvideAppDatabase(
//    database: AppDatabase,
//    content: @Composable () -> Unit
//) {
//    CompositionLocalProvider(LocalAppDatabase provides database) {
//        content()
//    }
//}
