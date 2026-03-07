package com.example.trying_native.BroadCastReceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.trying_native.analytics.Analytics
import com.example.trying_native.logD
import com.example.trying_native.workManager.ResetAlarmAfterBoot

class BootReceiver : BroadcastReceiver(){
	override fun onReceive(context: Context, intent: Intent) {
		if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
			intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
			val inputData = Data.Builder()
				.putBoolean("isPackageReplaced", intent.action == Intent.ACTION_MY_PACKAGE_REPLACED)
				.putBoolean("isBootCompleted", intent.action == Intent.ACTION_BOOT_COMPLETED)
				.build()

			val workRequest = OneTimeWorkRequestBuilder<ResetAlarmAfterBoot>()
				.setInputData(inputData)
				.build()
			WorkManager.getInstance(context).enqueue(workRequest)
			logD("scheduled the alarm reset work and exiting")
		}
	}

}