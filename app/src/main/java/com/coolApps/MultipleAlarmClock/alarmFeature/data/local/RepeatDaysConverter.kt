package com.coolApps.MultipleAlarmClock.alarmFeature.data.local

import androidx.room.TypeConverter

class RepeatDaysConverter {
    @TypeConverter
    fun fromInt(bits: Int?): RepeatDays? = bits?.let { RepeatDays(it) }

    @TypeConverter
    fun toInt(repeatDays: RepeatDays?): Int? = repeatDays?.bits
}
