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
import com.example.trying_native.dataBase.AlarmDatabase
import com.ibm.icu.impl.Assert.fail
import kotlinx.coroutines.delay
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
//    private  val alarmData: AlarmData = mockk(relaxed=true)

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = Shadows.shadowOf(alarmManager)
        alarmsController = AlarmsController()
        alarmsController.scope = TestScope()
        val context: Context = ApplicationProvider.getApplicationContext()
        alarmDao = Room.inMemoryDatabaseBuilder(context, AlarmDatabase::class.java)
            .allowMainThreadQueries().build().alarmDao()
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

    @Test
    fun `test multiple alarms are scheduled and able to run`() {
        runCatching {
            runBlocking {
                // Setup: 3:00 to 5:00 with 5-minute frequency
                val testReceiver = NextAlarmReceiver()
                testReceiver.alarmDao = alarmDao
                // 2. Register it dynamically in the test context
                // This ensures the Intent with action "ALARM_TRIGGERED" comes here
                val filter = IntentFilter("com.example.trying_native.ALARM_TRIGGERED")
                context.registerReceiver(testReceiver, filter, Context.RECEIVER_EXPORTED)
                val startCalendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 13) ; set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }


                val endCalendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 15)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val dateInLong = startCalendar.timeInMillis
                val frequencyMinutes = 5
                SystemClock.setCurrentTimeMillis(
                    Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 12) }.timeInMillis
                )
                alarmsController = AlarmsController(TestTimeProvider(Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 12) }.timeInMillis))
                testReceiver.alarmsController = alarmsController
                println("\n\n the current time that we set is ${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(startCalendar.timeInMillis - frequencyMinutes.toLong() * 60_000)}")
                alarmsController.scheduleMultipleAlarms(
                    alarmManager = alarmManager, dateInLong = dateInLong, calendarForStartTime = startCalendar, calendarForEndTime = endCalendar,
                    freqAfterTheCallback = frequencyMinutes, activityContext = context, alarmDao = alarmDao, messageForDB = "Test alarm"
                ).getOrThrow()
                var scheduledAlarms = shadowAlarmManager.scheduledAlarms
                assert(scheduledAlarms.isNotEmpty())
                var currentTime = startCalendar.timeInMillis
                val endTime = endCalendar.timeInMillis
                var iterCount = 0
                val freqMs = frequencyMinutes * 60_000L
                val triggeredAlarmTimes = mutableListOf<Long>()
                var alarmInfo = info(
                    startTime = startCalendar.timeInMillis,
                    endTime = endCalendar.timeInMillis,
                    currentTime = currentTime,
                    alarmsController = alarmsController,
                    freqInMin = frequencyMinutes
                )
                val expectedAlarmsAtTime = getExpectedTriggerTime(alarmInfo)
                SystemClock.setCurrentTimeMillis(startCalendar.timeInMillis - 10000L)
                // now the thing is that if this is true then when we fire the first NextAlarmReceiver then it will set the future alarm and then it
                // will recursively call till the end, idk why and without it we are able to control
                ShadowAlarmManager.setAutoSchedule(false)

                // eg if alarms are from 3:00 -> 3:08 then we would have 8 + 1 alarms
                var index = 0
                while (currentTime <= endTime) {
                    shadowMainLooper().idle() // Tells Robolectric to execute all pending tasks
                    delay(100)               // Small buffer for coroutines
                    shadowMainLooper().runUntilEmpty()
                    alarmInfo = info(startTime = startCalendar.timeInMillis, endTime = endCalendar.timeInMillis, currentTime = currentTime, alarmsController = alarmsController, freqInMin = frequencyMinutes, iterCount = iterCount)
                    println("-------at index:$index and the alarmInfo is $alarmInfo")
                    iterCount++
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
//                        val pi = alarm.operation
//                        val shadowPI = shadowOf(pi)
//                        val intent = shadowPI.savedIntent
//                        val receiverName = intent.component?.className ?: "Unknown"
//                        println("trigger Alarm for: $receiverName at ${alarm.triggerAtMs}")
                        // 3. FIRE THE ALARM ONLY ONCE
                        // Use the shadowAlarmManager to fire the specific PendingIntent
                    }
                    check(scheduledAlarms.isNotEmpty()) { "the size of the scheduled alarms for a time is not 3, it is ${scheduledAlarms.size} when it should be one; alarmInfo: $alarmInfo " }
                    check(expectedAlarmsAtTime.contains(nextAlarmTriggerTime)) { "the trigger time ${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(nextAlarmTriggerTime)  } is not in the list of expected alarms, $alarmInfo"
                    }

                    triggeredAlarmTimes.add(nextAlarmTriggerTime)
                    SystemClock.setCurrentTimeMillis(nextAlarmTriggerTime + 1000L)
                    ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(nextAlarmTriggerTime - currentTime))
                    currentTime += freqMs
                    val nextTrigger = scheduledAlarms.minOf { it.triggerAtMs }
                    println("\n  the currentTime after update is ${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(currentTime)} ")
                    println("\n the nextAlarm trigger time is ${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(nextAlarmTriggerTime)} ")
                    ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(nextTrigger - SystemClock.currentThreadTimeMillis()))
                    shadowOf(Looper.getMainLooper()).idle()
                    println(":) 2 and index is $index")
                    index += 1
                }
                check(triggeredAlarmTimes.size == expectedAlarmsAtTime.size) { "triggerAlarmTimes.Size:${triggeredAlarmTimes.size} != expectedAlarmsAtTime.size:${expectedAlarmsAtTime.size} " }
                check(triggeredAlarmTimes == expectedAlarmsAtTime) { "triggerAlarmTimes:${triggeredAlarmTimes.size} != expectedAlarmsAtTime:${expectedAlarmsAtTime.size} " }
            }
        }.fold(onSuccess = {}, onFailure = { err ->
            println("the error in fun 'test multiple alarms are scheduled and able to run' is ->\n ${err.message}\n\n-- and full error is ->$err")
            fail(err.message)
        })
    }

    private fun getExpectedTriggerTime(alarmInfo: info): List<Long> {
        val res = mutableListOf<Long>()
        val alarmInfo2 = alarmInfo.copy()
        println("expecting alarms at ->")
        while (alarmInfo2.endTime > alarmInfo2.currentTime) {
            res.add(alarmInfo2.currentTime)
            print(
                "--|--${
                    alarmInfo2.alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(
                        alarmInfo2.currentTime
                    )
                } "
            )
            alarmInfo2.currentTime += 5 * 60_000
        }
        print("\n")
        return res
    }
}
