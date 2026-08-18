package com.coolApps.MultipleAlarmClock.ErrorHandling

import android.util.Log
import com.coolApps.MultipleAlarmClock.AlarmLogic.AlarmControllerError
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.coolApps.MultipleAlarmClock.notification.NotificationChannelType
import com.coolApps.MultipleAlarmClock.notification.NotificationHandler
import com.coolApps.MultipleAlarmClock.utils.Result.Result


// class will init, and take in the error message to display user and the exception, make the notification and log it and report it to the server
// also follow single responsibility principle
class ErrorHandler(val notificationHandler: NotificationHandler, val analytics: Analytics) {

	fun <E : AlarmControllerError> handleError(error: Result.Failure<E>): Unit {
		logD("got an error messageToDisplay to user:${error.errorClass} and exception:${error.internalException.message}")
		notifyUserAboutError(error)
	}

	private fun <E : AlarmControllerError> notifyUserAboutError(error: Result.Failure<E>): Unit {
		val resolvedMessage = error.errorClass.messageToDisplayUser.asString(notificationHandler.context)
		val resolvedTitle = error.errorClass.titleToDisplayUser.asString(notificationHandler.context)
		val notification = notificationHandler.build(
			notificationChannel = NotificationChannelType.ErrorChannel,
			notificationTitle = resolvedTitle,
			notificationText = resolvedMessage
		)
		notificationHandler.show(notification)

		analytics.captureEvent(
			"Error occurred", mapOf(
				"error message displayed to user" to resolvedMessage,
				"exception occurred" to error.internalException.toString(),
				"stack trace" to error.internalException.stackTraceToString(),
				"cause" to (error.internalException.cause?.toString() ?: "No cause"),
				"error class name" to error.errorClass.javaClass.name,
				"exception" to error.internalException
			)
		)
	}

	private fun logD(msg: String): Unit {
		Log.d("AAAAAA", "[ErrorHandling] $msg")
	}
}

