package com.example.trying_native.components_for_ui_compose

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Context
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import java.util.Calendar
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmData
import com.example.trying_native.logD
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.material3.DatePicker
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.lifecycleScope
import com.example.trying_native.AlarmLogic.AlarmsController
import com.example.trying_native.FirstLaunchAskForPermission.FirstLaunchAskForPermission
import com.example.trying_native.getDateForDisplay
import com.example.trying_native.notification.NotificationBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date


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


@SuppressLint("FlowOperatorInvokedInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmContainer(AlarmDao: AlarmDao, alarmManager: AlarmManager, context_of_activity: ComponentActivity, askUserForPermissionToScheduleAlarm:()->Unit) {


    val alarmsController = AlarmsController()


    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val fontSize = (screenHeight * 0.05f).value.sp

//    val coroutineScope = remember {CoroutineScope(SupervisorJob() + Dispatchers.Default)  }
    val coroutineScope = context_of_activity.lifecycleScope
    val uncancellableScope = remember {
        CoroutineScope(coroutineScope.coroutineContext + NonCancellable)
    }


    val askUserForPermission by remember { mutableStateOf(Settings.canDrawOverlays(context_of_activity)) }

    // Collect the Flow as State
    val alarms1 by produceState<List<AlarmData>>(initialValue = emptyList()) {
        withContext(Dispatchers.IO){
            AlarmDao.getAllAlarmsFlow().collect{
                value = it
            }
        }
    }
//    val alarms by AlarmDao.getAllAlarmsFlow().flowOn(Dispatchers.IO).collectAsStateWithLifecycle(initialValue = emptyList())
    var showTheDialogToTheUserToAskForPermission by remember { mutableStateOf(false) }
    // Setup clipboard manager
    val clipboardManager = LocalClipboardManager.current


    val snackbarHostState = remember { SnackbarHostState() }
    Box(
        modifier = Modifier
            .testTag("AlarmContainer")
            .fillMaxSize()
            .background(color = Color.Black)
    ) {
                                                                                                                                                                                                                                                                                                                                                    
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 106.dp)
                .zIndex(10f)
        ) { snackbarData ->
            Snackbar(
                snackbarData = snackbarData,
                shape = RoundedCornerShape(45.dp), // Apply the same rounding as your alarm card
                containerColor = Color.Blue, // Set background color
                contentColor = Color.White, // Set text color for contrast
                modifier = Modifier
//                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
            )
        }


        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(alarms1){indexOfIndividualAlarmInAlarm, individualAlarm ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(screenHeight / 4)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        // Copy the alarm message to clipboard
                                        logD(
                                            "the snack bar message is ->" + AnnotatedString(
                                                individualAlarm.message
                                            ).toString()
                                        )
//                                        Toast.makeText(this, "", Toast.LENGTH_SHORT).show()
                                        clipboardManager.setText(AnnotatedString((individualAlarm.message)))
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "Copied the alarm message",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                )
                            }
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
                                        coroutineScope.launch(Dispatchers.IO) {
                                            alarmsController.cancelAlarmByCancelingPendingIntent(
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
                                            coroutineScope.launch(Dispatchers.IO) {
                                                alarmsController.cancelAlarmByCancelingPendingIntent(
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
                                            // -- reset function abstract it away and change it --
                                            uncancellableScope.launch {
                                                logD("about to reset the alarm-+")
                                              val exception=  alarmsController.resetAlarms(alarmData = individualAlarm, alarmManager = alarmManager,
                                                    activityContext = context_of_activity, alarmDao = AlarmDao, coroutineScope = coroutineScope
                                                )
                                              logD("the exception form the resetAlarm is ->$exception")
                                                if(exception != null){
                                                        NotificationBuilder(context_of_activity,"error returned in creating multiple alarm ","execution returned exception in schedule multiple alarm  -->${exception}").showNotification()
                                                        logD("error in the schedulemultiple -->${exception}")
//                                                    }

                                                }
                                            }
//
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(Color(0xFF0eaae3))
                                ) {
                                    Text(if (individualAlarm.isReadyToUse) "remove" else "reset")
                                }
                            }
                        }
                    }
            }
        }
        if (showTheDialogToTheUserToAskForPermission){
            logD("displaying the dialog to ask user about the alarm")
            DialogToAskUserAboutAlarmUnified(onDismissRequest = {logD("Dismissed by new one");showTheDialogToTheUserToAskForPermission= false },
                onConfirmation = {startTimeHour, startTimeMinute, endTimeHour, endTimeMinute, startDateInMilliSec, endDateInMilliSec, frequency, alarmMessage ->
                    logD("got the value in the dialogToAskUserAboutAlarmUnified and it is startTimeHour:$startTimeHour StartMin:$startTimeMinute, endHour:$endTimeHour, endMin:$endTimeMinute, startDateMilliSecond:$startDateInMilliSec, endDateMilliSecond:$endDateInMilliSec, freq:$frequency, Message:$alarmMessage --- and now closing it")
                    try {
                        val dateForDisplay = if (getDateInHumanReadableFormat(startDateInMilliSec) == getDateInHumanReadableFormat(endDateInMilliSec)) getDateInHumanReadableFormat(startDateInMilliSec) else getDateInHumanReadableFormat(startDateInMilliSec)+"-->"+getDateInHumanReadableFormat(endDateInMilliSec)
                        val startTimeCal = Calendar.getInstance().apply { timeInMillis = startDateInMilliSec; set(Calendar.HOUR_OF_DAY, startTimeHour); set(Calendar.MINUTE, startTimeMinute)  }
                        val endTimeCal = Calendar.getInstance().apply { timeInMillis = endDateInMilliSec; set(Calendar.HOUR_OF_DAY, endTimeHour); set(Calendar.MINUTE, endTimeMinute)  }
                        // date_in_long is safe to pass as any as it is redundant and no one is using it
                        uncancellableScope.launch {
                            val exception =alarmsController.scheduleMultipleAlarms(alarmManager, dateForDisplay, startDateInMilliSec, alarmDao = AlarmDao, messageForDB = alarmMessage,
                                calendar_for_start_time = startTimeCal, calendar_for_end_time = endTimeCal, freq_after_the_callback = frequency, activity_context = context_of_activity
                            )
                            if (exception == null){
                                logD("the scheduleMultipleAlarms func ran fine and no exceptions ")
                                return@launch
                            }
                            NotificationBuilder(context = context_of_activity, title = "there is a error/Exception  in making new alarm", notificationText = exception.toString()).showNotification()
                            logD("there is a error/Exception  in making new alarm-->${exception}")
                        }
                    }catch (e: Exception){
                        NotificationBuilder(context = context_of_activity, title = "there is a error/Exception  in making new alarm", notificationText = e.toString()).showNotification()
                        logD(" there is a error/Exception  in making new alarm and was cought by the try catch-->${e} ")
                    }
                    showTheDialogToTheUserToAskForPermission= false
                },
            )
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
        }, context = context_of_activity)
        }
    }
}


