package com.coolApps.MultipleAlarmClock

import android.app.AlarmManager
import android.content.Context
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.coolApps.MultipleAlarmClock.AlarmLogic.AlarmsController
import com.coolApps.MultipleAlarmClock.Hilt.DispatcherModule
import com.coolApps.MultipleAlarmClock.Hilt.IoDispatcher
import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.alarmFeature.domain.AlarmRepository
import com.coolApps.MultipleAlarmClock.utils.Result.Result
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
import java.time.Duration
import java.util.Calendar


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
	fun `alarm series fires every minute and schedules the next alarm until end time`() = runTest {
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
			logD("alarm")

			val nextTrigger = expectedTrigger + frequency
			expectedTrigger = nextTrigger
		}

		assertThat(firedCount).isEqualTo(expectedAlarmList.size -1)
	}


	// TODO: make the validation while loop abstract such that I can test if current time a) before(this one) start time, b) in b/w the time interval, c) after the time interval (here I want to
	//  schedule it for next day)
	//  also do the same testing for the reset alarm one

	private fun logD(str: String){
		Log.d("AAAA", "[Test] $str")
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
