package com.example.trying_native

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.trying_native.components_for_ui_compose.ALARM_ACTION
import com.example.trying_native.components_for_ui_compose.scheduleMultipleAlarms
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowApplication
import org.robolectric.shadows.ShadowLog
import org.robolectric.shadows.ShadowSystemClock
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.Calendar
import java.util.Locale
import java.util.Random
import java.util.concurrent.TimeUnit
import kotlin.math.log


@Config(application = Application::class, sdk = [34], packageName = "com.example.trying_native")
@RunWith(RobolectricTestRunner::class)
class AlarmFlowRobolectricTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var shadowAlarmManager: ShadowAlarmManager
    private lateinit var shadowApplication: ShadowApplication
    private lateinit var inMemoryAlarmDao: AlarmDao
    private lateinit var database: AlarmDatabase

    @Before
    fun setup() {
        ShadowLog.stream = System.out
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = shadowOf(alarmManager)
        shadowApplication = shadowOf(context as Application)
        database = Room.inMemoryDatabaseBuilder(context, AlarmDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        inMemoryAlarmDao = database.alarmDao()
        ShadowLog.setupLogging()

        val initialTime = ShadowSystemClock.currentTimeMillis()

        val twentyFourHoursInMillis = 24 * 60 * 60 * 1000L
        shadowSystemClock.advanceBy(twentyFourHoursInMillis, java.util.concurrent.TimeUnit.MILLISECONDS)

        val expectedCurrentTime = initialTime + twentyFourHoursInMillis
        val expectedUptime = initialUptime + twentyFourHoursInMillis
        // If you also rely on uptime, it's good to set that too, relative to current time.
        // For simplicity, let's assume uptime starts at 0 relative to setCurrentTimeMillis
        // or you can set it to a desired value.
        SystemClock.setCurrentTimeMillis()

    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testAlarmSchedule() {
        logD("test started +++")
        val initialCalendar = Calendar.getInstance()
        val initialTimeMillis = initialCalendar.timeInMillis
        // Use this:
//        ShadowSystemClock.advanceBy(Duration.ofMillis(initialTimeMillis))
        logD("Initial ShadowSystemClock time set to: ${getFormattedTime(ShadowSystemClock.currentTimeMillis()).getOrNull()}")
        ShadowSystemClock.advanceBy(Duration.ofMillis(initialTimeMillis))
        logD("the time of the shadow clock after setting it to today's time is -> ${getFormattedTime(ShadowSystemClock.currentTimeMillis())} the time from the calendar is ${getFormattedTime(initialTimeMillis)}")

        val randomAlarmResult  = getRandomAlarmTimesForDay(5, ShadowSystemClock.currentTimeMillis()) // Use ShadowSystemClock.currentTimeMillis() here for consistency
        logD("the result from the random alarm is $randomAlarmResult")
        assertTrue( "error getting the random starting time from the function and it is $randomAlarmResult",randomAlarmResult.isSuccess )
        val randomlySelectedTime = randomAlarmResult.getOrNull()
        assertNotNull( "randomlySelectedTime should not be null we got the value to be $randomlySelectedTime",randomlySelectedTime )
        if(randomlySelectedTime == null){
            return
        }
        val messageForAlarm = "test Alarm"
        val alarmFreq = 28 // This is in minutes
        // ... (rest of your alarm scheduling logic)

        val exceptionFromSettingAlarm = runBlocking {
            scheduleMultipleAlarms(
                alarmManager = alarmManager,
                activity_context = context,
                alarmDao = inMemoryAlarmDao,
                calendar_for_start_time = Calendar.getInstance().apply { timeInMillis = randomlySelectedTime.startTimeMillis },
                calendar_for_end_time = Calendar.getInstance().apply { timeInMillis = randomlySelectedTime.endTimeMillis } ,
                messageForDB = messageForAlarm,
                selected_date_for_display = getFormattedTime(randomlySelectedTime.startTimeMillis).getOrDefault(" "),
                date_in_long =Calendar.getInstance().apply { timeInMillis = randomlySelectedTime.startTimeMillis }.timeInMillis,
                coroutineScope =CoroutineScope(Dispatchers.IO) ,
                is_alarm_ready_to_use = true,
                new_is_ready_to_use = true,
                freq_after_the_callback = alarmFreq
            )
        }
        logD("is the return type from the setting alarm func is null ${exceptionFromSettingAlarm == null}, and the obj is $exceptionFromSettingAlarm ")
        assertNull("the exception from setting alarm is not null, there is a problem setting alarms and it is $exceptionFromSettingAlarm",exceptionFromSettingAlarm)

        val nextAlarm = shadowAlarmManager.scheduledAlarms
        assertFalse("No alarms scheduled!", nextAlarm.isEmpty()) // Ensure an alarm is actually scheduled

        val firstAlarm = nextAlarm[0]
        logD("0 --> the next alarm in alarm manager is ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(firstAlarm.triggerAtMs)}")

        val currentTimeBeforeAdvance = ShadowSystemClock.currentTimeMillis()
        logD("the current system time before advance is ${getFormattedTime(currentTimeBeforeAdvance).getOrDefault("error")}")
        logD("The target alarm trigger time is ${getFormattedTime(firstAlarm.triggerAtMs).getOrDefault("error")}")

        // Calculate the duration to advance by
        // It's crucial that firstAlarm.triggerAtMs is greater than currentTimeBeforeAdvance
//        val durationToAdvance = firstAlarm.triggerAtMs - currentTimeBeforeAdvance
//
//        // This assertion might now pass if your initial currentTimeBeforeAdvance is 0
//        assertTrue("The current time should be less than the time of the upcoming alarm", currentTimeBeforeAdvance < firstAlarm.triggerAtMs)
//        assertTrue("The duration to advance should be positive to reach the alarm time", durationToAdvance > 0)
//
//        logD("Advancing system clock by ${durationToAdvance} milliseconds to reach the alarm time.")

        // *** REMOVE THIS LINE: val res = SystemClock.setCurrentTimeMillis(timeToAdvance) ***
        // It's causing the conflicting time reports and incorrect clock state for ShadowSystemClock.

        // Advance the system clock by the calculated duration
        // Add a small buffer (+1) to ensure we pass the trigger time, unless you have exact match logic
//        ShadowSystemClock.advanceBy(durationToAdvance + 1, TimeUnit.MILLISECONDS)
//
//        // Idle the main looper to allow the alarm broadcast to be dispatched
//        shadowOf(Looper.getMainLooper()).idle()
//
//        logD("the current system time after the time advancement is ${getFormattedTime(ShadowSystemClock.currentTimeMillis()).getOrDefault("error")}")
//        logD(" \n\n +++ Time after advance (ShadowSystemClock.currentTimeMillis()): ${getFormattedTime(ShadowSystemClock.currentTimeMillis()).getOrNull()}. Checking registered receivers.")
//
//        // Now, assert that the current time is at or after the alarm's trigger time
//        assertTrue("The current time should be at or after the alarm's trigger time", ShadowSystemClock.currentTimeMillis() >= firstAlarm.triggerAtMs)
//
//        val broadcastIntents = shadowApplication.broadcastIntents
//        logD("the broadcast intents are (in size) ${broadcastIntents.size} and they are $broadcastIntents")
//        assertFalse("No broadcast intents found after advancing time and idling. Alarm might not have fired.", broadcastIntents.isEmpty())
//
//        assertTrue("Expected at least one broadcast for ALARM_ACTION", broadcastIntents.any { it.action == ALARM_ACTION })
//        val receivedAlarmIntent = broadcastIntents.firstOrNull { it.action == ALARM_ACTION }
//        assertNotNull("Should have received the alarm intent", receivedAlarmIntent)
//        assertEquals(firstAlarm.triggerAtMs, receivedAlarmIntent?.getLongExtra("startTime", 0L))
//        assertEquals(randomlySelectedTime?.startTimeMillis, receivedAlarmIntent?.getLongExtra("startTimeForDb", 0L))
        // Add more assertions as needed for other extras or the AlarmReceiver's behavior
    }

    /** this fun get the random start time for the alarm , the start time will be in the day*/
    data class RandomAlarmSelectedTime(
        val startTimeMillis: Long,
        val endTimeMillis: Long
    )

    private fun getRandomAlarmTimesForDay(minGapHours: Int, baseTimeMillis: Long): Result<RandomAlarmSelectedTime> {
        return runCatching {
            require(minGapHours in 0 until 24) {
                "minGapHours must be between 0 and 23"
            }

            val random = Random()

            // 1) build millis for start/end of the 'day' relative to the baseTimeMillis
            val calendar = Calendar.getInstance().apply {
                timeInMillis = baseTimeMillis // Start from the provided base time
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calendar.timeInMillis

            calendar.apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val endOfDay = calendar.timeInMillis

            // Ensure startOfDay is not in the past relative to the current simulated time
            val adjustedStartOfDay = maxOf(startOfDay, baseTimeMillis)

            // 2) compute random gap in millis (≥ minGapHours, ≤ remaining hours)
            val minGapMillis = minGapHours * 60L * 60L * 1000L
            val maxPossibleGap = endOfDay - adjustedStartOfDay
            require(minGapMillis <= maxPossibleGap) {
                "minGapHours is too large to fit in the remaining part of the day from $baseTimeMillis to end of day"
            }
            // pick a gap between [minGapMillis, maxPossibleGap]
            val gapMillis = minGapMillis + random.nextLong(maxPossibleGap - minGapMillis + 1)

            // 3) pick a start time so that start + gap ≤ endOfDay
            val latestStart = endOfDay - gapMillis
            // Ensure startTime is generated *after* the current simulated time (adjustedStartOfDay)
            val startTime = adjustedStartOfDay + random.nextLong(latestStart - adjustedStartOfDay + 1)

            // 4) derive end time
            val endTime = startTime + gapMillis

            logD(" in 12-hour format: startTime is ${getFormattedTime(startTime).getOrNull()}-- and the end time is ${getFormattedTime(endTime).getOrNull()}")

            RandomAlarmSelectedTime(startTimeMillis = startTime, endTimeMillis = endTime)
        }
    }

    /** takes in the time and returns the date and time if format type is default */
    private fun getFormattedTime(timeInMillis: Long, formatType: String = "yyyy-MM-dd hh:mm:ss a"): Result<String> {
        return runCatching {
            SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault()).format(timeInMillis)
        }
    }

    /** this func takes the time in milliseconds and freq int that are randomly generated and returns
    the expected alarms that will occur
     */
    private fun getExpectedAlarms(startTime: Long, endTime: Long, freq: Int): Result<Int> {
        return runCatching {
            logD("randomly generate start time:$startTime , endTime:$endTime and the freq is $freq ")
            assertTrue(" the start time is greater than the end time which should not be possible ", endTime > startTime)
            val noOfAlarms = (((endTime - startTime) + 1) / (freq * 60000)).toInt() // freq in min so 60 * 1000
            logD("the number of alarms we are expecting is $noOfAlarms")
            noOfAlarms
        }
    }

    private fun logD(message: String): Unit {
        Log.d("AAAAA", "$message")
    }
}
