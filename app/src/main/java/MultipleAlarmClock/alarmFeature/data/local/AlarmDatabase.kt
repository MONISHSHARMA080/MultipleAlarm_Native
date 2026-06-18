package MultipleAlarmClock.alarmFeature.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [AlarmData::class], version = 2)
@TypeConverters(AlarmTypeConverters::class)
abstract class AlarmDatabase : RoomDatabase() {
	abstract fun alarmDao(): AlarmDao
}
