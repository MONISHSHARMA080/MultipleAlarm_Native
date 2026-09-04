package com.coolApps.MultipleAlarmClock

import android.content.Context
import com.coolApps.MultipleAlarmClock.AlarmLogic.AlarmControllerErrorSet
import com.coolApps.MultipleAlarmClock.AlarmLogic.AlarmsController
import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.AlarmDataValidationResult
import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.RepeatDays
import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.RepeatDaysConverter
import com.coolApps.MultipleAlarmClock.alarmFeature.domain.AlarmRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepeatDaysTest {

    @Test
    fun `empty bits throws IllegalArgumentException on init`() {
        assertThrows(IllegalArgumentException::class.java) {
            RepeatDays(0)
        }
    }

    @Test
    fun `RepeatDays of with no args throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            RepeatDays.of()
        }
    }

    @Test
    fun `RepeatDays of and isSet works for specified days`() {
        val mwf = RepeatDays.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        assertThat(mwf.isSet(DayOfWeek.MONDAY)).isTrue()
        assertThat(mwf.isSet(DayOfWeek.TUESDAY)).isFalse()
        assertThat(mwf.isSet(DayOfWeek.WEDNESDAY)).isTrue()
        assertThat(mwf.isSet(DayOfWeek.THURSDAY)).isFalse()
        assertThat(mwf.isSet(DayOfWeek.FRIDAY)).isTrue()
        assertThat(mwf.isSet(DayOfWeek.SATURDAY)).isFalse()
        assertThat(mwf.isSet(DayOfWeek.SUNDAY)).isFalse()

        assertThat(mwf.toSet()).containsExactly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
    }

    @Test
    fun `RepeatDays with and without work correctly`() {
        val mondayOnly = RepeatDays.of(DayOfWeek.MONDAY)
        val monWed = mondayOnly.with(DayOfWeek.WEDNESDAY)
        assertThat(monWed.isSet(DayOfWeek.MONDAY)).isTrue()
        assertThat(monWed.isSet(DayOfWeek.WEDNESDAY)).isTrue()

        val wedOnly = monWed.without(DayOfWeek.MONDAY)
        assertThat(wedOnly.isSet(DayOfWeek.MONDAY)).isFalse()
        assertThat(wedOnly.isSet(DayOfWeek.WEDNESDAY)).isTrue()
    }

    @Test
    fun `RepeatDays EVERY_DAY contains all 7 days`() {
        assertThat(RepeatDays.EVERY_DAY.toSet()).containsExactlyElementsIn(DayOfWeek.entries)
    }

    @Test
    fun `nextRepeatDate returns today if today is set`() {
        val monday = LocalDate.of(2026, 9, 7)
        val repeatDays = RepeatDays.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)

        val next = repeatDays.nextRepeatDate(monday)
        assertThat(next).isEqualTo(monday)
    }

    @Test
    fun `nextRepeatDate finds next day in same week`() {
        val tuesday = LocalDate.of(2026, 9, 8)
        val repeatDays = RepeatDays.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)

        val next = repeatDays.nextRepeatDate(tuesday)
        assertThat(next).isEqualTo(LocalDate.of(2026, 9, 9))
    }

    @Test
    fun `nextRepeatDate wraps to next week`() {
        val friday = LocalDate.of(2026, 9, 11)
        val repeatDays = RepeatDays.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)

        val next = repeatDays.nextRepeatDate(friday)
        assertThat(next).isEqualTo(LocalDate.of(2026, 9, 14))
    }

    @Test
    fun `RepeatDaysConverter converts between Int and RepeatDays safely`() {
        val converter = RepeatDaysConverter()
        assertThat(converter.fromInt(null)).isNull()
        assertThat(converter.toInt(null)).isNull()

        val repeatDays = RepeatDays.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)
        val bits = converter.toInt(repeatDays)
        assertThat(bits).isNotNull()
        assertThat(converter.fromInt(bits)).isEqualTo(repeatDays)
    }

    @Test
    fun `AlarmData validate rejects weekday mismatch when repeatDays is set`() {
        val mondayCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 7, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val mondayEndCal = (mondayCal.clone() as Calendar).apply {
            add(Calendar.MINUTE, 30)
        }

        val tuesdayOnlyRepeat = RepeatDays.of(DayOfWeek.TUESDAY)

        val alarmData = AlarmData(
            startTime = mondayCal.timeInMillis,
            endTime = mondayEndCal.timeInMillis,
            message = "Test",
            frequencyInMin = 5,
            sound = null,
            isReadyToUse = true,
            repeatDays = tuesdayOnlyRepeat
        )

        val result = alarmData.validate()
        assertThat(result).isInstanceOf(AlarmDataValidationResult.WeekdayMismatch::class.java)
    }

    @Test
    fun `startAlarmSeriesHandler fails validation when repeatDays mismatches start date without upserting`() = runBlocking {
        val repository = mockk<AlarmRepository>(relaxed = true)
        val controller = AlarmsController(
            alarmRepository = repository,
            timeProvider = mockk(relaxed = true),
            alarmManager = mockk(relaxed = true),
            analytics = mockk(relaxed = true),
            errorHandler = mockk(relaxed = true),
            context = mockk(relaxed = true)
        )

        val mondayCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 7, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val mondayEndCal = (mondayCal.clone() as Calendar).apply {
            add(Calendar.MINUTE, 30)
        }

        val invalidAlarm = AlarmData(
            startTime = mondayCal.timeInMillis,
            endTime = mondayEndCal.timeInMillis,
            message = "Mismatch weekday alarm",
            frequencyInMin = 5,
            sound = null,
            isReadyToUse = true,
            repeatDays = RepeatDays.of(DayOfWeek.WEDNESDAY) // Start date is Monday, repeat is Wednesday
        )

        val result = controller.startAlarmSeriesHandler(invalidAlarm, mockk(relaxed = true), mockk<Context>(relaxed = true))
        assertThat(result.isErr()).isTrue()
        val error = result.fold(onSuccess = { null }, onError = { it })
        assertThat(error).isInstanceOf(AlarmControllerErrorSet.ValidationFailed::class.java)

        // Verify repository was NOT called to upsert invalid alarm
        coVerify(exactly = 0) { repository.upsertAlarm(any()) }
    }

    @Test
    fun `calculateNextAlarmInfo rolls to next calendar day when repeatDays is null`() {
        val controller = AlarmsController(
            alarmRepository = mockk(relaxed = true),
            timeProvider = mockk(relaxed = true),
            alarmManager = mockk(relaxed = true),
            analytics = mockk(relaxed = true),
            errorHandler = mockk(relaxed = true),
            context = mockk(relaxed = true)
        )

        val yesterdayCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val yesterdayEndCal = (yesterdayCal.clone() as Calendar).apply {
            add(Calendar.MINUTE, 30)
        }

        val alarmData = AlarmData(
            startTime = yesterdayCal.timeInMillis,
            endTime = yesterdayEndCal.timeInMillis,
            message = "Test",
            frequencyInMin = 5,
            sound = null,
            isReadyToUse = true,
            repeatDays = null
        )

        val nextAlarmInfo = controller.calculateNextAlarmInfo(alarmData).fold(
            onSuccess = { it },
            onError = { throw AssertionError("Failed to calculate next alarm info: $it") }
        )

        val expectedTomorrow = LocalDate.now().plusDays(1)
        val resultCal = Calendar.getInstance().apply { timeInMillis = nextAlarmInfo.newSeriesStartTime }
        val resultLocalDate = LocalDate.ofInstant(resultCal.toInstant(), ZoneId.systemDefault())

        assertThat(resultLocalDate).isEqualTo(expectedTomorrow)
        assertThat(resultCal.get(Calendar.HOUR_OF_DAY)).isEqualTo(8)
        assertThat(resultCal.get(Calendar.MINUTE)).isEqualTo(0)
    }

    @Test
    fun `calculateNextAlarmInfo with MWF repeat days on Tuesday rolls to Wednesday`() {
        val controller = AlarmsController(
            alarmRepository = mockk(relaxed = true),
            timeProvider = mockk(relaxed = true),
            alarmManager = mockk(relaxed = true),
            analytics = mockk(relaxed = true),
            errorHandler = mockk(relaxed = true),
            context = mockk(relaxed = true)
        )

        val repeatDays = RepeatDays.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)

        val pastCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -2)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val pastEndCal = (pastCal.clone() as Calendar).apply {
            add(Calendar.MINUTE, 20)
        }

        val alarmData = AlarmData(
            startTime = pastCal.timeInMillis,
            endTime = pastEndCal.timeInMillis,
            message = "Test MWF",
            frequencyInMin = 5,
            sound = null,
            isReadyToUse = true,
            repeatDays = repeatDays
        )

        val nextAlarmInfo = controller.calculateNextAlarmInfo(alarmData).fold(
            onSuccess = { it },
            onError = { throw AssertionError("Failed: $it") }
        )

        val searchFromDate = LocalDate.now().plusDays(1)
        val expectedDate = repeatDays.nextRepeatDate(searchFromDate)

        val resultCal = Calendar.getInstance().apply { timeInMillis = nextAlarmInfo.newSeriesStartTime }
        val resultLocalDate = LocalDate.ofInstant(resultCal.toInstant(), ZoneId.systemDefault())

        assertThat(resultLocalDate).isEqualTo(expectedDate)
        assertThat(repeatDays.isSet(resultLocalDate.dayOfWeek)).isTrue()
        assertThat(resultCal.get(Calendar.HOUR_OF_DAY)).isEqualTo(9)
        assertThat(resultCal.get(Calendar.MINUTE)).isEqualTo(0)
    }

    @Test
    fun `calculateNextAlarmInfo with MWF repeat on Friday after series end wraps to Monday`() {
        val repeatDays = RepeatDays.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)

        val saturday = LocalDate.of(2026, 9, 12)
        val nextDate = repeatDays.nextRepeatDate(saturday)

        assertThat(nextDate).isEqualTo(LocalDate.of(2026, 9, 14))
        assertThat(nextDate!!.dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
    }
}
