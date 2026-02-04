package com.example.trying_native.utils.Result


sealed class ResultEasy<out T>{
    data class Success<T>(val data:T):ResultEasy<T>()
    data class failure(val msgToDisplayUser:String, val exception: Exception):ResultEasy<Nothing>()

}
