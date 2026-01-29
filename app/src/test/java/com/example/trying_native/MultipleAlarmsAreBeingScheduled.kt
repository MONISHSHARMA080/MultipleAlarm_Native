package com.example.trying_native

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.content.IntentFilter
import android.os.Looper
import android.os.SystemClock
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.trying_native.AlarmLogic.AlarmsController
import com.example.trying_native.AlarmLogic.TimeProvider
import com.example.trying_native.BroadCastReceivers.NextAlarmReceiver
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmData
import com.example.trying_native.dataBase.AlarmDatabase
import com.ibm.icu.impl.Assert.fail
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowLooper.shadowMainLooper
import org.robolectric.shadows.ShadowSystemClock
import java.util.Calendar
import kotlin.math.exp
import kotlin.math.min
import kotlin.random.Random

class TestTimeProvider(var fixedTime: Long) : TimeProvider {
    override fun getCurrentTime(): Long {
        return  fixedTime
    }
}

@Config(application = Application::class, sdk = [34])
@RunWith(RobolectricTestRunner::class)
class AlarmFlowRobolectricTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var shadowAlarmManager: ShadowAlarmManager
    private lateinit var alarmDao: AlarmDao
    private lateinit var alarmsController: AlarmsController
    private lateinit var testReceiver: NextAlarmReceiver

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = Shadows.shadowOf(alarmManager)
        alarmsController = AlarmsController()
        alarmsController.scope = TestScope()
        val context: Context = ApplicationProvider.getApplicationContext()
        alarmDao = Room.inMemoryDatabaseBuilder(context, AlarmDatabase::class.java).allowMainThreadQueries().build().alarmDao()
        testReceiver = NextAlarmReceiver()
        testReceiver.alarmDao = alarmDao
        // 2. Register it dynamically in the test context
        // This ensures the Intent with action "ALARM_TRIGGERED" comes here
        val filter = IntentFilter("com.example.trying_native.ALARM_TRIGGERED")
        context.registerReceiver(testReceiver, filter, Context.RECEIVER_EXPORTED)

    }

    data class info(
        var currentTime: Long,
        val startTime: Long,
        val endTime: Long,
        val freqInMin: Int,
        val iterCount: Int = 0,
        val alarmsController: AlarmsController
    ) {
        override fun toString(): String {
            return "currentTime: ${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(currentTime)}, startTime: ${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(startTime)}, endTime: ${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(endTime)}, freq(Min):$freqInMin"
        }
    }
    private fun getRandomStartAndEndCal():Pair<Calendar, Calendar>{
        // this can't be 24 as if it is then end hour would also have to be 24 and no alarms be scheduled in worst case
        val startHour:Int = getRandomInt(0, 23)
        // worst case is that the start time is 00:00 and the freq(max) 30, so to prevent this and able to set
        val startMin:Int  = getRandomInt(31,60)
        val endMin:Int    = getRandomInt(0,60)
        var endHour =getRandomInt(startHour+1, 24)
        // correction
        var notDiffEnough = true
        while (notDiffEnough){
            notDiffEnough = endHour - startHour < 1
            if (notDiffEnough){
                endHour = getRandomInt(startHour+1, 24)
            }
        }

        val startCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startHour) ; set(Calendar.MINUTE, startMin)
        }

        val endCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, endHour); set(Calendar.MINUTE, endMin) // doesn't depend on  start min
        }
        check(startCalendar.timeInMillis < endCalendar.timeInMillis) {"the start time:${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(startCalendar.timeInMillis)} is not greater than the end time:${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(endCalendar.timeInMillis)}"}
        return Pair(startCalendar, endCalendar)
    }

