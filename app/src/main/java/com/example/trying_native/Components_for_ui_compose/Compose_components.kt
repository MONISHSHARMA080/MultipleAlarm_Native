package com.example.trying_native.components_for_ui_compose

import android.R
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Context
import android.content.Context.ALARM_SERVICE
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
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
import com.example.trying_native.logD
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.material3.DatePicker
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.trying_native.AlarmLogic.AlarmsController
import com.example.trying_native.Components_for_ui_compose.AlarmListScreen
import com.example.trying_native.Components_for_ui_compose.AlarmPickerScreen
import com.example.trying_native.Components_for_ui_compose.NavigationStack
import com.example.trying_native.FirstLaunchAskForPermission.FirstLaunchAskForPermission
import com.example.trying_native.dataBase.AlarmData
import com.example.trying_native.dataBase.AlarmDatabase
import com.example.trying_native.notification.NotificationBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import java.time.LocalTime
import java.util.Date
import kotlin.time.Duration.Companion.seconds

@SuppressLint("FlowOperatorInvokedInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmContainer(activityContext: ComponentActivity, alarmDao: AlarmDao, alarmManager: AlarmManager,  onNavigateToEdit: (AlarmData) -> Unit, onNavigateToCreate: () -> Unit ) {
    val coroutineScope = activityContext.lifecycleScope
    val uncancellableScope = CoroutineScope(coroutineScope.coroutineContext + NonCancellable)
    val alarms by alarmDao.getAllAlarmsFlow().flowOn(Dispatchers.IO).collectAsStateWithLifecycle(initialValue = emptyList())
    AlarmListScreen(
        alarmManager =alarmManager, alarmDao = alarmDao, onNavigateToEdit=onNavigateToEdit, onNavigateToCreate=onNavigateToCreate,
        uncancellableScope = uncancellableScope, activityContext =activityContext,alarms = alarms
    )
}


@Composable
fun RoundPlusIcon(
    modifier: Modifier = Modifier, size: Dp , backgroundColor: Color = Color.Blue, onClick: () -> Unit, context:Context) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        logD("is granted is $isGranted")
        // No need to handle permission result since we want onClick to run anyway
    }
    val coroutineScope = rememberCoroutineScope()
    Box(
        modifier = modifier
            .size(size).zIndex(4f)
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
    val distanceForEndTime = 30 * 60 * 1000L
    var endCalendar by remember { mutableStateOf(Calendar.getInstance().apply { timeInMillis = calInstance.timeInMillis + distanceForEndTime } ) }
    var startHour by remember { mutableIntStateOf(calInstance.get(Calendar.HOUR_OF_DAY)) }
    var startMinute by remember { mutableIntStateOf(calInstance.get(Calendar.MINUTE)) }
//    endCalendar.add(Calendar.MINUTE, distanceForEndTime.toInt() )
//    var endHour   by remember { mutableIntStateOf(endCalendar.get(Calendar.HOUR_OF_DAY ) ) }
//    var endMinute by remember { mutableIntStateOf(endCalendar.get(Calendar.MINUTE)) }
    val endHour by remember(endCalendar){
        derivedStateOf { endCalendar.get(Calendar.HOUR_OF_DAY) }
    }
    val endMinute by remember(endCalendar){
        derivedStateOf {endCalendar.get(Calendar.MINUTE)  }
    }


    var frequency by remember { mutableIntStateOf(1) }
    var alarmMessage by remember { mutableStateOf("") }
    var startDateToView by remember { mutableStateOf(getDateInHumanReadableFormat(calInstance.timeInMillis)) }
    var endDateToView by remember { mutableStateOf(getDateInHumanReadableFormat(endCalendar.timeInMillis)) }
    var startDateToReturn by remember { mutableLongStateOf(calInstance.timeInMillis) }
    var endDateToReturn by remember { mutableLongStateOf(endCalendar.timeInMillis) }

    var showTimePickerFor by remember { mutableStateOf<InputPickerType?>(null) }
    var showDatePickerFor by remember { mutableStateOf<InputPickerType?>(null) }

    logD(   "\n\n  --endTime is ${getTimeInHumanReadableFormatProtectFrom0Included(endCalendar.timeInMillis)} " +
            "\n startTime is ${getTimeInHumanReadableFormatProtectFrom0Included(calInstance.timeInMillis) }" +
            "\n -- endTIme (from startTime) is ${getTimeInHumanReadableFormatProtectFrom0Included(calInstance.timeInMillis + distanceForEndTime)} " +
            "\n - cal Instance new one is ${getTimeInHumanReadableFormatProtectFrom0Included(Calendar.getInstance().timeInMillis)}" +
            "\n end date -> ${getDateInHumanReadableFormat(endCalendar.timeInMillis)}" +
            "\n\n--"
    )

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
                    Text(text = "date")
                    InputPickerField(startDateToView, onClick = {
                        showDatePickerFor = InputPickerType.START
                        logD(" the start date for the date picker is $showDatePickerFor")
                    }
                    )
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
                        modifier = Modifier.width(120.dp).background(Color.Black).border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline))
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
                        modifier = Modifier.weight(1f).background(Color.Black).border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline))   // Takes remaining space
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
                        val updatedEndCal =Calendar.getInstance().apply { timeInMillis = calInstance.timeInMillis
                         set(Calendar.HOUR_OF_DAY, newTime.hour)
                         set(Calendar.MINUTE, newTime.minute)
                        }
                        endCalendar = updatedEndCal.apply { timeInMillis = updatedEndCal.timeInMillis + distanceForEndTime }
                    }
                    InputPickerType.END -> {
                        logD("changed the end time to $newTime or Hour:${newTime.hour} and Min:${newTime.minute}")
                        endCalendar = Calendar.getInstance().apply {
                            timeInMillis = endCalendar.timeInMillis
                            set(Calendar.HOUR_OF_DAY, newTime.hour)
                            set(Calendar.MINUTE, newTime.minute)
                        }
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

private  fun getTimeInHumanReadableFormatProtectFrom0Included(t:Long): String{
    if (t == 0L) return "--the time here(probablyFromTheIntent) is 0--"
    return SimpleDateFormat("dd-MM-yyyy h:mm:ss a", Locale.getDefault()).format(Date(t))
}
