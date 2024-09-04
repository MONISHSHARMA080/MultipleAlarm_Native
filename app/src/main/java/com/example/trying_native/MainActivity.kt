package com.example.trying_native

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getSystemService
import com.example.trying_native.Components_for_ui_compose.Button_for_alarm
import com.example.trying_native.Components_for_ui_compose.DialExample
import com.example.trying_native.Components_for_ui_compose.*
import com.example.trying_native.Components_for_ui_compose.DialExample_2
import com.example.trying_native.ui.theme.Trying_nativeTheme

class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (Settings.canDrawOverlays(this)) {
                // Permission granted, schedule the alarm
                scheduleAlarmInternal()
            } else {
                Log.d("AA", "Overlay permission denied")
            }
        }
    }

    private val exactAlarmPermissionLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        // Check the result to see if the permission was granted
        scheduleAlarmInternal() // Schedule the alarm anyway, as the system might still allow it
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Trying_nativeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
                    Column(modifier = Modifier.padding(paddingValues)) {
                        var showTimePicker by remember { mutableStateOf(false) }
                        // First element
                        Button_for_alarm("Click Me", Modifier.padding(8.dp)) {
                            scheduleAlarm()
                        }
                        Button(onClick = {
                            Log.d("AA","$showTimePicker");showTimePicker=!showTimePicker; Log.d("AA","--$showTimePicker")

                                         }, modifier = Modifier.padding(10.dp)) {
                            Text("Toggle Timer")
                        }
                        if(showTimePicker){
                            DialExample_2(
                                onConfirm = { timePickerState ->
                                    val selectedTime = "${timePickerState.hour}:${timePickerState.minute}"
                                    logD("Selected time: $selectedTime")
                                    showTimePicker = false
                                    // Proceed with scheduling the alarm using the selected time
                                },
                                onDismiss = {
                                    logD("TimePicker dismissed")
                                    showTimePicker = false
                                }
                            )
                        }
                        // State to control the visibility of the DatePickerModal
                        var showDatePicker by remember { mutableStateOf(false) }

                        Button(onClick = {
                            showDatePicker  =!showDatePicker; Log.d("AA","showDatePicker--$showDatePicker")

                        }, modifier = Modifier.padding(10.dp)) {
                            Text("Toggle Date picker ")
                        }
                        // State to hold the selected date
                        var selectedDate by remember { mutableStateOf<Long?>(null) }


                        if(showDatePicker){
                            DatePickerModal(
                                onDateSelected = { date ->
                                    selectedDate = date
                                    showDatePicker = !showDatePicker
                                    logD("Date picker ended -->$selectedDate")
                                }, onDismiss = {
                                    showDatePicker = !showDatePicker
                                    logD("Date picker dismissed ")
                                }
                            )
                        }
//                        AdvancedTimePickerDialog("Time Picker Title",
//                            onDismiss = {
//                                logD("date picker dismissed")
//                                showTimePicker = false
//                            },
//                            onConfirm = {
//                                logD("date picker confirmed")
//                            },
//
//                            )
                    }
                }
            }
        }
    }


    private fun scheduleAlarm() {
        // Check for SYSTEM_ALERT_WINDOW permission
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            overlayPermissionLauncher.launch(intent)
            return
        }
        // Check for SCHEDULE_EXACT_ALARM permission (only required for Android 12+)
        // If both permissions are granted, proceed with scheduling the alarm
        scheduleAlarmInternal()
    }

    private fun scheduleAlarmInternal() {
        Log.d("AA", "Clicked on the schedule alarm func")
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60000, pendingIntent)
    }
}

fun logD(a:String):Unit{
    Log.d("aa","----->>$a")
}