package com.example.trying_native

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getSystemService
import com.example.trying_native.Components_for_ui_compose.Button_for_alarm
import com.example.trying_native.Components_for_ui_compose.*
import com.example.trying_native.ui.theme.Trying_nativeTheme
import java.util.Date

class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (Settings.canDrawOverlays(this)) {
                // Permission granted, schedule the alarm
                permissionToScheduleAlarm()
            } else {
                Log.d("AA", "Overlay permission denied")
            }
        }
    }

    private val exactAlarmPermissionLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        // Check the result to see if the permission was granted
        permissionToScheduleAlarm() // Schedule the alarm anyway, as the system might still allow it
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var startTime_after_the_callback: Long? = null
        var endTime_after_the_callback: Long? = null
        var date_after_the_callback: Long? = null
        var freq_after_the_callback: Long? = null


        setContent {
            Trying_nativeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
                    Column(modifier = Modifier.padding(paddingValues)) {
                        Button_for_alarm("Click Me", Modifier.padding(8.dp)) {
                            scheduleAlarm(SystemClock.elapsedRealtime() + 1000)
                        }

                        AbstractFunction_TimePickerSection(
                            "Select starting time",
                            onTimeSelected_func_to_handle_value_returned = { timePickerState ->
                            logD("in the abstract timepicker func and the value gotted was -> $timePickerState")
                        })

                        AbstractFunction_TimePickerSection(
                            "Select ending time",
                            onTimeSelected_func_to_handle_value_returned = { timePickerState ->
                                logD("in the abstract timepicker func and the value gotted was -> $timePickerState; time is ${timePickerState.hour}:${timePickerState.minute}")
                            })

                        AbstractFunction_DatePickerSection(
                            "Select a date",
                            onDateSelected_func_to_handle_value_returned = { selectedDate ->
                                if (selectedDate != null){
                                    logD("Date Obj-->${Date(selectedDate)}")
                                }
                                logD("Date selected: $selectedDate")
                            }
                        )
                        NumberField("Enter your Frequency number",
                            onFrequencyChanged = {
                                string_received ->
                               if(string_received.isNotBlank()) { // or else app will crash if it is null or empty
                                   logD("String received -->$string_received")
                                   freq_after_the_callback = string_received.toLong()
                                   logD("freq_after_the_callback  -->$freq_after_the_callback")
                               }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun permissionToScheduleAlarm() {
        // Check for SYSTEM_ALERT_WINDOW permission
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            overlayPermissionLauncher.launch(intent)
            return
        }
        // Check for SCHEDULE_EXACT_ALARM permission (only required for Android 12+)
        // If both permissions are granted, proceed with scheduling the alarm
        // scheduleAlarmInternal()
    }

    private fun scheduleAlarm(triggerTime:Long) {

        Log.d("AA", "Clicked on the schedule alarm func")
        var triggerTime_1 = triggerTime
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
//        val currentTime = SystemClock.elapsedRealtime()
//        val triggerAtMillis = currentTime + (triggerTime - System.currentTimeMillis())
        val intent = Intent(this, AlarmReceiver::class.java)
        logD("Trigger time in the scheduleAlarm func is --> ${triggerTime_1.toString()} ")
        intent.putExtra("triggerTime", triggerTime_1)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime_1, pendingIntent)

    }
}

fun logD(a:String):Unit{
    Log.d("aa","----->>$a")
}