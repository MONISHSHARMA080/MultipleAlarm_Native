package com.coolApps.MultipleAlarmClock.domain.usecase

import com.coolApps.MultipleAlarmClock.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.domain.repository.AlarmRepository

class ToggleAlarmUseCase(private val alarmRepository: AlarmRepository) {
    suspend operator fun invoke(alarmData: AlarmData, isReadyToUse: Boolean) {
        alarmRepository.updateAlarm(alarmData.copy(isReadyToUse = isReadyToUse))
    }
}
