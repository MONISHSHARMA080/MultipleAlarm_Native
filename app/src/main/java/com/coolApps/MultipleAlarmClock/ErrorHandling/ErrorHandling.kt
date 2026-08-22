package com.coolApps.MultipleAlarmClock.ErrorHandling

import android.util.Log
import com.coolApps.MultipleAlarmClock.AlarmLogic.AlarmControllerError
import com.coolApps.MultipleAlarmClock.AlarmLogic.toDebugString
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.coolApps.MultipleAlarmClock.notification.NotificationChannelType
import com.coolApps.MultipleAlarmClock.notification.NotificationHandler
import com.coolApps.MultipleAlarmClock.utils.Result.Result
import jakarta.inject.Inject


// class will init, and take in the error message to display user and the exception, make the notification and log it and report it to the server
// also follow single responsibility principle
class ErrorHandler @Inject constructor(val notificationHandler: NotificationHandler, val analytics: Analytics) {

	fun  handleError(error: Result.Failure<AlarmControllerError>): Unit {
		logD("got an error messageToDisplay to user:${error.errorClass} and internalErrorMessage:${error.errorClass.internalErrorMessage}")
		val resolvedMessage = error.errorClass.messageToDisplayUser.asString(notificationHandler.context)
		val resolvedTitle = error.errorClass.titleToDisplayUser.asString(notificationHandler.context)
		val internalErrorMessage = error.errorClass.internalErrorMessage
		notifyUserAboutError(
			displayMessage = resolvedMessage,
			displayTitle = resolvedTitle,
			internalErrorMessage = internalErrorMessage,
			errorClassName = error.errorClass.toDebugString(notificationHandler.context)
		)
	}
	fun  handleError(displayMessage:String, displayTitle:String, internalErrorMessage: String, errorClassName:String ): Unit {
		notifyUserAboutError(
			displayMessage = displayMessage,
			displayTitle = displayTitle,
			internalErrorMessage = internalErrorMessage,
			errorClassName = errorClassName
		)
	}

	private fun  notifyUserAboutError(displayMessage:String, displayTitle:String, internalErrorMessage: String, errorClassName:String ): Unit {
		val notification = notificationHandler.build(
			notificationChannel = NotificationChannelType.ErrorChannel,
			notificationTitle = displayTitle,
			notificationText = displayMessage
		)
		notificationHandler.show(notification)

		analytics.captureEvent(
			"Error occurred", mapOf(
				"error message displayed to user" to displayMessage,
				"internal error message" to internalErrorMessage,
				"error class name" to errorClassName ,
			)
		)
	}

	private fun logD(msg: String): Unit {
		Log.d("AAAAAA", "[ErrorHandling] $msg")
	}
}

