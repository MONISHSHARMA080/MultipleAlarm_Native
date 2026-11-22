package com.coolApps.MultipleAlarmClock

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

@HiltAndroidApp
class AlarmApp : Application(), Configuration.Provider {

	@Inject
	lateinit var workerFactory: HiltWorkerFactory

	override fun onCreate() {
		super.onCreate()
		CoroutineScope(Dispatchers.Main).launch {
			Purchases.logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.WARN
			Purchases.configure(
				PurchasesConfiguration.Builder(this@AlarmApp, BuildConfig.REVENUECAT_API_KEY)
					.preferredUILocaleOverride(Locale.getDefault().language)
					.build()
			)
			MobileAds.initialize(
				this@AlarmApp,
				InitializationConfig.Builder(BuildConfig.ADMOB_APP_ID).build(),
			)
		}
	}

	override val workManagerConfiguration: Configuration get() = Configuration.Builder()
			.setWorkerFactory(workerFactory)
			.build()

}
