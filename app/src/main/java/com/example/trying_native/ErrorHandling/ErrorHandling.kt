package com.example.trying_native.ErrorHandling

import android.util.Log
import com.example.trying_native.analytics.Analytics
import com.example.trying_native.notification.NotificationChannelType
import com.example.trying_native.notification.NotificationHandler
import com.example.trying_native.utils.Result.Error
import com.example.trying_native.utils.Result.Result


// class will init, and take in the error message to display user and the exception, make the notification and log it and report it to the server
// also follow single responsibility principle
class ErrorHandler(val notificationHandler: NotificationHandler, val analytics: Analytics ) {

	val defaultTitle = "Sorry an error occurred, Please try again"

	fun <E: Error>handleError(error: Result.Failure<E>, title: String = defaultTitle): Unit {
		logD("got an error messageTODisplay to user:${error.errorMessageToDisplayUser.messageToDisplayUser} and exception:${error.internalException.message}")
		notifyUserAboutError(error, title)
	}
	fun <E: Error> notifyUserAboutError(error: Result.Failure<E>, title: String ): Unit {
		val notification = notificationHandler.build( notificationChannel = NotificationChannelType.ErrorChannel, notificationTitle = "Sorry an error occurred, Please try again", notificationText = error.errorMessageToDisplayUser.messageToDisplayUser )
		notificationHandler.show(notification)
		analytics.captureEvent("Error occurred", mapOf(
			"error message displayed to user" to error.errorMessageToDisplayUser.messageToDisplayUser,
			"exception occurred" to error.internalException.toString()
		))
	}
	private  fun  logD(msg: String): Unit{
		Log.d("AAAAAA", "[ErrorHandling] $msg")
	}

}