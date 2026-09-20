package com.coolApps.MultipleAlarmClock

import android.app.AlarmManager
import android.content.Context
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.coolApps.MultipleAlarmClock.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.di.DispatcherModule
import com.coolApps.MultipleAlarmClock.di.IoDispatcher
import com.coolApps.MultipleAlarmClock.domain.repository.AlarmRepository
import com.coolApps.MultipleAlarmClock.domain.usecase.AlarmsController
import com.coolApps.MultipleAlarmClock.util.Result
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random


@HiltAndroidTest
@UninstallModules(DispatcherModule::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class AlarmSeriesLogicTest2 {

	@get:Rule
	val hiltRule = HiltAndroidRule(this)

	@BindValue
	@JvmField
	val fakeAlarmRepository: AlarmRepository = FakeAlarmRepository()

	private val testDispatcher = StandardTestDispatcher()

	@BindValue
	@JvmField
	@IoDispatcher
	val ioDispatcher: CoroutineDispatcher =
		testDispatcher

	@Inject
	lateinit var controller: AlarmsController

	private lateinit var context: Context
	private lateinit var alarmManager: AlarmManager
	private lateinit var shadowAlarmManager: ShadowAlarmManager

	@Before
	fun setUp() {
		hiltRule.inject()
		context = ApplicationProvider.getApplicationContext()
		alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
		shadowAlarmManager = shadowOf(alarmManager)
		ShadowAlarmManager.setAutoSchedule(true)
	}

	@Test
	fun `start alarm series form beginning`() = runTest {
		val startTime = Calendar.getInstance().apply {
			add(Calendar.DAY_OF_YEAR, 1)
			set(Calendar.HOUR_OF_DAY, 10)
			set(Calendar.MINUTE, 0)
			set(Calendar.SECOND, 0)
			set(Calendar.MILLISECOND, 0)
		}.timeInMillis

		val endTime = startTime + Duration.ofHours(8).toMillis()
		val frequency = Duration.ofMinutes(1).toMillis()

		val alarm = AlarmData(
			startTime = startTime,
			endTime = endTime,
			message = "test alarm",
			isReadyToUse = false,
			frequencyInMin = 1,
			repeatDays = null,
			sound = null
		)

		val expectedAlarmList  = mutableListOf<Long>()
		alarm.alarmTimeSequence().iterator().forEach {
			value-> expectedAlarmList.add(value)
		}

		// Start Robolectric clock at the alarm start.
		SystemClock.setCurrentTimeMillis(startTime - 200L)
		logD("test AlarmData:$alarm, expectedAlarms:${expectedAlarmList.size}")

		val result = controller.startAlarmSeriesHandler(
			alarm = alarm,
			alarmManager = alarmManager,
			activityContext = context
		)
		assertThat(result).isInstanceOf(Result.Success::class.java)

		val storedAlarm = fakeAlarmRepository.getAllAlarms().single()

		logD("test retried alarm:$storedAlarm")
		assertThat(storedAlarm.isReadyToUse).isTrue()
		assertThat(storedAlarm.startTime).isEqualTo(startTime)
		assertThat(storedAlarm.endTime).isEqualTo(endTime)
		assertThat(storedAlarm.frequencyInMin).isEqualTo(1)

		var expectedTrigger = startTime
		var firedCount = 0

		while (expectedTrigger < endTime) {

			val scheduled = shadowAlarmManager.peekNextScheduledAlarm()
			logD("\n\n(iteration:${firedCount}) Scheduled:$scheduled, scheduled:${scheduled?.triggerAtMs}, fired:$firedCount")
			logD("alarm scheduled at:${scheduled?.triggerAtMs} and alarm at this index's start time ${expectedAlarmList[firedCount]} are they same:${scheduled?.triggerAtMs == expectedAlarmList[firedCount]}")

			assertThat(scheduled).isNotNull()
			assertThat(scheduled!!.triggerAtMs).isEqualTo(expectedTrigger)

			val delta = expectedTrigger - SystemClock.uptimeMillis()

			shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(delta))
			testDispatcher.scheduler.advanceUntilIdle()
			shadowOf(Looper.getMainLooper()).idle()
			testDispatcher.scheduler.advanceUntilIdle()

			firedCount++

			val nextTrigger = expectedTrigger + frequency
			expectedTrigger = nextTrigger
		}

		assertThat(firedCount).isEqualTo(expectedAlarmList.size -1)
	}


	@Test
	fun `start alarm series form the middle`() = runTest {
		val startTime = Calendar.getInstance().apply {
			add(Calendar.DAY_OF_YEAR, 100)
			set(Calendar.HOUR_OF_DAY, 5)
			set(Calendar.MINUTE, 0)
			set(Calendar.SECOND, 0)
			set(Calendar.MILLISECOND, 0)
		}.timeInMillis
		val endTime = startTime + Duration.ofHours(18).toMillis()
		val freqInMin = Random.nextInt(1, 25).toLong()
		val frequency = Duration.ofMinutes(freqInMin).toMillis()

		val alarm = AlarmData(
			startTime = startTime, endTime = endTime, message = "test alarm",
			isReadyToUse = false, frequencyInMin = freqInMin, repeatDays = null, sound = null
		)

		val fullSequence = alarm.alarmTimeSequence().toList()

		// Wake up somewhere between two arbitrary consecutive ticks. Exclude the last
		// index so at least one future tick always remains; landing halfway (not
		// exactly on a tick) mirrors the "5:59 between 5:58 and 6:00" case.
		logD("${fullSequence.size * 0.8}, fullSequence:${fullSequence.size}")
		val wakeUpAfterIndex = Random.nextInt(0, ((fullSequence.size * 0.8).toInt()))
		val now = fullSequence[wakeUpAfterIndex] + frequency / 2

		val expectedAlarmList = fullSequence.filter { it > now }
		check(expectedAlarmList.isNotEmpty())
		val nowCalendar = Calendar.getInstance().apply { timeInMillis = now }

		logD("randomIndex:$wakeUpAfterIndex wakeUpAfterIndex:$wakeUpAfterIndex now:${getTimeInHumanReadableFormatProtectFrom0Included(now)} firstExpected:${getTimeInHumanReadableFormatProtectFrom0Included(expectedAlarmList.first())}")

		SystemClock.setCurrentTimeMillis(now)

		val result = controller.startAlarmSeriesHandler(
			alarm = alarm, alarmManager = alarmManager, activityContext = context, now =  nowCalendar
		)
		assertThat(result).isInstanceOf(Result.Success::class.java)

		val storedAlarm = fakeAlarmRepository.getAllAlarms().single()
		assertThat(storedAlarm.isReadyToUse).isTrue()
		assertThat(storedAlarm.startTime).isEqualTo(startTime)   // unchanged - no rollover expected
		assertThat(storedAlarm.endTime).isEqualTo(endTime)

		// Unconditional check: covers the case (reachable when the random pick lands on
		// the second-to-last tick) where only the final tick is left and the loop below
		// never executes a single iteration.
		val firstScheduled = shadowAlarmManager.peekNextScheduledAlarm()
		assertThat(firstScheduled).isNotNull()
		logD("alarm is $alarm")
		logD("next alarm at:${getTimeInHumanReadableFormatProtectFrom0Included(firstScheduled!!.triggerAtMs)}, first expected alarm:${getTimeInHumanReadableFormatProtectFrom0Included(expectedAlarmList.first())}, current time:${getTimeInHumanReadableFormatProtectFrom0Included(now)}")
		assertThat(firstScheduled.triggerAtMs).isEqualTo(expectedAlarmList.first())

		var expectedTrigger = expectedAlarmList.first()
		var firedCount = 0

		while (expectedTrigger <= endTime) {
			val scheduled = shadowAlarmManager.peekNextScheduledAlarm()
			assertThat(scheduled).isNotNull()
			assertThat(scheduled!!.triggerAtMs).isEqualTo(expectedTrigger)
			logD("\n\n(iteration:${firedCount}) Scheduled:$scheduled, scheduled:${geTimeWithoutDate(scheduled.triggerAtMs)}, fired:$firedCount, noOfAlarms:${expectedAlarmList.size}")

			val delta = expectedTrigger - SystemClock.uptimeMillis()
			shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(delta))
			testDispatcher.scheduler.advanceUntilIdle()
			shadowOf(Looper.getMainLooper()).idle()
			testDispatcher.scheduler.advanceUntilIdle()
			firedCount++
			expectedTrigger += frequency
		}

		assertThat(firedCount).isEqualTo(expectedAlarmList.size )
	}

	@Test
	fun `resetAlarmsHandler when current time is before alarm series start`() = runTest {
		val startTime = Calendar.getInstance().apply {
			add(Calendar.DAY_OF_YEAR, 1)
			set(Calendar.HOUR_OF_DAY, 10)
			set(Calendar.MINUTE, 0)
			set(Calendar.SECOND, 0)
			set(Calendar.MILLISECOND, 0)
		}.timeInMillis
		val endTime = startTime + Duration.ofHours(8).toMillis()

		val alarm = AlarmData(
			startTime = startTime, endTime = endTime, message = "test before start",
			isReadyToUse = false, frequencyInMin = 30, repeatDays = null, sound = null
		)
		val alarmId = fakeAlarmRepository.saveAlarm(alarm).toInt()
		val savedAlarm = alarm.copy(id = alarmId)

		// Set time to before the start time
		val now = startTime - Duration.ofHours(1).toMillis()
		SystemClock.setCurrentTimeMillis(now)
		val nowCalendar = Calendar.getInstance().apply { timeInMillis = now }

		val result = controller.resetAlarmsHandler(savedAlarm, alarmManager, context, nowCalendar)
		assertThat(result).isInstanceOf(Result.Success::class.java)

		var scheduled = shadowAlarmManager.peekNextScheduledAlarm()
		assertThat(scheduled).isNotNull()
		assertThat(scheduled!!.triggerAtMs).isEqualTo(startTime)

		var expectedTrigger = startTime
		var firedCount = 0

		while (expectedTrigger <= endTime) {
			scheduled = shadowAlarmManager.peekNextScheduledAlarm()
			assertThat(scheduled).isNotNull()
			assertThat(scheduled!!.triggerAtMs).isEqualTo(expectedTrigger)

			val delta = expectedTrigger - SystemClock.uptimeMillis()
			shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(delta))
			testDispatcher.scheduler.advanceUntilIdle()
			shadowOf(Looper.getMainLooper()).idle()
			testDispatcher.scheduler.advanceUntilIdle()
			
			firedCount++
			expectedTrigger += Duration.ofMinutes(30).toMillis()
		}
		
		val expectedAlarmListSize = savedAlarm.alarmTimeSequence().toList().size
		assertThat(firedCount).isEqualTo(expectedAlarmListSize)
	}

	@Test
	fun `resetAlarmsHandler when current time is in the middle of alarm series`() = runTest {
		val startTime = Calendar.getInstance().apply {
			add(Calendar.DAY_OF_YEAR, 1)
			set(Calendar.HOUR_OF_DAY, 10)
			set(Calendar.MINUTE, 0)
			set(Calendar.SECOND, 0)
			set(Calendar.MILLISECOND, 0)
		}.timeInMillis
		val endTime = startTime + Duration.ofHours(8).toMillis()
		val frequency = Duration.ofMinutes(30).toMillis()

		val alarm = AlarmData(
			startTime = startTime, endTime = endTime, message = "test in middle",
			isReadyToUse = false, frequencyInMin = 30, repeatDays = null, sound = null
		)
		val alarmId = fakeAlarmRepository.saveAlarm(alarm).toInt()
		val savedAlarm = alarm.copy(id = alarmId)

		val fullSequence = savedAlarm.alarmTimeSequence().toList()
		val middleIndex = fullSequence.size / 2
		
		// Set time exactly between two alarm ticks
		val now = fullSequence[middleIndex] + frequency / 2 
		SystemClock.setCurrentTimeMillis(now)
		val nowCalendar = Calendar.getInstance().apply { timeInMillis = now }

		val result = controller.resetAlarmsHandler(savedAlarm, alarmManager, context, nowCalendar)
		assertThat(result).isInstanceOf(Result.Success::class.java)

		val expectedNextTick = fullSequence[middleIndex + 1]
		var scheduled = shadowAlarmManager.peekNextScheduledAlarm()
		
		assertThat(scheduled).isNotNull()
		assertThat(scheduled!!.triggerAtMs).isEqualTo(expectedNextTick)

		var expectedTrigger = expectedNextTick
		var firedCount = 0

		while (expectedTrigger <= endTime) {
			scheduled = shadowAlarmManager.peekNextScheduledAlarm()
			assertThat(scheduled).isNotNull()
			assertThat(scheduled!!.triggerAtMs).isEqualTo(expectedTrigger)

			val delta = expectedTrigger - SystemClock.uptimeMillis()
			shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(delta))
			testDispatcher.scheduler.advanceUntilIdle()
			shadowOf(Looper.getMainLooper()).idle()
			testDispatcher.scheduler.advanceUntilIdle()
			
			firedCount++
			expectedTrigger += frequency
		}
		
		// The number of alarms fired should be the remaining sequence from middleIndex+1 to the end
		val expectedAlarmListSize = fullSequence.size - (middleIndex + 1)
		assertThat(firedCount).isEqualTo(expectedAlarmListSize)
	}

	@Test
	fun `resetAlarmsHandler when current time is past the end time`() = runTest {
		val startTime = Calendar.getInstance().apply {
			add(Calendar.DAY_OF_YEAR, 1)
			set(Calendar.HOUR_OF_DAY, 10)
			set(Calendar.MINUTE, 0)
			set(Calendar.SECOND, 0)
			set(Calendar.MILLISECOND, 0)
		}.timeInMillis
		val endTime = startTime + Duration.ofHours(8).toMillis()

		val alarm = AlarmData(
			startTime = startTime, endTime = endTime, message = "test past end",
			isReadyToUse = false, frequencyInMin = 30, repeatDays = null, sound = null
		)
		val alarmId = fakeAlarmRepository.saveAlarm(alarm).toInt()
		val savedAlarm = alarm.copy(id = alarmId)

		// Set time past the end time
		val now = endTime + Duration.ofHours(1).toMillis()
		SystemClock.setCurrentTimeMillis(now)
		val nowCalendar = Calendar.getInstance().apply { timeInMillis = now }

		val result = controller.resetAlarmsHandler(savedAlarm, alarmManager, context, nowCalendar)
		assertThat(result).isInstanceOf(Result.Success::class.java)

		val updatedAlarm = fakeAlarmRepository.getAlarmById(alarmId)
		assertThat(updatedAlarm).isNotNull()
		// Because time interval passed, rollOverIfTimeIntervalPassed should update the startTime to the next day or period
		assertThat(updatedAlarm!!.startTime).isGreaterThan(startTime)
		
		var scheduled = shadowAlarmManager.peekNextScheduledAlarm()
		assertThat(scheduled).isNotNull()
		assertThat(scheduled!!.triggerAtMs).isEqualTo(updatedAlarm.startTime)

		var expectedTrigger = updatedAlarm.startTime
		var firedCount = 0

		while (expectedTrigger <= updatedAlarm.endTime) {
			scheduled = shadowAlarmManager.peekNextScheduledAlarm()
			assertThat(scheduled).isNotNull()
			assertThat(scheduled!!.triggerAtMs).isEqualTo(expectedTrigger)

			val delta = expectedTrigger - SystemClock.uptimeMillis()
			shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(delta))
			testDispatcher.scheduler.advanceUntilIdle()
			shadowOf(Looper.getMainLooper()).idle()
			testDispatcher.scheduler.advanceUntilIdle()
			
			firedCount++
			expectedTrigger += Duration.ofMinutes(30).toMillis()
		}
		
		val expectedAlarmListSize = updatedAlarm.alarmTimeSequence().toList().size
		assertThat(firedCount).isEqualTo(expectedAlarmListSize)
	}

	private fun logD(str: String){
		Log.d("AAAA", "[Test] $str")
	}
	fun getTimeInHumanReadableFormatProtectFrom0Included(t:Long): String{
		if (t == 0L) return "--the time here(probablyFromTheIntent) is 0--"
		return SimpleDateFormat("h:mm:ss a yyyy-MM-dd", Locale.getDefault()).format(Date(t))
	}
	fun geTimeWithoutDate(t:Long): String{
		if (t == 0L) return "--the time here(probablyFromTheIntent) is 0--"
		return SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date(t))
	}


}

