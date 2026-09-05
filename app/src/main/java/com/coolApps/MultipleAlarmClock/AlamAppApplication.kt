package com.coolApps.MultipleAlarmClock

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject

@HiltAndroidApp
class AlarmApp : Application(), Configuration.Provider {

	@Inject
	lateinit var workerFactory: HiltWorkerFactory

	override fun onCreate() {
		super.onCreate()
		Purchases.logLevel = LogLevel.DEBUG
		Purchases.configure(
			PurchasesConfiguration.Builder(this, BuildConfig.REVENUECAT_API_KEY).build()
		)
	}

	override val workManagerConfiguration: Configuration get() = Configuration.Builder()
			.setWorkerFactory(workerFactory)
			.build()

}
