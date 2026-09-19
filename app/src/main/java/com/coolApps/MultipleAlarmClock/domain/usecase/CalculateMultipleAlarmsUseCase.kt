package com.coolApps.MultipleAlarmClock.domain.usecase

import com.coolApps.MultipleAlarmClock.data.local.AlarmData
import java.util.Calendar

class CalculateMultipleAlarmsUseCase {
    fun getNextAlarmTriggerTime(alarmData: AlarmData, now: Calendar = Calendar.getInstance()): Long? {
        return alarmData.getNextAlarmTriggerTime(now)
    }

    fun rollOverIfTimeIntervalPassed(alarmData: AlarmData, now: Calendar = Calendar.getInstance()): AlarmData {
        return alarmData.rollOverIfTimeIntervalPassed(now)
    }

    fun getAlarmTimeSequence(alarmData: AlarmData): Sequence<Long> {
        return alarmData.alarmTimeSequence()
    }
}
