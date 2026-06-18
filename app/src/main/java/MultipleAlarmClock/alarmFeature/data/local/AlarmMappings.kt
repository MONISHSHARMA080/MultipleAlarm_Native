package MultipleAlarmClock.alarmFeature.data.local

import MultipleAlarmClock.alarmFeature.domain.model.AlarmObject
import androidx.core.net.toUri
import java.util.Calendar

fun AlarmData.toDomain(): AlarmObject = AlarmObject(
	startTime = Calendar.getInstance().apply { timeInMillis = startTime },
	endTime = Calendar.getInstance().apply { timeInMillis = endTime },
	date = startTime,
	message = message,
	freqGottenAfterCallback = frequencyInMin,
	alarmSoundUri = sound?.toUri()
)

fun AlarmObject.toEntity(id:Int,isReadyToUse: Boolean = true): AlarmData = AlarmData(
	id = id,
	startTime = startTime.timeInMillis,
	endTime = endTime.timeInMillis,
	message = message,
	frequencyInMin = freqGottenAfterCallback,
	isReadyToUse = isReadyToUse,
	sound = alarmSoundUri?.toString()
)