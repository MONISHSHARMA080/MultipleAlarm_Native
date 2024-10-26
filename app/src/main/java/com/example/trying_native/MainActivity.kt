package com.example.trying_native

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.lifecycleScope
import com.example.trying_native.Components_for_ui_compose.*
import com.example.trying_native.ui.theme.Trying_nativeTheme
import java.util.Calendar
import androidx.room.Room
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmData
import com.example.trying_native.dataBase.AlarmDatabase
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    companion object {
        const val POSTHOG_API_KEY = "phc_HKK1ZuTn3bjPFLZs0yA8ApLpgPt48JvrB614zbOduFQ"
        const val POSTHOG_HOST = "https://us.i.posthog.com"
    }

    private val overlayPermissionLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (Settings.canDrawOverlays(this)) {
                // Permission granted, schedule the alarm

                permissionToScheduleAlarm()
            } else {
                logD( "Overlay permission denied")
            }
        }
    }


    private val exactAlarmPermissionLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        // Check the result to see if the permission was granted
        permissionToScheduleAlarm() // Schedule the alarm anyway, as the system might still allow it
    }



    var startHour_after_the_callback: Int? = null
    var startMin_after_the_callback: Int? = null
    var endHour_after_the_callback: Int? = null
    var endMin_after_the_callback: Int? = null
    var date_after_the_callback: Long? = null
    var freq_after_the_callback: Long? = null
//    var selected_date_for_display :String? = null


    private lateinit var alarmDao: AlarmDao

//    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "AlarmData")
//    val database = DatabaseManager.getInstance(applicationContext)
 //-form docsd

