package com.coolApps.MultipleAlarmClock.alarmFeature.repository

import android.content.Context
import android.media.RingtoneManager
import androidx.core.net.toUri
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

	fun resolveSound(uriString: String?): AlarmSound? {
		if (uriString == null) return null
		val uri = uriString.toUri()
		val title = RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: return null
		return AlarmSound(title = title, soundUri = uri)
	}
}
