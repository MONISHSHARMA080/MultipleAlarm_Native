
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
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import java.util.concurrent.atomic.AtomicInteger


class TestAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "TestAlarmReceiver"
        const val ALARM_ACTION = "com.example.trying_native.ALARM_TRIGGERED"
        private val receivedAlarmCount = AtomicInteger(0)

        fun getReceivedAlarmCount(): Int = receivedAlarmCount.get()

        fun resetCounter() {
            receivedAlarmCount.set(0)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        logD(" in the2025-01-20 12:29:35.626 12394-12408 AAA                     com.example.trying_native            D  -->Setting time to: 1737356555525 my receiver ++++")
        if (intent.action != ALARM_ACTION) {
            logD("Received intent with wrong action: ${intent.action}")
            return
        }

        val currentCount = receivedAlarmCount.incrementAndGet()
        val currentTime = System.currentTimeMillis()
        val triggerTime = intent.getLongExtra("triggerTime", -1)

        logD("""
            Alarm received #$currentCount
            Current time: $currentTime
            Trigger time: $triggerTime
            Time difference: ${currentTime - triggerTime}ms
            Intent extras: ${intent.extras?.keySet()?.joinToString(", ") { "$it: ${intent.extras?.get(it)}" }}
        """.trimIndent())
    }
}



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
    val helperClass = E2ETestHelper()

    @Before
    fun setup() {
//        context.unregisterReceiver(AlarmReceiver::class.java)
//        context.registerReceiver(
//            object : BroadcastReceiver() {
//                override fun onReceive(context: Context, intent: Intent) {
//                    logD("got a new broadcast in the receiver and the receiver counter is ${alarmReceiverCounter.get()} ")
//                    alarmReceiverCounter.incrementAndGet()
//                }
//            },
//            IntentFilter(ALARM_ACTION), // Use the same action string
//            Context.RECEIVER_EXPORTED
//        )

    }

    @Test
    fun testAlarmScheduling() = runTest {
        // Reset the counter at start of test
        TestAlarmReceiver.resetCounter()

        val activityContext = composeTestRule.activity
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmDao = helperClass.getAlarmDao(context)

        // Register receiver programmatically to ensure it's active during test
        context.registerReceiver(
            TestAlarmReceiver(),
            IntentFilter(TestAlarmReceiver.ALARM_ACTION),
            Context.RECEIVER_EXPORTED
        )

        // Test parameters
        val startInMin = 3
        val endMin = 20
        val freqToSkipAlarm = 2

        val expectedAlarms = helperClass.expectedAlarmToBePlayed(endMin, startInMin, freqToSkipAlarm)

        // Get current time before scheduling
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
            new_is_ready_to_use = false,
            receiverClass = TestAlarmReceiver::class.java
        )

        assertNull("Error scheduling alarms: $exception", exception)
        logD("Number of alarms scheduled: $expectedAlarms")

        // Fast forward time for each expected alarm
        repeat(expectedAlarms) { alarmIndex ->
            val nextAlarmTime = startTime + (alarmIndex * freqToSkipAlarm * 60 * 1000)
            logD("Setting time to: $nextAlarmTime")

            // Set time and allow for alarm processing

            try {
                instrumentation.runOnMainSync {
                    SystemClock.setCurrentTimeMillis(nextAlarmTime)
                }
            }catch (e:Exception){
                logD(" error occurred in skipping the time \n\n -->$e")
            }

            // Wait a bit longer for alarm processing
            SystemClock.sleep(2000)

            // maybe see the activity and not the Broadcast receiver


            // Verify the alarm count
            val currentCount = TestAlarmReceiver.getReceivedAlarmCount()
            logD("Current alarm count: $currentCount, Expected: ${alarmIndex + 1}")

            assertTrue(
                "Alarm $alarmIndex did not trigger. Count: $currentCount",
                currentCount == alarmIndex + 1
            )
        }

        // Final verification
        assertEquals(
            "Incorrect final number of alarm triggers",
            expectedAlarms,
            TestAlarmReceiver.getReceivedAlarmCount()
        )
    }
}