/**
 * In-memory fake for AlarmRepository.
 */
class FakeAlarmRepository : AlarmRepository {
	private val store = LinkedHashMap<Int, AlarmData>()
	private val _alarmsStream = MutableStateFlow<List<AlarmData>>(emptyList())
	private var nextId = 1

	private fun updateStream() {
		_alarmsStream.value = store.values.toList()
	}

	fun all(): List<AlarmData> = store.values.toList()

	override fun getAlarmsStream(): Flow<List<AlarmData>> = _alarmsStream.asStateFlow()

	override suspend fun getAllAlarms(): List<AlarmData> = store.values.toList()

	override suspend fun getAlarmById(id: Int): AlarmData? = store[id]

	override suspend fun saveAlarm(alarm: AlarmData): Long {
		val id = if (alarm.id == 0) nextId++ else alarm.id
		val newAlarm = alarm.copy(id = id)
		store[id] = newAlarm
		updateStream()
		return id.toLong()
	}

	override suspend fun upsertAlarm(alarm: AlarmData): Long {
		return if (store.containsKey(alarm.id)) {
			store[alarm.id] = alarm
			updateStream()
			-1L
		} else {
			saveAlarm(alarm)
		}
	}

	override suspend fun updateAlarm(alarm: AlarmData): Int {
		return if (store.containsKey(alarm.id)) {
			store[alarm.id] = alarm
			updateStream()
			1
		} else {
			0
		}
	}

	override suspend fun deleteAlarm(alarm: AlarmData): Int {
		val removed = store.remove(alarm.id) != null
		if (removed) updateStream()
		return if (removed) 1 else 0
	}
}
