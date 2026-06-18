package MultipleAlarmClock.alarmFeature.data.local.repository

import MultipleAlarmClock.alarmFeature.data.AlarmLocalDataSource
import MultipleAlarmClock.alarmFeature.data.local.AlarmData
import MultipleAlarmClock.alarmFeature.domain.AlarmRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

class AlarmRepositoryImpl @Inject constructor(
	private val localDataSource: AlarmLocalDataSource
) : AlarmRepository {
	override fun getAlarmsStream(): Flow<List<AlarmData>> {
		return localDataSource.getAlarmsStream()
	}

	override suspend fun getAlarmById(id: Int): AlarmData? {
		return localDataSource.getAlarmById(id)
	}

	override suspend fun saveAlarm(alarm: AlarmData): Long {
		return localDataSource.insertAlarm(alarm)
	}

	override suspend fun upsertAlarm(alarm: AlarmData): Long {
		return localDataSource.upsertAlarm(alarm)
	}

	override suspend fun deleteAlarm(alarm: AlarmData): Int {
		return  localDataSource.deleteAlarm(alarm)
	}

	override suspend fun updateAlarm(alarm: AlarmData): Int {
		return localDataSource.updateAlarm(alarm)
	}

}