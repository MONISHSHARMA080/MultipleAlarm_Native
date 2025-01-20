package com.example.trying_native.testHelperFile

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.TestContext
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
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
        var frequency = freq  * 60000
       var i =0
       while (startTime <= endTime){
           startTime += frequency
           i++
       }
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
    fun triggerPendingAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        // Create a latch to wait for alarm processing
        val latch = CountDownLatch(1)

        instrumentation.runOnMainSync {
            // Get next alarm time
            val info = alarmManager.nextAlarmClock
            if (info != null) {
                // Fast forward to just after the alarm time
                alarmManager.setTime(info.triggerTime + 100)
                // Allow for alarm processing
                Thread.sleep(WAIT_TIME)
            }
            latch.countDown()
        }

        // Wait for alarm processing to complete
        latch.await(WAIT_TIME, TimeUnit.MILLISECONDS)
    }

}