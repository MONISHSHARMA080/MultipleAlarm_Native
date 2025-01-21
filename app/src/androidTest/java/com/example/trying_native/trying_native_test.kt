
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class AlarmContainerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val coroutineScope = TestScope()
    val helperClass = E2ETestHelper()

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val alarmActivityRule = createAndroidComposeRule<AlarmActivity>()

    @Test
    fun testToSeeThatTheAlarmSetAreEqualToAlarmReceivedAfterTimeSkipping() = runTest {
//    fun testToSeeThatTheAlarmSetAreEqualToAlarmReceivedAfterTimeSkipping()  {
        val activityContext = composeTestRule.activity
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmDao = helperClass.getAlarmDao(context)

        // Test parameters
        val startInMin = 2
        val endMin = 63
        val freqToSkipAlarm = 1

        val expectedAlarms = helperClass.expectedAlarmToBePlayed(endMin, startInMin, freqToSkipAlarm)
        helperClass.startMonitoringActivity(AlarmActivity::class.java)
        logD("current activity hits are --> ${helperClass.getActivityHits()}")

        // Schedule the alarms
        val exception = scheduleMultipleAlarms(
                alarmManager = alarmManager, activity_context = activityContext, alarmDao = alarmDao,
                calendar_for_start_time = helperClass.getTheCalenderInstanceAndSkipTheMinIn(startInMin),
                calendar_for_end_time = helperClass.getTheCalenderInstanceAndSkipTheMinIn(endMin),
                date_in_long = helperClass.getDateInLong(), coroutineScope = coroutineScope, freq_after_the_callback = freqToSkipAlarm,
                selected_date_for_display = helperClass.getDateString(helperClass.getDateInLong()),
                is_alarm_ready_to_use = true, new_is_ready_to_use = false, message = "--- Graduation ---",
            )



        assertNull("Error scheduling alarms: $exception", exception)
        logD("Number of alarms scheduled: $expectedAlarms")
        logD("---------skipping the time---------")

        // Fast forward time for each expected alarm
            helperClass.triggerPendingAlarms(context, endMin)

            SystemClock.sleep(14000)
        logD("-----about to exit")
        logD(" activity hits are --> ${helperClass.getActivityHits()} and the alarms scheduled are --> $expectedAlarms")

        assertTrue("the number of expected alarms played does not match the alarms set  ", helperClass.getActivityHits() == expectedAlarms)
        SystemClock.sleep(5000)

        helperClass.cleanupActivityMonitor()

        // ----- now try to make just one alarm and then see if that work -----

    }
}
