package com.example.MultipleAlarmClock.Ui.Permissions

import android.os.Build
import android.provider.Settings

sealed class PermissionStep(
	val title: String,
	val rationale: String,
	val action: String? = null
) {
	object PostNotification : PermissionStep(
		"Notifications",
		"Required to show the alarm notification"
	)
	object ExactAlarm : PermissionStep(
		"Exact Alarms",
		"Required to ensure your alarm rings exactly on time, even in power-saving mode.",
		Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
	)
	object FullScreenIntent : PermissionStep(
		"Display over other apps",
		"Required to show the alarm clock on your lock screen.",
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT    else  null
	)
	object XiaomiAutostart : PermissionStep(
		"Autostart (Xiaomi)",
		"Xiaomi devices require Autostart to schedule alarms, without this app might not function well"
	)
}
