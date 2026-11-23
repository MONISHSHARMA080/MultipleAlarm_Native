package com.coolApps.MultipleAlarmClock.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri

fun getFirstInstallTimeMs(context: Context): Long = runCatching {
	context.packageManager.getPackageInfo(
		context.packageName,
		PackageManager.PackageInfoFlags.of(0)
	).firstInstallTime
}.getOrDefault(0L)

fun openPlayStore(context: Context) {
	val packageName = context.packageName
	try {
		val intent = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()).apply {
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		}
		context.startActivity(intent)
	} catch (_: ActivityNotFoundException) {
		try {
			val webIntent = Intent(
				Intent.ACTION_VIEW,
				"https://play.google.com/store/apps/details?id=$packageName".toUri()
			).apply {
				addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			}
			context.startActivity(webIntent)
		} catch (_: ActivityNotFoundException) {
		}
	}
}
