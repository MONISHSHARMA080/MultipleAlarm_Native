package MultipleAlarmClock.alarmFeature.data

import MultipleAlarmClock.alarmFeature.data.local.AlarmDao
import MultipleAlarmClock.alarmFeature.data.local.AlarmData
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

class AlarmLocalDataSource @Inject constructor(
	private val alarmDao: AlarmDao
) {
	fun getAlarmsStream(): Flow<List<AlarmData>> = alarmDao.getAllAlarmsFlow()

	suspend fun getAllAlarms(): List<AlarmData> = alarmDao.getAllAlarms()

	suspend fun getAlarmById(id: Int): AlarmData? = alarmDao.getAlarmById(id)

	suspend fun insertAlarm(alarm: AlarmData): Long = alarmDao.insert(alarm)

	suspend fun upsertAlarm(alarm: AlarmData): Long = alarmDao.updateOrInsert(alarm)

	suspend fun deleteAlarm(alarm: AlarmData): Int = alarmDao.deleteAlarm(alarm)

	suspend fun updateAlarm(alarm: AlarmData): Int = alarmDao.updateAlarm(alarm)
}