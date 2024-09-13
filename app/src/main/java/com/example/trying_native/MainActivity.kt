package com.example.trying_native

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import androidx.lifecycle.lifecycleScope
import com.example.trying_native.Components_for_ui_compose.Button_for_alarm
import com.example.trying_native.Components_for_ui_compose.*
import com.example.trying_native.ui.theme.Trying_nativeTheme
import java.util.Date
import java.util.Calendar
import androidx.room.Room
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmData
import com.example.trying_native.dataBase.AlarmDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class MainActivity : ComponentActivity() {


    var startHour_after_the_callback: Int? = null
    var startMin_after_the_callback: Int? = null
    var endHour_after_the_callback: Int? = null
    var endMin_after_the_callback: Int? = null
    var date_after_the_callback: Long? = null
    var freq_after_the_callback: Long? = null
    var selected_date_for_display :String? = null


    private lateinit var alarmDao: AlarmDao

//    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "AlarmData")
//    val database = DatabaseManager.getInstance(applicationContext)
 //-form docs

val activity_context = this

    @SuppressLint("SuspiciousIndentation")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {

//        lifecycleScope.launch(Dispatchers.IO) {
            val db = Room.databaseBuilder(
                applicationContext,
                AlarmDatabase::class.java, "alarm-database"
            ).build()
            alarmDao = db.alarmDao()

//        }


        super.onCreate(savedInstanceState)
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        setContent {
            Trying_nativeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
                    Column(modifier = Modifier.padding(paddingValues)) {
                        val context = LocalContext.current
                        val showDialog = remember { mutableStateOf(false) }
                        val dialogMessage = remember { mutableStateOf("") }
                        myTexts(alarmDao)
                        // Schedule button
                        Button_for_alarm("Schedule", Modifier.padding(8.dp)) {
                            logD("-------in the Button_for_alarm ")
                            doAllFieldChecksIfFineRunScheduleMultipleAlarm(showDialog, dialogMessage,alarmManager, activity_context,selected_date_for_display, startHour_after_the_callback, startMin_after_the_callback, endHour_after_the_callback, endMin_after_the_callback )
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
                                    var selected_date = Date(selectedDate)
                                    logD("Date Obj-->${selected_date}")
                                    date_after_the_callback = selectedDate // add it here selected_date_for_display
                                    val date_from_callB =  Instant.ofEpochMilli(selectedDate).atZone(ZoneId.systemDefault())
                                    selected_date_for_display = "${date_from_callB.dayOfMonth}/${date_from_callB.monthValue}/${date_from_callB.year}"
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
                        Button(onClick = {

                            lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime(Calendar.getInstance().timeInMillis + 60000, activity_context, alarmManager, "alarm_start_time_to_search_db", "alarm_end_time_to_search_db", Calendar.getInstance().timeInMillis + 5000, LastAlarmUpdateDBReceiver())


                        }){
                            Text("lastPendingIntentWithMessageForDbOperations")
                        }

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
                        AlarmContainer(alarmDao, alarmManager, activity_context)
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
    private fun doAllFieldChecksIfFineRunScheduleMultipleAlarm(showDialog: MutableState<Boolean>, dialogMessage: MutableState<String>, alarmManager:AlarmManager, context: Context, selected_date_for_display:String?, startHour_after_the_callback:Int?, startMin_after_the_callback: Int?, endHour_after_the_callback:Int?, endMin_after_the_callback:Int? ) {
        logD("fun areAllFieldsNotFilled ->${areAllFieldsNotFilled()};;; func areSomeFieldNotFilled ->${areSomeFieldNotFilled()} ")
        if (areAllFieldsNotFilled()) {
            dialogMessage.value = "Please fill in all the required fields."
            showDialog.value = true
        }
        else if (areSomeFieldNotFilled()) {
            dialogMessage.value = "Please select the ${emptyFieldAndTheirName()}."
            showDialog.value = true
        } else {
            logD("in the doAllFieldChecksIfFineRunScheduleMultipleAlarm else statememt ")
            var selected_date_for_display_1 = selected_date_for_display
            var startHour_after_the_callback_1  = startHour_after_the_callback
            var startMin_after_the_callback_1  = startMin_after_the_callback
            var endHour_after_the_callback_1 = endHour_after_the_callback
            var endMin_after_the_callback_1 = endMin_after_the_callback



            if (selected_date_for_display_1 == null || startMin_after_the_callback_1 == null || startHour_after_the_callback_1 == null || endHour_after_the_callback_1== null || endMin_after_the_callback_1 == null){
                logD("in the selected date to null field---")
                // selected_date_for_display_1 was null
                logD("--$selected_date_for_display_1---$startMin_after_the_callback_1 -- $startHour_after_the_callback_1 --- $endHour_after_the_callback_1--- $endMin_after_the_callback_1")
                dialogMessage.value = "Error occured , error code is 0#276gde7h32, can't serialize data "
                showDialog.value = true
            }else if (selected_date_for_display_1 != null && startHour_after_the_callback_1 != null && startMin_after_the_callback_1 != null && endHour_after_the_callback_1 != null && endMin_after_the_callback_1 != null){
                logD("About to launch the alarm ---")
                scheduleMultipleAlarms(alarmManager, context, selected_date_for_display_1, startHour_after_the_callback_1, startMin_after_the_callback_1, endHour_after_the_callback_1, endMin_after_the_callback_1  )
            }
//            scheduleAlarm(SystemClock.elapsedRealtime() + 1000,alarmManager)
        }
    }

    private fun scheduleAlarm(triggerTime: Long, alarmManager:AlarmManager) {
        logD( "Clicked on the schedule alarm func")
        var triggerTime_1 = triggerTime
        val intent = Intent(this, AlarmReceiver::class.java)
        intent.putExtra("last_alarm_info1","from the schedule alarm function")
        logD("Trigger time in the scheduleAlarm func is --> $triggerTime_1 ")
        intent.putExtra("triggerTime", triggerTime_1)
        val pendingIntent = PendingIntent.getBroadcast(this, triggerTime.toInt(), intent, PendingIntent.FLAG_MUTABLE)
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime_1, pendingIntent)
    }
    // this should fix it as I changed FLAG_IMUTABLE to FLAG_MUTABLE



    @SuppressLint("SuspiciousIndentation")
    private  fun scheduleMultipleAlarms(alarmManager: AlarmManager, context: Context, selected_date_for_display:String, startHour_after_the_callback:Int, startMin_after_the_callback: Int, endHour_after_the_callback:Int, endMin_after_the_callback:Int){
    // should probably make some checks like if the user ST->11:30 pm today and end time 1 am tomorrow (basically should be in a day)

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = date_after_the_callback?: 0L
        calendar.set(Calendar.HOUR_OF_DAY, startHour_after_the_callback ?: 0)
        calendar.set(Calendar.MINUTE, startMin_after_the_callback ?: 0)
        calendar.set(Calendar.SECOND, 0) // Set seconds to zero
        calendar.set(Calendar.MILLISECOND, 0) // Set milliseconds to zero
        var startTimeInMillis = calendar.timeInMillis
        val startTimeInMillisendForDb= startTimeInMillis
        val start_time_for_display = SimpleDateFormat("hh:mm", Locale.getDefault()).format(calendar.time)
        val start_am_pm = SimpleDateFormat("a", Locale.getDefault()).format(calendar.time).trim()
        calendar.set(Calendar.HOUR_OF_DAY, endHour_after_the_callback ?: 0)
        calendar.set(Calendar.MINUTE, endMin_after_the_callback ?: 0)
        var endTimeInMillis = calendar.timeInMillis
        val endTimeInMillisendForDb= endTimeInMillis
        val end_time_for_display = SimpleDateFormat("hh:mm", Locale.getDefault()).format(calendar.time)
        val end_am_pm =  SimpleDateFormat("a", Locale.getDefault()).format(calendar.time).trim()

        //  can make the

        logD(" \n\n am_pm_start_time-->$start_time_for_display $start_am_pm ; endtime-->$end_time_for_display $end_am_pm")
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
            // this line added the freq in the last pending intent and now to get time for the last time we
            // need to - frq from it
            i+=1
        }
        // making a broadcast to the receiver to update the alarm
//        cancelAPendingIntent(startTimeInMillis - freq_in_min,activity_context, alarmManager)
        // now making the last
        logD("about to set lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime ")
        lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime(startTimeInMillisendForDb, activity_context, alarmManager, "alarm_start_time_to_search_db", "alarm_end_time_to_search_db", endTimeInMillisendForDb, LastAlarmUpdateDBReceiver())
        var startTimeNow = startTimeInMillis - freq_in_min

//        lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime((startTimeInMillis - freq_in_min)+2000,activity_context, alarmManager, startTimeNow, startTimeNow, "form the lastPendingIntentWithMessageForDbOperations form", AlarmReceiver() )
        alarmSetComplete = true
           lifecycleScope.launch {
               try {
                   val newAlarm = AlarmData(
                       first_value = startTimeInMillisendForDb,
                       second_value = endTimeInMillisendForDb,
                       freq_in_min = freq_in_min,
                       isReadyToUse = alarmSetComplete,
                       date_for_display = selected_date_for_display,
                       start_time_for_display = start_time_for_display ,
                       end_time_for_display = end_time_for_display,
                       start_am_pm = start_am_pm ,
                       end_am_pm = end_am_pm

                   )
                   val insertedId = alarmDao.insert(newAlarm)
                   logD("Inserted alarm with ID: $insertedId")
               } catch (e: Exception) {
                   logD("Exception occurred when inserting in the db: $e")
               }
       }

        // now add this in the data base
    }
}

fun logD(message:String):Unit{
    Log.d("AAA","-->$message")
}