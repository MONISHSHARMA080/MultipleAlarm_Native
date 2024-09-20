package com.example.trying_native.Components_for_ui_compose

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.ui.platform.testTag
import com.example.trying_native.AlarmReceiver
import com.example.trying_native.LastAlarmUpdateDBReceiver
import com.example.trying_native.lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime
import java.time.format.DateTimeFormatter

@Composable
fun Button_for_alarm(
    name: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = { onClick() }, // Trigger the passed function when the button is clicked
        modifier = modifier
    ) {
        Text(text = name)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialExample(
    onConfirm: (TimePickerState) -> Unit,
    onDismiss: () -> Unit,
) {
    val currentTime = Calendar.getInstance()

    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
        initialMinute = currentTime.get(Calendar.MINUTE),
        is24Hour = true,
    )

    Column {
        TimePicker(
            state = timePickerState,
        )
        Button(onClick = onDismiss) {
            Text("Dismiss picker")
        }
        Button(onClick = { onConfirm(timePickerState) }) {
            Text("Confirm selection")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialExample_2(
    onConfirm: (TimePickerState) -> Unit,
    onDismiss: () -> Unit,
) {

    val currentTime = Calendar.getInstance()

    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
        initialMinute = currentTime.get(Calendar.MINUTE),
        is24Hour = false,
    )

    /** Determines whether the time picker is dial or input */
    var showDial by remember { mutableStateOf(true) }

    /** The icon used for the icon button that switches from dial to input */
    val toggleIcon = if (showDial) {
        Icons.Filled.EditCalendar
    } else {
        Icons.Filled.AccessTime
    }

    AdvancedTimePickerDialog(
        onDismiss = { onDismiss() },
        onConfirm = { onConfirm(timePickerState) },
        toggle = {
            IconButton(onClick = { showDial = !showDial }) {
                Icon(
                    imageVector = toggleIcon,
                    contentDescription = "Time picker type toggle",
                )
            }
        },
    ) {
        if (showDial) {
            TimePicker(
                state = timePickerState,
            )
        } else {
            TimeInput(
                state = timePickerState,
            )
        }
    }
}


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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbstractFunction_TimePickerSection(
    message_on_button:String,
    modifier: Modifier = Modifier,
    onTimeSelected_func_to_handle_value_returned: (TimePickerState) -> Unit
) {
//    function handles creating the state and managing it such that we only have to think about
//    getting value in the callback function

//    -------- // ------------
//    further abstraction --> well function just takes in a button creates state that is toggled by the
//    button and then displays the ui (compose) if the state is true ; so we can inject the last ui
//    -------- // ------------

    var showTimePicker by remember { mutableStateOf(false) }

    Button(
        onClick = {
            Log.d("AA", "$showTimePicker")
            showTimePicker = !showTimePicker
            Log.d("AA", "--$showTimePicker")
        },
        modifier = modifier,
    ) {
        Text(message_on_button)
    }

    if (showTimePicker) {
        DialExample_2(
            onConfirm = { timePickerState ->
                val selectedTime = "${timePickerState.hour}:${timePickerState.minute}"
                logD("Selected time: $selectedTime, ${selectedTime}")
                showTimePicker = false
                onTimeSelected_func_to_handle_value_returned(timePickerState)
            },
            onDismiss = {
                logD("TimePicker dismissed")
                showTimePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbstractFunction_DatePickerSection(
    message_on_button: String,
    modifier: Modifier = Modifier,
    onDateSelected_func_to_handle_value_returned: (Long?) -> Unit
) {
    // State to control the visibility of the DatePickerModal
    var showDatePicker by remember { mutableStateOf(false) }

    // Button to toggle the DatePicker visibility
    Button(
        onClick = {
            Log.d("AA", "showDatePicker--$showDatePicker")
            showDatePicker = !showDatePicker
        },
        modifier = modifier
    ) {
        Text(message_on_button)
    }

    // If the DatePicker should be shown, display it
    if (showDatePicker) {
        DatePickerModal(
            onDateSelected = { date ->
                Log.d("AA", "Date picker ended -->$date")
                showDatePicker = false
                onDateSelected_func_to_handle_value_returned(date)
            },
            onDismiss = {
                Log.d("AA", "Date picker dismissed")
                showDatePicker = false
            }
        )
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

@Composable
fun MyAlertDialog(shouldShowDialog: MutableState<Boolean>) {
    if (shouldShowDialog.value) { // 2
        AlertDialog( // 3
            onDismissRequest = { // 4
                shouldShowDialog.value = false
            },
            // 5
            title = { Text(text = "Alert Dialog") },
            text = { Text(text = "Jetpack Compose Alert Dialog") },
            confirmButton = { // 6
                Button(
                    onClick = {
                        shouldShowDialog.value = false
                    }
                ) {
                    Text(
                        text = "Confirm",
                        color = Color.White
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmContainer(AlarmDao: AlarmDao, alarmManager: AlarmManager, context_of_activity: ComponentActivity) {

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val fontSize = (screenHeight * 0.05f).value.sp
    val coroutineScope = rememberCoroutineScope()

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
//                logD("hhhhjjjjkkk-->$indexOfIndividualAlarmInAlarm")
//                item {
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
                                .background(color = Color(0xFF0D388C))
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
                                    },
                                    colors = ButtonDefaults.buttonColors(Color(0xFF0eaae3))
                                ) {
                                    Text("remove")
                                }
                            }
                        }
                    }
//                }
            }
        }
        //-----------
        if (showTheDialogToTheUserToAskForPermission){
            DialogToAskUserAboutAlarm(onDismissRequest = {showTheDialogToTheUserToAskForPermission = false}, onConfirmation = {a,b, c ->logD("in the confirm ${a.hour}:${a.minute},--||-- ${c.selectedDateMillis}")}
            , activity_context = context_of_activity, alarmDao = AlarmDao, alarmManager = alarmManager)
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = screenHeight / 15)
                .testTag("RoundPlusIcon")
        ) {
            RoundPlusIcon(size = screenHeight/10, onClick = {showTheDialogToTheUserToAskForPermission = !showTheDialogToTheUserToAskForPermission})
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePicker_without_dialog(
    showDatePickerToTheUser: Boolean = true,
    onDismiss: () -> Unit,
    nextButton: String = "Next",
    onConfirm: (DatePickerState) -> Unit,

) {
    var showDatePicker by remember { mutableStateOf(showDatePickerToTheUser) }
    val today = Calendar.getInstance().timeInMillis

    // Initialize DatePickerState with today's date
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = today
    )
//    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    Box(modifier = Modifier.fillMaxWidth()) {
        if (showDatePicker) {
            Card( ){
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
//                        .offset(y = screenHeight/28)
                        .shadow(elevation = 4.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    Column {
                        DatePicker(
                            state = datePickerState,
                            showModeToggle = false,
                            modifier = Modifier
                            .testTag("datePicker")
                        )
//                        if (user_mistake_message_show != ""){
//                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(onClick = onDismiss) {
                                Text("Dismiss")
                            }
                            Button(
                                onClick = { onConfirm(datePickerState) },
                            ) {
                                Text(nextButton)
                            }

                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SuspiciousIndentation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogToAskUserAboutAlarm(
    alarmManager: AlarmManager,  activity_context: ComponentActivity, alarmDao: AlarmDao,
    onDismissRequest: () -> Unit,
    onConfirmation: (startTime: TimePickerState, endTime:TimePickerState, datePickerState: DatePickerState) -> Unit,
) {
    // Step state to determine whether we are showing the TimePicker or DatePicker
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val screenWidth = LocalConfiguration.current.screenWidthDp

    val screenHeight_normal = (screenHeight /1.4).dp
    val screenHeight_if_message_not_present = (screenHeight /1.23).dp
    val screenWidth_1 = (screenWidth ).dp


    // Variables to store the picked time and date
    var startTime: TimePickerState? by remember { mutableStateOf(null) }
    var endTime: TimePickerState? by remember { mutableStateOf(null) }
    var pickedDateState: DatePickerState? by remember { mutableStateOf(null) }
    var a  by remember { mutableStateOf(0) }
    var freq_returned_by_user  by remember { mutableStateOf(0) }
    var mistake_message_for_func  by remember { mutableStateOf("") }

    val cardHeight = if (mistake_message_for_func.isEmpty()) {
        screenHeight_normal
    } else {
        screenHeight_if_message_not_present
    }

    Dialog(onDismissRequest = { onDismissRequest() }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("DialogToAskUserAboutAlarm")
                .height((cardHeight))
                .width(screenWidth_1),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

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
                        // Show DatePicker next
                        DatePicker_without_dialog(
                            onConfirm = { dateState ->
                                pickedDateState = dateState
                                    // Both time and date are picked, call confirmation
                                        onConfirmation(startTime!!,endTime!! ,pickedDateState!!)
                                a ++
                            },
                            onDismiss = onDismissRequest,
                            nextButton = "Confirm",
                        )
                      }
                    3->{
                        var startTime_obj_form_calender:Calendar = Calendar.getInstance().apply {
                            timeInMillis = pickedDateState?.selectedDateMillis!!
                            set(Calendar.HOUR_OF_DAY, startTime?.hour!!)
                            set(Calendar.MINUTE, startTime?.minute!! )
                        }
                        var endTime_obj_form_calender = Calendar.getInstance().apply {
                            timeInMillis = pickedDateState?.selectedDateMillis!!
                            set(Calendar.HOUR_OF_DAY, endTime?.hour!!)
                            set(Calendar.MINUTE, endTime?.minute!! )
                        }

                        // since alarms are being set on the same day( eg if it is on 03 (am) and end one has to be 15or .. ( pm) if it
                        // reached 2 am it is not possible as the time picker not allows it so a bad input on the  user part, as alarm cant end beofr it starts )
                        // it should be the the start time is bigger than the end time

                        if (startTime_obj_form_calender.timeInMillis >= endTime_obj_form_calender.timeInMillis){
                            logD("${startTime_obj_form_calender.timeInMillis},---, ${endTime_obj_form_calender.timeInMillis} ")
                            val formatter = SimpleDateFormat("h:mm a MM/dd/yy", Locale.getDefault())
                            val startTimeToShowUser = formatter.format(startTime_obj_form_calender.time)
                            val endTimeToShowUser = formatter.format(endTime_obj_form_calender.time)
                            mistake_message_for_func = " Your start Time($startTimeToShowUser) should be bigger than the end time ($endTimeToShowUser). we can't set that alarm "
                            a = 0
                        }else{
                            a++
                        }
                    }
                    4 ->{
                        // freq and example of it
                        var startTime_obj_form_calender:Calendar = Calendar.getInstance().apply {
                            timeInMillis = pickedDateState?.selectedDateMillis!!
                            set(Calendar.HOUR_OF_DAY, startTime?.hour!!)
                            set(Calendar.MINUTE, startTime?.minute!! )
                        }
                        var endTime_obj_form_calender = Calendar.getInstance().apply {
                            timeInMillis = pickedDateState?.selectedDateMillis!!
                            set(Calendar.HOUR_OF_DAY, endTime?.hour!!)
                            set(Calendar.MINUTE, endTime?.minute!! )
                        }
                        freq_without_dialog(onDismiss = onDismissRequest, nextButton = "Set the alarm", onConfirm = {freq_returned ->
                            logD("frequency returned ->$freq_returned..........")

                            ; freq_returned_by_user = freq_returned; a++ },
                           calender_instance_at_start_time = startTime_obj_form_calender, calender_instance_at_end_time = endTime_obj_form_calender  )


                    }
                    // some checks like end time is bigger than start time, (do convert them in milliseconds with  selected date)
                    // in the dialog box declare time ---NOW--- (milli) and date millisecond, and pass them to the both the
                    // components then we can escape form the null checks  and on confirm replace them

               5->{

                   var startTime_obj_form_calender:Calendar = Calendar.getInstance().apply {
                       timeInMillis = pickedDateState?.selectedDateMillis!!
                       set(Calendar.HOUR_OF_DAY, startTime?.hour!!)
                       set(Calendar.MINUTE, startTime?.minute!! )
                   }

                   var endTime_obj_form_calender = Calendar.getInstance().apply {
                       timeInMillis = pickedDateState?.selectedDateMillis!!
                       set(Calendar.HOUR_OF_DAY, endTime?.hour!!)
                       set(Calendar.MINUTE, endTime?.minute!! )
                   }

                   var date = pickedDateState!!.selectedDateMillis?.let {
                       java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                   }?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

                   scheduleMultipleAlarms(alarmManager, activity_context = activity_context, alarmDao = alarmDao,
                       calendar_for_start_time = startTime_obj_form_calender, calendar_for_end_time = endTime_obj_form_calender, freq_after_the_callback = freq_returned_by_user,
                       selected_date_for_display =  date!! )

                   onDismissRequest()

               }
                }
            }
        }
    }
}


@SuppressLint("DefaultLocale")
@Composable
fun freq_without_dialog(onDismiss:()->Unit, nextButton:String, onConfirm:(text_entered_by_user:Int)->Unit, calender_instance_at_start_time:Calendar, calender_instance_at_end_time:Calendar ) {

//    logD("is the both of them same -->${calender_instance_at_end_time.timeInMillis == calender_instance_at_start_time.timeInMillis}")

    var text_entered_by_user by remember { mutableStateOf(0) }
    var numberToDisplay:Array<String>  by remember { mutableStateOf(Array(5) { "" }) }

    val screenHeight = LocalConfiguration.current.screenHeightDp
    val screenWidth = LocalConfiguration.current.screenHeightDp

//    var startTime = cal_ins.apply {
//        timeInMillis = dateInMillis
//        set(Calendar.HOUR_OF_DAY, hour)
//        set(Calendar.MINUTE, minute)
//    }

    var formattedTime:String

    if (text_entered_by_user != 0){
        val formattedStartTime_1 = String.format(
            "%02d:%02d",
            calender_instance_at_start_time.get(Calendar.HOUR_OF_DAY),
            calender_instance_at_start_time.get(Calendar.MINUTE)
        )
        val formattedEndTime_1 = String.format(
            "%02d:%02d",
            calender_instance_at_start_time.get(Calendar.HOUR_OF_DAY),
            calender_instance_at_start_time.get(Calendar.MINUTE)
        )
        logD("calender_instance_at_start_time.timeInMillis->${calender_instance_at_start_time.timeInMillis}.....${calender_instance_at_end_time.timeInMillis}" +
                "\n\n $formattedStartTime_1-----$formattedEndTime_1")
      for ( i in 1..4 ) {

          // if the start time entered by the user or by for loop is >= than the end time then just add the end time
          // eg --> 12:30 --> 12:40, 4 min freq, 12:30, 12:34, 12:38, 12:42 which is greater than 12:40 (or consider 12:40 which is same as 12:40)
          // so we will add it to the array and exit, case 2-> I should not allow user to enter end time > than the start one

          if (calender_instance_at_start_time.timeInMillis >= calender_instance_at_end_time.timeInMillis){

              logD("the conditon is true and will exit from the for loop")

              numberToDisplay[i-1]= String.format(
                  "%02d:%02d",
                  calender_instance_at_end_time.get(Calendar.HOUR_OF_DAY),
                  calender_instance_at_end_time.get(Calendar.MINUTE)
              )
              break
          }
          formattedTime = String.format(
              "%02d:%02d",
              calender_instance_at_start_time.get(Calendar.HOUR_OF_DAY),
              calender_instance_at_start_time.get(Calendar.MINUTE)
          )
          logD("formattedTime -->$formattedTime")
          numberToDisplay[i-1]= formattedTime
          calender_instance_at_start_time.add(Calendar.MINUTE, text_entered_by_user)

      }
      logD("array -->${numberToDisplay[0]}")
  }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start // Aligns content to the start (left)
    ) {
        Text("Enter frequency -->", fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = (screenHeight/99).dp,  horizontal = (screenWidth/93).dp ))

        NumberField("Enter the frequency(in min)", { changed_freq_string -> text_entered_by_user = changed_freq_string.toInt() })
        if (text_entered_by_user != 0){
            Text( "Alarms will go on --> ${numberToDisplay.joinToString(", ")}....", fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = (screenHeight/99).dp, horizontal = (screenWidth/83).dp  )  )
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
            onClick = { if(text_entered_by_user != 0) { onConfirm(text_entered_by_user) } },
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





 @SuppressLint("CoroutineCreationDuringComposition")
 @Composable
 fun scheduleMultipleAlarms(alarmManager: AlarmManager, selected_date_for_display:String,
       calendar_for_start_time:Calendar, calendar_for_end_time:Calendar, freq_after_the_callback:Int, activity_context:ComponentActivity, alarmDao:AlarmDao )
 {
    // should probably make some checks like if the user ST->11:30 pm today and end time 1 am tomorrow (basically should be in a day)
     val coroutineScope = rememberCoroutineScope()
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
    var alarmSetComplete = false

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
    alarmSetComplete = true
     coroutineScope.launch {
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
                end_am_pm = end_am_pm,
                freq_in_min_to_display = (freq_in_min/60000).toInt(),

                )
            val insertedId = alarmDao.insert(newAlarm)
            logD("Inserted alarm with ID: $insertedId")
        } catch (e: Exception) {
            logD("Exception occurred when inserting in the db: $e")
        }
    }
}
