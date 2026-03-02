package com.example.trying_native.utils.Result

interface Error {
    val messageToDisplayUser: String
}

sealed class Result<out SuccessType, out ErrorType : Error> {
    data class Success<out T>(val value: T) : Result<T, Nothing>()
//    data class Failure<out E : Error>(val errorMessageToDisplayUser: E, val exception: Throwable) : Result<Nothing, E>()
    data class Failure<out E : Error>(
        val errorMessageToDisplayUser: E,
        val internalException: Throwable
    ) : Result<Nothing, E>() {
        // Secondary constructor that creates exception from error message
        constructor(errorMessageToDisplayUser: E) : this(
            errorMessageToDisplayUser,
            Exception(errorMessageToDisplayUser.messageToDisplayUser)
        )
    }

    fun isOk(): Boolean = this is Success
    fun isErr(): Boolean = this is Failure

    fun getSuccess(): SuccessType? = when (this) {
        is Success -> value
        is Failure -> null
    }

    fun getErrorToDisplay(): ErrorType? = when (this) {
        is Success -> null
        is Failure -> errorMessageToDisplayUser
    }
    fun getException(): Throwable? = when (this) {
        is Success -> null
        is Failure -> internalException
    }


    inline fun <R> map(transform: (SuccessType) -> R): Result<R, ErrorType> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> Failure(errorMessageToDisplayUser, internalException)
    }

    inline fun <R : Error> mapErr(transform: (ErrorType) -> R): Result<SuccessType, R> = when (this) {
        is Success -> Success(value)
        is Failure -> Failure(transform(errorMessageToDisplayUser), internalException)
    }

    inline fun <R>fold(
        onSuccess: (SuccessType) -> R,
        onError: (ErrorType, Throwable) -> R
    ):R = when(this){
        is Success -> onSuccess(value)
        is Failure -> onError(errorMessageToDisplayUser, internalException)
    }

    companion object{
        /** a run catching fun, eg if got an exception then will still display an default error.
         * [defaultErrorMessage] - here you give me a generic error and the exception will be included for you  */
        inline  fun <SuccessType, ErrorType : Error > runCatching(defaultErrorMessage:ErrorType, codeBlock: () -> SuccessType  ): Result<SuccessType, ErrorType>{
            return try {
                Result.Success(codeBlock())
            }catch (e: Exception){
                Result.Failure(defaultErrorMessage, e)
            }
        }
        /** a run catching fun, eg if got an exception then will still display an default error.
         * [defaultErrorMessage] - here you give me a generic error and the exception will be included for you  */
        inline  fun <SuccessType, ErrorType : Error > runCatching(defaultErrorMessage:(Throwable)->ErrorType, codeBlock: () -> SuccessType  ): Result<SuccessType, ErrorType>{
            return try {
                Result.Success(codeBlock())
            }catch (e: Exception){
                Result.Failure(defaultErrorMessage(e), e)
            }
        }

    }

}