val activity_context = this

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {


        val config = PostHogAndroidConfig(
            apiKey = POSTHOG_API_KEY,
            host = POSTHOG_HOST
        )
        config.sessionReplay = true
        // choose whether to mask images or text
        config.sessionReplayConfig.maskAllImages = false
        config.sessionReplayConfig.maskAllTextInputs = true
        // screenshot is disabled by default
        // The screenshot may contain sensitive information, use with caution
        config.sessionReplayConfig.screenshot = true

        PostHogAndroid.setup(this, config)

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
            var a by remember { mutableStateOf(false) }
            Trying_nativeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
                    Column(modifier = Modifier.padding(paddingValues)) {
                        Button(onClick = { a = !a }) { Text("Date")  }
                        if (a){
                            DatePickerModal(onDateSelected = {a->logD(a.toString())}, onDismiss = {a = !a})
                        }

                      AlarmContainer(alarmDao, alarmManager, activity_context, askUserForPermissionToScheduleAlarm = { permissionToScheduleAlarm() } )
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

//    private fun doAllFieldChecksIfFineRunScheduleMultipleAlarm(showDialog: MutableState<Boolean>, dialogMessage: MutableState<String>, alarmManager:AlarmManager, context: Context, selected_date_for_display:String?, startHour_after_the_callback:Int?, startMin_after_the_callback: Int?, endHour_after_the_callback:Int?, endMin_after_the_callback:Int? ) {
//        logD("fun areAllFieldsNotFilled ->${areAllFieldsNotFilled()};;; func areSomeFieldNotFilled ->${areSomeFieldNotFilled()} ")
//        if (areAllFieldsNotFilled()) {
//            dialogMessage.value = "Please fill in all the required fields."
//            showDialog.value = true
//        }
//        else if (areSomeFieldNotFilled()) {
//            dialogMessage.value = "Please select the ${emptyFieldAndTheirName()}."
//            showDialog.value = true
//        } else {
//            logD("in the doAllFieldChecksIfFineRunScheduleMultipleAlarm else statememt ")
//            var selected_date_for_display_1 = selected_date_for_display
//            var startHour_after_the_callback_1  = startHour_after_the_callback
//            var startMin_after_the_callback_1  = startMin_after_the_callback
//            var endHour_after_the_callback_1 = endHour_after_the_callback
//            var endMin_after_the_callback_1 = endMin_after_the_callback
//
//
//
//            if (selected_date_for_display_1 == null || startMin_after_the_callback_1 == null || startHour_after_the_callback_1 == null || endHour_after_the_callback_1== null || endMin_after_the_callback_1 == null){
//                logD("in the selected date to null field---")
//                // selected_date_for_display_1 was null
//                logD("--$selected_date_for_display_1---$startMin_after_the_callback_1 -- $startHour_after_the_callback_1 --- $endHour_after_the_callback_1--- $endMin_after_the_callback_1")
//                dialogMessage.value = "Error occured , error code is 0#276gde7h32, can't serialize data "
//                showDialog.value = true
//            }else if (selected_date_for_display_1 != null && startHour_after_the_callback_1 != null && startMin_after_the_callback_1 != null && endHour_after_the_callback_1 != null && endMin_after_the_callback_1 != null){
//                logD("About to launch the alarm ---")
//                scheduleMultipleAlarms(alarmManager, context, selected_date_for_display_1, startHour_after_the_callback_1, startMin_after_the_callback_1, endHour_after_the_callback_1, endMin_after_the_callback_1  )
//            }
////            scheduleAlarm(SystemClock.elapsedRealtime() + 1000,alarmManager)
//        }
//    }

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


    // should probally remove it --------------------


//    @SuppressLint("SuspiciousIndentation")
//    private  fun scheduleMultipleAlarms(alarmManager: AlarmManager, context: Context, selected_date_for_display:String, startHour_after_the_callback:Int, startMin_after_the_callback: Int, endHour_after_the_callback:Int, endMin_after_the_callback:Int){
//    // should probably make some checks like if the user ST->11:30 pm today and end time 1 am tomorrow (basically should be in a day)
//
//        val calendar = Calendar.getInstance()
//        calendar.timeInMillis = date_after_the_callback?: 0L
//        calendar.set(Calendar.HOUR_OF_DAY, startHour_after_the_callback ?: 0)
//        calendar.set(Calendar.MINUTE, startMin_after_the_callback ?: 0)
//        var startTimeInMillis = calendar.timeInMillis
//        val startTimeInMillisendForDb= startTimeInMillis
//        val start_time_for_display = SimpleDateFormat("hh:mm", Locale.getDefault()).format(calendar.time)
//        val start_am_pm = SimpleDateFormat("a", Locale.getDefault()).format(calendar.time).trim()
//        calendar.set(Calendar.HOUR_OF_DAY, endHour_after_the_callback ?: 0)
//        calendar.set(Calendar.MINUTE, endMin_after_the_callback ?: 0)
//        var endTimeInMillis = calendar.timeInMillis
//        val endTimeInMillisendForDb= endTimeInMillis
//        val end_time_for_display = SimpleDateFormat("hh:mm", Locale.getDefault()).format(calendar.time)
//        val end_am_pm =  SimpleDateFormat("a", Locale.getDefault()).format(calendar.time).trim()
//
//        logD(" \n\n am_pm_start_time-->$start_time_for_display $start_am_pm ; endtime-->$end_time_for_display $end_am_pm")
//        var freq_in_milli : Long
//        if(freq_after_the_callback != null){
//            freq_in_milli = freq_after_the_callback as Long
//        }else{freq_in_milli = 2}
//        var freq_in_min = freq_in_milli * 60000
//         logD("startTimeInMillis --$startTimeInMillis, endTimeInMillis--$endTimeInMillis,, equal?-->${startTimeInMillis==endTimeInMillis} ::--:: freq->$freq_in_min")
//        var i=0
//        var alarmSetComplete = false
//
//        while (startTimeInMillis <= endTimeInMillis){
//            logD("round $i")
//            scheduleAlarm(startTimeInMillis,alarmManager)
//            startTimeInMillis = startTimeInMillis + freq_in_min
//            // this line added the freq in the last pending intent and now to get time for the last time we
//            // need to - frq from it
//            i+=1
//        }
//        // making a broadcast to the receiver to update the alarm
////        cancelAPendingIntent(startTimeInMillis - freq_in_min,activity_context, alarmManager)
//        // now making the last
//        logD("about to set lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime ")
//        lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime(startTimeInMillisendForDb, activity_context, alarmManager, "alarm_start_time_to_search_db", "alarm_end_time_to_search_db", endTimeInMillisendForDb, LastAlarmUpdateDBReceiver())
//
////        lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime((startTimeInMillis - freq_in_min)+2000,activity_context, alarmManager, startTimeNow, startTimeNow, "form the lastPendingIntentWithMessageForDbOperations form", AlarmReceiver() )
//        alarmSetComplete = true
//           lifecycleScope.launch {
//               try {
//                   val newAlarm = AlarmData(
//                       first_value = startTimeInMillisendForDb,
//                       second_value = endTimeInMillisendForDb,
//                       freq_in_min = freq_in_min,
//                       isReadyToUse = alarmSetComplete,
//                       date_for_display = selected_date_for_display,
//                       start_time_for_display = start_time_for_display ,
//                       end_time_for_display = end_time_for_display,
//                       start_am_pm = start_am_pm ,
//                       end_am_pm = end_am_pm,
//                       freq_in_min_to_display = (freq_in_min/60000).toInt(),
//                       date_in_long =
//
//                   )
//                   val insertedId = alarmDao.insert(newAlarm)
//                   logD("Inserted alarm with ID: $insertedId")
//               } catch (e: Exception) {
//                   logD("Exception occurred when inserting in the db: $e")
//               }
//       }
//    }
//
}

fun logD(message:String):Unit{
    Log.d("AAA","-->$message")
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDocked() {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val selectedDate = datePickerState.selectedDateMillis?.let {
        convertMillisToDate(it)
    } ?: ""

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedDate,
            onValueChange = { },
            label = { Text("DOB") },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = !showDatePicker }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select date"
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        )

        if (showDatePicker) {
            Popup(
                onDismissRequest = { showDatePicker = false },
                alignment = Alignment.TopStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = 64.dp)
                        .shadow(elevation = 4.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    DatePicker(
                        state = datePickerState,
                        showModeToggle = false
                    )
                }
            }
        }
    }
}

@Composable
fun DatePickerFieldToModal(modifier: Modifier = Modifier) {
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var showModal by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = selectedDate?.let { convertMillisToDate(it) } ?: "",
        onValueChange = { },
        label = { Text("DOB") },
        placeholder = { Text("MM/DD/YYYY") },
        trailingIcon = {
            Icon(Icons.Default.DateRange, contentDescription = "Select date")
        },
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(selectedDate) {
                awaitEachGesture {
                    // Modifier.clickable doesn't work for text fields, so we use Modifier.pointerInput
                    // in the Initial pass to observe events before the text field consumes them
                    // in the Main pass.
                    awaitFirstDown(pass = PointerEventPass.Initial)
                    val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    if (upEvent != null) {
                        showModal = true
                    }
                }
            }
    )

    if (showModal) {
        DatePickerModal(
            onDateSelected = { selectedDate = it },
            onDismiss = { showModal = false }
        )
    }
}

fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}

