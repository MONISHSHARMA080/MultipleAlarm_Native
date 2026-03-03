package com.example.trying_native.ErrorHandling

import android.util.Log
import com.example.trying_native.utils.Result.Error
import com.example.trying_native.utils.Result.Result


// class will init, and take in the error message to display user and the exception, make the notification and log it and report it to the server
// also follow single responsibility principle
class ErrorHandler {
	init {
	}
	fun <E: Error>handleError(error: Result.Failure<E>): Unit {
		logD("got an error messageTODisplay to user:${error.errorMessageToDisplayUser.messageToDisplayUser} and exception:${error.internalException.message}")
		// log the error
		// user notification
		// report the error

	}
	fun notifyUserAboutError(): Unit {

	}
	fun reportError(): Unit {

	}
	private  fun  logD(msg: String): Unit{
		Log.d("AAAAAA", "[ErrorHandling] $msg")
	}

}