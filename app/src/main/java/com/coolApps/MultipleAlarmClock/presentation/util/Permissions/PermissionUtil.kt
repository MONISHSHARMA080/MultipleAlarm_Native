package com.coolApps.MultipleAlarmClock.presentation.util.Permissions

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.coolApps.MultipleAlarmClock.presentation.logD

object PermissionUtils {

	fun isPostNotificationsGranted(context: Context): Boolean =
		ContextCompat.checkSelfPermission(
			context, Manifest.permission.POST_NOTIFICATIONS
		) == PackageManager.PERMISSION_GRANTED

	fun isFullScreenIntentGranted(context: Context): Boolean =
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			context.getSystemService(NotificationManager::class.java)
				?.canUseFullScreenIntent() ?: false
		} else {
			true
		}

	/**
	 * All "hard" permissions are granted.
	 */
	fun allCriticalPermissionsGranted(context: Context): Boolean = isPostNotificationsGranted(context) && isFullScreenIntentGranted(context)

	fun getRequiredPermissionSteps(context: Context): List<PermissionStep> {
		val missingSteps = mutableListOf<PermissionStep>()

		if (!isPostNotificationsGranted(context)) {
			logD("isPostNotificationsGranted: ${isPostNotificationsGranted(context)}")
			missingSteps.add(PermissionStep.PostNotification)
		}

		// Add logic for Exact Alarm (Android 12+)
		val alarmManager = context.getSystemService(AlarmManager::class.java)
		if (!alarmManager.canScheduleExactAlarms()) {
			missingSteps.add(PermissionStep.ExactAlarm)
			logD("alarmManager.canScheduleExactAlarms")

		}

		if (!isFullScreenIntentGranted(context)) {
			missingSteps.add(PermissionStep.FullScreenIntent)
			logD("isFullScreenIntentGranted")

		}

		return missingSteps
	}
}