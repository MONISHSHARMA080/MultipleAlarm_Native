package com.coolApps.MultipleAlarmClock.alarmFeature.data.local

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate

@Serializable
@JvmInline
value class RepeatDays(val bits: Int) {
    init {
        require(bits != 0) {
            "RepeatDays must not be empty when non-null — use null on AlarmData for 'unrestricted', not RepeatDays(0)"
        }
    }

	override fun toString(): String {
		return "RepeatDays:bit:$bits"
	}
    fun isSet(day: DayOfWeek): Boolean = (bits and bitFor(day)) != 0
    fun with(day: DayOfWeek): RepeatDays = RepeatDays(bits or bitFor(day))
    fun without(day: DayOfWeek): RepeatDays = RepeatDays(bits and bitFor(day).inv())
    fun toSet(): Set<DayOfWeek> = DayOfWeek.entries.filter { isSet(it) }.toSet()

    /** Next date (today included) whose weekday is set, searching forward from [from]. */
    fun nextRepeatDate(from: LocalDate): LocalDate? =
        (0..7).asSequence()
            .map { from.plusDays(it.toLong()) }
            .firstOrNull { isSet(it.dayOfWeek) }

    companion object {
		private fun bitFor(day: DayOfWeek) = 1 shl (day.value - 1)

		fun of(days: Set<DayOfWeek>): RepeatDays? =
			if (days.isEmpty()) null
			else RepeatDays(days.fold(0) { acc, d -> acc or bitFor(d) })
    }
}
