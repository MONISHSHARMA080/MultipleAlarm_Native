package com.coolApps.MultipleAlarmClock.ErrorHandling

import android.util.Log
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.coolApps.MultipleAlarmClock.notification.NotificationChannelType
import com.coolApps.MultipleAlarmClock.notification.NotificationHandler
import com.coolApps.MultipleAlarmClock.utils.Result.Error
import com.coolApps.MultipleAlarmClock.utils.Result.Result


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
			"exception occurred" to error.internalException.toString(),
			"stack trace" to error.internalException.stackTraceToString(),
			"cause" to (error.internalException.cause?.toString() ?: "No cause" ) ,
			"error class name" to error.errorMessageToDisplayUser.javaClass.name,
			"exception" to error.internalException
		))
	}
	private  fun  logD(msg: String): Unit{
		Log.d("AAAAAA", "[ErrorHandling] $msg")
	}

}