package com.coolApps.MultipleAlarmClock.dataBase

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
import com.coolApps.MultipleAlarmClock.utils.Result.GenericDataIterator
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Serializable
@Entity(indices = [Index(value = ["startTime", "endTime"])])
data class AlarmData(

    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "startTime") var startTime: Long,
    @ColumnInfo(name = "endTime") var endTime: Long,
    @ColumnInfo(name = "message") val message:String,
    /** this is the freq enter by the user */
    @ColumnInfo(name = "freq_used_to_skip_start_alarm") val frequencyInMin: Long,
    @ColumnInfo(name = "is_ready_to_use") val isReadyToUse: Boolean
){
    override fun toString(): String {
        return "alarmData: id:$id, startTime:${getDateTimeFormatted(startTime)}, endTime:${getDateTimeFormatted(endTime)}, date:${getDateFormatted(startTime)}, message:$message, freqGottenAfterCallback:$frequencyInMin, isReadyToUse:$isReadyToUse"
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
        return AlarmObject(startTime = Calendar.getInstance().apply { timeInMillis = startTime }, endTime=Calendar.getInstance().apply { timeInMillis = endTime }, message=message, freqGottenAfterCallback=frequencyInMin, date = startTime)
    }

    fun isValid(): ValidationResultAlarmData {
        if (startTime > endTime) return ValidationResultAlarmData(false, "expected startTime:${getDateTimeFormatted(startTime)} to be greater than endTime:${getDateTimeFormatted(endTime)}")
        if (frequencyInMin !in 1..700) return ValidationResultAlarmData(false, "Expected frequency must be between 1 and 700 minutes.")
        val startTimeCal = Calendar.getInstance().apply { timeInMillis = startTime }
        val endTimeCal = Calendar.getInstance().apply { timeInMillis = endTime }
        val startDate = startTimeCal.get(Calendar.YEAR) to startTimeCal.get(Calendar.DAY_OF_YEAR)
        val endDate = endTimeCal.get(Calendar.YEAR) to endTimeCal.get(Calendar.DAY_OF_YEAR)
        if (startDate != endDate ) return ValidationResultAlarmData(false, "Expected the date to be same but got date for startTime:${getDateFormatted(startTime)}, endTime:${getDateFormatted(endTime)}")
        return ValidationResultAlarmData(true, "")
    }

}

@Database(entities = [AlarmData::class], version = 1, )
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
    suspend fun updateAlarmForReset(alarmData: AlarmData): Int

    @Query("DELETE  FROM AlarmData")
    suspend fun deleteAllAlarmsFromDb()
}


data class ValidationResultAlarmData(
    val isValid: Boolean,
    val errorMessage: String
)

enum class AlarmErrorField {
    Time,
//    EndTime, // since not useful
    DATE, FREQUENCY, MESSAGE, AlarmIsNotDiff
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Failure(val field: AlarmErrorField, val message: String) : ValidationResult()
}
data class AlarmObject(
    val startTime: Calendar,
    val endTime: Calendar,
    val date: Long,
    val message:String,
    /** this is same as the oen used to skip the time just provide it and will just skip it */
    val freqGottenAfterCallback: Long
){
//    fun isOk(alarmData: AlarmData? ): Boolean{
//        val currentDate = Calendar.getInstance()
//        val selectedDate = Calendar.getInstance().apply { timeInMillis = date}
//        val dateSame = currentDate.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
//        val sameDateInTime = startTime.get(Calendar.DAY_OF_YEAR) == endTime.get(Calendar.DAY_OF_YEAR)
//        val baseValidation = startTime.timeInMillis < endTime.timeInMillis && freqGottenAfterCallback in 1..700 && (dateSame || selectedDate.after(currentDate)) &&sameDateInTime
//        if (alarmData == null) return baseValidation
//        val alarmObjDate = Calendar.getInstance().apply { timeInMillis = date }
//        val alarmDataDate = Calendar.getInstance().apply { timeInMillis = alarmData.startTime }
//
//        val dateChanged = alarmObjDate.get(Calendar.DAY_OF_YEAR) != alarmDataDate.get(Calendar.DAY_OF_YEAR) ||
//                alarmObjDate.get(Calendar.YEAR) != alarmDataDate.get(Calendar.YEAR)
//
//        val hasChanged = startTime.timeInMillis != alarmData.startTime || endTime.timeInMillis != alarmData.endTime ||
//                freqGottenAfterCallback != alarmData.frequencyInMin || message != alarmData.message || dateChanged
//
//        return baseValidation && hasChanged
//	}

    fun getFreqInMillisecond(): Long {
        return this.freqGottenAfterCallback * 60000
    }

    fun deepCopy(): AlarmObject {
        return this.copy(
            startTime = (this.startTime.clone() as Calendar),
            endTime = (this.endTime.clone() as Calendar)
        )
    }

    fun toAlarmData(id:Int,isReadyToUse: Boolean = true): AlarmData{
        return AlarmData(
            startTime = startTime.timeInMillis,
            endTime = endTime.timeInMillis,
            message = message,
            id = id,
            frequencyInMin = freqGottenAfterCallback,
            isReadyToUse = isReadyToUse
        )
    }
    // when we get a weGood == false then we can call this function to see what value produced an error and then display it
    fun validate(alarmData: AlarmData?): ValidationResult{
        if (startTime.timeInMillis >= endTime.timeInMillis) {
            return ValidationResult.Failure( message = "Start time must be before end time.", field = AlarmErrorField.Time)
        }
        if (freqGottenAfterCallback !in 1..700) {
            return ValidationResult.Failure(AlarmErrorField.FREQUENCY, "Frequency must be between 1 and 700 minutes.")
        }
        val currentDate = Calendar.getInstance()
        val selectedDate = Calendar.getInstance().apply { timeInMillis = date}
        val dateSame = currentDate.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
        val startAndEndTimeHaveSameDate = startTime.get(Calendar.DAY_OF_YEAR) == endTime.get(Calendar.DAY_OF_YEAR)
        if (!startAndEndTimeHaveSameDate) return ValidationResult.Failure(AlarmErrorField.DATE, "expected the date in startTime and endTime to be same but got startTimeDate:${getDateTimeFormatted(startTime.timeInMillis)} endDateTime:${getDateTimeFormatted(endTime.timeInMillis)}")
        if ( !(dateSame || selectedDate.after(currentDate)) ){
            return ValidationResult.Failure(AlarmErrorField.DATE, "Date must be today or in the future.")
        }
        // 2. Check for Changes (If in Edit Mode)
        if (alarmData != null) {
            val hasChanged = startTime.timeInMillis != alarmData.startTime ||
                    endTime.timeInMillis != alarmData.endTime ||
                    freqGottenAfterCallback != alarmData.frequencyInMin ||
                    message != alarmData.message

            if (!hasChanged) {
                return ValidationResult.Failure(AlarmErrorField.AlarmIsNotDiff, "No changes detected. Change something to update the alarm.")
            }
        }
        return ValidationResult.Success
    }
    private fun getDateTimeFormatted(time:Long):String{
        return SimpleDateFormat("hh:mm a dd/MM/yyyy", Locale.getDefault()).format(time)
    }
    override fun toString(): String {
        return "alarmObject: startTime:${getDateTimeFormatted(startTime.timeInMillis)}, endTime:${getDateTimeFormatted(endTime.timeInMillis)}, date:${getDateTimeFormatted(date)}, message:$message freqGottenAfterCallback:$freqGottenAfterCallback"
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


