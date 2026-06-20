package MultipleAlarmClock.alarmFeature.domain.model

import MultipleAlarmClock.alarmFeature.data.local.AlarmData
import android.net.Uri
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.logD
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class AlarmObject(
	val startTime: Calendar,
	val endTime: Calendar,
	val date: Long,
	val message:String,
	/** this is same as the oen used to skip the time just provide it and will just skip it */
	val freqGottenAfterCallback: Long,
	val alarmSoundUri: Uri?
){

	fun getFreqInMillisecond(): Long {
		return this.freqGottenAfterCallback * 60000
	}

	fun deepCopy(): AlarmObject {
		return this.copy(
			startTime = (this.startTime.clone() as Calendar),
			endTime = (this.endTime.clone() as Calendar)
		)
	}

	fun incrementDateToCurrentDate(): AlarmObject {
		val now = Calendar.getInstance()
		val alarmDateCal = Calendar.getInstance().apply { timeInMillis = date }

		val isSameDay = alarmDateCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
				alarmDateCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)

		return if (!isSameDay && alarmDateCal.before(now)) {
			val todayYear = now.get(Calendar.YEAR)
			val todayDayOfYear = now.get(Calendar.DAY_OF_YEAR)

			val newStartTime = (startTime.clone() as Calendar).apply {
				set(Calendar.YEAR, todayYear)
				set(Calendar.DAY_OF_YEAR, todayDayOfYear)
			}
			val newEndTime = (endTime.clone() as Calendar).apply {
				set(Calendar.YEAR, todayYear)
				set(Calendar.DAY_OF_YEAR, todayDayOfYear)
			}

			this.copy(
				startTime = newStartTime,
				endTime = newEndTime,
				date = newStartTime.timeInMillis
			)
		} else {
			this
		}
	}

	/**
	 * Yields startTime first, then each subsequent step up to and including endTime.
	 */
	fun alarmTimeSequence(): Sequence<Long> = sequence {
		var current = startTime.timeInMillis
		while (current <= endTime.timeInMillis) {
			yield(current)
			current += getFreqInMillisecond()
		}
	}


	fun toAlarmData(id:Int,isReadyToUse: Boolean = true): AlarmData{
		return AlarmData(
			startTime = startTime.timeInMillis,
			endTime = endTime.timeInMillis,
			message = message,
			id = id,
			frequencyInMin = freqGottenAfterCallback,
			isReadyToUse = isReadyToUse,
			sound = alarmSoundUri?.toString()
		)
	}
	// when we get a weGood == false then we can call this function to see what value produced an error and then display it
	fun validate(alarmData: AlarmData?): ValidationResult{
		logD(" -- validation the alarm")
		if (startTime.timeInMillis >= endTime.timeInMillis) {
			return ValidationResult.Failure( messageResId = R.string.alarm_error_time_range, field = AlarmErrorField.Time)
		}
		if (freqGottenAfterCallback !in 1..700) {
			return ValidationResult.Failure(AlarmErrorField.FREQUENCY, R.string.alarm_error_frequency_range)
		}
		val currentDate = Calendar.getInstance()
		val selectedDate = Calendar.getInstance().apply { timeInMillis = date}
		val dateSame = currentDate.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
		val startAndEndTimeHaveSameDate = startTime.get(Calendar.DAY_OF_YEAR) == endTime.get(Calendar.DAY_OF_YEAR)
		if (!startAndEndTimeHaveSameDate) return ValidationResult.Failure(AlarmErrorField.DATE, R.string.alarm_error_date_mismatch)
		if ( !(dateSame || selectedDate.after(currentDate)) ){
			return ValidationResult.Failure(AlarmErrorField.DATE, R.string.alarm_error_past_date)
		}
		// 2. Check for Changes (If in Edit Mode)
		if (alarmData != null) {
			val hasChanged = startTime.timeInMillis != alarmData.startTime ||
					endTime.timeInMillis != alarmData.endTime ||
					freqGottenAfterCallback != alarmData.frequencyInMin ||
					message != alarmData.message ||
					alarmSoundUri?.toString() != alarmData.sound

			if (!hasChanged) {
				return ValidationResult.Failure(AlarmErrorField.AlarmIsNotDiff, R.string.alarm_error_no_changes)
			}
		}
		return ValidationResult.Success
	}
	private fun getDateTimeFormatted(time:Long):String{
		return SimpleDateFormat("hh:mm a dd/MM/yyyy", Locale.getDefault()).format(time)
	}
	override fun toString(): String {
		return "alarmObject: startTime:${getDateTimeFormatted(startTime.timeInMillis)}, endTime:${getDateTimeFormatted(endTime.timeInMillis)}, date:${getDateTimeFormatted(date)}, message:$message freqGottenAfterCallback:$freqGottenAfterCallback alarmSoundUri:$alarmSoundUri"
	}
}

