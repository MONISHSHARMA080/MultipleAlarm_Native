package com.coolApps.MultipleAlarmClock.data.ads

import android.app.Activity
import com.coolApps.MultipleAlarmClock.BuildConfig
import com.coolApps.MultipleAlarmClock.data.billing.EntitlementManager
import com.coolApps.MultipleAlarmClock.presentation.logD
import com.coolApps.MultipleAlarmClock.util.Analytics
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.nextgen.loadAndTrackInterstitialAd
import com.revenuecat.purchases.admob.nextgen.setTrackingAdEventCallback
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

@Singleton
class AdManager @Inject constructor(
	entitlementManager: EntitlementManager,
	analytics: Analytics,
) {
	companion object {
		const val PLACEMENT_HOME_LIST = "home_alarm_list_banner"
		const val PLACEMENT_SET_ALARM = "set_alarm_interstitial"
		const val PLACEMENT_ALARM_RING = "alarm_ring_banner"
		private val INTERSTITIAL_EXPIRY_MS = 55.minutes.inWholeMilliseconds
	}

	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

	val shouldShowBannerAds: StateFlow<Boolean> = combine(
		entitlementManager.isPremium,
		analytics.featureFlagsData,
	) { isPremium, flags ->
		!isPremium && flags?.isBannerAdsEnabled == true
	}.stateIn(scope, SharingStarted.Eagerly, false)

	val shouldShowInterstitialAds: StateFlow<Boolean> = combine(
		entitlementManager.isPremium,
		analytics.featureFlagsData,
	) { isPremium, flags ->
		!isPremium && flags?.isInterstitialAdsEnabled == true
	}.stateIn(scope, SharingStarted.Eagerly, false)

	private var setAlarmInterstitial: InterstitialAd? = null
	private var interstitialLoadedAtMs: Long = 0L
	private var isLoadingInterstitial = false

	init {
		shouldShowInterstitialAds
			.onEach { if (!it) clearCachedInterstitial() }
			.launchIn(scope)
	}

	fun preloadSetAlarmInterstitial() {
		if (!shouldShowInterstitialAds.value || isLoadingInterstitial || hasFreshInterstitial() || !Purchases.isConfigured) return
		clearCachedInterstitial()
		isLoadingInterstitial = true

		scope.launch {
			val request = AdRequest.Builder(BuildConfig.ADMOB_INTERSTITIAL_SET_ALARM_AD_UNIT_ID).build()
			when (val result = Purchases.sharedInstance.adTracker.loadAndTrackInterstitialAd(request, PLACEMENT_SET_ALARM)) {
				is AdLoadResult.Success -> if (shouldShowInterstitialAds.value) {
					setAlarmInterstitial = result.ad
					interstitialLoadedAtMs = System.currentTimeMillis()
				}
				is AdLoadResult.Failure -> logD("Interstitial ad failed to load: ${result.error.message}")
			}
			isLoadingInterstitial = false
		}
	}

	fun showSetAlarmInterstitial(activity: Activity?, onFinished: () -> Unit) {
		if (!shouldShowInterstitialAds.value || activity == null || activity.isFinishing || activity.isDestroyed) {
			if (!shouldShowInterstitialAds.value) clearCachedInterstitial()
			onFinished()
			return
		}

		val ad = setAlarmInterstitial.takeIf { hasFreshInterstitial() }
		clearCachedInterstitial()
		if (ad == null) {
			preloadSetAlarmInterstitial()
			onFinished()
			return
		}

		fun complete() {
			scope.launch {
				onFinished()
				preloadSetAlarmInterstitial()
			}
		}

		ad.setTrackingAdEventCallback(
			object : InterstitialAdEventCallback {
				override fun onAdDismissedFullScreenContent() = complete()
				override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
					logD("Interstitial ad failed to show: ${fullScreenContentError.message}")
					complete()
				}
			}
		)
		ad.show(activity)
	}

	private fun hasFreshInterstitial(): Boolean =
		setAlarmInterstitial != null && (System.currentTimeMillis() - interstitialLoadedAtMs) < INTERSTITIAL_EXPIRY_MS

	private fun clearCachedInterstitial() {
		setAlarmInterstitial = null
		interstitialLoadedAtMs = 0L
	}
}
