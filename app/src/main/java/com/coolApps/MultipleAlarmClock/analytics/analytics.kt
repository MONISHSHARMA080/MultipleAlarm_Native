package com.coolApps.MultipleAlarmClock.analytics

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import com.coolApps.MultipleAlarmClock.BuildConfig
import com.coolApps.MultipleAlarmClock.logD
import com.coolApps.MultipleAlarmClock.notification.offline.OfflineNotificationTimeSlot
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

object FeatureFlagKeys {
	const val IN_APP_REVIEW_ENABLED = "in_app_review_enabled"
	const val MIN_ALARMS_CREATED = "min_alarms_created"
	const val COOLDOWN_DAYS = "cooldown_days"
	const val MIN_DAYS_SINCE_INSTALL = "min_days_since_install"

	const val PUSH_NOTIFICATION_ENABLED = "push_notification_enabled"
	const val PUSH_NOTIFICATION_INACTIVE_DAYS = "push_notification_inactive_days"
	const val PUSH_NOTIFICATION_COOLDOWN_DAYS = "push_notification_cooldown_days"
	const val PUSH_NOTIFICATION_INTERVAL_HOURS = "push_notification_check_interval_hours"
	const val PUSH_NOTIFICATION_TIME_SLOT = "push_notification_time_slot"
}


