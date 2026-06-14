package com.coolApps.MultipleAlarmClock.Activities

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class AlarmActivityIntentData(
    val alarmIdInDb: Int,
    val startTimeForDb: Long,
    val startTime: Long,
    val endTime: Long,
    val message: String,
) : Parcelable
