
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trying_native.AlarmActivity
import com.example.trying_native.MainActivity
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class AlarmContainerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val coroutineScope = TestScope()

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val alarmActivityRule = createAndroidComposeRule<AlarmActivity>()

    private val alarmReceiverCounter = AtomicInteger(0)

    @Before
    fun setup() {
        // Register a test BroadcastReceiver to count alarm triggers
        context.registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    alarmReceiverCounter.incrementAndGet()
                }
            },
            IntentFilter("YOUR_ALARM_ACTION"), Context.RECEIVER_NOT_EXPORTED
        )
    }

    @Test
    fun testAlarmScheduling() = runTest {
        val activityContext = composeTestRule.activity
        val helperClass = E2ETestHelper()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmDao = helperClass.getAlarmDao(context)

        // Test parameters
        val startInMin = 3
        val endMin = 20
        val freqToSkipAlarm = 2

        // Calculate expected number of alarms
        val expectedAlarms = ((endMin - startInMin) / freqToSkipAlarm) + 1

        // Set system clock to just before first alarm
        val startTime = Calendar.getInstance().apply {
            add(Calendar.MINUTE, startInMin)
        }.timeInMillis

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
            new_is_ready_to_use = false
        )
        logD("execption is -->${exception}")

        assertNull( "Error scheduling alarms: $exception",  exception )

        // Fast forward time for each expected alarm
        repeat(expectedAlarms) { alarmIndex ->
            // Calculate next alarm time
            val nextAlarmTime = startTime + (alarmIndex * freqToSkipAlarm * 60 * 1000)

            // Use instrumentation to manipulate time
            instrumentation.runOnMainSync {
                SystemClock.setCurrentTimeMillis(nextAlarmTime)
            }

            // Wait briefly for alarm to trigger
            SystemClock.sleep(1000)
            logD("alarmReceiverCounter is ${alarmReceiverCounter.get()} and  the alarm index is --> $alarmIndex")
            // Verify alarm triggered
            assertTrue(
                "Alarm $alarmIndex did not trigger",
                alarmReceiverCounter.get() == alarmIndex + 1
            )
        }

        // Verify final count matches expected
        assertEquals(
            "Incorrect number of alarm triggers",
            expectedAlarms,
            alarmReceiverCounter.get()
        )
    }
}
