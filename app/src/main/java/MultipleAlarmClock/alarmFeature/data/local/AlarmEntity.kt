package MultipleAlarmClock.alarmFeature.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Serializable
@Entity(indices = [Index(value = ["startTime", "endTime"])], )
data class AlarmData(
	@PrimaryKey(autoGenerate = true) val id: Int = 0,
	@ColumnInfo(name = "startTime") val startTime: Long,
	@ColumnInfo(name = "endTime") val endTime: Long,
	@ColumnInfo(name = "message") val message: String,
	@ColumnInfo(name = "freq_used_to_skip_start_alarm") val frequencyInMin: Long,
	val sound: String?,
	@ColumnInfo(name = "is_ready_to_use") val isReadyToUse: Boolean
){
	private fun getDateTimeFormatted(time:Long):String{
		return SimpleDateFormat("hh:mm a dd/MM/yyyy", Locale.getDefault()).format(time)
	}
	override fun toString(): String {
		return "AlarmData: startTime:${getDateTimeFormatted(startTime)}, endTime:${getDateTimeFormatted(endTime)}, message:$message freqGottenAfterCallback:$frequencyInMin alarmSoundUri:$sound"
	}

	fun rollOverIfTimeIntervalPassed(now: Calendar = Calendar.getInstance()): AlarmData{
		val durationMillis = endTime - startTime
		val startCalendar = Calendar.getInstance().apply {timeInMillis = startTime  }
		val endCalendar = Calendar.getInstance().apply {timeInMillis = endTime  }


		if (endCalendar.before(now)) {
			startCalendar.add(Calendar.DAY_OF_YEAR, 1)
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
			isReadyToUse = this.isReadyToUse
		)
	}

	fun alarmTimeSequence(): Sequence<Long> = sequence {
		var current = startTime
		while (current <= endTime) {
			yield(current)
			current += getFreqInMillisecond()
		}
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

		if (startCalendar.get(Calendar.DAY_OF_YEAR) !=endCalendar.get(Calendar.DAY_OF_YEAR)  &&   startCalendar.get(Calendar.YEAR) != endCalendar.get(Calendar.YEAR)  ) {
			return AlarmDataValidationResult.DifferentDate("StartTime:${timeFormatted(startTime)} and endTime:${timeFormatted(endTime)} must have same date")
		}

		// 9:00 - 11:00 and now is 10:00, 8:00, 11:30
		if (endCalendar.timeInMillis < now.timeInMillis){
			return AlarmDataValidationResult.IntervalNotInFuture("StartTime:${timeFormatted(startTime)} should be in future ahead of now:${ timeFormatted(now.timeInMillis) }.")
		}
		return AlarmDataValidationResult.Success
	}

	private fun timeFormatted(x: Long): String {
		return SimpleDateFormat("h:mm:ss a yyyy-MM-dd", Locale.getDefault()).format(Date(x))

	}

}


/**[IntervalNotInFuture]-increment the date to fix that*/
sealed interface AlarmDataValidationResult{
	data object Success: AlarmDataValidationResult
	/** startTime must be less than end time*/
	data class TimeIntervalError(val errorMessage: String):AlarmDataValidationResult
	data class IntervalNotInFuture(val errorMessage: String):AlarmDataValidationResult
	data class DifferentDate(val errorMessage: String): AlarmDataValidationResult // for end time and start time
	data class Frequency(val errorMessage: String): AlarmDataValidationResult

	fun className(): String{
		return when(this){
			Success ->"Success"
			is TimeIntervalError -> "TimeIntervalError"
			is IntervalNotInFuture ->"IntervalNotInFuture"
			is DifferentDate -> "DifferentDate"
			is Frequency -> "Frequency"
		}
	}
	fun getErrorMessage(): String{
		return when(this){
			Success ->""
			is TimeIntervalError -> this.errorMessage
			is IntervalNotInFuture ->this.errorMessage
			is DifferentDate ->this.errorMessage
			is Frequency ->this.errorMessage
		}
	}
}
