package com.coolApps.MultipleAlarmClock.alarmFeature.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [AlarmData::class], version = 3)
@TypeConverters(RepeatDaysConverter::class)
abstract class AlarmDatabase : RoomDatabase() {
	abstract fun alarmDao(): AlarmDao
}
