package com.coolApps.MultipleAlarmClock.AlarmLogic

import android.content.Context
import androidx.compose.runtime.Composable
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.utils.UiText


sealed interface AlarmControllerError {
	val messageToDisplayUser: UiText
	val titleToDisplayUser: UiText
	val internalErrorMessage: String get() = ""

	fun getTitleForUser(context: Context): String = titleToDisplayUser.asString(context)
	@Composable
	fun getTitleForUser(): String = titleToDisplayUser.asString()

	fun getErrorMessageForUser(context: Context): String = messageToDisplayUser.asString(context)
	@Composable
	fun getErrorMessageForUser(): String = messageToDisplayUser.asString()
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

///** Legal failures for [AlarmsController]. */
//sealed interface StartAlarmSeriesError : AlarmControllerError

/** Legal failures for [AlarmsController.startAlarmSeriesHandler]. */
sealed interface StartAlarmSeriesHandlerError : AlarmControllerError

/** Legal failures for a cancelAlarm-style function. */
sealed interface CancelAlarmError : AlarmControllerError

/** Legal failures for [AlarmsController.cancelAlarmHandler]. */
sealed interface CancelAlarmHandlerError : AlarmControllerError

/** Legal failures for [AlarmsController.deleteAlarmHandler]. */
sealed interface DeleteAlarmHandlerError : AlarmControllerError

/** Legal failures for [AlarmsController.updateAlarmStateInDb]. */
sealed interface UpdateAlarmInDbError : AlarmControllerError

///** Legal failures for [AlarmsController.rescheduleAlarm]. */
sealed interface RescheduleAlarmError : AlarmControllerError

/** Legal failures for [AlarmsController.resetAlarmsHandler]. */
sealed interface ResetAlarmError : AlarmControllerError

/** Legal failures for [AlarmsController.calculateNextAlarmInfo]. */
sealed interface CalculateNextAlarmInfo : AlarmControllerError


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
			override val internalErrorMessage: String = ""
	) : ScheduleAlarmError, GetPendingIntentForAlarmError,
		StartAlarmSeriesHandlerError, CancelAlarmError, CancelAlarmHandlerError,
		DeleteAlarmHandlerError, UpdateAlarmInDbError, RescheduleAlarmError,
		ResetAlarmError, CalculateNextAlarmInfo {
		override fun toString() = toDebugString()
	}

	data class DatabaseOperationFailed(
			override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_database_failure),
			override val titleToDisplayUser: UiText = dbErrorTitle,
			override val internalErrorMessage: String = ""
	) : UpdateAlarmInDbError, CancelAlarmHandlerError, DeleteAlarmHandlerError,
		StartAlarmSeriesHandlerError, RescheduleAlarmError, ResetAlarmError {
		override fun toString() = toDebugString()
	}

	data class ValidationFailed(
			override val messageToDisplayUser: UiText = defaultErrorToDisplayUser,
			override val titleToDisplayUser: UiText = alarmErrorTitle,
			override val internalErrorMessage: String = ""
	) : ScheduleAlarmError, ResetAlarmError,
		StartAlarmSeriesHandlerError, CancelAlarmError, CancelAlarmHandlerError,
		DeleteAlarmHandlerError, RescheduleAlarmError, CalculateNextAlarmInfo {
		override fun toString() = toDebugString()
	}

	data class PendingIntentNotFound(
			override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_alarm_schedule_failed),
			override val titleToDisplayUser: UiText = alarmErrorTitle,
			override val internalErrorMessage: String = ""
	) : ScheduleAlarmError,  StartAlarmSeriesHandlerError,
		RescheduleAlarmError, ResetAlarmError {
		override fun toString() = toDebugString()
	}

	data class PendingIntentAlreadyExist(
			override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_alarm_already_exists),
			override val titleToDisplayUser: UiText = alarmErrorTitle,
			override val internalErrorMessage: String = ""
	) : GetPendingIntentForAlarmError, ScheduleAlarmError,
		StartAlarmSeriesHandlerError, RescheduleAlarmError, ResetAlarmError {
		override fun toString() = toDebugString()
	}

	data class CancellingAlarmError(
			override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_alarm_schedule_failed),
			override val titleToDisplayUser: UiText = alarmErrorTitle,
			override val internalErrorMessage: String = ""
	) : CancelAlarmError, CancelAlarmHandlerError, DeleteAlarmHandlerError,
		StartAlarmSeriesHandlerError, RescheduleAlarmError, ResetAlarmError {
		override fun toString() = toDebugString()
	}
}


private val alarmErrorTitle = UiText.StringResource(R.string.error_title_alarm)
private val dbErrorTitle = UiText.StringResource(R.string.error_title_database)
private val defaultErrorTitle = UiText.StringResource(R.string.error_title_generic)
private val defaultErrorToDisplayUser = UiText.StringResource(R.string.error_generic)




/**
 * Shared debug string. Every leaf error delegates `toString()` to this instead
 * of hand-maintaining a `when` over every class name — new error types don't
 * require touching this function.
 */
fun AlarmControllerError.toDebugString(contextForUiText: Context?=null): String {
	val className = when(this){
		is AlarmControllerErrorSet.DatabaseOperationFailed -> "DatabaseOperationFailed"
		is AlarmControllerErrorSet.PendingIntentAlreadyExist -> "PendingIntentAlreadyExist"
		is AlarmControllerErrorSet.PendingIntentNotFound -> "PendingIntentNotFound"
		is AlarmControllerErrorSet.Unknown -> "Unknown"
		is AlarmControllerErrorSet.CancellingAlarmError -> "CancellingAlarmError"
		is AlarmControllerErrorSet.ValidationFailed -> "ValidationFailed"
	}
	// if the context available then give me the res text else
	val notificationTitleForUser = if (contextForUiText == null) this.titleToDisplayUser else  this.titleToDisplayUser.asString(contextForUiText)
	val messageToDisplayUser = if (contextForUiText == null) this.messageToDisplayUser else  this.messageToDisplayUser.asString(contextForUiText)
	val errorMessage = internalErrorMessage
	val res = "Error_Class:$className, errorMessage:$errorMessage, notificationTitleForUser:$notificationTitleForUser, messageToDisplayUser:$messageToDisplayUser "
	return res
}

