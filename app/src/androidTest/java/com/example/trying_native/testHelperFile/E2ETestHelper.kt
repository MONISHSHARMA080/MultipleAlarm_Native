package com.example.trying_native.testHelperFile

import android.app.Activity
import android.app.AlarmManager
import android.app.Instrumentation
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.ui.test.TestContext
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trying_native.AlarmActivity
import com.example.trying_native.AlarmReceiver
import com.example.trying_native.MainActivity
import com.example.trying_native.components_for_ui_compose.ALARM_ACTION
import com.example.trying_native.components_for_ui_compose.scheduleMultipleAlarms
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmDatabase
import com.example.trying_native.logD
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Objects
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class E2ETestHelper {
    private val WAIT_TIME = 3000L
     var currentMonitor: Instrumentation.ActivityMonitor? = null
     lateinit var instrumentation :Instrumentation

    fun getAlarmDao(applicationContext: Context):AlarmDao{
        return Room.databaseBuilder(
            applicationContext,
            AlarmDatabase::class.java, "alarm-database"
        ).build().alarmDao()
    }

    fun scheduleMultipleAlarmHelper(activityContext:MainActivity, context: Context, startInMin: Int, endMin: Int,freqToSkipAlarm:Int, coroutineScope: CoroutineScope,  broadcastReceiverClass: BroadcastReceiver? = null   ) :Exception? {
//        val context =  ApplicationProvider.getApplicationContext<Context>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmDao = getAlarmDao(context)


        val exception = runBlocking {
            if(broadcastReceiverClass != null){
                logD("-----++------++----++---11111")
                scheduleMultipleAlarms(
                    alarmManager = alarmManager,
                    activity_context = activityContext,
                    alarmDao = alarmDao,
                    calendar_for_start_time = getTheCalenderInstanceAndSkipTheMinIn(startInMin),
                    calendar_for_end_time = getTheCalenderInstanceAndSkipTheMinIn(endMin),
                    date_in_long = getDateInLong(),
                    coroutineScope = coroutineScope,
                    freq_after_the_callback = freqToSkipAlarm,
                    selected_date_for_display = getDateString(getDateInLong()),
                    is_alarm_ready_to_use = true,
                    new_is_ready_to_use = false,
                    message = "--- Burn ---",
                    receiverClass = broadcastReceiverClass.javaClass,
                    messageForDB = "--- Burn ---"
                )
            }else{
                scheduleMultipleAlarms(
                    alarmManager = alarmManager,
                    activity_context = activityContext,
                    alarmDao = alarmDao,
                    calendar_for_start_time = getTheCalenderInstanceAndSkipTheMinIn(startInMin),
                    calendar_for_end_time = getTheCalenderInstanceAndSkipTheMinIn(endMin),
                    date_in_long = getDateInLong(),
                    coroutineScope = coroutineScope,
                    freq_after_the_callback = freqToSkipAlarm,
                    selected_date_for_display = getDateString(getDateInLong()),
                    is_alarm_ready_to_use = true,
                    new_is_ready_to_use = false,
                    message = "--- Burn ---",
                    messageForDB= "--- Burn ---"
                )

            }
        }
        return exception
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

    fun triggerPendingAlarms(context: Context, startTimeMin: Int, endTimeMin: Int, frequency: Int,) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Calculate how many alarms should be triggered
        val expectedAlarms = expectedAlarmCount(startTimeMin, endTimeMin, frequency)
        var triggeredCount = 0

        val startTime = getCalendarWithMinutes(startTimeMin).timeInMillis
        val endTime = getCalendarWithMinutes(endTimeMin).timeInMillis
        var currentTime = startTime

        while (currentTime <= endTime) {
            // Set a test alarm clock for the current time
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                currentTime.toInt(),
                Intent("TEST_ALARM_ACTION"),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(currentTime, pendingIntent),
                pendingIntent
            )

            // Wait for alarm processing
            Thread.sleep(WAIT_TIME)

            triggeredCount++
            currentTime += (frequency * 60 * 1000) // Add frequency minutes in milliseconds
        }

        // Verify alarms were triggered
        assertEquals("Expected number of alarms should be triggered", expectedAlarms, triggeredCount)
    }

    /**
     * Calculates expected number of alarms
     */
    private fun expectedAlarmCount(startMin: Int, endMin: Int, frequencyMin: Int): Int {
        val totalMinutes = endMin - startMin
        return (totalMinutes / frequencyMin) + 1 // +1 for initial alarm
    }

    /**
     * Creates Calendar instance for specified minutes
     */
    private fun getCalendarWithMinutes(minutes: Int): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.MINUTE, minutes)
        }
    }

    /**
     * Monitors alarm triggers using ActivityMonitor
     */
    fun monitorAlarmActivity(): Instrumentation.ActivityMonitor {
        instrumentation = InstrumentationRegistry.getInstrumentation()
        return instrumentation.addMonitor(
            AlarmActivity::class.java.name,
            null,
            false
        )
    }

    /**
     * Clean up resources
     */
    fun cleanup(context: Context, receiver: BroadcastReceiver?) {
        receiver?.let {
            context.unregisterReceiver(it)
        }
        // Clear any pending alarms
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

    fun getAllPendingIntentsOfAllAlarms(startTimeMin: Int, endTimeInMin: Int, frequencyMin: Int, context: Context, makeTheIntentNullIfAny:Boolean ):List<PendingIntent>{
        val receiverClass: Class<out BroadcastReceiver> = AlarmReceiver::class.java
        val pendingIntentsList = mutableListOf<PendingIntent>()
        val frequencyMilliSec = frequencyMin * 60000
        var startTimeInMilliSec =getTheCalenderInstanceAndSkipTheMinIn(startTimeMin).timeInMillis
        val endTimeInMilliSec =getTheCalenderInstanceAndSkipTheMinIn(endTimeInMin).timeInMillis
        val intent = Intent(ALARM_ACTION)
        intent.setClass(context, receiverClass)
        var intentAdded = 0
//        var pendingIntent:PendingIntent = PendingIntent()
//        logD("checking if the intent is null or not -->${intent !== null}")
        val flags = if (makeTheIntentNullIfAny) {
            // Keep existing flags if makeTheIntentNullIfAny is true
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            // Only use FLAG_NO_CREATE if makeTheIntentNullIfAny is false
            PendingIntent.FLAG_IMMUTABLE
        }

        while (startTimeInMilliSec <= endTimeInMilliSec){
            val  pendingIntent = PendingIntent.getBroadcast(context, startTimeInMilliSec.toInt(), intent, flags )
            logD("adding the pending intent to the list")
            pendingIntent?.let { pendingIntentsList.add(it) ; intentAdded++ }
            startTimeInMilliSec += frequencyMilliSec
        }
        return pendingIntentsList

    }

    fun printPendingIntents(pendingIntents: List<PendingIntent>) {
        logD("about to log the intent ")
        pendingIntents.forEachIndexed { index, pendingIntent ->
            logD("Intent $index: ${pendingIntent.intentSender}")
            // You can add more details like:
            val originalIntent = pendingIntent.javaClass.name
            logD( "Intent class name: ${originalIntent}")
            logD( "Request Code: ${pendingIntent.creatorUid}")
        }
    }

}
