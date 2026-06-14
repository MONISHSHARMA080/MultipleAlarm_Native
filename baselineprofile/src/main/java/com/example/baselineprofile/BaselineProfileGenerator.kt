package com.example.baselineprofile

import android.content.Intent
import android.net.Uri
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.coolApps.MultipleAlarmClock.Activities.AlarmActivityIntentData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test class generates a basic startup baseline profile for the target package.
 *
 * We recommend you start with this but add important user flows to the profile to improve their performance.
 * Refer to the [baseline profile documentation](https://d.android.com/topic/performance/baselineprofiles)
 * for more information.
 *
 * You can run the generator with the "Generate Baseline Profile" run configuration in Android Studio or
 * the equivalent `generateBaselineProfile` gradle task:
 * ```
 * ./gradlew :app:generateReleaseBaselineProfile
 * ```
 * The run configuration runs the Gradle task and applies filtering to run only the generators.
 *
 * Check [documentation](https://d.android.com/topic/performance/benchmarking/macrobenchmark-instrumentation-args)
 * for more information about available instrumentation arguments.
 *
 * After you run the generator, you can verify the improvements running the [StartupBenchmarks] benchmark.
 *
 * When using this class to generate a baseline profile, only API 33+ or rooted API 28+ are supported.
 *
 * The minimum required version of androidx.benchmark to generate a baseline profile is 1.2.0.
 **/
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

	@get:Rule
	val rule = BaselineProfileRule()

	@Test
	fun generate() {
		rule.collect(
			packageName = InstrumentationRegistry.getArguments().getString("targetAppId")
				?: throw Exception("targetAppId not passed as instrumentation runner arg"),
			includeInStartupProfile = true,
		) {
			pressHome()
			startActivityAndWait(
				Intent().apply {
					action = Intent.ACTION_VIEW
					data = Uri.parse("alarmapp://home")
					setPackage(packageName)
				}
			)
			device.waitForIdle()
		}
	}

	@Test
	fun generateAlarmProfile() {
		rule.collect(
			packageName = InstrumentationRegistry.getArguments().getString("targetAppId")
				?: throw Exception("targetAppId not passed as instrumentation runner arg"),
			includeInStartupProfile = true, // Important for background components
		) {
			// 1. Start the app to capture initial setup
			pressHome()
			startActivityAndWait()

			// 2. Simulate the Alarm broadcast
			val mockData = AlarmActivityIntentData(
				alarmIdInDb = 1,
				startTimeForDb = System.currentTimeMillis(),
				startTime = System.currentTimeMillis() + 5000,
				endTime = System.currentTimeMillis() + 86700,
				message = "Baseline Profile Optimization"
			)

			// We use strings for Action and Extras because we don't have access to the App's constants
			val intent = Intent("com.coolApps.trying_native.ALARM_TRIGGERED").apply {
				setPackage(packageName)
				component = android.content.ComponentName(
					packageName,
					"com.example.MultipleAlarmClock.BroadCastReceivers.AlarmReceiver"
				)
				putExtra("intentData", mockData)
			}

			// Trigger the receiver
			InstrumentationRegistry.getInstrumentation().context.sendBroadcast(intent)

			// 3. Give the system time to process the receiver and start the service
			device.waitForIdle()
		}
	}
}
