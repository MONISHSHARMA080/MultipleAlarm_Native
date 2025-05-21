package com.example.trying_native

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowApplication
import org.robolectric.shadows.ShadowSystemClock
import java.text.SimpleDateFormat
import kotlin.jvm.java
import java.util.Calendar
import java.util.Random
import java.time.Duration
import java.util.Locale

//@Config(application = ComponentActivity::class)
@Config(application = Application::class)
@RunWith(RobolectricTestRunner::class)
class AlarmFlowRobolectricTest {

        private lateinit var context: Context
//    private lateinit var context: Application
    private lateinit var alarmManager: AlarmManager
    private lateinit var shadowAlarmManager: ShadowAlarmManager
    private lateinit var shadowApplication: ShadowApplication
    private lateinit var inMemoryAlarmDao: AlarmDao

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = shadowOf(alarmManager)
        shadowApplication = shadowOf(context as Application)
        inMemoryAlarmDao = Room.inMemoryDatabaseBuilder(context, AlarmDatabase::class.java)
            .allowMainThreadQueries()
            .build()
            .alarmDao()
    }

    @Test
    fun testAlarmSchedule() = runBlocking {
        //  implement it
        getRandomAlarmTimesForDay(5)

        // Generate a random time during the day for the alarm (ensuring at least 5 minutes between start and end)
//        ShadowSystemClock.advanceBy(Duration.ofMillis(timeBeforeAlarm - System.currentTimeMillis()))

        // Current time that will be stored in the database
//        val currentSystemTime = ShadowSystemClock.currentTimeMillis()

    }



    /** this fun get the random start time for the alarm , the start time will be in the day*/
    data class RandomAlarmSelectedTime(
        val startTimeMillis: Long,
        val endTimeMillis: Long
    )

    private fun getRandomAlarmTimesForDay(minGapHours: Int): Result<RandomAlarmSelectedTime> {
        return runCatching {
            require(minGapHours in 0 until 24) {
                "minGapHours must be between 0 and 23"
            }

            val random = Random()

            // 1) build millis for start/end of today
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE,      0)
                set(Calendar.SECOND,      0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calendar.timeInMillis

            calendar.apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE,      59)
                set(Calendar.SECOND,      59)
                set(Calendar.MILLISECOND, 999)
            }
            val endOfDay = calendar.timeInMillis

            // 2) compute random gap in millis (≥ minGapHours, ≤ remaining hours)
            val minGapMillis = minGapHours * 60L * 60L * 1000L
            val maxPossibleGap = endOfDay - startOfDay
            require(minGapMillis <= maxPossibleGap) {
                "minGapHours is too large to fit in one day"
            }
            // pick a gap between [minGapMillis, maxPossibleGap]
            val gapMillis = minGapMillis + random.nextLong(maxPossibleGap - minGapMillis + 1)

            // 3) pick a start time so that start + gap ≤ endOfDay
            val latestStart = endOfDay - gapMillis
            val startTime = startOfDay + random.nextLong(latestStart - startOfDay + 1)

            // 4) derive end time
            val endTime = startTime + gapMillis

           logD(" in random start TIme selection func the startTime is ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(startTime)}-- and the end time is ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(endTime)}")

            RandomAlarmSelectedTime(startTimeMillis = startTime, endTimeMillis = endTime)
        }
    }
}
