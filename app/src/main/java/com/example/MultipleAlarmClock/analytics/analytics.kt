package com.coolApps.MultipleAlarmClock.analytics

import android.content.Context
import androidx.core.content.edit
import com.coolApps.MultipleAlarmClock.BuildConfig
import com.coolApps.MultipleAlarmClock.logD
import com.google.android.gms.appset.AppSet
import com.google.android.gms.appset.AppSetIdInfo
import com.google.android.gms.tasks.Task
import com.posthog.PersonProfiles
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID


class Analytics(val context: Context){
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
				sessionReplay = true
				debug = BuildConfig.DEBUG
				optOut = BuildConfig.DEBUG
				sessionReplayConfig.captureLogcat = true
			sessionReplayConfig.screenshot = true
		}
		PostHogAndroid.setup(context, postHogConfig)
		logD("the buildConfig.Debug is ${BuildConfig.DEBUG}")
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

	fun screen(screenName: String, properties: Map<String, Any>? = null){
		PostHog.screen(screenName, properties)
	}

	 fun identifyAnonymousUser() {
		 logD("called identifyAnonymousUser")
		 val client = AppSet.getClient(context)
		 val task: Task<AppSetIdInfo> = client.appSetIdInfo

		 task.addOnSuccessListener {
			 // Determine current scope of app set ID.
			 val scope: Int =
				 it.scope // idk if the id is only for the same app or for  same for distinct apps  just give it to me
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