package com.coolApps.MultipleAlarmClock.alarmFeature.repository

import android.content.Context
import android.media.RingtoneManager
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker.data.AlarmSound
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext


class AlarmSoundRepository @Inject constructor(
		@ApplicationContext private val context: Context
) {
	fun getAlarmSoundsStream(): Flow<List<AlarmSound>> = flow {
		emit(withContext(Dispatchers.IO) { queryRingtones() })
	}

	private fun queryRingtones(): List<AlarmSound> {
		val ringtoneManager = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_ALARM) }
		val cursor = ringtoneManager.cursor
		val sounds = mutableListOf<AlarmSound>()
		while (cursor.moveToNext()) {
			val position = cursor.position
			val title = ringtoneManager.getRingtone(position)?.getTitle(context) ?: "Unknown"
			sounds += AlarmSound(title = title, soundUri = ringtoneManager.getRingtoneUri(position))
		}
		cursor.close()
		return sounds
	}
}
