package com.example.trying_native.Activities

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AlarmActivityIntentData(
    val alarmIdInDb: Int,
    val startTimeForDb: Long,
    val startTime: Long,
    val endTime: Long,
    val message: String
) : Parcelable {
    override fun toString(): String {
        return "AlarmActivityIntentData: alarmIdInDb=$alarmIdInDb, startTimeForDb=$startTimeForDb, startTime=$startTime, endTime=$endTime, message:$message"
    }
}