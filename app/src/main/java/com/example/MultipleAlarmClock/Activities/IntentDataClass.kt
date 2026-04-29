package com.coolApps.MultipleAlarmClock.Activities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AlarmActivityIntentData(
    val alarmIdInDb: Int,
    val startTimeForDb: Long,
    val startTime: Long,
    val endTime: Long,
    val message: String,
//    val ringtoneUri : String,
) : Parcelable {
    override fun toString(): String {
        return "AlarmActivityIntentData: alarmIdInDb=$alarmIdInDb, startTimeForDb=$startTimeForDb, startTime=$startTime, endTime=$endTime, message:$message"
    }
}