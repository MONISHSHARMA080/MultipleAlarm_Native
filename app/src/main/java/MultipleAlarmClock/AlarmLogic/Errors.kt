package com.coolApps.MultipleAlarmClock.AlarmLogic

import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.utils.Result.Error
import com.coolApps.MultipleAlarmClock.utils.UiText


val defaultErrorToDisplayUser = UiText.StringResource(R.string.error_generic)
val defaultErrorTitle = UiText.StringResource(R.string.error_title_generic)
val alarmErrorTitle = UiText.StringResource(R.string.error_title_alarm)
val dbErrorTitle = UiText.StringResource(R.string.error_title_database)
val internalErrorTitle = UiText.StringResource(R.string.error_title_internal)

sealed class scheduleAlarmError: Error{
	/**error made by me as a programmer */
	data class ProgrammerError(
		override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_internal),
		override val titleToDisplayUser: UiText = internalErrorTitle
	): scheduleAlarmError()
	data class PendingIntentNotFound(
		override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_alarm_schedule_failed),
		override val titleToDisplayUser: UiText = alarmErrorTitle
	): scheduleAlarmError()
	data class GenericError(
		override val messageToDisplayUser: UiText = defaultErrorToDisplayUser,
		override val titleToDisplayUser: UiText = defaultErrorTitle
	): scheduleAlarmError()
}

sealed class GetPendingIntentForAlarmError: Error{
	data class PendingIntentAlreadyExist(
		override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_alarm_already_exists),
		override val titleToDisplayUser: UiText = alarmErrorTitle
	): GetPendingIntentForAlarmError()
	data class GenericError(
		override val messageToDisplayUser: UiText = defaultErrorToDisplayUser,
		override val titleToDisplayUser: UiText = defaultErrorTitle
	): GetPendingIntentForAlarmError()
}

sealed class StartAlarmSeriesError: Error{
	data class ErrorSchedulingAlarm(
		override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_alarm_schedule_failed),
		override val titleToDisplayUser: UiText = alarmErrorTitle
	): StartAlarmSeriesError()
	data class GenericError(
		override val messageToDisplayUser: UiText = defaultErrorToDisplayUser,
		override val titleToDisplayUser: UiText = defaultErrorTitle
	): StartAlarmSeriesError()
	data class ProgrammerError(
		override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_internal),
		override val titleToDisplayUser: UiText = internalErrorTitle
	): StartAlarmSeriesError()
}

sealed class StartAlarmSeriesHandlerError: Error{
	data class ErrorSchedulingAlarm(
		override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_alarm_schedule_failed),
		override val titleToDisplayUser: UiText = alarmErrorTitle
	): StartAlarmSeriesHandlerError()
	data class FailureToInsertAlarmInDb(
		override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_database_failure),
		override val titleToDisplayUser: UiText = dbErrorTitle
	): StartAlarmSeriesHandlerError()
	data class GenericError(
		override val messageToDisplayUser: UiText = defaultErrorToDisplayUser,
		override val titleToDisplayUser: UiText = defaultErrorTitle
	): StartAlarmSeriesHandlerError()
}

sealed class RescheduleAlarmError: Error{
	data class GenericError(
		override val messageToDisplayUser: UiText = defaultErrorToDisplayUser,
		override val titleToDisplayUser: UiText = defaultErrorTitle
	): RescheduleAlarmError()
	data class ProgrammerError(
		override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_internal),
		override val titleToDisplayUser: UiText = internalErrorTitle
	): RescheduleAlarmError()
	data class AlarmScheduleError(
		override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_alarm_schedule_failed),
		override val titleToDisplayUser: UiText = alarmErrorTitle
	): RescheduleAlarmError()
}

sealed class CancelAlarmHandlerError : Error{
	data class GenericError(
		override val messageToDisplayUser: UiText = defaultErrorToDisplayUser,
		override val titleToDisplayUser: UiText = defaultErrorTitle
	): CancelAlarmHandlerError()
	data class CancellingAlarmError(
		override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_alarm_schedule_failed),
		override val titleToDisplayUser: UiText = alarmErrorTitle
	): CancelAlarmHandlerError()
	data class ErrorDeletingAlarmFromDb(
		override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_database_failure),
		override val titleToDisplayUser: UiText = dbErrorTitle
	): CancelAlarmHandlerError()
}

sealed class DeleteAlarmHandlerError : Error{
	data class GenericError(
		override val messageToDisplayUser: UiText = defaultErrorToDisplayUser,
		override val titleToDisplayUser: UiText = defaultErrorTitle
	): DeleteAlarmHandlerError()
	data class AlarmNotInDbToDelete(
		override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_alarm_not_found),
		override val titleToDisplayUser: UiText = alarmErrorTitle
	): DeleteAlarmHandlerError()
}

sealed class CancelAlarmError : Error{
	data class GenericError(
		override val messageToDisplayUser: UiText = defaultErrorToDisplayUser,
		override val titleToDisplayUser: UiText = defaultErrorTitle
	): CancelAlarmError()
	data class AlarmNotInDbToDelete(
		override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_alarm_not_found),
		override val titleToDisplayUser: UiText = alarmErrorTitle
	): CancelAlarmError()
	data class ProgrammerError(
		override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_internal),
		override val titleToDisplayUser: UiText = internalErrorTitle
	): CancelAlarmError()
}

sealed class UpdateAlarmInDbError : Error{
	data class GenericError(
		override val messageToDisplayUser: UiText = defaultErrorToDisplayUser,
		override val titleToDisplayUser: UiText = defaultErrorTitle
	): UpdateAlarmInDbError()
	data class NoAlarmUpdated(
		override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_alarm_not_found),
		override val titleToDisplayUser: UiText = alarmErrorTitle
	): UpdateAlarmInDbError()
}

sealed class DeleteAlarmInDbError : Error{
	data class GenericError(
		override val messageToDisplayUser: UiText = defaultErrorToDisplayUser,
		override val titleToDisplayUser: UiText = defaultErrorTitle
	): DeleteAlarmInDbError()
	data class NoAlarmDeleted(
		override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_alarm_not_found),
		override val titleToDisplayUser: UiText = alarmErrorTitle
	): DeleteAlarmInDbError()
}

sealed class ResetAlarmError : Error{
	data class GenericError(
		override val messageToDisplayUser: UiText = defaultErrorToDisplayUser,
		override val titleToDisplayUser: UiText = defaultErrorTitle
	): ResetAlarmError()
	data class CalculateNextAlarmError(
		override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_invalid_settings),
		override val titleToDisplayUser: UiText = alarmErrorTitle
	): ResetAlarmError()
	data class ProgrammerError(
		override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_internal),
		override val titleToDisplayUser: UiText = internalErrorTitle
	): ResetAlarmError()
	data class SchedulingAlarmError(
		override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_alarm_schedule_failed),
		override val titleToDisplayUser: UiText = alarmErrorTitle
	): ResetAlarmError()
}

sealed class CalculateNextAlarmInfo : Error{
	data class GenericError(
		override val messageToDisplayUser: UiText = defaultErrorToDisplayUser,
		override val titleToDisplayUser: UiText = defaultErrorTitle
	): CalculateNextAlarmInfo()
	data class ProgrammerError(
		override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_internal),
		override val titleToDisplayUser: UiText = internalErrorTitle
	): CalculateNextAlarmInfo()
	data class IllegalStateError(
		override val messageToDisplayUser: UiText = UiText.StringResource(R.string.error_internal),
		override val titleToDisplayUser: UiText = internalErrorTitle
	): CalculateNextAlarmInfo()
}
