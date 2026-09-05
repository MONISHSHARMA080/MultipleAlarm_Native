package com.coolApps.MultipleAlarmClock.alarmFeature.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Serializable
@Entity(indices = [Index(value = ["startTime", "endTime"])], )
data class AlarmData(
	@PrimaryKey(autoGenerate = true) val id: Int = 0,
	// this start time and end time denotes the time on say day,
	@ColumnInfo(name = "startTime") val startTime: Long,
	@ColumnInfo(name = "endTime") val endTime: Long,
	@ColumnInfo(name = "message") val message: String,
	@ColumnInfo(name = "freq_used_to_skip_start_alarm") val frequencyInMin: Long,
	val sound: String?,
	@ColumnInfo(name = "is_ready_to_use") val isReadyToUse: Boolean,
	@ColumnInfo(name = "repeat_days", defaultValue = "NULL") val repeatDays: RepeatDays? = null
){
	private fun getDateTimeFormatted(time:Long):String{
		return SimpleDateFormat("hh:mm a dd/MM/yyyy", Locale.getDefault()).format(time)
	}
	override fun toString(): String {
		return "AlarmData: startTime:${getDateTimeFormatted(startTime)}, endTime:${getDateTimeFormatted(endTime)}, message:$message freqGottenAfterCallback:$frequencyInMin alarmSoundUri:$sound repeatDays:$repeatDays"
	}

	val startTimeCalendar: Calendar get() = Calendar.getInstance().apply { timeInMillis = startTime }

	val endTimeCalendar: Calendar get() = Calendar.getInstance().apply { timeInMillis = endTime }

	fun rollOverIfTimeIntervalPassed(now: Calendar = Calendar.getInstance()): AlarmData{
		// Keep advancing a full day at a time until the interval's end is no longer in the past.
		// This lets an alarm that's several days stale jump straight to
		// the right date in one shot (today, if today's slot hasn't passed yet -
		// otherwise tomorrow) instead of needing one call per day of drift.
		val durationMillis = endTime - startTime
		val startCalendar = Calendar.getInstance().apply { timeInMillis = startTime }
		while (startCalendar.timeInMillis + durationMillis < now.timeInMillis) {
			startCalendar.add(Calendar.DAY_OF_YEAR, 1)
		}
		// Step 2: if repeat days are set, keep advancing until we land on one of them.
		// Bounded to at most 6 extra iterations since RepeatDays can never be empty.
		repeatDays?.let { days ->
				val landedOn = startCalendar.toLocalDate()
				val nextValid = days.nextRepeatDate(landedOn) ?: landedOn
				val deltaDays = ChronoUnit.DAYS.between(landedOn, nextValid).toInt()
				if (deltaDays > 0) {
					startCalendar.add(Calendar.DAY_OF_YEAR, deltaDays)
				}
		}

		val candidateEnd = (startCalendar.clone() as Calendar).apply {
			timeInMillis = startCalendar.timeInMillis + durationMillis
		}
		return AlarmData(
			startTime = startCalendar.timeInMillis,
			endTime = candidateEnd.timeInMillis,
			frequencyInMin = this.frequencyInMin,
			sound = this.sound,
			message = this.message,
			isReadyToUse = this.isReadyToUse,
			repeatDays = this.repeatDays,
			id = id
		)
	}

	private fun Calendar.toLocalDate(): LocalDate = LocalDate.of(get(Calendar.YEAR), get(Calendar.MONTH) + 1, get(Calendar.DAY_OF_MONTH))

	fun alarmTimeSequence(): Sequence<Long> = sequence {
		var current = startTime
		while (current <= endTime) {
			yield(current)
			current += getFreqInMillisecond()
		}
	}

	// long means that the time is not found
	fun getNextAlarmTriggerTime(now: Calendar = Calendar.getInstance()): Long? {
		return alarmTimeSequence().firstOrNull{ it > now.timeInMillis }

	}

	fun getFreqInMillisecond(): Long {
		return this.frequencyInMin * 60000
	}


	fun validate():AlarmDataValidationResult {
		if ( startTime >= endTime ) {
			return AlarmDataValidationResult.TimeIntervalError("StartTime:${timeFormatted(startTime)} must be less than endTime:${timeFormatted(endTime)}.")
		}
		if (frequencyInMin !in 1..700) {
			return AlarmDataValidationResult.Frequency("Expected a value between 1 to 700 minutes, but Got $frequencyInMin")
		}
		val now = Calendar.getInstance()
		val startCalendar = Calendar.getInstance().apply { timeInMillis = startTime }
		val endCalendar = Calendar.getInstance().apply { timeInMillis = endTime }

		if (startCalendar.get(Calendar.DAY_OF_YEAR) != endCalendar.get(Calendar.DAY_OF_YEAR) || startCalendar.get(Calendar.YEAR) != endCalendar.get(Calendar.YEAR)) {
			return AlarmDataValidationResult.DifferentDate("StartTime:${timeFormatted(startTime)} and endTime:${timeFormatted(endTime)} must have same date")
		}

		// 9:00 - 11:00 and now is 10:00, 8:00, 11:30
		if (endCalendar.timeInMillis < now.timeInMillis){
			return AlarmDataValidationResult.IntervalNotInFuture("StartTime:${timeFormatted(startTime)} should be in future ahead of now:${ timeFormatted(now.timeInMillis) }.")
		}
		// maybe for the weekday check is the bit a day of week, for future

//		if (repeatDays != null) {
//			val localDate = LocalDate.ofInstant(startCalendar.toInstant(), startCalendar.timeZone.toZoneId())
//			if (!repeatDays.isSet(localDate.dayOfWeek)) {
//				return AlarmDataValidationResult.WeekdayMismatch("Start date weekday (${localDate.dayOfWeek}) is not in configured repeatDays ($repeatDays)")
//			}
//		}

		return AlarmDataValidationResult.Success
	}

	private fun timeFormatted(x: Long): String {
		return SimpleDateFormat("h:mm:ss a yyyy-MM-dd", Locale.getDefault()).format(Date(x))
	}

}


