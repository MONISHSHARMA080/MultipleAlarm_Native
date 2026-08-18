package com.coolApps.MultipleAlarmClock.utils.Result

import com.coolApps.MultipleAlarmClock.AlarmLogic.AlarmControllerError


sealed class Result<out SuccessType, out ErrorType : AlarmControllerError> {
    data class Success<out T>(val value: T) : Result<T, Nothing>()
    data class Failure<out E : AlarmControllerError>(
			val errorClass: E,
		    val internalException: Throwable
    ) : Result<Nothing, E>() {
        // Secondary constructor that creates exception from error message
        constructor(errorMessageToDisplayUser: E) : this(
            errorMessageToDisplayUser,
            Exception("Error occurred: ${errorMessageToDisplayUser.javaClass.simpleName}")
        )
    }

    fun isOk(): Boolean = this is Success
    fun isErr(): Boolean = this is Failure


    inline fun <R> map(transform: (SuccessType) -> R): Result<R, ErrorType> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> Failure(errorClass, internalException)
    }
    inline fun <R>fold(
        onSuccess: (SuccessType) -> R,
        onError: (ErrorType, Throwable) -> R
    ):R = when(this){
        is Success -> onSuccess(value)
        is Failure -> onError(errorClass, internalException)
    }

    companion object{
        /** a run catching fun, eg if got an exception then will still display an default error.
         * [defaultErrorMessage] - here you give me a generic error and the exception will be included for you  */
        inline  fun <SuccessType, ErrorType : AlarmControllerError > runCatching(defaultErrorMessage:ErrorType, codeBlock: () -> SuccessType  ): Result<SuccessType, ErrorType>{
            return try {
                Result.Success(codeBlock())
            }catch (e: Exception){
                Result.Failure(defaultErrorMessage, e)
            }
        }
        /** a run catching fun, eg if got an exception then will still display an default error.
         * [defaultErrorMessage] - here you give me a generic error and the exception will be included for you  */
        inline  fun <SuccessType, ErrorType : AlarmControllerError > runCatching(defaultErrorMessage:(Throwable)->ErrorType, codeBlock: () -> SuccessType  ): Result<SuccessType, ErrorType>{
			// have it such that the defaultErrorMessage  is a error class/type and we init it here with the error being generic but the message for me would be in the class.ErrorMessage
            return try {
                Success(codeBlock())
            }catch (e: Exception){
                Failure(errorClass = defaultErrorMessage(e), e,  )
            }
        }

    }
}