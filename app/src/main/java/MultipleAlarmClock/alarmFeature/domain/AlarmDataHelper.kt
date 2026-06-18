package MultipleAlarmClock.alarmFeature.domain

import MultipleAlarmClock.alarmFeature.data.local.AlarmData
import MultipleAlarmClock.alarmFeature.domain.model.ValidationResultAlarmData
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun AlarmData.getFreqInMillisecond(): Long = frequencyInMin * 60_000L

fun AlarmData.isValid(): ValidationResultAlarmData {
	if (startTime > endTime) return ValidationResultAlarmData(
		false,
		"expected startTime:${getDateTimeFormatted(startTime)} to be greater than endTime:${
			getDateTimeFormatted(endTime)
		}"
	)
	if (frequencyInMin !in 1..700) return ValidationResultAlarmData(
		false,
		"Expected frequency must be between 1 and 700 minutes."
	)
	val startTimeCal = Calendar.getInstance().apply { timeInMillis = startTime }
	val endTimeCal = Calendar.getInstance().apply { timeInMillis = endTime }
	val startDate = startTimeCal.get(Calendar.YEAR) to startTimeCal.get(Calendar.DAY_OF_YEAR)
	val endDate = endTimeCal.get(Calendar.YEAR) to endTimeCal.get(Calendar.DAY_OF_YEAR)
	if (startDate != endDate ) return ValidationResultAlarmData(
		false,
		"Expected the date to be same but got date for startTime:${getDateFormatted(startTime)}, endTime:${
			getDateFormatted(endTime)
		}"
	)
	return ValidationResultAlarmData(true, "")
}


private fun getDateTimeFormatted(time:Long):String{
	return SimpleDateFormat("hh:mm a dd/MM/yyyy ", Locale.getDefault()).format(time)
}
private fun getDateFormatted(time:Long):String{
	return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(time).trim()
}
