package com.example.trying_native.Components_for_ui_compose

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Calendar
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.trying_native.cancelAlarmByCancelingPendingIntent
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmData
import com.example.trying_native.logD
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Popup
import com.example.trying_native.AlarmReceiver
import com.example.trying_native.LastAlarmUpdateDBReceiver
import com.example.trying_native.lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import java.util.Date

@Composable
fun AdvancedTimePickerDialog(
    title: String = "Select Time",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    toggle: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier =
            Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface
                ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = title,
                    style = MaterialTheme.typography.labelMedium
                )
                content()
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                ) {
                    toggle()
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = onConfirm) { Text("OK") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun NumberField(
    placeHolderText: String,
    onFrequencyChanged: (String) -> Unit
) {
    // State to hold the current text input
    var text by remember { mutableStateOf("") }

    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            text = newText
            onFrequencyChanged(newText) // Callback to handle the input change
        },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        placeholder = {
            Text(text = placeHolderText)
        },
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmContainer(AlarmDao: AlarmDao, alarmManager: AlarmManager, context_of_activity: ComponentActivity, askUserForPermissionToScheduleAlarm:()->Unit) {

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val fontSize = (screenHeight * 0.05f).value.sp
    val coroutineScope = rememberCoroutineScope()
    var askUserForPermission by remember { mutableStateOf(Settings.canDrawOverlays(context_of_activity)) }

    // Collect the Flow as State
    val alarms by AlarmDao.getAllAlarmsFlow().collectAsState(initial = emptyList())
    var showTheDialogToTheUserToAskForPermission by remember { mutableStateOf(false) }


    Box(
        modifier = Modifier
            .testTag("AlarmContainer")
            .fillMaxSize()
            .background(color = Color.Black)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(alarms){indexOfIndividualAlarmInAlarm, individualAlarm ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(screenHeight / 4)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(45.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = if (!individualAlarm.isReadyToUse) Color(0xFF666b75) else Color(
                                        0xFF0D388C
                                    )

                                )
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Start time
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = individualAlarm.start_time_for_display,
                                        fontSize = (fontSize / 1.2),
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    Text(
                                        text = individualAlarm.start_am_pm,
                                        fontSize = (fontSize / 2.2),
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }

                                // Middle text
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "-->",
                                        fontSize = (fontSize / 1.5),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // End time
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = individualAlarm.end_time_for_display,
                                        fontSize = (fontSize / 1.2),
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    Text(
                                        text = individualAlarm.end_am_pm,
                                        fontSize = (fontSize / 2.3),
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "after every ${individualAlarm.freq_in_min_to_display} min",
                                    fontSize = (fontSize / 2.7),
                                    fontWeight = FontWeight.W600,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Button(
                                    onClick = {
                                        logD("++==${individualAlarm.id} ---- $indexOfIndividualAlarmInAlarm ")
                                        coroutineScope.launch {
                                            cancelAlarmByCancelingPendingIntent(
                                                context_of_activity = context_of_activity,
                                                startTime = individualAlarm.first_value,
                                                endTime = individualAlarm.second_value,
                                                frequency_in_min = individualAlarm.freq_in_min,
                                                alarmDao = AlarmDao,
                                                alarmManager = alarmManager,
                                                delete_the_alarm_from_db = true
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(Color(0xFF0eaae3))
                                ) {
                                    Text("delete")
                                }
                                Text(
                                    text = "On: ${individualAlarm.date_for_display}",
                                    textAlign = TextAlign.Right,
                                    fontSize = (fontSize / 2.43),
                                    fontWeight = FontWeight.W600,
                                    modifier = Modifier.padding(vertical = screenHeight / 74),
                                )
                                Button(
                                    onClick = {
                                        if (individualAlarm.isReadyToUse){
                                            coroutineScope.launch {
                                                cancelAlarmByCancelingPendingIntent(
                                                    context_of_activity = context_of_activity,
                                                    startTime = individualAlarm.first_value,
                                                    endTime = individualAlarm.second_value,
                                                    frequency_in_min = individualAlarm.freq_in_min,
                                                    alarmDao = AlarmDao,
                                                    alarmManager = alarmManager,
                                                    delete_the_alarm_from_db = false
                                                )
                                            }
                                        }
                                       // -------------------------
                                        else {
                                            val calenderInstance = Calendar.getInstance()
                                            val cal1 = calenderInstance.apply { timeInMillis = Calendar.getInstance().timeInMillis }
                                            val cal2 = calenderInstance.apply { timeInMillis = individualAlarm.date_in_long }
                                            // dates in db and the current
                                            val areTheDatesSame = cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                                                    cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                                                    cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)

                                            logD("are the both the date same --> $areTheDatesSame, ")
                                            var startTime_obj_form_calender:Calendar = calenderInstance.apply {
                                                timeInMillis = individualAlarm.first_value
                                            }
                                            var endTime_obj_form_calender = calenderInstance.apply {
                                                timeInMillis = individualAlarm.second_value
                                            }
                                            var date : Long
                                            if (areTheDatesSame){
                                                date = individualAlarm.date_in_long
                                            }else  {
                                                date = cal1.timeInMillis
                                            }
                                            coroutineScope.launch {
                                                scheduleMultipleAlarms(alarmManager, activity_context = context_of_activity, alarmDao = AlarmDao,
                                                    calendar_for_start_time = startTime_obj_form_calender, calendar_for_end_time = endTime_obj_form_calender, freq_after_the_callback = individualAlarm.freq_in_min_to_display,
                                                    selected_date_for_display =  individualAlarm.date_for_display , date_in_long= date, coroutineScope = this, is_alarm_ready_to_use = true , is_this_func_call_to_update_an_existing_alarm = true, new_is_ready_to_use = true  )
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(Color(0xFF0eaae3))
                                ) {
                                    Text(if (individualAlarm.isReadyToUse) "remove" else "reset")
                                }
                            }
                        }
                    }
//                }
            }
        }
        if (showTheDialogToTheUserToAskForPermission){
            DialogToAskUserAboutAlarm(onDismissRequest = {
                logD("DialogToAskUserAboutAlarm is about to be set to false")
                showTheDialogToTheUserToAskForPermission = false }, onConfirmation = {a,b, c ->logD("in the confirm ${a.hour}:${a.minute},--||-- ${c}"); logD("got the confirmation in DialogToAskUserAboutAlarm")}
            , activity_context = context_of_activity, alarmDao = AlarmDao, alarmManager = alarmManager)
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = screenHeight / 15)
                .testTag("RoundPlusIcon")
        ) {
            RoundPlusIcon(size = screenHeight/10, onClick = {showTheDialogToTheUserToAskForPermission = !showTheDialogToTheUserToAskForPermission;
            if(askUserForPermission == true){
                askUserForPermissionToScheduleAlarm()
            }
        })
        }
    }
}


@Composable
fun RoundPlusIcon(modifier: Modifier = Modifier, size: Dp , backgroundColor: Color = Color.Blue, onClick: () -> Unit) {
//    var plusIconClicked by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size)
            .zIndex(4f)
            .background(color = backgroundColor, shape = CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add",
            modifier = Modifier.size(size / 2)
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun timePicker_without_dialog(onConfirm: (TimePickerState) -> Unit, onDismiss: () -> Unit, nextButton:String = "Next", text_at_the_top:String, mistake_message:String = "" ){

    var user_mistake_message_show by remember { mutableStateOf(mistake_message) }
    val currentTime = Calendar.getInstance()
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val fontSize = (screenHeight * 0.028f).value.sp


    // -----------------
    // now just need to handle the function (just like the previous one)
    // or when the user clicks the next or whatever button just return the timePickerState
    //  --------done -----------


val timePickerState = rememberTimePickerState(
    initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
    initialMinute = currentTime.get(Calendar.MINUTE),
    is24Hour = false, // allow user to choose it in future , pass from env or setting from now
)
    logD("time picker -->${timePickerState.hour}:${timePickerState.minute}")

Column {

    Text(text_at_the_top, modifier = Modifier.padding(vertical = screenHeight/53, horizontal = screenWidth/19),
        fontSize = fontSize, fontWeight = FontWeight.W500, )

    TimePicker(modifier = Modifier
        .padding(horizontal = screenWidth / 22)
        .testTag("TimePickerTestTag"),
        state = timePickerState,
    )
    if( user_mistake_message_show != ""){
        Text(user_mistake_message_show, modifier = Modifier.padding(vertical = screenHeight/53, horizontal = screenWidth/19),
            fontSize = (screenHeight * 0.0206f).value.sp, fontWeight = FontWeight.W500, color = Color.Red  )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Button(onClick = onDismiss) {
        Text("Dismiss")
    }
        Button(onClick = { onConfirm(timePickerState) }, modifier = Modifier.testTag("NextButtonInTimePicker")) {
            Text(nextButton)
        }
    }
  }
}

fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModalByMe(
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    textForNextButton:String,
    textForDismissButton:String,
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                var dateInMilli = datePickerState.selectedDateMillis
                if (dateInMilli != null){
                    onDateSelected(dateInMilli)
                }else{
                    logD("in DatePickerModalByMe dateInMilli is null ")
                    onDismiss()
                }
            }) {
                Text(textForNextButton)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(textForDismissButton)
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}


@SuppressLint("SuspiciousIndentation", "CoroutineCreationDuringComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogToAskUserAboutAlarm(
    alarmManager: AlarmManager,  activity_context: ComponentActivity, alarmDao: AlarmDao,
    onDismissRequest: () -> Unit,
    onConfirmation: (startTime: TimePickerState, endTime:TimePickerState, datePickerState: Long) -> Unit,
) {
    // Step state to determine whether we are showing the TimePicker or DatePicker
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val screenWidth = LocalConfiguration.current.screenWidthDp

    val screenHeight_normal = (screenHeight /1.4).dp
    val screenHeight_if_message_not_present = (screenHeight /1.23).dp
//    var showDatePickerModal by remember { mutableStateOf(false) }
    var showDatePickerModal by remember { mutableStateOf(false) }

    // Variables to store the picked time and date
    var startTime: TimePickerState? by remember { mutableStateOf(null) }
    var endTime: TimePickerState? by remember { mutableStateOf(null) }
    var pickedDateState: Long? by remember { mutableStateOf(null) }
    var a  by remember { mutableStateOf(0) }
    var freq_returned_by_user  by remember { mutableStateOf(0) }
    var mistake_message_for_func  by remember { mutableStateOf("") }

    val cardHeight = if (mistake_message_for_func.isEmpty()) {
        screenHeight_normal
    } else {
        screenHeight_if_message_not_present
    }
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = { onDismissRequest() },
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("DialogToAskUserAboutAlarm")
                .height((cardHeight))
                .width(screenWidth.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                logD("value of a(show the modal to the user) is $a")

                when (a) {
                    0 -> {
                        timePicker_without_dialog(
                            onConfirm = { timeState ->
                                startTime = timeState
                                a = 1 // Move to the next step (show DatePicker)
                                logD("clicker next")
                            },
                            onDismiss = onDismissRequest,
                            nextButton = "Next",
                            text_at_the_top = "Starting time",
                            mistake_message = mistake_message_for_func
                        )
//                        freq_without_dialog(onDismiss = onDismissRequest, nextButton = "Confirm", onConfirm = {freq_returned ->logD("frequency returned ->$freq_returned")} )
                    }
                    1->{
                        timePicker_without_dialog(
                            onConfirm = { timeState ->
                                endTime = timeState
                                a ++ // Move to the next step (show DatePicker)
                                logD("clicker next")
                            },
                            onDismiss = onDismissRequest,
                            nextButton = "Next",
                            text_at_the_top = "Ending time",
                            mistake_message = mistake_message_for_func
                        )
                    }

                    2 -> {
                        showDatePickerModal = true

                        if (showDatePickerModal) {
                            DatePickerModalByMe(
                                onDateSelected = { dateState ->
                                    showDatePickerModal = false // Dismiss DatePicker
                                    onConfirmation(startTime!!, endTime!!, dateState)
                                   // Move to the next state
                                    pickedDateState =dateState
                                    a++
                                    logD("value of a after running onConfirmation is $a")
                                },
                                onDismiss = {
                                    showDatePickerModal = false
                                    onDismissRequest()
                                    logD("on dismiss function is running in DatePickerModalByMe")
                                },
                                textForNextButton = "Confirm",
                                textForDismissButton = "Cancel"
                            )
                        } else {
                            // When 'a' is set to 2, trigger DatePicker display
                        }
                    }

                    3->{
                        logD("in the a==3 camp")
                        val dateInMilliSec = pickedDateState
                        if (dateInMilliSec==null){
                            onDismissRequest()
                            logD("in the a->3 dateInMilliSec is null")
                        }else {

                            // something is wrong with the logic here as If I want the time to be form the
                            // 11:45 to 12:15 of this date it will return false

                            var startTime_obj_form_calender: Calendar =
                                Calendar.getInstance().apply {
                                    timeInMillis = dateInMilliSec
                                    set(Calendar.HOUR_OF_DAY, startTime?.hour!!)
                                    set(Calendar.MINUTE, startTime?.minute!!)
                                }
                            var endTime_obj_form_calender = Calendar.getInstance().apply {
                                timeInMillis = pickedDateState!!
                                set(Calendar.HOUR_OF_DAY, endTime?.hour!!)
                                set(Calendar.MINUTE, endTime?.minute!!)
                            }
                            logD("in the a==3.1 camp")


                            // since alarms are being set on the same day( eg if it is on 03 (am) and end one has to be 15or .. ( pm) if it
                            // reached 2 am it is not possible as the time picker not allows it so a bad input on the  user part, as alarm cant end beofr it starts )
                            // it should be the the start time is bigger than the end time

                            if (startTime_obj_form_calender.timeInMillis >= endTime_obj_form_calender.timeInMillis) {
                                logD("${startTime_obj_form_calender.timeInMillis},---, ${endTime_obj_form_calender.timeInMillis} ")
                                val formatter =
                                    SimpleDateFormat("h:mm a MM/dd/yy", Locale.getDefault())
                                val startTimeToShowUser =
                                    formatter.format(startTime_obj_form_calender.time)
                                val endTimeToShowUser =
                                    formatter.format(endTime_obj_form_calender.time)
                                mistake_message_for_func =
                                    " Your start Time($startTimeToShowUser) should be bigger than the end time ($endTimeToShowUser). we can't set that alarm "
                                a = 0
                                logD("in the a==3.2 camp")
                            } else {
                                a++
                                logD("in the a==3.21 camp")
                            }
                        }
                    }
                    4 ->{
                        logD("in the a==4 camp")
                        // freq and example of it
                        val dateInMilliSec = pickedDateState
                        if (dateInMilliSec != null){
                            var startTime_obj_form_calender:Calendar = Calendar.getInstance().apply {
                                timeInMillis = dateInMilliSec
                                set(Calendar.HOUR_OF_DAY, startTime?.hour!!)
                                set(Calendar.MINUTE, startTime?.minute!! )
                            }
                            var endTime_obj_form_calender = Calendar.getInstance().apply {
                                timeInMillis = dateInMilliSec
                                set(Calendar.HOUR_OF_DAY, endTime?.hour!!)
                                set(Calendar.MINUTE, endTime?.minute!! )
                            }
                            freq_without_dialog(onDismiss = onDismissRequest, nextButton = "Set the alarm", onConfirm = {freq_returned ->
                                logD("frequency returned ->$freq_returned..........")

                                ; freq_returned_by_user = freq_returned; a++ },
                                calender_instance_at_start_time = startTime_obj_form_calender, calender_instance_at_end_time = endTime_obj_form_calender  )
                        }


                    }
                    // some checks like end time is bigger than start time, (do convert them in milliseconds with  selected date)
                    // in the dialog box declare time ---NOW--- (milli) and date millisecond, and pass them to the both the
                    // components then we can escape form the null checks  and on confirm replace them

               5->{
                   val dateInMilliSec = pickedDateState
                    if ( dateInMilliSec != null){
                        var startTime_obj_form_calender:Calendar = Calendar.getInstance().apply {
                            timeInMillis = dateInMilliSec
                            set(Calendar.HOUR_OF_DAY, startTime?.hour!!)
                            set(Calendar.MINUTE, startTime?.minute!! )
                        }



                        var endTime_obj_form_calender = Calendar.getInstance().apply {
                            timeInMillis = dateInMilliSec
                            set(Calendar.HOUR_OF_DAY, endTime?.hour!!)
                            set(Calendar.MINUTE, endTime?.minute!! )
                        }

                        var date = pickedDateState!!?.let {
                            java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        }?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        coroutineScope.launch {
                            scheduleMultipleAlarms(alarmManager, activity_context = activity_context, alarmDao = alarmDao,
                                calendar_for_start_time = startTime_obj_form_calender, calendar_for_end_time = endTime_obj_form_calender, freq_after_the_callback = freq_returned_by_user,
                                selected_date_for_display =  date!!, date_in_long = dateInMilliSec, coroutineScope = this, is_alarm_ready_to_use = true, new_is_ready_to_use = false  )
                        }
                    }

                   onDismissRequest()
               }
                }
            }
        }
    }
}


@SuppressLint("DefaultLocale")
@Composable
fun freq_without_dialog(
    onDismiss: () -> Unit,
    nextButton: String,
    onConfirm: (text_entered_by_user: Int) -> Unit,
    calender_instance_at_start_time: Calendar,
    calender_instance_at_end_time: Calendar
) {
    var text_entered_by_user by remember { mutableStateOf(0) }
    var numberToDisplay by remember { mutableStateOf(Array(5) { "" }) }

    val screenHeight = LocalConfiguration.current.screenHeightDp
    val screenWidth = LocalConfiguration.current.screenWidthDp

    // Update numberToDisplay whenever text_entered_by_user changes
    LaunchedEffect(text_entered_by_user) {
        if (text_entered_by_user > 0) {
            val updatedTimes = Array(5) { "" }
            val tempStartTime = calender_instance_at_start_time.clone() as Calendar

            for (i in 1..4) {
                if (tempStartTime.timeInMillis >= calender_instance_at_end_time.timeInMillis) {
                    updatedTimes[i - 1] = String.format(
                        "%02d:%02d",
                        calender_instance_at_end_time.get(Calendar.HOUR_OF_DAY),
                        calender_instance_at_end_time.get(Calendar.MINUTE)
                    )
                    break
                }
                updatedTimes[i - 1] = String.format(
                    "%02d:%02d",
                    tempStartTime.get(Calendar.HOUR_OF_DAY),
                    tempStartTime.get(Calendar.MINUTE)
                )
                tempStartTime.add(Calendar.MINUTE, text_entered_by_user)
            }
            numberToDisplay = updatedTimes
        } else {
            numberToDisplay = Array(5) { "" }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            "Enter frequency -->",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                vertical = (screenHeight / 99).dp,
                horizontal = (screenWidth / 93).dp
            )
        )

        NumberField("Enter the frequency(in min)")  { changed_freq_string ->
            text_entered_by_user = changed_freq_string.toIntOrNull() ?: -0
        }

        if (text_entered_by_user > 0) {
            Text(
                "Alarms will go on --> ${numberToDisplay.joinToString(", ")}....",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(
                    vertical = (screenHeight / 99).dp,
                    horizontal = (screenWidth / 83).dp
                )
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 7.dp, start = 8.dp, end = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(onClick = onDismiss) {
            Text("Dismiss")
        }
        Button(
            onClick = { if (text_entered_by_user > 0) onConfirm(text_entered_by_user) }
        ) {
            Text(nextButton)
        }
    }
}




 fun scheduleAlarm(triggerTime: Long, alarmManager:AlarmManager, componentActivity: ComponentActivity) {
    logD( "Clicked on the schedule alarm func")
    var triggerTime_1 = triggerTime
    val intent = Intent(componentActivity, AlarmReceiver::class.java)
    intent.putExtra("last_alarm_info1","from the schedule alarm function")
    logD("Trigger time in the scheduleAlarm func is --> $triggerTime_1 ")
    intent.putExtra("triggerTime", triggerTime_1)
    val pendingIntent = PendingIntent.getBroadcast(componentActivity, triggerTime.toInt(), intent, PendingIntent.FLAG_MUTABLE)
    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime_1, pendingIntent)
// this should fix it as I changed FLAG_IMUTABLE to FLAG_MUTABLE
}





 suspend fun scheduleMultipleAlarms(alarmManager: AlarmManager, selected_date_for_display:String, date_in_long: Long, coroutineScope: CoroutineScope, is_alarm_ready_to_use:Boolean,
                                    calendar_for_start_time:Calendar, calendar_for_end_time:Calendar, freq_after_the_callback:Int, activity_context:ComponentActivity, alarmDao:AlarmDao,
                                    is_this_func_call_to_update_an_existing_alarm: Boolean = false , new_is_ready_to_use:Boolean                   )

 {
    // should probably make some checks like if the user ST->11:30 pm today and end time 1 am tomorrow (basically should be in a day)
     var startTimeInMillis = calendar_for_start_time.timeInMillis
    val startTimeInMillisendForDb= startTimeInMillis
    val start_time_for_display = SimpleDateFormat("hh:mm", Locale.getDefault()).format(calendar_for_start_time.time)
    val start_am_pm = SimpleDateFormat("a", Locale.getDefault()).format(calendar_for_start_time.time).trim()

    var endTimeInMillis = calendar_for_end_time.timeInMillis
    val endTimeInMillisendForDb= endTimeInMillis
    val end_time_for_display = SimpleDateFormat("hh:mm", Locale.getDefault()).format(calendar_for_end_time.time)
    val end_am_pm =  SimpleDateFormat("a", Locale.getDefault()).format(calendar_for_start_time.time).trim()

    logD(" \n\n am_pm_start_time-->$start_time_for_display $start_am_pm ; endtime-->$end_time_for_display $end_am_pm")
    var freq_in_milli : Long
     freq_in_milli = freq_after_the_callback.toLong()
    var freq_in_min = freq_in_milli * 60000
    logD("startTimeInMillis --$startTimeInMillis, endTimeInMillis--$endTimeInMillis,, equal?-->${startTimeInMillis==endTimeInMillis} ::--:: freq->$freq_in_min")
    var i=0

    while (startTimeInMillis <= endTimeInMillis){
        logD("round $i")
        scheduleAlarm(startTimeInMillis,alarmManager, activity_context)
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

//        lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime((startTimeInMillis - freq_in_min)+2000,activity_context, alarmManager, startTimeNow, startTimeNow, "form the lastPendingIntentWithMessageForDbOperations form", AlarmReceiver() )
 if (!is_this_func_call_to_update_an_existing_alarm ){
     withContext(Dispatchers.IO) {
         logD("here to  insert a new one")
         try {
             val newAlarm = AlarmData(
                 first_value = startTimeInMillisendForDb,
                 second_value = endTimeInMillisendForDb,
                 freq_in_min = freq_in_min,
                 isReadyToUse = is_alarm_ready_to_use,
                 date_for_display = selected_date_for_display,
                 start_time_for_display = start_time_for_display,
                 end_time_for_display = end_time_for_display,
                 start_am_pm = start_am_pm,
                 end_am_pm = end_am_pm,
                 freq_in_min_to_display = (freq_in_min / 60000).toInt(),
                 date_in_long = date_in_long
             )
             val insertedId = alarmDao.insert(newAlarm)
             logD("Inserted alarm with ID: $insertedId")
         } catch (e: Exception) {
             logD("Exception occurred when inserting in the db: $e")
         }
     }
   }
     else{
         logD("here to update the alarm---------------------------------------")
    alarmDao.updateReadyToUse(
        firstValue = startTimeInMillisendForDb,
        secondValue = endTimeInMillisendForDb,
        freqInMin = freq_in_min,
        dateInLong = date_in_long,
        isReadyToUse = new_is_ready_to_use
    )
     }
}
