package com.example.trying_native

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import com.example.trying_native.AlarmLogic.AlarmsController
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmData
import com.example.trying_native.dataBase.AlarmDatabase
import com.ibm.icu.impl.Assert.fail
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowApplication
import java.util.Calendar
import java.util.Timer


@Config(application = Application::class, sdk = [34])
@RunWith(RobolectricTestRunner::class)
class AlarmFlowRobolectricTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var shadowAlarmManager: ShadowAlarmManager
    private lateinit var alarmDao: AlarmDao
    private lateinit var alarmsController: AlarmsController
    private  val alarmData: AlarmData = mockk(relaxed=true)

    @Before
    fun setup() {
         context = ApplicationProvider.getApplicationContext()
         alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = Shadows.shadowOf(alarmManager)
        alarmsController = AlarmsController()
        alarmDao = mockk<AlarmDao>(relaxed = true)
    }

    data class info(
        var currentTime:Long,
        val startTime:Long,
        val endTime:Long,
        val freqInMin: Int,
        val iterCount:Int = 0,
        val alarmsController: AlarmsController
    ){ override fun toString(): String { return "currentTime: ${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(currentTime)}, startTime: ${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(startTime)}, endTime: ${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(endTime)}, freq(Min):$freqInMin" } }

    @Test
    fun `test multiple alarms are scheduled and able to run`() {
        runCatching {
            runBlocking {
                // Setup: 3:00 to 5:00 with 5-minute frequency
                val startCalendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 13)
                    set(Calendar.MINUTE, 0)
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
                SystemClock.setCurrentTimeMillis(startCalendar.timeInMillis - alarmData.getFreqInMillisecond(50L))
                println("\n\n the current time that we set is ${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(startCalendar.timeInMillis - alarmData.getFreqInMillisecond(frequencyMinutes.toLong()))}")

                alarmsController.scheduleMultipleAlarms(
                    alarmManager = alarmManager,
                    dateInLong = dateInLong,
                    calendarForStartTime = startCalendar,
                    calendarForEndTime = endCalendar,
                    freqAfterTheCallback = frequencyMinutes,
                    activityContext = context,
                    alarmDao = alarmDao,
                    messageForDB = "Test alarm"
                ).getOrThrow()

                var scheduledAlarms = shadowAlarmManager.scheduledAlarms
                assert(scheduledAlarms.isNotEmpty())
                println("the scheduled alarms are $scheduledAlarms\n")
                var curentTime =startCalendar.timeInMillis
                val endTime =endCalendar.timeInMillis
                var iterCount = 0
                val triggeredAlarmTimes = mutableListOf<Long>()
                var alarmInfo = info(startTime = startCalendar.timeInMillis, endTime = endCalendar.timeInMillis, currentTime = curentTime, alarmsController = alarmsController, freqInMin = frequencyMinutes)
                val expectedAlarmsAtTime = getExpectedTriggerTime(alarmInfo)
                SystemClock.setCurrentTimeMillis(startCalendar.timeInMillis - 10000L)


                // eg if alarms are from 3:00 -> 3:08 then we would have 8 + 1 alarms
                val totalExpectedAlarms = (( endCalendar.timeInMillis -startCalendar.timeInMillis  ) / frequencyMinutes) + 1
                while (curentTime < endTime || iterCount < totalExpectedAlarms){
                     alarmInfo = info(startTime = startCalendar.timeInMillis, endTime = endCalendar.timeInMillis, currentTime = curentTime, alarmsController = alarmsController, freqInMin = frequencyMinutes, iterCount = iterCount)
                    iterCount++
                    scheduledAlarms = shadowAlarmManager.scheduledAlarms
                    val nextAlarmTriggerTime = scheduledAlarms.first().triggerAtMs
                    check(scheduledAlarms.isNotEmpty()) {"scheduled alarms for future are not there for $alarmInfo"}
                    check(scheduledAlarms.size == 1) {"the size of the scheduled alarms for a time is more than 1, when it should be one; alarmInfo: $alarmInfo " }
                    check(expectedAlarmsAtTime.contains(nextAlarmTriggerTime)) {"the trigger time ${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(nextAlarmTriggerTime)} is not in the list of expected alarms, $alarmInfo"}
                     triggeredAlarmTimes.add(nextAlarmTriggerTime)
                    SystemClock.setCurrentTimeMillis(nextAlarmTriggerTime + 1000L)
                    curentTime = nextAlarmTriggerTime
                }

                check(triggeredAlarmTimes.size == expectedAlarmsAtTime.size)
            }
        }.fold(onSuccess = {}, onFailure = {err->
            println("the error in fun 'test multiple alarms are scheduled and able to run' is ->\n ${err.message}\n\n-- and full error is ->$err")
            fail(err.message)
        })
    }

    private  fun getExpectedTriggerTime(alarmInfo: info): List<Long>{
        val res = mutableListOf<Long>()
        val alarmInfo2 = alarmInfo.copy()
        println("expecting alarms at ->")
        while (alarmInfo2.endTime> alarmInfo2.currentTime) {
            res.add(alarmInfo2.currentTime)
            println("--${alarmInfo2.alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(alarmInfo2.currentTime)} ")
            alarmInfo2.currentTime += alarmData.getFreqInMillisecond(alarmInfo2.freqInMin.toLong())
        }
        return res
    }

    @Test
    fun `test alarms where the end date is diff from start date is giving error `() {

    }


}
