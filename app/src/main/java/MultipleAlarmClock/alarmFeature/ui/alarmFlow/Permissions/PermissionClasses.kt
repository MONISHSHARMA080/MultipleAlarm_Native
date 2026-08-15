package com.example.MultipleAlarmClock.Ui.Permissions

import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import com.coolApps.MultipleAlarmClock.R


sealed class PermissionStep(
	val title: String,
	val rationale: String,
	val id: String,
	@StringRes val titleRes: Int,
	@StringRes val rationaleRes: Int,
	val action: String? = null
) {
	object PostNotification : PermissionStep(
		"Notifications",
		"Required to show the alarm notification ",
		"notifications",
		R.string.perm_notifications_title,
		R.string.perm_notifications_rationale
	)
	object ExactAlarm : PermissionStep(
		"Exact Alarms",
		"Required to ensure your alarm rings exactly on time, even in power-saving mode.",
		"exact_alarm",
		R.string.perm_exact_alarm_title,
		R.string.perm_exact_alarm_rationale,
		Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
	)
	object FullScreenIntent : PermissionStep(
		"Display over other apps",
		"Required to show the alarm clock on your lock screen.",
		"full_screen_intent",
		R.string.perm_fsi_title,
		R.string.perm_fsi_rationale,
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT    else  null
	)
	object XiaomiAutostart : PermissionStep(
		"Autostart (Xiaomi)",
		"Xiaomi devices require Autostart to schedule alarms, without this app will not function properly",
		"xiaomi_autostart",
		R.string.perm_xiaomi_title,
		R.string.perm_xiaomi_rationale
	)
}
