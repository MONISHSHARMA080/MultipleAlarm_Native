package com.example.trying_native.testHelperFile

import android.app.Activity
import android.app.AlarmManager
import android.app.Instrumentation
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.TestContext
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trying_native.AlarmReceiver
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmDatabase
import com.example.trying_native.logD
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class E2ETestHelper {
    private val WAIT_TIME = 30400L
     var currentMonitor: Instrumentation.ActivityMonitor? = null

    fun getAlarmDao(applicationContext: Context):AlarmDao{
        return Room.databaseBuilder(
            applicationContext,
            AlarmDatabase::class.java, "alarm-database"
        ).build().alarmDao()
    }

    fun getTheCalenderInstanceAndSkipTheMinIn(min:Int):Calendar{
        return Calendar.getInstance().apply {
            set(Calendar.MINUTE, min)
        }
    }

    fun getDateInLong():Long{
        return  Calendar.getInstance().timeInMillis
    }

    fun getDateString(epochMillis: Long):String{
            return Instant.ofEpochMilli(epochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }
    fun executeADBCommandAndReturnTheResponse(minutesToFastForwardToo:Int): Pair<String,Exception?> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, minutesToFastForwardToo)

        val adbCommand = "adb shell su -c \"date $(date +%m%d%H%M%Y.%S --date='@${
            calendar.timeInMillis / 1000
        }')\""
        logD("the command to execute is -->${adbCommand}")
//        val adbCommand ="adb devices"
        return try {
            // Execute the ADB command using ProcessBuilder
            val process = ProcessBuilder("sh", "-c", adbCommand)
                .redirectErrorStream(true)
                .start()

            // Read the output from the command
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor() // Wait for the process to finish
            return Pair(output, null)
        } catch (e: Exception) {
          Pair("",e)
        }
    }

    fun expectedAlarmToBePlayed(endTimeInMin:Int, startTimeInMin:Int, freq:Int):Int{
        // get the time in the calender form
        var startTime = getTheCalenderInstanceAndSkipTheMinIn(startTimeInMin).timeInMillis
        val endTime = getTheCalenderInstanceAndSkipTheMinIn(endTimeInMin).timeInMillis
        var frequency = freq  * 60_000
       var i = 0 // may be this should be 1
       while (startTime <= endTime){
           logD(" round in alarm  $i")
           startTime += frequency
           i++
       }
        logD("")
        return  i
    }

    fun getTestAlarmReceiverClass(): Class<out BroadcastReceiver> {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                logD("Test Alarm Receiver: Received alarm with intent: $intent")
                logD("Test Alarm Receiver: Trigger time: ${intent.getLongExtra("triggerTime", -1)}")
                logD("Test Alarm Receiver: Has message: ${intent.getBooleanExtra("isMessagePresent", false)}")
                if (intent.getBooleanExtra("isMessagePresent", false)) {
                    logD("Test Alarm Receiver: Message content: ${intent.getStringExtra("message")}")
                }
//                alarmReceiverCounter.incrementAndGet()
            }
        }.javaClass
    }

    fun triggerPendingAlarms(context: Context, endMin:Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        // Create a latch to wait for alarm processing
        val latch = CountDownLatch(1)

        instrumentation.runOnMainSync {
            // Get next alarm time
            val info = alarmManager.nextAlarmClock
            if (info != null) {
                // Fast forward to just after the alarm time
                alarmManager.setTime(getTheCalenderInstanceAndSkipTheMinIn(endMin).timeInMillis + 19000)
                // Allow for alarm processing
                Thread.sleep(WAIT_TIME)
            }
            latch.countDown()
        }

        // Wait for alarm processing to complete
        latch.await(WAIT_TIME, TimeUnit.MILLISECONDS)
    }

    /**
     * Triggers the next pending alarm by advancing the alarm clock to exactly that alarm's time
     * Returns true if an alarm was triggered, false if no alarms were pending
     */
    fun triggerNextAlarm(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        val latch = CountDownLatch(1)
        var alarmTriggered = false

        instrumentation.runOnMainSync {
            val info = alarmManager.nextAlarmClock
            if (info != null) {
                // Set time to exactly the trigger time
                alarmManager.setTime(info.triggerTime)
                alarmTriggered = true
                // Small delay to allow alarm processing
                Thread.sleep(WAIT_TIME)
            }
            latch.countDown()
        }

        // Wait for alarm processing to complete
        latch.await(WAIT_TIME, TimeUnit.MILLISECONDS)
        return alarmTriggered
    }


    /**
     * Starts monitoring launches for a specific activity
     * @param activityClass The class of the activity to monitor
     */
    fun startMonitoringActivity(activityClass: Class<out Activity>) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        // Clean up any existing monitor
        currentMonitor?.let {
            instrumentation.removeMonitor(it)
        }

        // Create and set new monitor
        currentMonitor = Instrumentation.ActivityMonitor(
            activityClass.name,
            null,
            false
        ).also {
            instrumentation.addMonitor(it)
        }
    }

    /**
     * Gets the number of times the monitored activity was launched
     * @return The number of hits for the monitored activity, or 0 if no monitor is active
     */
    fun getActivityHits(): Int {
        return currentMonitor?.hits ?: 0
    }

    /**
     * Cleans up the activity monitor. Call this in your test cleanup.
     */
    fun cleanupActivityMonitor() {
        currentMonitor?.let {
            InstrumentationRegistry.getInstrumentation().removeMonitor(it)
            currentMonitor = null
        }
    }
}
