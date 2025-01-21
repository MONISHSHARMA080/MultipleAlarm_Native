
import android.app.Activity
import android.app.AlarmManager
import android.app.Application
import android.app.Instrumentation
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.PatternMatcher
import android.os.SystemClock
import android.view.WindowManager
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trying_native.AlarmActivity
import com.example.trying_native.AlarmReceiver
import com.example.trying_native.MainActivity
import com.example.trying_native.components_for_ui_compose.ALARM_ACTION
import com.example.trying_native.components_for_ui_compose.scheduleMultipleAlarms
import com.example.trying_native.logD
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import com.example.trying_native.testHelperFile.E2ETestHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.log

@RunWith(AndroidJUnit4::class)
class AlarmContainerTest : Application.ActivityLifecycleCallbacks {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val coroutineScope = TestScope()
    val helperClass = E2ETestHelper()
    private var alarmActivityCount = 0

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val alarmActivityRule = createAndroidComposeRule<AlarmActivity>()

    // Lifecycle callback implementations
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity is AlarmActivity && savedInstanceState == null) {
            alarmActivityCount++
            logD("Alarm activity created. Count: $alarmActivityCount")
        }
    }

    override fun onActivityStarted(activity: Activity) {
        if (activity is AlarmActivity) {
            logD("Alarm activity started")
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (activity is AlarmActivity) {
            logD("Alarm activity resumed")
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (activity is AlarmActivity) {
            logD("Alarm activity paused")
        }
    }

    override fun onActivityStopped(activity: Activity) {
        if (activity is AlarmActivity) {
            logD("Alarm activity stopped")
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        if (activity is AlarmActivity) {
            logD("Alarm activity instance state saved")
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (activity is AlarmActivity) {
            logD("Alarm activity destroyed")
        }
    }

    @Before
    fun setup() {
        // Register lifecycle callbacks
        (context as Application).registerActivityLifecycleCallbacks(this)
        alarmActivityCount = 0
    }

    @After
    fun teardown() {
        // Unregister lifecycle callbacks
        (context as Application).unregisterActivityLifecycleCallbacks(this)
    }

    @Test
    fun testToSeeThatTheAlarmSetAreEqualToAlarmReceivedAfterTimeSkipping() = runTest {
        val activityContext = composeTestRule.activity
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmDao = helperClass.getAlarmDao(context)

        // Test parameters
        val startInMin = 2
        val endMin =13
        val freqToSkipAlarm = 1
        val expectedAlarms = helperClass.expectedAlarmToBePlayed(endMin, startInMin, freqToSkipAlarm)

        logD("expected alarm to played is $expectedAlarms")
        // Schedule the alarms
        val exception = scheduleMultipleAlarms(
            alarmManager = alarmManager,
            activity_context = activityContext,
            alarmDao = alarmDao,
            calendar_for_start_time = helperClass.getTheCalenderInstanceAndSkipTheMinIn(startInMin),
            calendar_for_end_time = helperClass.getTheCalenderInstanceAndSkipTheMinIn(endMin),
            date_in_long = helperClass.getDateInLong(),
            coroutineScope = coroutineScope,
            freq_after_the_callback = freqToSkipAlarm,
            selected_date_for_display = helperClass.getDateString(helperClass.getDateInLong()),
            is_alarm_ready_to_use = true,
            new_is_ready_to_use = false,
            message = "--- Graduation ---"
        )

        helperClass.triggerPendingAlarms(context, startInMin, endMin, freqToSkipAlarm)

        // Wait for all alarms to trigger
        delay( 15000L)  // Adjust delay as needed


        logD("Actual alarm triggers: $alarmActivityCount")
        assertEquals(
            "Number of alarm triggers doesn't match expected",
            expectedAlarms,
            alarmActivityCount
        )


        assertNull("Error scheduling alarms: $exception", exception)
        logD("the alarm activity played ->$alarmActivityCount and the expected alarm is $expectedAlarms")

        assertEquals("no of alarms created is not equal to the alarms played",alarmActivityCount, expectedAlarms )
        helperClass.cleanup(context, null)

        // probably use something like   instrumentation.waitForIdleSync

    }

}