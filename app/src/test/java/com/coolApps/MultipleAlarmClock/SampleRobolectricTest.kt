package com.coolApps.MultipleAlarmClock

import android.app.AlarmManager
import android.content.Context
import android.os.Looper
import android.os.SystemClock
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.coolApps.MultipleAlarmClock.AlarmLogic.AlarmsController
import com.coolApps.MultipleAlarmClock.Data.dataStore.Settings
import com.coolApps.MultipleAlarmClock.Data.dataStore.SettingsSerializer
import com.coolApps.MultipleAlarmClock.ErrorHandling.ErrorHandler
import com.coolApps.MultipleAlarmClock.Hilt.AppModule
import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.AlarmDao
import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.AlarmDatabase
import com.coolApps.MultipleAlarmClock.alarmFeature.domain.AlarmRepository
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.coolApps.MultipleAlarmClock.notification.NotificationHandler
import com.coolApps.MultipleAlarmClock.utils.Result.Result
import com.google.common.truth.Truth.assertThat
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
@UninstallModules(AppModule::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class AlarmSeriesLogicTest2 {

	/**
	 * Re-provides everything AppModule provided except AlarmRepository,
	 * which is replaced by the @BindValue fake below.
	 */
	@Module
	@InstallIn(SingletonComponent::class)
	object TestAppModule {

		@Provides
		@Singleton
		fun provideDatabase(@ApplicationContext context: Context): AlarmDatabase =
			Room.inMemoryDatabaseBuilder(context, AlarmDatabase::class.java)
				.allowMainThreadQueries()
				.build()

		@Provides
		fun provideAlarmDao(db: AlarmDatabase): AlarmDao = db.alarmDao()

		@Provides
		@Singleton
		fun provideProtoDataStore(@ApplicationContext context: Context): DataStore<Settings> =
			DataStoreFactory.create(
				serializer = SettingsSerializer,
				produceFile = { context.dataStoreFile("test_user_settings.pb") },
			)

		@Provides
		fun provideAlarmManager(@ApplicationContext context: Context): AlarmManager =
			context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

		@Provides
		@Singleton
		fun provideAnalytics(@ApplicationContext context: Context): Analytics =
			Analytics(context)

		@Provides
		fun provideNotificationHandler(@ApplicationContext context: Context): NotificationHandler =
			NotificationHandler(context)

		@Provides
		fun provideErrorHandler(
			notificationHandler: NotificationHandler,
			analytics: Analytics,
		): ErrorHandler = ErrorHandler(notificationHandler, analytics)
	}

	@get:Rule
	val hiltRule = HiltAndroidRule(this)

	/** Replaces AppModule.provideAlarmRepository in the test component. */
	@BindValue
	@JvmField
	val fakeAlarmRepository: AlarmRepository = FakeAlarmRepository()

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
			set(Calendar.HOUR_OF_DAY, 20)
			set(Calendar.MINUTE, 0)
			set(Calendar.SECOND, 0)
			set(Calendar.MILLISECOND, 0)
		}.timeInMillis

		val endTime = startTime + Duration.ofHours(3).toMillis()
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
		logD("test AlarmData:$alarm")

		// Start Robolectric clock at the alarm start.
		SystemClock.setCurrentTimeMillis(startTime)

		val result = controller.startAlarmSeriesHandler(
			alarm = alarm,
			alarmManager = alarmManager,
			activityContext = context
		)
		assertThat(result).isInstanceOf(Result.Success::class.java)

		val storedAlarm = fakeAlarmRepository.getAllAlarms().single()

		logD("test retrived alarm:$storedAlarm")
		assertThat(storedAlarm.isReadyToUse).isTrue()
		assertThat(storedAlarm.startTime).isEqualTo(startTime)
		assertThat(storedAlarm.endTime).isEqualTo(endTime)
		assertThat(storedAlarm.frequencyInMin).isEqualTo(1)

		var expectedTrigger = startTime
		var firedCount = 0

		while (expectedTrigger < endTime) {

			val scheduled = shadowAlarmManager.peekNextScheduledAlarm()
			logD("Scheduled:$scheduled, scheduled:${scheduled?.triggerAtMs}, fired:$firedCount")

			assertThat(scheduled).isNotNull()
			assertThat(scheduled!!.triggerAtMs)
				.isEqualTo(expectedTrigger)

			shadowOf(Looper.getMainLooper())
				.idleFor(Duration.ofMillis(
					expectedTrigger - System.currentTimeMillis()
				))

			firedCount++

			val nextTrigger = expectedTrigger + frequency

			if (nextTrigger < endTime) {
				val nextScheduled =
					shadowAlarmManager.peekNextScheduledAlarm()

				assertThat(nextScheduled).isNotNull()
				assertThat(nextScheduled!!.triggerAtMs)
					.isEqualTo(nextTrigger)
			} else {
				assertThat(
					shadowAlarmManager.peekNextScheduledAlarm()
				).isNull()
			}

			expectedTrigger = nextTrigger
		}

		assertThat(firedCount).isEqualTo(180)
	}

}


