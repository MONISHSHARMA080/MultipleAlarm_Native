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
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Locale

@Entity(indices = [Index(value = ["first_value", "second_value"])])
data class AlarmData(

    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "first_value") var startTime: Long,
    @ColumnInfo(name = "second_value") var endTime: Long,

//    @ColumnInfo(name = "start_time_for_display") val start_time_for_display: String, // don't need as create it on fly
//    @ColumnInfo(name = "end_am_pm") val end_am_pm: String, // **** here we can make both of them as bools and use a simple ****
    // case to get it
//    @ColumnInfo(name = "start_am_pm") val start_am_pm: String,
//    @ColumnInfo(name = "end_time_for_display") val end_time_for_display: String,

    @ColumnInfo(name = "date_in_long") val date: Long,
    @ColumnInfo(name = "message") val message:String,

//    @ColumnInfo(name = "freq_in_min") val freq_in_min: Long,// don't need

    // we can just add freq_in_min to the start time in millisecond to recreate the behaviour
    /** this is same as the oen used to skip the time just provide it and will just skip it */
    @ColumnInfo(name = "freq_used_to_skip_start_alarm") val freqGottenAfterCallback: Long,

//    @ColumnInfo(name = "date_for_display") val date_for_display: String, // can create it during the run time
//    @ColumnInfo(name = "freq_in_min_to_display") val freq_in_min_to_display: Int, // use the freqGottenAfterCallback, don't need
    @ColumnInfo(name = "is_ready_to_use") val isReadyToUse: Boolean
){
    /** converts the freq that we got in min to millisecond for same time, eg 6 min to 6 min in millisecond*/
     fun getFreqInMillisecond(): Long {
        return this.freqGottenAfterCallback * 60000
    }
    /** converts the freq that we got in min to millisecond for same time, eg 6 min to 6 min in millisecond*/
     fun getFreqInMillisecond(freqInMin:Long): Long {
        return freqInMin * 60000
    }
    fun getTimeFormatted(time:Long):String{
        return SimpleDateFormat("hh:mm", Locale.getDefault()).format(time)
    }
    fun getFormattedAmPm(time:Long):String{
        return SimpleDateFormat("a", Locale.getDefault()).format(time).trim()
    }
    fun getDateFormatted(time:Long):String{
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(time).trim()
    }

}

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

    @Query(" UPDATE AlarmData SET is_ready_to_use = :isReadyToUse ,first_value = :firstValue, second_value = :second_value  WHERE id = :id")
    suspend fun updateAlarmForReset(id: Int, firstValue: Long, second_value: Long,  isReadyToUse: Boolean)

    @Update
    suspend fun updateAlarmForReset(alarmData: AlarmData)

    @Query("SELECT ")
    suspend fun updateAlarmAndGetUpdatedValue(alarmData: AlarmData): AlarmData

//    @Query("""
//        UPDATE AlarmData  SET is_ready_to_use = :isReadyToUse  WHERE first_value = :firstValue
//        AND second_value = :secondValue  AND freq_in_min = :freqInMin AND date_in_long = :dateInLong
//    """)
//    suspend fun updateReadyToUse(firstValue: Long, secondValue: Long, freqInMin: Long, dateInLong: Long, isReadyToUse: Boolean)
}
