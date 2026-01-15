package com.example.trying_native.components_for_ui_compose

import android.R.attr.top
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.trying_native.AlarmLogic.AlarmsController
import com.example.trying_native.FirstLaunchAskForPermission.FirstLaunchAskForPermission
import com.example.trying_native.notification.NotificationBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.util.Date

@SuppressLint("FlowOperatorInvokedInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmContainer(alarmDao: AlarmDao, alarmManager: AlarmManager, activityContext: ComponentActivity, askUserForPermissionToScheduleAlarm:()->Unit) {
    val alarmsController = AlarmsController()
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val fontSize = (screenHeight * 0.05f).value.sp
    val coroutineScope = activityContext.lifecycleScope
    val uncancellableScope = remember {
        CoroutineScope(coroutineScope.coroutineContext + NonCancellable)
    }
    val askUserForPermission by lazy  {Settings.canDrawOverlays(activityContext) }

    val alarms1 by alarmDao.getAllAlarmsFlow().flowOn(Dispatchers.IO).collectAsStateWithLifecycle(initialValue = emptyList())

    var showTheDialogToTheUserToAskForPermission by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val snackBarHostState = remember { SnackbarHostState() }

    Box(
        modifier = Modifier
            .testTag("AlarmContainer")
            .fillMaxSize()
            .background(color = Color.Black)
    ) {
                                                                                                                                                                                                                                                                                                                                                    
        SnackbarHost(
            hostState = snackBarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 106.dp)
                .zIndex(10f)
        ) { snackbarData ->
            Snackbar(
                snackbarData = snackbarData,
                shape = RoundedCornerShape(45.dp), // Apply the same rounding as your alarm card
                containerColor = Color.Blue,
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth()
            )
        }


        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() ,
                bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 10.dp,
            )
        ) {
            itemsIndexed(alarms1, key = {_ , alarm -> alarm.id}){indexOfIndividualAlarmInAlarm, individualAlarm ->
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
                                        clipboardManager.setText(AnnotatedString((individualAlarm.message)))
                                        coroutineScope.launch {
                                            snackBarHostState.showSnackbar(
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
                                        text = individualAlarm.getTimeFormatted(individualAlarm.startTime),
                                        fontSize = (fontSize / 1.2),
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    Text(
                                        text = individualAlarm.getFormattedAmPm(individualAlarm.startTime),
                                        fontSize = (fontSize / 2.2),
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "to",
                                        modifier = Modifier.size(38.dp) ,

                                    )
                                }
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = individualAlarm.getTimeFormatted(individualAlarm.endTime),
                                        fontSize = (fontSize / 1.2),
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    Text(
                                        text = individualAlarm.getFormattedAmPm(individualAlarm.endTime),
                                        fontSize = (fontSize / 2.3),
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "after every ${individualAlarm.freqGottenAfterCallback} min",
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
                                                context_of_activity = activityContext,
                                                startTime = individualAlarm.startTime,
                                                endTime = individualAlarm.endTime,
                                                frequency_in_min = individualAlarm.getFreqInMillisecond(),
                                                alarmDao = alarmDao,
                                                alarmManager = alarmManager,
                                                delete_the_alarm_from_db = true,
                                                alarmData = individualAlarm
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(Color(0xFF0eaae3))
                                ) {
                                    Text("delete")
                                }
                                Text(
                                    text = "On: ${individualAlarm.getDateFormatted(individualAlarm.startTime)}",
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
                                                    context_of_activity = activityContext,
                                                    startTime = individualAlarm.startTime,
                                                    endTime = individualAlarm.endTime,
                                                    frequency_in_min = individualAlarm.getFreqInMillisecond(),
                                                    alarmDao = alarmDao,
                                                    alarmManager = alarmManager,
                                                    delete_the_alarm_from_db = false,
                                                    alarmData = individualAlarm
                                                )
                                            }
                                        }
                                       // -------------------------
                                        else {
                                            // -- reset function abstract it away and change it --
                                            uncancellableScope.launch {
                                                logD("about to reset the alarm-+")
                                              val exception=  alarmsController.resetAlarms(alarmData = individualAlarm, alarmManager = alarmManager,
                                                    activityContext = activityContext, alarmDao = alarmDao,
                                                )
                                              logD("the exception form the resetAlarm is ->$exception")
                                                exception.fold(onSuccess = {}, onFailure = {
                                                    NotificationBuilder(activityContext, title = "error returned in creating multiple alarm ", notificationText = "execution returned exception in schedule multiple alarm  -->${exception}").showNotification()
                                                    logD("error in the schedule multiple -->${exception}")
                                                })
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
//                        val dateForDisplay = if (getDateInHumanReadableFormat(startDateInMilliSec) == getDateInHumanReadableFormat(endDateInMilliSec)) getDateInHumanReadableFormat(startDateInMilliSec) else getDateInHumanReadableFormat(startDateInMilliSec)+"-->"+getDateInHumanReadableFormat(endDateInMilliSec)
                        val startTimeCal = Calendar.getInstance().apply { timeInMillis = startDateInMilliSec; set(Calendar.HOUR_OF_DAY, startTimeHour); set(Calendar.MINUTE, startTimeMinute); set(Calendar.SECOND, 0)   }
                        val endTimeCal = Calendar.getInstance().apply { timeInMillis = endDateInMilliSec; set(Calendar.HOUR_OF_DAY, endTimeHour); set(Calendar.MINUTE, endTimeMinute);set(Calendar.SECOND, 0)   }
                        // date_in_long is safe to pass as any as it is redundant and no one is using it
                        uncancellableScope.launch {
                            val exception =alarmsController.scheduleMultipleAlarms(alarmManager,  startDateInMilliSec, alarmDao = alarmDao, messageForDB = alarmMessage,
                                calendarForStartTime = startTimeCal, calendarForEndTime = endTimeCal, freqAfterTheCallback = frequency, activityContext = activityContext
                            )
                            exception.fold(onSuccess = { return@launch}, onFailure = { excp ->
                                NotificationBuilder(context = activityContext, title = "there is a error/Exception  in making new alarm", notificationText = excp.toString()).showNotification()
                                logD("there is a error/Exception  in making new alarm-->${excp}")

                            })
                        }
                    }catch (e: Exception){
                        NotificationBuilder(context = activityContext, title = "there is a error/Exception  in making new alarm", notificationText = e.toString()).showNotification()
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
            if(!askUserForPermission){
//                logD("\n\n[ OVERLAY PERMISSION] -> is not there ..$askUserForPermission \n\n")
                askUserForPermissionToScheduleAlarm()
            }
        }, context = activityContext)
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


    var frequency by remember { mutableIntStateOf(2) }
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
