package MultipleAlarmClock.alarmFeature.domain

import MultipleAlarmClock.alarmFeature.data.local.AlarmData
import kotlinx.coroutines.flow.Flow

interface AlarmRepository {
	fun getAlarmsStream(): Flow<List<AlarmData>>
	suspend fun getAlarmById(id: Int): AlarmData?
	suspend fun saveAlarm(alarm: AlarmData): Long
	suspend fun upsertAlarm(alarm: AlarmData): Long
	suspend fun deleteAlarm(alarm: AlarmData): Int
	suspend fun updateAlarm(alarm: AlarmData): Int
}