//@RunWith(AndroidJUnit4::class)
//class AlarmSeriesLogicTest {
//
//	private lateinit var context: Context
//	private lateinit var alarmManager: AlarmManager
//	private lateinit var shadowAlarmManager: ShadowAlarmManager
//	private lateinit var repo: AlarmRepository
//	private lateinit var controller: AlarmsController
//
//	@Before
//	fun setUp() {
//		context = ApplicationProvider.getApplicationContext()
//		alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//		shadowAlarmManager = shadowOf(alarmManager)
//		repo = FakeAlarmRepository()
//		controller = AlarmsController(
//			alarmRepository = repo,
//			alarmManager = alarmManager,
//			analytics = mockk(relaxed = true),   // swap for mockk<Analytics>(relaxed = true) w/ real type
//			errorHandler = mockk(relaxed = true), // same for ErrorHandler
//			context = context,
//		)
//	}
//
//	@Test
//	fun `alarm fires every 2 minutes from 5-00 to 6-00`() = runTest {
//		// Anchor "now" so any Calendar.getInstance()/currentTimeMillis()
//		// calls inside your validate()/cancelAlarm() logic see a controlled
//		// clock instead of the real wall clock.
//		val today5am = Calendar.getInstance().apply {
//			set(Calendar.HOUR_OF_DAY, 5); set(Calendar.MINUTE, 0)
//			set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
//		}
//		SystemClock.setCurrentTimeMillis(today5am.timeInMillis - 60_000L)
//
//		val startTime = today5am.timeInMillis
//		val endTime = startTime + TimeUnit.HOURS.toMillis(1)
//
//		val alarm = AlarmData(
//			id = 0,
//			startTime = startTime,
//			endTime = endTime,
//			message = "test alarm",
//			isReadyToUse = false,
//			frequencyInMin = 2,
//			repeatDays = null,
//			sound = null,
//		)
//
//		val startResult = controller.startAlarmSeriesHandler(alarm, alarmManager, context)
//		check(startResult is Result.Success) { "setup failed: $startResult" }
//
//		val insertedId = repo.all().single().id
//		val firedTimes = mutableListOf<Long>()
//
//		var guard = 0
//		while (true) {
//			val due = shadowAlarmManager.peekNextScheduledAlarm() ?: break
//			if (due.triggerAtTime >= endTime) break // window's done; this is the rollover/next occurrence
//
//			shadowAlarmManager.getNextScheduledAlarm() // pops the same alarm we just peeked
//			firedTimes += due.triggerAtTime
//
//			controller.scheduleNextAlarmInSeries(
//				AlarmActivityIntentData(
//					startTimeForDb = startTime,
//					alarmTriggerTime = due.triggerAtTime,
//					endTime = endTime,
//					message = alarm.message,
//					alarmIdInDb = insertedId,
//				)
//			)
//
//			if (++guard > 200) error("more alarms than expected - check the rollover/off-by-one logic")
//		}
//
//		assertThat(firedTimes.first()).isEqualTo(startTime)
//		// 1 hour / 2 min = 30, if your boundary check is strictly "< endTime"
//		// and the first trigger is exactly startTime. Adjust if your
//		// fencepost differs.
//		assertThat(firedTimes).hasSize(30)
//		firedTimes.zipWithNext().forEach { (a, b) ->
//			assertThat(b - a).isEqualTo(TimeUnit.MINUTES.toMillis(2))
//		}
//
//		// Whatever's left scheduled now should be the next day's rollover -
//		// useful as a second assertion once you know rollOverIfTimeIntervalPassed()'s exact contract:
//		// val rollover = shadowAlarmManager.peekNextScheduledAlarm()
//		// assertThat(rollover?.triggerAtTime).isGreaterThan(endTime)
//	}
//}

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