@Composable
fun RoundPlusIcon(modifier: Modifier = Modifier, size: Dp , backgroundColor: Color = Color.Blue, onClick: () -> Unit, context:Context) {
//    var plusIconClicked by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        logD("is granted is $isGranted")
        // No need to handle permission result since we want onClick to run anyway
    }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .size(size)
            .zIndex(4f)
            .background(color = backgroundColor, shape = CircleShape)
            .clickable {
                coroutineScope.launch {
                    FirstLaunchAskForPermission(context).checkAndRequestPermissions()
                }
                coroutineScope.launch {
                    onClick()
                }
            },
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
    val alarmsController = AlarmsController()
    // Step state to determine whether we are showing the TimePicker or DatePicker
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val screenWidth = LocalConfiguration.current.screenWidthDp

    val screenHeight_normal = (screenHeight /1.4).dp
    val screenHeight_if_message_not_present = (screenHeight /1.23).dp
//    var showDatePickerModal by remember { mutableStateOf(false) }
    var showDatePickerModal by remember { mutableStateOf(false) }
    var messageInAlarm by remember { mutableStateOf("") }
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
//    val coroutineScope = rememberCoroutineScope()
    val coroutineScope = activity_context.lifecycleScope
    val uncancellableScope = remember {
        CoroutineScope(coroutineScope.coroutineContext + NonCancellable)
    }


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
                        } else {
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

                            Spacer(modifier = Modifier.height(24.dp)) // Space between the time and the button
                            MessageInput(
                                message = messageInAlarm,
                                onMessageChange = { messageInAlarm = it }
                            )
                        }


                    }
                    // some checks like end time is bigger than start time, (do convert them in milliseconds with  selected date)
                    // in the dialog box declare time ---NOW--- (milli) and date millisecond, and pass them to the both the
                    // components then we can escape form the null checks  and on confirm replace them

               5->{
                   val dateInMilliSec = pickedDateState
                    if ( dateInMilliSec != null){
                        val startTime_obj_form_calender:Calendar = Calendar.getInstance().apply {
                            timeInMillis = dateInMilliSec
                            set(Calendar.HOUR_OF_DAY, startTime?.hour!!)
                            set(Calendar.MINUTE, startTime?.minute!! )
                        }



                        var endTime_obj_form_calender = Calendar.getInstance().apply {
                            timeInMillis = dateInMilliSec
                            set(Calendar.HOUR_OF_DAY, endTime?.hour!!)
                            set(Calendar.MINUTE, endTime?.minute!! )
                        }

                        val date = pickedDateState?.let {
                            java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        }?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        uncancellableScope.launch {
                            try {
                               val exception= alarmsController.scheduleMultipleAlarms(alarmManager, activity_context = activity_context, alarmDao = alarmDao,
                                calendar_for_start_time = startTime_obj_form_calender, calendar_for_end_time = endTime_obj_form_calender, freq_after_the_callback = freq_returned_by_user,
                                selected_date_for_display =  date!!, date_in_long = dateInMilliSec,   messageForDB = messageInAlarm  )
                                if (exception != null){
                                    logD("there is a error while scheduling alarm-->${exception}")
                                    NotificationBuilder(activity_context,"the error was -->${exception}", "Error occurred in creating alarm", ).showNotification()
                                }
                            }catch (e:Exception){
                                logD("there is a error while scheduling alarm-->${e}")
                                NotificationBuilder(activity_context, "Error occurred in creating alarm", "the error was -->${e}").showNotification()
                            }
                        }
                    }
                   onDismissRequest()
               }
                }
            }
        }
    }
}


