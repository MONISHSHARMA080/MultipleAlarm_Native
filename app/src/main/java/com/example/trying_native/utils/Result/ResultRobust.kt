package com.example.trying_native.utils.Result

interface Error {
    val message: String
}

sealed class ResultRobust<out SuccessType, out ErrorType : Error> {
    data class Success<out T>(val value: T) : ResultRobust<T, Nothing>()
    data class Failure<out E : Error>(val errorMessageToDisplayUser: E, val exception: Throwable) : ResultRobust<Nothing, E>()

    fun isOk(): Boolean = this is Success
    fun isErr(): Boolean = this is Failure

    inline fun <R> map(transform: (SuccessType) -> R): ResultRobust<R, ErrorType> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> Failure(errorMessageToDisplayUser, exception)
    }

    inline fun <R : Error> mapErr(transform: (ErrorType) -> R): ResultRobust<SuccessType, R> = when (this) {
        is Success -> Success(value)
        is Failure -> Failure(transform(errorMessageToDisplayUser), exception)
    }

    inline fun <R>fold(
        onSuccess: (SuccessType) -> R,
        onError: (ErrorType, Throwable) -> R
    ):R = when(this){
        is Success -> onSuccess(value)
        is Failure -> onError(errorMessageToDisplayUser, exception)
    }

}