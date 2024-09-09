package com.example.trying_native

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.trying_native.Components_for_ui_compose.Button_for_alarm
import com.example.trying_native.Components_for_ui_compose.*
import com.example.trying_native.ui.theme.Trying_nativeTheme
import java.util.Date
import java.util.Calendar
import androidx.room.Room
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmData
import com.example.trying_native.dataBase.AlarmDatabase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {


    var startHour_after_the_callback: Int? = null
    var startMin_after_the_callback: Int? = null
    var endHour_after_the_callback: Int? = null
    var endMin_after_the_callback: Int? = null
    var date_after_the_callback: Long? = null
    var freq_after_the_callback: Long? = null

    private lateinit var alarmDao: AlarmDao

//    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "AlarmData")
//    val database = DatabaseManager.getInstance(applicationContext)
 //-form docs


    @SuppressLint("SuspiciousIndentation")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val db = Room.databaseBuilder(
            applicationContext,
            AlarmDatabase::class.java, "alarm-database"
        ).build()
        alarmDao = db.alarmDao()

        // Insert a new alarm into the database
//        GlobalScope.launch {
            val alarmData = AlarmData(
                first_value = 1234,
                second_value = 1235,
                freq_in_min = 3,
                isCompleted = true,
                uid = 238123981
            )
//
//            try {
//                alarmDao.insert(alarmData)
//            }catch(e:Exception) {
//                Log.d("AAA", "Error got in insertion-->${e.toString()}")
//            }
//        }

        // Retrieve all alarms from the database
        GlobalScope.launch {
            val alarms = alarmDao.getAllAlarms()
            alarms.forEach { alarm ->
                Log.d("AA", alarm.toString())
            }
        }

//        try {
//            // Building the Room database instance
//
//            logD("----||${db.toString()}")
//        } catch (e: Exception) {
//            // Log the error for debugging purposes
//            logD( "Error creating database: ${e.message}")
//            e.printStackTrace()  // Optional: Print the full stack trace for more detailed debugging
//        }
        super.onCreate(savedInstanceState)
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

    val activity_context = this
        setContent {
            Trying_nativeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
                    Column(modifier = Modifier.padding(paddingValues)) {
                        val context = LocalContext.current
                        val showDialog = remember { mutableStateOf(false) }
                        val dialogMessage = remember { mutableStateOf("") }
//                        val AlarmDao = db.userDao()
//                        AlarmDao.insertAll(AlarmData(1,1234,1235,3,true))
//                        val alarms_in_db: List<AlarmData> = AlarmDao.getAll()
                        Text("dataBase here")
//                        alarms_in_db.forEach { (AlarmData)-> Text(AlarmData.toString()) }
                        myTexts(alarmDao)
                        Text("dataBase here")
                        // Schedule button
                        Button_for_alarm("Schedule", Modifier.padding(8.dp)) {
                            doAllFieldChecksIfFineRunScheduleMultipleAlarm(showDialog, dialogMessage,alarmManager, activity_context )
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

    private fun al(a:Long,b:Long,c:Long,d:Boolean){

    }

    private fun areAllFieldsNotFilled(): Boolean {
        var freq= freq_after_the_callback
        logD("changing freq to null->$freq_after_the_callback")

        if (freq?.toInt() == 0){
            logD("changing freq to null->$freq")
            freq = null
        }else{
            freq = null
        }
        return startHour_after_the_callback == null && startMin_after_the_callback == null &&
                endHour_after_the_callback == null && endMin_after_the_callback == null &&
                date_after_the_callback == null && freq == null
    }
    private fun areSomeFieldNotFilled(): Boolean {
        return startHour_after_the_callback == null || startMin_after_the_callback == null ||
                endHour_after_the_callback == null || endMin_after_the_callback == null ||
                date_after_the_callback == null || freq_after_the_callback == null
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
    private fun doAllFieldChecksIfFineRunScheduleMultipleAlarm(showDialog: MutableState<Boolean>, dialogMessage: MutableState<String>, alarmManager:AlarmManager, context: Context){
        logD("fun areAllFieldsNotFilled ->${areAllFieldsNotFilled()};;; func areSomeFieldNotFilled ->${areSomeFieldNotFilled()} ")
        if (areAllFieldsNotFilled()) {
            dialogMessage.value = "Please fill in all the required fields."
            showDialog.value = true
        }
        else if (areSomeFieldNotFilled()) {
            dialogMessage.value = "Please select the ${emptyFieldAndTheirName()}."
            showDialog.value = true
        } else {
//            scheduleAlarm(SystemClock.elapsedRealtime() + 1000,alarmManager)
            scheduleMultipleAlarms(alarmManager, context )
        }
    }

    private fun scheduleAlarm(triggerTime: Long, alarmManager:AlarmManager) {
        logD( "Clicked on the schedule alarm func")
        var triggerTime_1 = triggerTime
        val intent = Intent(this, AlarmReceiver::class.java)
        logD("Trigger time in the scheduleAlarm func is --> ${triggerTime_1.toString()} ")
        intent.putExtra("triggerTime", triggerTime_1)
        val pendingIntent = PendingIntent.getBroadcast(this, triggerTime.toInt(), intent, PendingIntent.FLAG_IMMUTABLE)
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime_1, pendingIntent)
    }

    private  fun scheduleMultipleAlarms(alarmManager: AlarmManager, context: Context){
    // should probably make some checks like if the user ST->11:30 pm today and end time 1 am tomorrow (basically should be in a day)
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = date_after_the_callback?: 0L
        calendar.set(Calendar.HOUR_OF_DAY, startHour_after_the_callback ?: 0)
        calendar.set(Calendar.MINUTE, startMin_after_the_callback ?: 0)
        calendar.set(Calendar.SECOND, 0) // Set seconds to zero
        calendar.set(Calendar.MILLISECOND, 0) // Set milliseconds to zero
        var startTimeInMillis = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, endHour_after_the_callback ?: 0)
        calendar.set(Calendar.MINUTE, endMin_after_the_callback ?: 0)
        var endTimeInMillis = calendar.timeInMillis
        var freq_in_milli : Long
        if(freq_after_the_callback != null){
            freq_in_milli = freq_after_the_callback as Long
        }else{freq_in_milli = 2}
        var freq_in_min = freq_in_milli * 60000
         logD("startTimeInMillis --$startTimeInMillis, endTimeInMillis--$endTimeInMillis,, equal?-->${startTimeInMillis==endTimeInMillis} ::--:: freq->$freq_in_min")
        var i=0
        var alarmSetComplete = false
        while (startTimeInMillis <= endTimeInMillis){
            logD("round $i")
            scheduleAlarm(startTimeInMillis,alarmManager)
            startTimeInMillis = startTimeInMillis + freq_in_min
            i+=1
        }
        alarmSetComplete = true
        // now add this in the data base
    }
}

fun logD(message:String):Unit{
    Log.d("AAA","-->$message")
}