enum class InputPickerType { START, END }
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogToAskUserAboutAlarmUnified(
    onDismissRequest: () -> Unit,
    onConfirmation: (startTimeHour: Int, startTimeMinute: Int, endTimeHour: Int, endTimeMinute: Int, startDateInMilliSec: Long, endDateInMilliSec: Long, frequency: Int, alarmMessage: String) -> Unit,
) {
    // here user will get the time to be current and if they want it to be diff then they can just make
    // it and would have to set the   end time
    val calInstance = Calendar.getInstance()
    var startHour by remember { mutableStateOf(calInstance.get(Calendar.HOUR_OF_DAY)) }
    var startMinute by remember { mutableIntStateOf(calInstance.get(Calendar.MINUTE)) }
    var endHour by remember { mutableIntStateOf(calInstance.get(Calendar.HOUR_OF_DAY ) ) }
    var endMinute by remember { mutableIntStateOf(calInstance.get(Calendar.MINUTE)) }
    var frequency by remember { mutableIntStateOf(2) }
    var alarmMessage by remember { mutableStateOf("") }
    var startDateToView by remember { mutableStateOf(getDateInHumanReadableFormat(calInstance.timeInMillis)) }
    var endDateToView by remember { mutableStateOf(getDateInHumanReadableFormat(calInstance.timeInMillis)) }
    var startDateToReturn by remember { mutableLongStateOf(calInstance.timeInMillis) }
    var endDateToReturn by remember { mutableLongStateOf(calInstance.timeInMillis) }

    var showTimePickerFor by remember { mutableStateOf<InputPickerType?>(null) }
    var showDatePickerFor by remember { mutableStateOf<InputPickerType?>(null) }

    val areInputsValid by remember(frequency, startDateToReturn, endDateToReturn) {
        derivedStateOf {
            areThePramsValid(frequency, startDateToReturn, endDateToReturn, startHour, startMinute, endHour, endMinute )
        }
    }
    val dismissButtonColor by remember(areInputsValid) {
        derivedStateOf {
            if (areInputsValid) Color(0xFF0D388D) else Color.Red
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "Set alarm", style = MaterialTheme.typography.headlineSmall)

                // Start time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Start time")
                    InputPickerField(getFromattedTimeToShowUser(startHour, startMinute), onClick = {
                        showTimePickerFor = InputPickerType.START
                        logD("clicked the input for the start time and the activeTimePicker is $showTimePickerFor")
                        }
                    )
                }
                // End time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "End time")
                    // This would ideally trigger a TimePicker

                    InputPickerField(getFromattedTimeToShowUser(endHour, endMinute), onClick = {
                        showTimePickerFor = InputPickerType.END
                        logD("clicked the input for the start time and the activeTimePicker is $showTimePickerFor")
                    }
                    )
                }
                // Start date and End date (assuming they are the same in this context)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Start date")
                        InputPickerField(startDateToView, onClick = {
                            showDatePickerFor = InputPickerType.START
                            logD(" the start date for the date picker is $showDatePickerFor")
                        }
                        )
                    }
                    Column {
                        Text(text = "End date", modifier = Modifier.padding(start = 20.dp))
                        InputPickerField(endDateToView, onClick = {
                            showDatePickerFor = InputPickerType.END
                            logD(" the end date for the date picker is $showDatePickerFor")
                        }
                        )
                    }
                }
                // Frequency
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Frequency")
                    OutlinedTextField(
                        value = frequency.toString(),
                        onValueChange = {newFreq->
                            val newFreqquency = newFreq.toIntOrNull()
                            logD("the new freq is ->$newFreqquency")
                            if (newFreqquency == null){
                                frequency = 0
                            }else{
                                frequency = newFreqquency
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(120.dp).background(Color.White).border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline))
                    )
                }
                // Alarm message/name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Message", modifier = Modifier.padding(end = 30.dp))
                    OutlinedTextField(
                        value = alarmMessage,
                        onValueChange = { alarmMessage = it },
                        modifier = Modifier.weight(1f).background(Color.White).border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline))   // Takes remaining space
                    )
                }
                Spacer(modifier = Modifier.height(8.dp)) // Spacer before buttons
                // Dismiss and Set buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Dismiss")
                    }

                    Button(
                        onClick = {
                            if(areInputsValid){
                                logD("the inputs are valid")
                                onConfirmation(
                                    startHour,
                                    startMinute,
                                    endHour,
                                    endMinute,
                                    startDateToReturn,
                                    endDateToReturn,
                                    frequency,
                                    alarmMessage
                                )
                            }else{logD("the freq is $frequency and it is <1 so can't allow you to click")}
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = dismissButtonColor
                        )
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }

    if (showTimePickerFor!= null){
        logD("in the time picker in the dialog to ask user about alarm")
        TimePickerDialog(
            onDismissRequest = {    showTimePickerFor = null  },
            onTimeSelected = { newTime ->
                when (showTimePickerFor) {
                    InputPickerType.START -> {
                        logD("changed the start time to $newTime or Hour:${newTime.hour} and Min:${newTime.minute}")
                        startHour = newTime.hour
                        startMinute = newTime.minute
                    }
                    InputPickerType.END -> {
                        logD("changed the end time to $newTime or Hour:${newTime.hour} and Min:${newTime.minute}")
                        endHour = newTime.hour
                        endMinute = newTime.minute
                    }
                    null -> { /* Should not happen */ }
                }
                showTimePickerFor = null
            },
            initialTime = when (showTimePickerFor) {
                InputPickerType.START -> LocalTime.of(startHour, startMinute)
                InputPickerType.END -> LocalTime.of(endHour, endMinute)
                else -> LocalTime.now()
            }
        )
    }
    if (showDatePickerFor !== null){
        logD("in the date picker in the dialog to ask user about alarm")
        DatePickerDialog(
            onDismissRequest = { showDatePickerFor = null },
            onDateSelected = { newDateMillis ->
                when (showDatePickerFor) {
                    InputPickerType.START -> {
                        startDateToReturn = newDateMillis
                        startDateToView = getDateInHumanReadableFormat(newDateMillis)
                        // cause most often than not my start date and the end date are the same if the
                        // user wants a diff one they can change it
                        endDateToReturn = newDateMillis
                        endDateToView = getDateInHumanReadableFormat(newDateMillis)
                        logD("the start date is $startDateToReturn and end date also set (as this is for start time) is $endDateToReturn")
                        logD("the human readable date in start date picker is start date $startDateToView and end date $endDateToView")
                    }
                    InputPickerType.END -> {
                        endDateToReturn = newDateMillis
                        endDateToView = getDateInHumanReadableFormat(newDateMillis)
                        logD("the human readable date in end date picker and the end date is $endDateToView")
                        logD("the end date (in end date picker) is $endDateToReturn ")
                    }
                    null -> { /* Should not happen */ }
                }
                showDatePickerFor = null
            },
            initialDate = when (showDatePickerFor) {
                InputPickerType.START -> startDateToReturn
                InputPickerType.END -> endDateToReturn
                else -> calInstance.timeInMillis
            }
        )
    }
}



