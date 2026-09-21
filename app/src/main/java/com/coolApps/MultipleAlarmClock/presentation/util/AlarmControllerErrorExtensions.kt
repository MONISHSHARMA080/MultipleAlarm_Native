@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.coolApps.MultipleAlarmClock.presentation.util

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.coolApps.MultipleAlarmClock.domain.model.AlarmControllerErrorSet

import android.content.Context
import androidx.compose.runtime.Composable
import com.coolApps.MultipleAlarmClock.domain.model.AlarmControllerError

fun AlarmControllerError.getTitleForUser(context: Context): String = titleToDisplayUser.asString(context)

@Composable
fun AlarmControllerError.getTitleForUser(): String = titleToDisplayUser.asString()

fun AlarmControllerError.getErrorMessageForUser(context: Context): String = messageToDisplayUser.asString(context)

@Composable
fun AlarmControllerError.getErrorMessageForUser(): String = messageToDisplayUser.asString()



/**
 * Shared debug string. Every leaf error delegates `toString()` to this instead
 * of hand-maintaining a `when` over every class name — new error types don't
 * require touching this function.
 */
fun AlarmControllerError.getDetailedDebugString(contextForUiText: Context?=null): String {
	val className = when(this){
		is AlarmControllerErrorSet.DatabaseOperationFailed -> "DatabaseOperationFailed"
		is AlarmControllerErrorSet.PendingIntentAlreadyExist -> "PendingIntentAlreadyExist"
		is AlarmControllerErrorSet.PendingIntentNotFound -> "PendingIntentNotFound"
		is AlarmControllerErrorSet.Unknown -> "Unknown"
		is AlarmControllerErrorSet.CancellingAlarmError -> "CancellingAlarmError"
		is AlarmControllerErrorSet.ValidationFailed -> "ValidationFailed"
	}
	// if the context available then give me the res text else
	val notificationTitleForUser = if (contextForUiText == null) this.titleToDisplayUser else  this.titleToDisplayUser.asString(contextForUiText)
	val messageToDisplayUser = if (contextForUiText == null) this.messageToDisplayUser else  this.messageToDisplayUser.asString(contextForUiText)
	val errorMessage = internalErrorMessage
	val res = "Error_Class:$className, errorMessage:$errorMessage, notificationTitleForUser:$notificationTitleForUser, messageToDisplayUser:$messageToDisplayUser "
	return res
}