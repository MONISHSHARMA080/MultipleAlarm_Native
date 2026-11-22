package com.coolApps.MultipleAlarmClock.data.local

import androidx.room3.ColumnTypeConverter

class RepeatDaysConverter {
    @ColumnTypeConverter
    fun fromInt(bits: Int?): RepeatDays? = bits?.let { RepeatDays(it) }

    @ColumnTypeConverter
    fun toInt(repeatDays: RepeatDays?): Int? = repeatDays?.bits
}