@Composable
fun InputPickerField(valueOfSelectedInput: String, onClick: () -> Unit){
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .padding(start = 16.dp)
            .indication(indication = null, interactionSource =interactionSource )
            .clickable(indication = null, interactionSource =interactionSource) {
                onClick()
            },
        contentAlignment = Alignment.CenterEnd
    ) {
        // Use a non-clickable Surface styled like TextField
        Surface(
            modifier = Modifier
                .width(120.dp)
                .indication(indication = null, interactionSource =interactionSource )
                .height(56.dp),
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 12.dp)
                    .indication(indication = null, interactionSource =interactionSource ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(valueOfSelectedInput)
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    initialTime: LocalTime
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = "Select Time") },
        text = {
            TimePicker(state = timePickerState)
        },
        confirmButton = {
            Button(onClick = {
                val newTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                onTimeSelected(newTime)
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    onDateSelected: (Long) -> Unit, // Returns selected date in milliseconds
    initialDate: Long
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
    )

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(onClick = {
                datePickerState.selectedDateMillis?.let {
                    onDateSelected(it)
                }
                onDismissRequest()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}




@Composable
fun MessageInput(
    message: String,
    onMessageChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = message,
        onValueChange = onMessageChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        label = { Text("Enter message") },
        singleLine = true
    )
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

/** this fun will get you the date once you give it the time */
private  fun getDateInHumanReadableFormat(t:Long): String{
    return SimpleDateFormat(" dd-MM-yyyy ", Locale.getDefault()).format(Date(t))
}

/**
 * checks the params and if we are safe to send it to the setMultipleAlarms
 * @param startTimeHour it is for the 24 hours format and not  12 hours one
 * @param endTimeHour it is for the 24 hours format and not  12 hours one
 * @return true if  params are valid
 */
private  fun areThePramsValid(frequency: Int, startDate: Long, endDate: Long, startTimeHour: Int, startTimeMinute: Int, endTimeHour: Int, endTimeMinute: Int): Boolean{
    if (frequency < 1) return false
    val startTimeMillisecond = Calendar.getInstance().apply { timeInMillis = startDate; set(Calendar.HOUR_OF_DAY, startTimeHour); set(Calendar.MINUTE, startTimeMinute); }.timeInMillis
    val endTimeMillisecond =    Calendar.getInstance().apply {timeInMillis = endDate  ; set(Calendar.HOUR_OF_DAY, endTimeHour); set(Calendar.MINUTE, endTimeMinute) }.timeInMillis
    logD("the startTimeMillisecond in millisecond is $startTimeMillisecond and the endTimeMillisecond is $endTimeMillisecond and startTime < endTime ${startTimeMillisecond < endTimeMillisecond}")
    return startTimeMillisecond < endTimeMillisecond
}

private  fun  getFromattedTimeToShowUser(Hour:Int, Minute:Int):String{
    return String.format(Locale.getDefault() , "%02d:%02d", Hour, Minute)
}