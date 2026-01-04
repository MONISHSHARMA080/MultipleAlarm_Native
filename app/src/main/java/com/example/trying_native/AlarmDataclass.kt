package com.example.trying_native.dataBase

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlin.jvm.functions.FunctionN

@Entity(indices = [Index(value = ["first_value", "second_value"])])
data class AlarmData(

    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "first_value") var first_value: Long,
    @ColumnInfo(name = "second_value") var second_value: Long,

    @ColumnInfo(name = "start_time_for_display") val start_time_for_display: String,
    @ColumnInfo(name = "start_am_pm") val start_am_pm: String,
    @ColumnInfo(name = "end_time_for_display") val end_time_for_display: String,
    @ColumnInfo(name = "end_am_pm") val end_am_pm: String,

    @ColumnInfo(name = "date_in_long") val date_in_long: Long,
    @ColumnInfo(name = "message") val message:String,

    @ColumnInfo(name = "freq_in_min") val freq_in_min: Long,

    // we can just add freq_in_min to the start time in millisecond to recreate the behaviour
    /** this is same as the oen used to skip the time just provide it and will just skip it */
    @ColumnInfo(name = "freq_used_to_skip_start_alarm") val freqGottenAfterCallback: Long,

    @ColumnInfo(name = "date_for_display") val date_for_display: String,
    @ColumnInfo(name = "freq_in_min_to_display") val freq_in_min_to_display: Int,
    @ColumnInfo(name = "is_ready_to_use") val isReadyToUse: Boolean
)

@Database(entities = [AlarmData::class], version = 2)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
}

@Dao
interface AlarmDao {

    @Insert
    suspend fun insert(alarmData: AlarmData): Long

    @Query("DELETE FROM AlarmData WHERE first_value = :firstValue AND second_value = :secondValue")
    suspend fun deleteAlarmByValues(firstValue: Long, secondValue: Long): Int

    @Query("SELECT * FROM AlarmData")
    fun getAll(): List<AlarmData>

    @Query("SELECT * FROM AlarmData")
    suspend fun getAllAlarms(): List<AlarmData>

    @Query("SELECT * FROM AlarmData ORDER BY date_in_long ASC, first_value ASC")
    fun getAllAlarmsFlow(): Flow<List<AlarmData>>

    // New function to retrieve an alarm by first_value and second_value
    @Query("SELECT * FROM AlarmData WHERE first_value = :firstValue AND second_value = :secondValue LIMIT 1")
    suspend fun getAlarmByValues(firstValue: Long, secondValue: Long): AlarmData?

    // New function to update the is_ready_to_use field of an alarm
    @Query("UPDATE AlarmData SET is_ready_to_use = :isReadyToUse WHERE first_value = :firstValue AND second_value = :second_value")
    suspend fun updateReadyToUseInAlarm(firstValue: Long, second_value:Long, isReadyToUse: Boolean)

    @Query("SELECT * FROM AlarmData WHERE id = :id")
    suspend fun getAlarmById(id:Int): AlarmData?

    @Query(" UPDATE AlarmData SET is_ready_to_use = :isReadyToUse ,first_value = :firstValue, second_value = :second_value, date_for_display = :date_for_display  WHERE id = :id")
    suspend fun updateAlarmForReset(id: Int, firstValue: Long, second_value: Long, date_for_display: String, isReadyToUse: Boolean)

    @Query("""
        UPDATE AlarmData  SET is_ready_to_use = :isReadyToUse  WHERE first_value = :firstValue 
        AND second_value = :secondValue  AND freq_in_min = :freqInMin AND date_in_long = :dateInLong
    """)
    suspend fun updateReadyToUse(firstValue: Long, secondValue: Long, freqInMin: Long, dateInLong: Long, isReadyToUse: Boolean)
}
