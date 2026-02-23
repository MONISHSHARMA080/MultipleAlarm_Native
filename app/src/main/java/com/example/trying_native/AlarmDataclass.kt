package com.example.trying_native.dataBase

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.trying_native.logD
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Serializable
@Entity(indices = [Index(value = ["first_value", "second_value"])])
data class AlarmData(

    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "first_value") var startTime: Long,
    @ColumnInfo(name = "second_value") var endTime: Long,
    @ColumnInfo(name = "date_in_long") val date: Long,
    @ColumnInfo(name = "message") val message:String,
    // we can just add freq_in_min to the start time in millisecond to recreate the behaviour
    /** this is same as the oen used to skip the time just provide it and will just skip it */
    @ColumnInfo(name = "freq_used_to_skip_start_alarm") val freqGottenAfterCallback: Long,
    @ColumnInfo(name = "is_ready_to_use") val isReadyToUse: Boolean
){
    override fun toString(): String {
        return "{id:$id, startTime:${getDateTimeFormatted(startTime)}, endTime:${getDateTimeFormatted(endTime)}, date:$date, message:$message, freqGottenAfterCallback:$freqGottenAfterCallback, isReadyToUse:$isReadyToUse}"
    }
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
    fun getDateTimeFormatted(time:Long):String{
        return SimpleDateFormat("hh:mm a dd/MM/yyyy ", Locale.getDefault()).format(time)
    }
    fun getFormattedAmPm(time:Long):String{
        return SimpleDateFormat("a", Locale.getDefault()).format(time).trim()
    }
    fun getDateFormatted(time:Long):String{
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(time).trim()
    }
    fun  toAlarmObject():AlarmObject{
        return AlarmObject(startTime = Calendar.getInstance().apply { timeInMillis = startTime }, endTime=Calendar.getInstance().apply { timeInMillis = endTime }, date=date, message=message, freqGottenAfterCallback=freqGottenAfterCallback)
    }
}

@Database(entities = [AlarmData::class], version = 1)
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

//    @Query("SELECT * FROM AlarmData ORDER BY date_in_long ASC, first_value ASC")
    @Query("SELECT * FROM AlarmData ORDER BY first_value ASC, second_value ASC")
    fun getAllAlarmsFlow(): Flow<List<AlarmData>>

    // New function to retrieve an alarm by first_value and second_value
    @Query("SELECT * FROM AlarmData WHERE first_value = :firstValue AND second_value = :secondValue LIMIT 1")
    suspend fun getAlarmByValues(firstValue: Long, secondValue: Long): AlarmData?

    // New function to update the is_ready_to_use field of an alarm
    @Query("UPDATE AlarmData SET is_ready_to_use = :isReadyToUse WHERE first_value = :firstValue AND second_value = :second_value")
    suspend fun updateReadyToUseInAlarm(firstValue: Long, second_value: Long, isReadyToUse: Boolean)

    @Query("SELECT * FROM AlarmData WHERE id = :id")
    suspend fun getAlarmById(id: Int): AlarmData?

    @Query(" UPDATE AlarmData SET is_ready_to_use = :isReadyToUse ,first_value = :firstValue, second_value = :second_value  WHERE id = :id")
    suspend fun updateAlarmForReset(id: Int, firstValue: Long, second_value: Long, isReadyToUse: Boolean)

    @Update
    suspend fun updateAlarmForReset(alarmData: AlarmData)

    @Query("DELETE  FROM AlarmData")
    suspend fun deleteAllAlarmsFromDb()


}

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)
data class AlarmObject(
    val startTime: Calendar,
    val endTime: Calendar,
    val date: Long,
    val message:String,
    /** this is same as the oen used to skip the time just provide it and will just skip it */
    val freqGottenAfterCallback: Long
){
    fun isOk(alarmData: AlarmData? = null): Boolean{
        val currentDate = Calendar.getInstance()
        val selectedDate = Calendar.getInstance().apply { timeInMillis = date}
        val dateSame = currentDate.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
        val baseValidation = startTime.timeInMillis < endTime.timeInMillis && freqGottenAfterCallback in 1..700 && (dateSame || selectedDate.after(currentDate))
        if (alarmData == null) return baseValidation
        val hasChanged = startTime.timeInMillis != alarmData.startTime || endTime.timeInMillis != alarmData.endTime ||
                freqGottenAfterCallback != alarmData.freqGottenAfterCallback || message != alarmData.message || date != alarmData.date
        return baseValidation && hasChanged
	}
    // when we get a weGood == false then we can call this function to see what value produced an error and then display it
    fun validate(alarmData: AlarmData?): ValidationResult{
        if (startTime.timeInMillis >= endTime.timeInMillis) {
            return ValidationResult(false, "Start time must be before end time.")
        }
        if (freqGottenAfterCallback !in 1..700) {
            return ValidationResult(false, "Frequency must be between 1 and 700 minutes.")
        }
        val currentDate = Calendar.getInstance()
        val selectedDate = Calendar.getInstance().apply { timeInMillis = date}
        val dateSame = currentDate.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
        if ( !(dateSame || selectedDate.after(currentDate)) ){
            return ValidationResult(false, "Date must be today or in the future.")
        }
        // 2. Check for Changes (If in Edit Mode)
        if (alarmData != null) {
            val hasChanged = startTime.timeInMillis != alarmData.startTime ||
                    endTime.timeInMillis != alarmData.endTime ||
                    freqGottenAfterCallback != alarmData.freqGottenAfterCallback ||
                    message != alarmData.message ||
                    date != alarmData.date

            if (!hasChanged) {
                return ValidationResult(false, "No changes detected. Change something to update the alarm.")
            }
        }

        // 3. Everything is fine
        return ValidationResult(true)
    }
    private fun getDateTimeFormatted(time:Long):String{
        return SimpleDateFormat("hh:mm a dd/MM/yyyy", Locale.getDefault()).format(time)
    }
    fun toAlarmData(alarmId:Int,isReadyToUse: Boolean = true ): AlarmData{
       return AlarmData(id = alarmId, startTime = startTime.timeInMillis, endTime= endTime.timeInMillis, date= date, message= message,  freqGottenAfterCallback=freqGottenAfterCallback, isReadyToUse = isReadyToUse)
    }
    override fun toString(): String {
        return "startTime:${getDateTimeFormatted(startTime.timeInMillis)}, endTime:${getDateTimeFormatted(endTime.timeInMillis)}, date:${getDateTimeFormatted(date)}, message:$message freqGottenAfterCallback:$freqGottenAfterCallback"
    }
}