/**[IntervalNotInFuture]-increment the date to fix that*/
sealed class AlarmDataValidationResult(
		private val errorMessage: String? = null
){
	data object Success: AlarmDataValidationResult()
	/** startTime must be less than end time*/
	data class TimeIntervalError( val errorMessage: String) : AlarmDataValidationResult(errorMessage)
	data class IntervalNotInFuture( val errorMessage: String) : AlarmDataValidationResult(errorMessage)
	data class DifferentDate( val errorMessage: String) : AlarmDataValidationResult(errorMessage)
	data class Frequency( val errorMessage: String) : AlarmDataValidationResult(errorMessage)
//	data class WeekdayMismatch( val errorMessage: String) : AlarmDataValidationResult(errorMessage)

	override fun toString(): String {
		var err = errorMessage
		err = if (err?.isEmpty() == true){
			""
		}else{
			"ErrorMessage: $err"
		}
		val className = when(this){
			Success ->"Success"
			is TimeIntervalError -> "TimeIntervalError"
			is IntervalNotInFuture ->"IntervalNotInFuture"
			is DifferentDate -> "DifferentDate"
			is Frequency -> "Frequency"
//			is WeekdayMismatch -> "WeekdayMismatch"
		}
		return "$className, $err "
	}

	fun isFailure(): Boolean{
		return this !is Success
	}

	fun errorMessage():String{
		return when(this){
			Success ->""
			is TimeIntervalError -> this.errorMessage
			is IntervalNotInFuture -> this.errorMessage
			is DifferentDate ->this.errorMessage
			is Frequency ->this.errorMessage
//			is WeekdayMismatch -> this.errorMessage
		}
	}
}
