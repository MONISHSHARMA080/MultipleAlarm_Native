package com.coolApps.MultipleAlarmClock.utils.Result

import com.coolApps.MultipleAlarmClock.AlarmLogic.AlarmControllerError
import com.coolApps.MultipleAlarmClock.AlarmLogic.toDebugString

sealed class Result<out SuccessType, out ErrorType : AlarmControllerError> {

	data class Success<out T>(val value: T) : Result<T, Nothing>()
	data class Failure<out E : AlarmControllerError>(val errorClass: E) : Result<Nothing, E>()

	fun isOk(): Boolean = this is Success
	fun isErr(): Boolean = this is Failure

	override fun toString(): String {
		return  when(this){
			is Failure -> "Result_Failure: errorClass:${errorClass.toDebugString()}"
			is Success -> "Result_Success: value:${value.toString()}"
		}
	}

	inline fun <R> map(transform: (SuccessType) -> R): Result<R, ErrorType> = when (this) {
		is Success -> Success(transform(value))
		is Failure -> Failure(errorClass)
	}

	inline fun <R> fold(
			onSuccess: (SuccessType) -> R,
			onError: (ErrorType) -> R          // <-- one param, not two
	): R = when (this) {
		is Success -> onSuccess(value)
		is Failure -> onError(errorClass)
	}

	companion object {
		/** [makeError] builds the ErrorType from the caught Throwable, so the
		 *  cause ends up living *inside* the error object, where it belongs. */
		inline fun <SuccessType, ErrorType : AlarmControllerError> runCatching(
				makeError: (Throwable) -> ErrorType,
				codeBlock: () -> SuccessType
		): Result<SuccessType, ErrorType> {
			return try {
				Success(codeBlock())
			} catch (e: Exception) {
				Failure(makeError(e))
			}
		}

		/** convenience overload when you just want a fixed error instance with no per-throw info */
		inline fun <SuccessType, ErrorType : AlarmControllerError> runCatching(
				defaultError: ErrorType,
				codeBlock: () -> SuccessType
		): Result<SuccessType, ErrorType> {
			return try {
				Success(codeBlock())
			} catch (e: Exception) {
				Failure(defaultError)
			}
		}
	}
}
