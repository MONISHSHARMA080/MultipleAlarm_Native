package com.coolApps.MultipleAlarmClock.data.local

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

val MIGRATION_3_4 = object : Migration(3, 4) {
	override fun migrate(db: SupportSQLiteDatabase) {
		db.execSQL("ALTER TABLE AlarmData ADD COLUMN is_force_loud_volume INTEGER NOT NULL DEFAULT 0")
	}
}