package com.example.trying_native.testHelperFile

import android.content.Context
import androidx.compose.ui.test.TestContext
import androidx.room.Room
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmDatabase
import com.example.trying_native.logD
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
    fun executeADBCommandAndReturnTheResponse(minutesToFastForwardToo:Int): Pair<String,Exception?> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, minutesToFastForwardToo)

        val adbCommand = "adb shell su -c \"date $(date +%m%d%H%M%Y.%S --date='@${
            calendar.timeInMillis / 1000
        }')\""
        logD("the command to execute is -->${adbCommand}")
//        val adbCommand ="adb devices"
        return try {
            // Execute the ADB command using ProcessBuilder
            val process = ProcessBuilder("sh", "-c", adbCommand)
                .redirectErrorStream(true)
                .start()

            // Read the output from the command
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor() // Wait for the process to finish
            return Pair(output, null)
        } catch (e: Exception) {
          Pair("",e)
        }
    }

}