class Analytics(
		val context: Context,
	){
	private var isFeatureFlagsLoaded = false
	companion object {
		const val POSTHOG_HOST = "https://us.i.posthog.com"

		fun isRunningOnEmulator(): Boolean {
			return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
					|| Build.FINGERPRINT.startsWith("generic")
					|| Build.FINGERPRINT.startsWith("unknown")
					|| Build.HARDWARE.contains("goldfish")
					|| Build.HARDWARE.contains("ranchu")
					|| Build.MODEL.contains("google_sdk")
					|| Build.MODEL.contains("Emulator")
					|| Build.MODEL.contains("Android SDK built for x86")
					|| Build.MANUFACTURER.contains("Genymotion")
					|| Build.PRODUCT.contains("sdk_google")
					|| Build.PRODUCT.contains("google_sdk")
					|| Build.PRODUCT.contains("sdk")
					|| Build.PRODUCT.contains("sdk_x86")
					|| Build.PRODUCT.contains("sdk_gphone")
					|| Build.PRODUCT.contains("vbox86p")
					|| Build.PRODUCT.contains("emulator")
					|| Build.PRODUCT.contains("simulator")
		}
	}

	val isEnabled: Boolean = !BuildConfig.DEBUG &&
			!BuildConfig.SKIP_POSTHOG &&
			BuildConfig.POSTHOG_API_KEY.isNotBlank() &&
			!isRunningOnEmulator()

	private val _featureFlagsData  = MutableStateFlow<FeatureFlagsData?>(null)
	// null means not loaded
	val featureFlagsData: StateFlow<FeatureFlagsData?> = _featureFlagsData.asStateFlow()

	var config: PostHogAndroidConfig? = null
	val coroutineScope = CoroutineScope(Dispatchers.IO)

	init {
		logD("Analytics isEnabled=$isEnabled (debug=${BuildConfig.DEBUG}, skipPostHog=${BuildConfig.SKIP_POSTHOG}, keyPresent=${BuildConfig.POSTHOG_API_KEY.isNotBlank()}, emulator=${isRunningOnEmulator()})")
		if (isEnabled) {
			val postHogConfig = PostHogAndroidConfig(
				apiKey = BuildConfig.POSTHOG_API_KEY,
				host = POSTHOG_HOST,
			).apply {
				captureScreenViews = true
				personProfiles = PersonProfiles.ALWAYS
				errorTrackingConfig.autoCapture = true
				sessionReplayConfig.maskAllTextInputs = false
				sessionReplayConfig.maskAllImages = false
				sessionReplayConfig.captureLogcat = true
				sessionReplay = true
				sessionReplayConfig.screenshot = true
				onFeatureFlags = PostHogOnFeatureFlags {
					isFeatureFlagsLoaded = true
					coroutineScope.launch {
						loadFeatureFlagsFromPostHog()
					}
				}
			}
			PostHogAndroid.setup(context, postHogConfig)
			PostHog.optIn()
			config = postHogConfig
			coroutineScope.launch {
				identifyAnonymousUser()
			}
		} else {
			// Provide fallback default feature flags so app doesn't hang in debug / emulator
			_featureFlagsData.value = FeatureFlagsData(
				inAppReviewEnabled = false,
				minAlarmsCreated = 3,
				cooldownDays = 7,
				minDaysSinceInstall = 3
			)
		}
	}

	private fun getIntFeatureFlag(key: String): Int? {
		if (!isEnabled) return null
		val value = PostHog.getFeatureFlagResult(key, sendFeatureFlagEvent = isEnabled) ?: return null
		return when (val payload = value.payload) {
			is Number -> payload.toInt()   // remote config numbers come back as Double from JSON
			is String -> payload.toIntOrNull()
			else -> null
		}
	}


	private fun loadFeatureFlagsFromPostHog() {
		if (!isEnabled) return
		val enabled = PostHog.isFeatureEnabled(FeatureFlagKeys.IN_APP_REVIEW_ENABLED, defaultValue = true)
		val minAlarms = getIntFeatureFlag(FeatureFlagKeys.MIN_ALARMS_CREATED)
		val cooldownDays = getIntFeatureFlag(FeatureFlagKeys.COOLDOWN_DAYS)
		val minDaysSinceInstall = getIntFeatureFlag(FeatureFlagKeys.MIN_DAYS_SINCE_INSTALL)

		logD("loading feature flag for posthog, got enabled:$enabled, minAlarm:$minAlarms, coolDownDays:$cooldownDays, minDaysSinceInstall:$minDaysSinceInstall")
		if (minAlarms == null || cooldownDays == null || minDaysSinceInstall == null) {
			this.captureEvent( "loading_featureFlags_failed",
				mapOf(
					"minAlarms" to minAlarms.toString(),
					"coolDownDays" to cooldownDays.toString(),
					"minDaysSinceInstall" to minDaysSinceInstall.toString()
				)
			)
			return
		}
		_featureFlagsData.value = FeatureFlagsData(
					inAppReviewEnabled = enabled,
					minAlarmsCreated = minAlarms,
					cooldownDays = cooldownDays,
					minDaysSinceInstall = minDaysSinceInstall
		)
	}

	fun getEngagementConfig(): EngagementConfig {
		if (!isEnabled) {
			return EngagementConfig(
				enabled = false,
				inactiveDays = 3L,
				checkIntervalHours = 12L,
				notificationTimeSlot = OfflineNotificationTimeSlot.Night
			)
		}

		val enabled =
			PostHog.isFeatureEnabled(
				FeatureFlagKeys.PUSH_NOTIFICATION_ENABLED,
				defaultValue = false
			)

		val inactiveDays =
			getIntFeatureFlag(
				FeatureFlagKeys.PUSH_NOTIFICATION_INACTIVE_DAYS
			)?.toLong() ?: 3L

		val cooldownDays =
			getIntFeatureFlag(
				FeatureFlagKeys.PUSH_NOTIFICATION_COOLDOWN_DAYS
			)?.toLong() ?: 7L

		val checkIntervalHours =
			getIntFeatureFlag(
				FeatureFlagKeys.PUSH_NOTIFICATION_INTERVAL_HOURS
			)?.toLong() ?: 12L

		val slot =
			getIntFeatureFlag(
				FeatureFlagKeys.PUSH_NOTIFICATION_TIME_SLOT
			)?: 3


		val timeSlot = when(slot){
			1-> OfflineNotificationTimeSlot.Morning
			2-> OfflineNotificationTimeSlot.Evening
			else-> OfflineNotificationTimeSlot.Night
		}

		return EngagementConfig(
			enabled = enabled,
			inactiveDays = inactiveDays,
//			cooldownDays = cooldownDays,
			checkIntervalHours = checkIntervalHours,
			notificationTimeSlot = timeSlot
		)
	}

	fun captureEvent(event: String, properties: Map<String, Any>): Unit {
		if (!isEnabled) {
			logD("PostHog disabled: skipping captureEvent($event)")
			return
		}
		PostHog.capture(
			event = event,
			properties = properties
		)
	}

	fun captureLog(message: String, severity: PostHogLogSeverity = PostHogLogSeverity.DEBUG) {
		if (!isEnabled) return
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
		if (!isEnabled) return
		PostHog.capture(
			event = "fcm_token_updated",
			properties = mapOf("fcm_token" to fid),
			userProperties = mapOf("fcm_token" to fid) // maps to $set under the hood
		)
	}

	fun screen(screenName: String, properties: Map<String, Any>? = null){
		if (!isEnabled) {
			logD("PostHog disabled: skipping screen($screenName)")
			return
		}
		PostHog.screen(screenName, properties)
	}

	 fun identifyAnonymousUser() {
		 if (!isEnabled) return
		 logD("called identifyAnonymousUser")
		 val client = AppSet.getClient(context)
		 val task: Task<AppSetIdInfo> = client.appSetIdInfo

		 task.addOnSuccessListener {
			 // Determine current scope of app set ID.
			 val scope: Int = it.scope // IDK if the id is only for the same app or for  same for distinct apps  just give it to me
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