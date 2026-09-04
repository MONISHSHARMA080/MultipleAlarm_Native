package com.coolApps.MultipleAlarmClock.alarmFeature.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
	override fun migrate(db: SupportSQLiteDatabase) {
		db.execSQL("ALTER TABLE AlarmData ADD COLUMN sound TEXT")
	}
}

val MIGRATION_2_3 = object : Migration(2, 3) {
	override fun migrate(db: SupportSQLiteDatabase) {
		db.execSQL("ALTER TABLE AlarmData ADD COLUMN repeat_days INTEGER DEFAULT NULL")
	}
}