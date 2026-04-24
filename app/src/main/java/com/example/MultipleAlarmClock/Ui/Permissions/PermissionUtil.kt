package com.example.MultipleAlarmClock.Ui.Permissions

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.coolApps.MultipleAlarmClock.logD
import com.example.MultipleAlarmClock.Data.dataStore.dataStore

object PermissionUtils {

	fun isPostNotificationsGranted(context: Context): Boolean =
		ContextCompat.checkSelfPermission(
			context, android.Manifest.permission.POST_NOTIFICATIONS
		) == PackageManager.PERMISSION_GRANTED

	fun isFullScreenIntentGranted(context: Context): Boolean =
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			context.getSystemService(NotificationManager::class.java)
				?.canUseFullScreenIntent() ?: false
		} else {
			true
		}

	fun isXiaomiDevice(): Boolean = Build.MANUFACTURER.equals("xiaomi", ignoreCase = true)

	/**
	 * All "hard" permissions are granted. Xiaomi autostart is
	 * advisory and intentionally excluded from this check.
	 */
	fun allCriticalPermissionsGranted(context: Context): Boolean = isPostNotificationsGranted(context) && isFullScreenIntentGranted(context)

	suspend fun checkAllPermissionAndUpdateDataStoreIfNotThere(context: Context, ): Boolean{
		val res = allCriticalPermissionsGranted(context)
		if (!res){
			context.dataStore.updateData { it.copy(allPermissionsGranted = false)}
		}
		return res
	}

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

		// Xiaomi specific check
		if (isXiaomiDevice()) {
			// Note: You'll need a custom check here as there's no standard API
			// for "autostart", usually handled by checking a preference or just showing it once.
			missingSteps.add(PermissionStep.XiaomiAutostart)
			logD("isXiaomiDevice")

		}

		return missingSteps
	}
	fun launchXiaomiSettings(context: Context) {
		val intent = Intent().apply {
			component = ComponentName(
				"com.miui.securitycenter",
				"com.miui.permcenter.autostart.AutoStartManagementActivity"
			)
		}
		try { context.startActivity(intent) } catch (e: Exception) { /* Fallback to settings */ }
	}
}