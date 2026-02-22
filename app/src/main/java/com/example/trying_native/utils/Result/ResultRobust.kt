package com.example.trying_native.utils.Result

interface Error {
    val message: String
}

sealed class ResultRobust<out T, out E : Error> {
    data class Ok<out T>(val value: T) : ResultRobust<T, Nothing>()
    data class Err<out E : Error>(val errorMessageToDisplayuser: E, val exception: Throwable) : ResultRobust<Nothing, E>()

    // Helper methods
    fun isOk(): Boolean = this is Ok
    fun isErr(): Boolean = this is Err

    inline fun <R> map(transform: (T) -> R): ResultRobust<R, E> = when (this) {
        is Ok -> Ok(transform(value))
        is Err -> Err(errorMessageToDisplayuser, exception)
    }

    inline fun <R : Error> mapErr(transform: (E) -> R): ResultRobust<T, R> = when (this) {
        is Ok -> Ok(value)
        is Err -> Err(transform(errorMessageToDisplayuser), exception)
    }
}
