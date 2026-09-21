@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.coolApps.MultipleAlarmClock.presentation.trigger

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class AlarmActivityIntentData(
		val alarmIdInDb: Int,
		val startTimeForDb: Long,
		val alarmTriggerTime: Long,
		val endTime: Long,
		val message: String,
) : Parcelable {
    override fun toString(): String {
        return "AlarmActivityIntentData: alarmIdInDb=$alarmIdInDb, startTimeForDb=$startTimeForDb, alarmTriggerTime=$alarmTriggerTime, endTime=$endTime, message:$message"
    }



}

fun  AlarmActivityIntentData.toIntent(){

}