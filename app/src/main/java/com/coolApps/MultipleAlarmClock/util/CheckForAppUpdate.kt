package com.coolApps.MultipleAlarmClock.util

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.ktx.AppUpdateResult
import com.google.android.play.core.ktx.requestCompleteUpdate
import com.google.android.play.core.ktx.requestUpdateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

class AppUpdateChecker(context: Context) {

	private val appUpdateManager by lazy { AppUpdateManagerFactory.create(context) }

	val updateState: Flow<AppUpdateResult>
		get() = appUpdateManager.requestUpdateFlow().catch { emit(AppUpdateResult.NotAvailable) }

	fun startFlexibleUpdate(info: AppUpdateInfo, launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean =
		startUpdate(info, launcher, AppUpdateType.FLEXIBLE)

	fun startImmediateUpdate(info: AppUpdateInfo, launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean =
		startUpdate(info, launcher, AppUpdateType.IMMEDIATE)

	suspend fun completeUpdate(): Boolean = runCatching { appUpdateManager.requestCompleteUpdate() }.isSuccess

	private fun startUpdate(
		info: AppUpdateInfo,
		launcher: ActivityResultLauncher<IntentSenderRequest>,
		updateType: Int
	): Boolean = try {
		appUpdateManager.startUpdateFlowForResult(
			info,
			launcher,
			AppUpdateOptions.newBuilder(updateType).build()
		)
	} catch (e: Exception) {
		false
	}

	companion object {
		const val MIN_STALENESS_DAYS_FOR_FLEXIBLE_PROMPT = 3
		const val IMMEDIATE_PRIORITY_THRESHOLD = 4
	}
}
