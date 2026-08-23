package com.coolApps.MultipleAlarmClock.analytics

import android.content.Context
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import com.coolApps.MultipleAlarmClock.BuildConfig
import com.coolApps.MultipleAlarmClock.Data.dataStore.InAppReviewState
import com.coolApps.MultipleAlarmClock.Data.dataStore.Settings
import com.coolApps.MultipleAlarmClock.Data.dataStore.copy
import com.coolApps.MultipleAlarmClock.logD
import com.google.android.gms.appset.AppSet
import com.google.android.gms.appset.AppSetIdInfo
import com.google.android.gms.tasks.Task
import com.posthog.PersonProfiles
import com.posthog.PostHog
import com.posthog.PostHogOnFeatureFlags
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import com.posthog.logs.PostHogLogSeverity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

object FeatureFlagKeys {
	const val IN_APP_REVIEW = "in-app-review-prompt-show"
}


class Analytics(
		val context: Context,
		private val dataStore: DataStore<Settings>
	){
	companion object {
		const val POSTHOG_API_KEY = "phc_wFUsQjwTmEznOhwyNUeAD0fe70cGWr5MuWRSjJMh5Cb"
		const val POSTHOG_HOST = "https://us.i.posthog.com"
	}

	var config: PostHogAndroidConfig
	val coroutineScope = CoroutineScope(Dispatchers.IO)

	init {
		val postHogConfig = PostHogAndroidConfig(
			apiKey = POSTHOG_API_KEY,
			host = POSTHOG_HOST,
		).apply {
				captureScreenViews= true
				personProfiles  = PersonProfiles.ALWAYS
				errorTrackingConfig.autoCapture = true
				sessionReplayConfig.maskAllTextInputs = false
				sessionReplayConfig.maskAllImages = false
				sessionReplayConfig.captureLogcat = true
				sessionReplay = true
				debug = BuildConfig.DEBUG
				optOut = BuildConfig.DEBUG || BuildConfig.SKIP_POSTHOG
				sessionReplayConfig.captureLogcat = true
				sessionReplayConfig.screenshot = true
				onFeatureFlags = PostHogOnFeatureFlags {
					coroutineScope.launch {
						val featureEnabled = PostHog.isFeatureEnabled(FeatureFlagKeys.IN_APP_REVIEW, defaultValue = false)
						logD("PostHog feature flag ${FeatureFlagKeys.IN_APP_REVIEW} = $featureEnabled")
						// State transitions,
						// NOT_ELIGIBLE -> default,
						// ELIGIBLE -> using feature flag and show the ui,
						// CONSUMED -> Users have seen the popUp, and now we are waiting for the isFeatureEnabled to return false, so we can turn it off / NOT_ELIGIBLE
						// b) when the user have seen the Popup the shouldWeShowInAppReview = Consumed and featureEnabled == true, then I to not make it Eligible, so for that
						dataStore.updateData { it.copy {
							shouldWeShowInAppReview = when{
								!featureEnabled -> InAppReviewState.NOT_ELIGIBLE
								shouldWeShowInAppReview == InAppReviewState.NOT_ELIGIBLE && featureEnabled -> InAppReviewState.ELIGIBLE
								else -> shouldWeShowInAppReview
							}
						}
						}
					}
				}

		}
		PostHogAndroid.setup(context, postHogConfig)
		logD("the buildConfig.Debug is ${BuildConfig.DEBUG} and SkipPosthog:${BuildConfig.SKIP_POSTHOG}")
		config = postHogConfig
		coroutineScope.launch {
			identifyAnonymousUser()
		}
	}

	fun captureEvent(event: String, properties: Map<String, Any>): Unit {
		PostHog.capture(
			event = event,
			properties = properties
		)
	}

	fun captureLog(message: String, severity: PostHogLogSeverity = PostHogLogSeverity.DEBUG) {
		when (severity) {
			PostHogLogSeverity.TRACE -> PostHog.logger.trace(message)
			PostHogLogSeverity.DEBUG -> PostHog.logger.debug(message)
			PostHogLogSeverity.INFO -> PostHog.logger.info(message)
			PostHogLogSeverity.WARN -> PostHog.logger.warn(message)
			PostHogLogSeverity.ERROR -> PostHog.logger.error(message)
			PostHogLogSeverity.FATAL -> PostHog.logger.fatal(message)
		}
	}

	fun setFcmToken(fid: String) {
		PostHog.capture(
			event = "fcm_token_updated",
			properties = mapOf("fcm_token" to fid),
			userProperties = mapOf("fcm_token" to fid) // maps to $set under the hood
		)
	}

	fun screen(screenName: String, properties: Map<String, Any>? = null){
		PostHog.screen(screenName, properties)
	}

	 fun identifyAnonymousUser() {
		 logD("called identifyAnonymousUser")
		 val client = AppSet.getClient(context)
		 val task: Task<AppSetIdInfo> = client.appSetIdInfo

		 task.addOnSuccessListener {
			 // Determine current scope of app set ID.
			 val scope: Int = it.scope // idk if the id is only for the same app or for  same for distinct apps  just give it to me
			 // Read app set ID value, which uses version 4 of the
			 // universally unique identifier (UUID) format.
			 val id: String = it.id
			 PostHog.identify(
				 distinctId = id,
				 userProperties = mapOf(
					 "app_set_id_scope" to if (scope == AppSetIdInfo.SCOPE_APP) "app" else "developer",
					 "id_source" to "app_set_id"
				 )
			 )
			 logD("anonymousId from appSet is $id")
		 }.addOnFailureListener {
			 // Fallback to classic UUID if Play Services fails
			 val sharedPrefs = context.getSharedPreferences("alarm_app_prefs", Context.MODE_PRIVATE)
			 val anonymousId = sharedPrefs.getString("anonymous_user_id", null) ?: run {
				 val newId = UUID.randomUUID().toString()
				 sharedPrefs.edit { putString("anonymous_user_id", newId) }
				 newId
			 }
			 PostHog.identify(
				 distinctId = anonymousId,
				 userProperties = mapOf("id_source" to "uuid_fallback")
			 )
			 logD("identified via UUID fallback: $anonymousId")
		 }
	}
}