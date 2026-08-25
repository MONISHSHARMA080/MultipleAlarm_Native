package com.coolApps.MultipleAlarmClock.notification.offline

import androidx.annotation.StringRes
import com.coolApps.MultipleAlarmClock.R
import java.util.Calendar

/**
 * Data class representing a localized offline notification message.
 *
 * @property id Unique identifier within the slot.
 * @property titleRes String resource ID for the notification title.
 * @property messageRes String resource ID for the notification content.
 */
data class OfflineNotificationContent(
	val id: Int,
	@StringRes val titleRes: Int,
	@StringRes val messageRes: Int,
)

/**
 * Sealed class defining the offline push notification time-of-day slots (Morning, Evening, Night).
 * Each slot defines default trigger hours/minutes and a list of localized notification contents.
 */
sealed class OfflineNotificationTimeSlot(
	val slotName: String,
	val defaultHour: Int,
	val defaultMinute: Int,
	val notifications: List<OfflineNotificationContent>,
) {
	data object Morning : OfflineNotificationTimeSlot(
		slotName = "morning",
		defaultHour = DEFAULT_MORNING_HOUR,
		defaultMinute = DEFAULT_MORNING_MINUTE,
		notifications = listOf(
			OfflineNotificationContent(1, R.string.offline_notification_morning_title_1, R.string.offline_notification_morning_msg_1),
			OfflineNotificationContent(2, R.string.offline_notification_morning_title_2, R.string.offline_notification_morning_msg_2),
			OfflineNotificationContent(3, R.string.offline_notification_morning_title_3, R.string.offline_notification_morning_msg_3),
			OfflineNotificationContent(4, R.string.offline_notification_morning_title_4, R.string.offline_notification_morning_msg_4),
		)
	)

	data object Evening : OfflineNotificationTimeSlot(
		slotName = "evening",
		defaultHour = DEFAULT_EVENING_HOUR,
		defaultMinute = DEFAULT_EVENING_MINUTE,
		notifications = listOf(
			OfflineNotificationContent(1, R.string.offline_notification_evening_title_1, R.string.offline_notification_evening_msg_1),
			OfflineNotificationContent(2, R.string.offline_notification_evening_title_2, R.string.offline_notification_evening_msg_2),
			OfflineNotificationContent(5, R.string.offline_notification_evening_title_5, R.string.offline_notification_evening_msg_5),
		)
	)

	data object Night : OfflineNotificationTimeSlot(
		slotName = "night",
		defaultHour = DEFAULT_NIGHT_HOUR,
		defaultMinute = DEFAULT_NIGHT_MINUTE,
		notifications = listOf(
			OfflineNotificationContent(1, R.string.offline_notification_night_title_1, R.string.offline_notification_night_msg_1),
			OfflineNotificationContent(2, R.string.offline_notification_night_title_2, R.string.offline_notification_night_msg_2),
		)
	)

	override fun toString(): String {
		return when(this){
			Evening -> "Evening"
			Morning -> "Morning"
			Night -> "Night"
		}
	}

	/**
	 * Returns a random notification content from this slot.
	 */
	fun getRandomNotification(): OfflineNotificationContent {
		return notifications.random()
	}

	/**
	 * Returns the notification with the given id, or a random one if not found.
	 */
	fun getNotificationById(id: Int): OfflineNotificationContent {
		return notifications.firstOrNull { it.id == id } ?: notifications.random()
	}

	/**
	 * Calculates the next epoch timestamp (in millis) for this slot.
	 * If the slot time for today has already passed, it calculates for tomorrow.
	 */
	fun calculateNextTriggerTime(fromMillis: Long = System.currentTimeMillis()): Long {
		val calendar = Calendar.getInstance().apply {
			timeInMillis = fromMillis
			set(Calendar.HOUR_OF_DAY, defaultHour)
			set(Calendar.MINUTE, defaultMinute)
			set(Calendar.SECOND, 0)
			set(Calendar.MILLISECOND, 0)
		}
		if (calendar.timeInMillis <= fromMillis) {
			calendar.add(Calendar.DAY_OF_YEAR, 1)
		}
		return calendar.timeInMillis
	}

	companion object {
		// Predefined constants for sendAt times
		const val DEFAULT_MORNING_HOUR = 8 // 9:00 AM
		const val DEFAULT_MORNING_MINUTE = 0

		const val DEFAULT_EVENING_HOUR = 21 // 6:00 PM
		const val DEFAULT_EVENING_MINUTE = 30

		const val DEFAULT_NIGHT_HOUR = 21 // 9:00 PM
		const val DEFAULT_NIGHT_MINUTE = 0

		val allSlots: List<OfflineNotificationTimeSlot> = listOf(Morning, Evening, Night)

		/**
		 * Resolves a slot from its name ("morning", "evening", "night").
		 * Falls back to the current time's appropriate slot if not recognized.
		 */
		fun fromSlotName(name: String?): OfflineNotificationTimeSlot {
			return when (name?.lowercase()) {
				"morning" -> Morning
				"evening" -> Evening
				"night" -> Night
				else -> getAppropriateSlotForCurrentTime()
			}
		}

		/**
		 * Determines the most logical slot based on current device hour:
		 * - 05:00 - 11:59 -> Morning
		 * - 12:00 - 19:59 -> Evening
		 * - 20:00 - 04:59 -> Night
		 */
		fun getAppropriateSlotForCurrentTime(nowMillis: Long = System.currentTimeMillis()): OfflineNotificationTimeSlot {
			val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
			val currentHour = calendar.get(Calendar.HOUR_OF_DAY) + 4 // idk set it 4 hours from now
			return when (currentHour) {
				in 5..11 -> Morning
				in 12..19 -> Evening
				else -> Night
			}
		}

		/**
		 * Gets the chronologically next upcoming slot and its trigger timestamp in millis.
		 */
		fun getNextUpcomingSlot(nowMillis: Long = System.currentTimeMillis()): Pair<OfflineNotificationTimeSlot, Long> {
			return allSlots
				.map { slot -> slot to slot.calculateNextTriggerTime(nowMillis) }
				.minByOrNull { it.second } ?: (Morning to Morning.calculateNextTriggerTime(nowMillis))
		}
	}
}
