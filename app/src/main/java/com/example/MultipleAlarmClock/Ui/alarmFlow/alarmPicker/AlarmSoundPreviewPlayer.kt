package com.example.MultipleAlarmClock.Ui.alarmPicker

import android.net.Uri
import jakarta.inject.Inject
import jakarta.inject.Singleton

interface AlarmSoundPreviewPlayer {

	fun play(uri: Uri)

	fun stop()
}
@Singleton
class AlarmSoundPreviewPlayerImpl @Inject constructor() :
	AlarmSoundPreviewPlayer {

	override fun play(uri: Uri) {
		// TODO
	}
	override fun stop() {
		// TODO
	}
}