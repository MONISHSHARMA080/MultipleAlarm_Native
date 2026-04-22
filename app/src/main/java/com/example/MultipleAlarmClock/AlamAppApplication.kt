package com.example.MultipleAlarmClock

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AlarmApp : Application() {
	override fun onCreate() {
		super.onCreate()
	}
}