//    @Test
    fun `test multiple alarms are scheduled and able to run`() {
        runCatching {
            runBlocking {
                // Setup: 3:00 to 5:00 with 5-minute frequency
                val calPairs =getRandomStartAndEndCal()
                val startCalendar = calPairs.first
                val endCalendar = calPairs.second
                val dateInLong = startCalendar.timeInMillis
                val frequencyMinutes = getRandomInt(max=30)
                val freqMs = frequencyMinutes * 60_000L
                println("\n\nthe randomly selected startTime is ${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(startCalendar.timeInMillis)} and endTime is ${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(endCalendar.timeInMillis)} and (Randomly selected) freq is $frequencyMinutes")
                SystemClock.setCurrentTimeMillis(startCalendar.timeInMillis - freqMs)
                alarmsController = AlarmsController(TestTimeProvider(startCalendar.timeInMillis -freqMs))
                testReceiver.alarmsController = alarmsController
                println(" the current time that we set is ${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(startCalendar.timeInMillis - frequencyMinutes.toLong() * 60_000)}")
                alarmsController.scheduleMultipleAlarms(
                    alarmManager = alarmManager, dateInLong = dateInLong, calendarForStartTime = startCalendar, calendarForEndTime = endCalendar,
                    freqAfterTheCallback = frequencyMinutes, activityContext = context, alarmDao = alarmDao, messageForDB = "Test alarm"
                ).getOrThrow()
                var scheduledAlarms = shadowAlarmManager.scheduledAlarms
                assert(scheduledAlarms.isNotEmpty())
                var currentTime = startCalendar.timeInMillis
                val endTime = endCalendar.timeInMillis
                val triggeredAlarmTimes = mutableListOf<Long>()
                var alarmInfo = info(
                    startTime = startCalendar.timeInMillis,
                    endTime = endCalendar.timeInMillis,
                    currentTime = currentTime,
                    alarmsController = alarmsController,
                    freqInMin = frequencyMinutes
                )
                val expectedAlarmsAtTime: List<Long> = getExpectedTriggerTime(alarmInfo)
                SystemClock.setCurrentTimeMillis(startCalendar.timeInMillis - 10000L)
                // now the thing is that if this is true then when we fire the first NextAlarmReceiver then it will set the future alarm and then it
                // will recursively call till the end, idk why and without it we are able to control
                ShadowAlarmManager.setAutoSchedule(false)

                // eg if alarms are from 3:00 -> 3:08 then we would have 8 + 1 alarms
                var index = 0
                while (currentTime <= endTime) {
                    shadowMainLooper().idle() // Tells Robolectric to execute all pending tasks
                    shadowMainLooper().runUntilEmpty()
                    alarmInfo = info(startTime = startCalendar.timeInMillis, endTime = endCalendar.timeInMillis, currentTime = currentTime, alarmsController = alarmsController, freqInMin = frequencyMinutes, iterCount = index)
                    println("-------at index:$index and the alarmInfo is $alarmInfo")
                    scheduledAlarms = shadowAlarmManager.scheduledAlarms
                    check(scheduledAlarms.isNotEmpty()) { "scheduled alarms for future are not there -> $alarmInfo\n and index is $index and triggeredAlarmsTimes is ${triggeredAlarmTimes.size} " }
                    val nextAlarmTriggerTime = scheduledAlarms.first().triggerAtMs
                    println("---")
                    for (alarm in scheduledAlarms) {
                        // 1. Get the PendingIntent using the non-deprecated getter
                        val operation = alarm.operation
                        val intent = shadowOf(operation).savedIntent

                        if (intent.component?.className == NextAlarmReceiver::class.java.name) {
                            println("We got the intent with the classname $intent")
                            shadowAlarmManager.fireAlarm(alarm)

                        }
                    }
                    check(scheduledAlarms.isNotEmpty()) { "the size of the scheduled alarms for a time is not 3, it is ${scheduledAlarms.size} when it should be one; alarmInfo: $alarmInfo " }
                    check(expectedAlarmsAtTime.contains(nextAlarmTriggerTime)) { "the trigger time ${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(nextAlarmTriggerTime)  } is not in the list of expected alarms, $alarmInfo"
                    }

                    triggeredAlarmTimes.add(currentTime)
                    SystemClock.setCurrentTimeMillis(nextAlarmTriggerTime + 1000L)
                    ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(nextAlarmTriggerTime - currentTime))
                    currentTime += freqMs
                    val nextTrigger = scheduledAlarms.minOf { it.triggerAtMs }
                    println("\n  the currentTime after update is ${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(currentTime)} ")
                    println(" the nextAlarm trigger time is ${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(nextAlarmTriggerTime)} ")
                    ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(nextTrigger - SystemClock.currentThreadTimeMillis()))
                    shadowOf(Looper.getMainLooper()).idle()
                    println(":) 2 and index is $index")
                    index += 1

                }
                for (alarm in triggeredAlarmTimes) {
                    println(alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(alarm))
                }
                println("\n\n\t\t------ expectedAlarmsAtTime ->")
                for (alarm in expectedAlarmsAtTime) {
                    println(alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(alarm))
                }
                println("")

                check(triggeredAlarmTimes.size == expectedAlarmsAtTime.size) { "triggerAlarmTimes.Size:${triggeredAlarmTimes.size} != expectedAlarmsAtTime.size:${expectedAlarmsAtTime.size} " }
                check(triggeredAlarmTimes == expectedAlarmsAtTime) { "triggerAlarmTimes:${triggeredAlarmTimes.size} != expectedAlarmsAtTime:${expectedAlarmsAtTime.size} " }
            }
        }.fold(onSuccess = {}, onFailure = { err ->
            println("the error in fun 'test multiple alarms are scheduled and able to run' is ->\n ${err.message}\n\n-- and full error is ->$err")
            println("triggeredAlarmTime ->")
            fail(err.message)
        })

    }
    private fun getRandomInt(min:Int = 1, max:Int):Int {
        return Random.nextInt(min, max)
    }
    private fun getExpectedTriggerTime(alarmInfo: info): List<Long> {
        val res = mutableListOf<Long>()
        val alarmInfo2 = alarmInfo.copy()
        println("expecting alarms at ->")
        while (alarmInfo2.currentTime <= alarmInfo2.endTime) {
            res.add(alarmInfo2.currentTime)
            print("--|--${alarmInfo2.alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(alarmInfo2.currentTime)} ")
            alarmInfo2.currentTime += alarmInfo2.freqInMin * 60_000
        }
        print("\n")
        return res
    }
    private fun getAllAlarmsInDb(): Result<List<AlarmData>> {
        return runCatching {runBlocking { alarmDao.getAllAlarms() } }
    }
    private inline fun waitTillTaskFinishWithLooper(){
        shadowMainLooper().idle()
        shadowMainLooper().runUntilEmpty()
    }


    @Test
    fun `test cancel alarm removes all pending intents and updates database`() {
        runCatching {
            runBlocking {
                // Setup: Schedule alarms first
                val calPairs = getRandomStartAndEndCal()
                val startCalendar = calPairs.first
                val endCalendar = calPairs.second
                val dateInLong = startCalendar.timeInMillis
                val frequencyMinutes = getRandomInt(max = 30)
                val freqMs = frequencyMinutes * 60_000L

                println("\n\nScheduling alarms - startTime: ${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(startCalendar.timeInMillis)}, endTime: ${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(endCalendar.timeInMillis)}, freq: $frequencyMinutes min")

                SystemClock.setCurrentTimeMillis(startCalendar.timeInMillis - freqMs)
                alarmsController = AlarmsController(TestTimeProvider(startCalendar.timeInMillis - freqMs))
                testReceiver.alarmsController = alarmsController

                // Schedule the alarms
                alarmsController.scheduleMultipleAlarms(
                    alarmManager = alarmManager,
                    dateInLong = dateInLong,
                    calendarForStartTime = startCalendar,
                    calendarForEndTime = endCalendar,
                    freqAfterTheCallback = frequencyMinutes,
                    activityContext = context,
                    alarmDao = alarmDao,
                    messageForDB = "Test alarm for cancellation"
                ).getOrThrow()
                waitTillTaskFinishWithLooper()
                val scheduledAlarmsAfterScheduling = shadowAlarmManager.scheduledAlarms
                println("Scheduled alarms count: ${scheduledAlarmsAfterScheduling.size}")
                assert(scheduledAlarmsAfterScheduling.isNotEmpty()){"expected the scheduler alarms to be not empty but got size of ${scheduledAlarmsAfterScheduling.size} "}

                // Get the alarm data from DB
                val alarmsInDb = alarmDao.getAllAlarms()
                assert(alarmsInDb.isNotEmpty()) { "No alarms in database" }
                assert(alarmsInDb.size ==1){"expected the size of the alarms in db to be 1 but got ${alarmsInDb.size}"}
                val alarmData = alarmsInDb.first()
                println("AlarmData from DB: startTime=${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(alarmData.startTime)}, endTime=${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(alarmData.endTime)}, isReadyToUse=${alarmData.isReadyToUse}")

                // Cancel the alarms (delete_the_alarm_from_db = true)
                println("\n--- Cancelling alarms (with DB deletion) ---")
                alarmsController.cancelAlarmByCancelingPendingIntent(
                    startTime = startCalendar.timeInMillis,
                    endTime = endCalendar.timeInMillis,
                    frequencyInMin = freqMs,
                    alarmDao = alarmDao,
                    alarmManager = alarmManager,
                    context_of_activity = context,
                    delete_the_alarm_from_db = true,
                    alarmData = alarmData
                )
                waitTillTaskFinishWithLooper()
                // Verify all alarms are cancelled
                val scheduledAlarms = shadowAlarmManager.scheduledAlarms
                println("Scheduled alarms count after cancellation: ${scheduledAlarms.size}")
                for (alarm in scheduledAlarms) {
                    // 1. Get the PendingIntent using the non-deprecated getter
                    val operation = alarm.operation
                    val intent = shadowOf(operation).savedIntent
                    println("the alarm intent in the scheduledAlarms after cancelling is is $intent")
                }
                assert(scheduledAlarms.isEmpty()) { "Alarms were not cancelled. Remaining: ${scheduledAlarms.size}" }
                val alarmsInDbAfterCancel = alarmDao.getAllAlarms()
                println("Alarms in DB after cancellation: ${alarmsInDbAfterCancel.size}")
                val alarminDb1 = alarmDao.getAllAlarms()
                println("the alarmDao is $alarmsInDbAfterCancel :${alarmsInDbAfterCancel.size} and after DB delete is ${alarminDb1} :${alarminDb1.size}")
                assert(alarmsInDbAfterCancel.isEmpty()) { "alarmDataInDb is not empty and it is $alarmData" }

                println("\n✓ Test passed: All alarms cancelled and removed from DB")
            }
        }.fold(
            onSuccess = {},
            onFailure = { err ->
                println("Test failed with error: ${err.message}\n${err.stackTraceToString()}")
                fail(err.message)
            }
        )
    }


}
