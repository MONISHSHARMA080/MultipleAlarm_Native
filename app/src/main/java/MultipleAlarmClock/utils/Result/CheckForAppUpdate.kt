package com.coolApps.MultipleAlarmClock.utils.Result

//import android.content.Context
//import com.coolApps.MultipleAlarmClock.analytics.Analytics
//import com.google.android.play.core.appupdate.AppUpdateManagerFactory
//import com.google.android.play.core.install.model.AppUpdateType
//import com.google.android.play.core.install.model.UpdateAvailability
//
//
//suspend fun checkForAppUpdateAndAskForIt(context: Context, analytics: Analytics){
//	val appUpdateManager = AppUpdateManagerFactory.create(context)
//
//// Returns an intent object that you use to check for an update.
//	val appUpdateInfoTask = appUpdateManager.appUpdateInfo
//
//// Checks that the platform will allow the specified type of update.
//	appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
//		if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
//			// This example applies an immediate update. To apply a flexible update
//			// instead, pass in AppUpdateType.FLEXIBLE
//			&& appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
//		) {
//			analytics.captureEvent("app update is available", mapOf())
//
//		}
//	}
//}