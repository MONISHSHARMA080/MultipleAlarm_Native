package com.coolApps.MultipleAlarmClock.alarmFeature.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AlarmData::class], version = 2)
abstract class AlarmDatabase : RoomDatabase() {
	abstract fun alarmDao(): AlarmDao
}
