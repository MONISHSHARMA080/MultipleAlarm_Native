package com.example.MultipleAlarmClock.Ui.alarmPicker.repositry

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import com.example.MultipleAlarmClock.Ui.alarmPicker.data.AlarmSound
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton

interface AlarmSoundRepository {
	suspend fun getAlarmSounds(): List<AlarmSound>
}

@Singleton
class AlarmSoundSRepository @Inject constructor(
	@ApplicationContext
	private val context: Context
) : AlarmSoundRepository {

	override suspend fun getAlarmSounds(): List<AlarmSound> {

		val ringtoneManager = RingtoneManager(context).apply {
			setType(RingtoneManager.TYPE_ALARM)
		}

		val cursor = ringtoneManager.cursor
		val sounds = mutableListOf<AlarmSound>()

		while (cursor.moveToNext()) {

			val position = cursor.position

			val title =
				ringtoneManager.getRingtone(position)
					?.getTitle(context) ?: continue

			val uri = ringtoneManager.getRingtoneUri(position)

			sounds += AlarmSound(
				title = title,
				soundUri = uri
			)
		}
		cursor.close()
		return sounds
	}

	fun getTitle(uri: Uri){}
}