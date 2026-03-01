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
import androidx.room.Upsert
import com.example.trying_native.utils.Result.GenericDataIterator
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Serializable
@Entity(indices = [Index(value = ["first_value", "second_value"])])
data class AlarmData(

    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "startTime") var startTime: Long,
    @ColumnInfo(name = "endTime") var endTime: Long,
    @ColumnInfo(name = "date_in_long") val date: Long,
    @ColumnInfo(name = "message") val message:String,
    /** this is the freq enter by the user */
    @ColumnInfo(name = "freq_used_to_skip_start_alarm") val frequencyInMin: Long,
    @ColumnInfo(name = "is_ready_to_use") val isReadyToUse: Boolean
){
    override fun toString(): String {
        return "{id:$id, startTime:${getDateTimeFormatted(startTime)}, endTime:${getDateTimeFormatted(endTime)}, date:${getDateFormatted(date)}, message:$message, freqGottenAfterCallback:$frequencyInMin, isReadyToUse:$isReadyToUse}"
    }
    fun iterator():AlarmDataIterator  {
        return AlarmDataIterator(this)
    }
    fun iteratorGeneric(): GenericDataIterator<AlarmData, Long> {
        return GenericDataIterator(
            data = this,
            startValue = this.startTime,
            endValue = this.endTime,
            incrementFunction = { alarm, currentTime ->
                currentTime + (alarm.frequencyInMin * 60 * 1000)
            }
        )
    }

    /** converts the freq that we got in min to millisecond for same time, eg 6 min to 6 min in millisecond*/
     fun getFreqInMillisecond(): Long {
        return this.frequencyInMin * 60000
    }
    /** converts the freq that we got in min to millisecond for same time, eg 6 min to 6 min in millisecond*/
     fun getFreqInMillisecond(freqInMin:Long): Long {
        return freqInMin * 60000
    }
    fun getDateTimeFormatted(time:Long):String{
        return SimpleDateFormat("hh:mm a dd/MM/yyyy ", Locale.getDefault()).format(time)
    }
    fun getDateFormatted(time:Long):String{
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(time).trim()
    }
    fun  toAlarmObject():AlarmObject{
        return AlarmObject(startTime = Calendar.getInstance().apply { timeInMillis = startTime }, endTime=Calendar.getInstance().apply { timeInMillis = endTime }, date=date, message=message, freqGottenAfterCallback=frequencyInMin)
    }

    fun isValid(): ValidationResult {
        if (startTime > endTime) return ValidationResult(false, "expected startTime:${getDateTimeFormatted(startTime)} to be greater than endTime:${getDateTimeFormatted(endTime)}")
        if (frequencyInMin !in 1..700) return ValidationResult(false, "Expected frequency must be between 1 and 700 minutes.")
        val startTimeCal = Calendar.getInstance().apply { timeInMillis = startTime }
        val endTimeCal = Calendar.getInstance().apply { timeInMillis = endTime }
        val dateCal = Calendar.getInstance().apply { timeInMillis = date }
        val dateNotSame = startTimeCal.get(Calendar.DAY_OF_YEAR) != endTimeCal.get(Calendar.DAY_OF_YEAR) || startTimeCal.get(Calendar.YEAR) == dateCal.get(Calendar.DAY_OF_YEAR)
        if (dateNotSame) return ValidationResult(false, "Expected the date to be same but got date for startTime:${getDateFormatted(startTime)}, endTime:${getDateFormatted(endTime)}, Date:${getDateFormatted(date)}")
        return ValidationResult(true, "")
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

    @Query("DELETE FROM AlarmData WHERE startTime= :firstValue AND endTime = :secondValue")
    suspend fun deleteAlarm(firstValue: Long, secondValue: Long): Int

    @Query("DELETE FROM AlarmData WHERE id = :id")
    suspend fun deleteAlarm(id: Int): Int

    @Delete
    suspend fun deleteAlarm(alarm: AlarmData): Int

    @Query("SELECT * FROM AlarmData")
    fun getAll(): List<AlarmData>

    @Query("SELECT * FROM AlarmData")
    suspend fun getAllAlarms(): List<AlarmData>

//    @Query("SELECT * FROM AlarmData ORDER BY date_in_long ASC, first_value ASC")
    @Query("SELECT * FROM AlarmData ORDER BY startTime ASC, endTime ASC")
    fun getAllAlarmsFlow(): Flow<List<AlarmData>>

    // New function to retrieve an alarm by first_value and second_value
    @Query("SELECT * FROM AlarmData WHERE startTime = :firstValue AND endTime = :secondValue LIMIT 1")
    suspend fun getAlarmByValues(firstValue: Long, secondValue: Long): AlarmData?

    /**-1L when update occurs and rest is the row ID*/
    @Upsert
    suspend fun updateOrInsert(alarmData: AlarmData): Long

    // New function to update the is_ready_to_use field of an alarm
    @Query("UPDATE AlarmData SET is_ready_to_use = :isReadyToUse WHERE startTime = :startTime AND endTime = :endTime")
    suspend fun updateReadyToUseInAlarm(startTime: Long, endTime: Long, isReadyToUse: Boolean)

    @Update
    suspend fun updateAlarm(alarmData: AlarmData): Int  // Returns number of rows updated

    @Query("SELECT * FROM AlarmData WHERE id = :id")
    suspend fun getAlarmById(id: Int): AlarmData?

    @Query(" UPDATE AlarmData SET is_ready_to_use = :isReadyToUse , startTime = :firstValue, endTime = :second_value  WHERE id = :id")
    suspend fun updateAlarmForReset(id: Int, firstValue: Long, second_value: Long, isReadyToUse: Boolean)

    @Update
    suspend fun updateAlarmForReset(alarmData: AlarmData): Long

    @Query("DELETE  FROM AlarmData")
    suspend fun deleteAllAlarmsFromDb()
}

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String
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
                freqGottenAfterCallback != alarmData.frequencyInMin || message != alarmData.message || date != alarmData.date
        return baseValidation && hasChanged
	}
    fun toAlarmData(id:Int,isReadyToUse: Boolean = true): AlarmData{
        return AlarmData(
            startTime = startTime.timeInMillis,
            endTime = endTime.timeInMillis,
            date = date,
            message = message,
            id = id,
            frequencyInMin = freqGottenAfterCallback,
            isReadyToUse = isReadyToUse
        )
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
                    freqGottenAfterCallback != alarmData.frequencyInMin ||
                    message != alarmData.message ||
                    date != alarmData.date

            if (!hasChanged) {
                return ValidationResult(false, "No changes detected. Change something to update the alarm.")
            }
        }
        // 3. Everything is fine
        return ValidationResult(true, "")
    }
    private fun getDateTimeFormatted(time:Long):String{
        return SimpleDateFormat("hh:mm a dd/MM/yyyy", Locale.getDefault()).format(time)
    }
    override fun toString(): String {
        return "startTime:${getDateTimeFormatted(startTime.timeInMillis)}, endTime:${getDateTimeFormatted(endTime.timeInMillis)}, date:${getDateTimeFormatted(date)}, message:$message freqGottenAfterCallback:$freqGottenAfterCallback"
    }
}

class AlarmDataIterator(alarm: AlarmData) {
    val alarmData = alarm
    var currentTime: Long = alarmData.startTime

    fun hasNext(): Boolean {
        return  alarmData.endTime >= currentTime
    }
    fun next():Long {
        val newAlarm = currentTime + alarmData.getFreqInMillisecond()
        currentTime = newAlarm
        return newAlarm
    }
}


