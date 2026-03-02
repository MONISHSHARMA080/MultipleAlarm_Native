package com.example.trying_native.AlarmLogic

import com.example.trying_native.utils.Result.Error


const val defaultErrorToDisplayUser = "Sorry an error occurred, please try again"

sealed class scheduleAlarmError: Error{
	/**error made by me as a programmer */
	data class ProgrammerError(override  val messageToDisplayUser: String =defaultErrorToDisplayUser): scheduleAlarmError()
	data class PendingIntentNotFound(override  val messageToDisplayUser: String): scheduleAlarmError()
	data class GenericError(override  val messageToDisplayUser: String): scheduleAlarmError()
}

sealed class GetPendingIntentForAlarmError: Error{
	data class PendingIntentAlreadyExist(override  val messageToDisplayUser: String =defaultErrorToDisplayUser): GetPendingIntentForAlarmError()
	data class GenericError(override  val messageToDisplayUser: String): GetPendingIntentForAlarmError()
}

sealed class StartAlarmSeriesError: Error{
	data class ErrorSchedulingAlarm(override  val messageToDisplayUser: String =defaultErrorToDisplayUser): StartAlarmSeriesError()
	data class GenericError(override  val messageToDisplayUser: String): StartAlarmSeriesError()
	data class ProgrammerError(override  val messageToDisplayUser: String =defaultErrorToDisplayUser): StartAlarmSeriesError()
}

sealed class StartAlarmSeriesHandlerError: Error{
	data class ErrorSchedulingAlarm(override  val messageToDisplayUser: String =defaultErrorToDisplayUser): StartAlarmSeriesHandlerError()
	data class GenericError(override  val messageToDisplayUser: String): StartAlarmSeriesHandlerError()
}

sealed class RescheduleAlarmError: Error{
	data class GenericError(override  val messageToDisplayUser: String = defaultErrorToDisplayUser): RescheduleAlarmError()
	data class ProgrammerError(override  val messageToDisplayUser: String = defaultErrorToDisplayUser): RescheduleAlarmError()
	data class AlarmScheduleError(override  val messageToDisplayUser: String): RescheduleAlarmError()
}

sealed class CancelAlarmHandlerError : Error{
	data class GenericError(override  val messageToDisplayUser: String = defaultErrorToDisplayUser): CancelAlarmHandlerError()
	data class CancellingAlarmError(override  val messageToDisplayUser: String): CancelAlarmHandlerError()
	data class ErrorDeletingAlarmFromDb(override  val messageToDisplayUser: String = defaultErrorToDisplayUser): CancelAlarmHandlerError()
}

sealed class DeleteAlarmHandlerError : Error{
	data class GenericError(override  val messageToDisplayUser: String = defaultErrorToDisplayUser): DeleteAlarmHandlerError()
	data class AlarmNotInDbToDelete(override  val messageToDisplayUser: String): DeleteAlarmHandlerError()
}

sealed class CancelAlarmError : Error{
	data class GenericError(override  val messageToDisplayUser: String = defaultErrorToDisplayUser): CancelAlarmError()
	data class AlarmNotInDbToDelete(override  val messageToDisplayUser: String): CancelAlarmError()
	data class ProgrammerError(override  val messageToDisplayUser: String =defaultErrorToDisplayUser): CancelAlarmError()
}

sealed class UpdateAlarmInDbError : Error{
	data class GenericError(override  val messageToDisplayUser: String = defaultErrorToDisplayUser): UpdateAlarmInDbError()
	data class NoAlarmUpdated(override  val messageToDisplayUser: String = defaultErrorToDisplayUser): UpdateAlarmInDbError()
}

sealed class DeleteAlarmInDbError : Error{
	data class GenericError(override  val messageToDisplayUser: String = defaultErrorToDisplayUser): DeleteAlarmInDbError()
	data class NoAlarmDeleted(override  val messageToDisplayUser: String = defaultErrorToDisplayUser): DeleteAlarmInDbError()
}

sealed class ResetAlarmError : Error{
	data class GenericError(override  val messageToDisplayUser: String = defaultErrorToDisplayUser): ResetAlarmError()
	data class CalculateNextAlarmError(override  val messageToDisplayUser: String = defaultErrorToDisplayUser): ResetAlarmError()
	data class ProgrammerError(override  val messageToDisplayUser: String = defaultErrorToDisplayUser): ResetAlarmError()
	data class SchedulingAlarmError(override  val messageToDisplayUser: String = defaultErrorToDisplayUser): ResetAlarmError()
}

sealed class CalculateNextAlarmInfo : Error{
	data class GenericError(override  val messageToDisplayUser: String = defaultErrorToDisplayUser): CalculateNextAlarmInfo()
	data class ProgrammerError(override  val messageToDisplayUser: String = defaultErrorToDisplayUser): CalculateNextAlarmInfo()
	data class IllegalStateError(override  val messageToDisplayUser: String = defaultErrorToDisplayUser): CalculateNextAlarmInfo()
}
