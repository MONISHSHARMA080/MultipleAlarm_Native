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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getSystemService
import com.example.trying_native.Components_for_ui_compose.Button_for_alarm
import com.example.trying_native.Components_for_ui_compose.*
import com.example.trying_native.ui.theme.Trying_nativeTheme
import java.util.Date

class MainActivity : ComponentActivity() {

    var startHour_after_the_callback: Int? = null
    var startMin_after_the_callback: Int? = null
    var endHour_after_the_callback: Int? = null
    var endMin_after_the_callback: Int? = null
    var date_after_the_callback: Long? = null
    var freq_after_the_callback: Long? = null

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Trying_nativeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
                    Column(modifier = Modifier.padding(paddingValues)) {
                        val context = LocalContext.current
                        val showDialog = remember { mutableStateOf(false) }
                        val dialogMessage = remember { mutableStateOf("") }



                        // Schedule button
                        Button_for_alarm("Schedule", Modifier.padding(8.dp)) {
                            logD("fun areAllFieldsNotFilled ->${areAllFieldsNotFilled()};;; func areSomeFieldNotFilled ->${areSomeFieldNotFilled()} ")

                            if (areAllFieldsNotFilled()) {
                                dialogMessage.value = "Please fill in all the required fields."
                                showDialog.value = true
                            }

                            else if (areSomeFieldNotFilled()) {
                                dialogMessage.value = "Please select the ${emptyFieldAndTheirName()}."
                                showDialog.value = true
                            } else {
                                scheduleAlarm(SystemClock.elapsedRealtime() + 1000)
                            }
                        }
                        // Time pickers, date picker, and frequency field
                        AbstractFunction_TimePickerSection(
                            "Select starting time",
                            onTimeSelected_func_to_handle_value_returned = { timePickerState ->
                                logD("in the abstract timepicker func and the value gotted was -> $timePickerState")
                                startHour_after_the_callback = timePickerState.hour
                                startMin_after_the_callback = timePickerState.minute
                            })

                        AbstractFunction_TimePickerSection(
                            "Select ending time",
                            onTimeSelected_func_to_handle_value_returned = { timePickerState ->
                                logD("in the abstract timepicker func and the value gotted was -> $timePickerState; time is ${timePickerState.hour}:${timePickerState.minute}")
                                endHour_after_the_callback = timePickerState.hour
                                endMin_after_the_callback = timePickerState.minute
                            })

                        AbstractFunction_DatePickerSection(
                            "Select a date",
                            onDateSelected_func_to_handle_value_returned = { selectedDate ->
                                if (selectedDate != null) {
                                    logD("Date Obj-->${Date(selectedDate)}")
                                    date_after_the_callback = selectedDate
                                }
                                logD("Date selected: $selectedDate")
                            }
                        )
                        NumberField("Enter your Frequency number",
                            onFrequencyChanged = { string_received ->
                                if (string_received.isNotBlank()) { // or else app will crash if it is null or empty
                                    logD("String received -->$string_received")
                                    freq_after_the_callback = string_received.toLong()
                                    logD("freq_after_the_callback  -->$freq_after_the_callback")
                                }
                            }
                        )

                        // Dialog box
                        if (showDialog.value) {
                            AlertDialog(
                                onDismissRequest = { showDialog.value = false },
                                title = { Text("Incomplete Information") },
                                text = { Text(dialogMessage.value) },  // Use the dynamic message here
                                confirmButton = {
                                    Button(onClick = {
                                        showDialog.value = false
                                    }) {
                                        Text("OK")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun areAllFieldsNotFilled(): Boolean {
        var freq= freq_after_the_callback
        logD("changing freq to null->$freq_after_the_callback")

        if (freq?.toInt() == 0){
            logD("changing freq to null->$freq")
            freq = null
        }
        return startHour_after_the_callback == null &&
                startMin_after_the_callback == null &&
                endHour_after_the_callback == null &&
                endMin_after_the_callback == null &&
                date_after_the_callback == null &&
                freq_after_the_callback == null
    }
    private fun areSomeFieldNotFilled(): Boolean {
        return startHour_after_the_callback == null ||
                startMin_after_the_callback == null ||
                endHour_after_the_callback == null ||
                endMin_after_the_callback == null ||
                date_after_the_callback == null ||
                freq_after_the_callback == null
    }

    private fun emptyFieldAndTheirName():( String){
        // this func is used when all the field are not null, so maybe we should return which field is null
       if(startHour_after_the_callback == null){
           return "Start time"
       }
        else if(startMin_after_the_callback == null){
            return "Start time"
        }
        else if(endHour_after_the_callback == null){
          return "End time"
       }
        else if (endMin_after_the_callback == null){
            return "End time"
       }
        else if (date_after_the_callback == null){
            return  "Date"
       }
        else{
            return "Frequency"
        }
    }

    private fun scheduleAlarm(triggerTime: Long) {
        Log.d("AA", "Clicked on the schedule alarm func")
        var triggerTime_1 = triggerTime
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
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