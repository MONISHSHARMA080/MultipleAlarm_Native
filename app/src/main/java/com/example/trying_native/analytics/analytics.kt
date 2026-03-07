package com.example.trying_native.analytics

import android.content.Context
import com.posthog.PersonProfiles
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import com.posthog.android.replay.PostHogSessionReplayConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import androidx.core.content.edit
import com.example.trying_native.logD
import kotlin.math.log


class  Analytics(val context: Context){
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
		}
		PostHogAndroid.setup(context, postHogConfig)
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
			val sharedPrefs = context.getSharedPreferences("alarm_app_prefs", Context.MODE_PRIVATE)
			var anonymousId = sharedPrefs.getString("anonymous_user_id", null)
			if (anonymousId == null) {
				anonymousId = UUID.randomUUID().toString()
				sharedPrefs.edit { putString("anonymous_user_id", anonymousId) }
				logD("UUid generated is $anonymousId")
			}
			logD("anonymousId is $anonymousId")
				PostHog.identify(
					distinctId = anonymousId,
				)
	}

}