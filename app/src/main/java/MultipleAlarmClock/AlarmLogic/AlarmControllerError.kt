package com.coolApps.MultipleAlarmClock.AlarmLogic

import android.content.Context
import androidx.compose.runtime.Composable
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.utils.UiText


sealed interface AlarmControllerError {
	val messageToDisplayUser: UiText
	val titleToDisplayUser: UiText
	val errorMessage: String get() = ""

	fun getTitleForUser(context: Context): String = titleToDisplayUser.asString(context)
	@Composable
	fun getTitleForUser(): String = titleToDisplayUser.asString()

	fun getErrorMessageForUser(context: Context): String = messageToDisplayUser.asString(context)
	@Composable
	fun getErrorMessageForUser(): String = messageToDisplayUser.asString()
}




/**
 * Shared debug string. Every leaf error delegates `toString()` to this instead
 * of hand-maintaining a `when` over every class name — new error types don't
 * require touching this function.
 */
fun AlarmControllerError.toDebugString(): String {
		val className = when(this){
			is AlarmControllerErrorSet.SchedulingAlarmFailed -> "SchedulingAlarmFailed"
			is AlarmControllerErrorSet.DatabaseFailed -> "DatabaseFailed"
			is AlarmControllerErrorSet.NotFound -> "NotFound"
			is AlarmControllerErrorSet.PendingIntentAlreadyExist -> "PendingIntentAlreadyExist"
			is AlarmControllerErrorSet.PendingIntentNotFound -> "PendingIntentNotFound"
			is AlarmControllerErrorSet.Unknown -> "Unknown"
			is AlarmControllerErrorSet.CancellingAlarmError -> "CancellingAlarmError"
			is AlarmControllerErrorSet.InvalidSettings -> "InvalidSettings"
			is AlarmControllerErrorSet.ValidationFailed -> "ValidationFailed"
		}
		val errorMessage = errorMessage
		val notificationTitleForUser = this.titleToDisplayUser
		val messageToDisplayUser = this.messageToDisplayUser
		val res = "Error_Class:$className, errorMessage:$errorMessage, notificationTitleForUser:$notificationTitleForUser, messageToDisplayUser:$messageToDisplayUser "
		return res

}


// =============================================================================
// 2. PER-FUNCTION ERROR SETS
// Pure markers — no new members. They exist only to tell the compiler "these
// are the legal outcomes of this function" so `when` is exhaustive per-function
// instead of over every error your app has ever defined.
// =============================================================================

/** Legal failures for [AlarmsController.scheduleAlarm]. */
sealed interface ScheduleAlarmError : AlarmControllerError

/** Legal failures for [AlarmsController.getPendingIntentForAlarm]. */
sealed interface GetPendingIntentForAlarmError : AlarmControllerError

/** Legal failures for [AlarmsController.startAlarmSeries]. */
sealed interface StartAlarmSeriesError : AlarmControllerError

/** Legal failures for a cancelAlarm-style function — included as a 3rd example. */
sealed interface CancelAlarmError : AlarmControllerError


// =============================================================================
// 3. CONCRETE ERRORS
// Each is defined exactly once. It opts into whichever set(s) it's actually a
// legal outcome for by listing them after the colon. Shared fields
// (messageToDisplayUser / titleToDisplayUser) are declared once per class,
// never duplicated per function.
//
// Nested in an object purely for namespacing (AlarmErrors.Unknown(), etc.) —
// nesting doesn't affect sealed-interface exhaustiveness, it's still resolved
// at the package/module level.
// =============================================================================

object AlarmControllerErrorSet {

	data class Unknown(
			override val messageToDisplayUser: UiText = defaultErrorToDisplayUser,
			override val titleToDisplayUser: UiText = defaultErrorTitle,
			override val errorMessage: String = ""
	) : ScheduleAlarmError, GetPendingIntentForAlarmError, StartAlarmSeriesError, CancelAlarmError {
		override fun toString() = toDebugString()
	}

	data class DatabaseFailed(
			override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_database_failure),
			override val titleToDisplayUser: UiText = dbErrorTitle,
			override val errorMessage: String = ""
	) : ScheduleAlarmError { // only scheduleAlarm touches the DB directly
		override fun toString() = toDebugString()
	}

	data class ValidationFailed(
			override val messageToDisplayUser: UiText = defaultErrorToDisplayUser,
			override val titleToDisplayUser: UiText = alarmErrorTitle,
			override val errorMessage: String = ""
	) : ScheduleAlarmError, StartAlarmSeriesError { // both call alarmData.validate()
		override fun toString() = toDebugString()
	}

	data class PendingIntentNotFound(
			override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_alarm_schedule_failed),
			override val titleToDisplayUser: UiText = alarmErrorTitle,
			override val errorMessage: String = ""
	) : ScheduleAlarmError, StartAlarmSeriesError, CancelAlarmError {
		override fun toString() = toDebugString()
	}

	data class PendingIntentAlreadyExist(
			override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_alarm_already_exists),
			override val titleToDisplayUser: UiText = alarmErrorTitle,
			override val errorMessage: String = ""
	) : GetPendingIntentForAlarmError { // only surfaces where the PI is actually created
		override fun toString() = toDebugString()
	}

	data class NotFound(
			override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_alarm_not_found),
			override val titleToDisplayUser: UiText = alarmErrorTitle,
			override val errorMessage: String = ""
	) : CancelAlarmError {
		override fun toString() = toDebugString()
	}

	data class CancellingAlarmError(
			override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_alarm_schedule_failed),
			override val titleToDisplayUser: UiText = alarmErrorTitle,
			override val errorMessage: String = ""
	) : CancelAlarmError {
		override fun toString() = toDebugString()
	}

	data class SchedulingAlarmFailed(
			override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_alarm_schedule_failed),
			override val titleToDisplayUser: UiText = alarmErrorTitle,
			override val errorMessage: String = ""
	) : ScheduleAlarmError {
		override fun toString() = toDebugString()
	}

	data class InvalidSettings(
			override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_invalid_settings),
			override val titleToDisplayUser: UiText = alarmErrorTitle,
			override val errorMessage: String = ""
	) : ScheduleAlarmError, StartAlarmSeriesError {
		override fun toString() = toDebugString()
	}
}


private val alarmErrorTitle = UiText.StringResource(R.string.error_title_alarm)
private val dbErrorTitle = UiText.StringResource(R.string.error_title_database)
private val defaultErrorTitle = UiText.StringResource(R.string.error_title_generic)
private val defaultErrorToDisplayUser = UiText.StringResource(R.string.error_generic)
private val internalErrorTitle = UiText.StringResource(R.string.error_title_internal)




