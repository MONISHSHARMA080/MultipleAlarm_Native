package com.example.trying_native.testHelperFile

import android.content.Context
import androidx.compose.ui.test.TestContext
import androidx.room.Room
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmDatabase
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

class E2ETestHelper {
    fun getAlarmDao(applicationContext: Context):AlarmDao{
        return Room.databaseBuilder(
            applicationContext,
            AlarmDatabase::class.java, "alarm-database"
        ).build().alarmDao()
    }

    fun getTheCalenderInstanceAndSkipTheMinIn(min:Int):Calendar{
        return Calendar.getInstance().apply {
            set(Calendar.MINUTE, min)
        }
    }

    fun getDateInLong():Long{
        return  Calendar.getInstance().timeInMillis
    }

    fun getDateString(epochMillis: Long):String{
            return Instant.ofEpochMilli(epochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }

}