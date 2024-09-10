package com.example.trying_native.Components_for_ui_compose

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.trying_native.ui.theme.Trying_nativeTheme
import java.util.Calendar

import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerFormatter
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmData
import com.example.trying_native.logD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Trying_nativeTheme {
        Greeting("Android")
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

    TextField(
        value = text,
        onValueChange = { newText ->
            text = newText
            onFrequencyChanged(newText) // Callback to handle the input change
        },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        placeholder = {
            Text(text = placeHolderText)
        }
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
@Composable
fun myTexts(alarmDao: AlarmDao) {
    var isLoading by remember { mutableStateOf(true) }
    var alarms by remember { mutableStateOf<List<AlarmData>?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {
        if (isLoading) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        alarms = withContext(Dispatchers.IO) {
                            try {
                                alarmDao.getAllAlarms()
                            } catch (e: Exception) {
                                // Handle the exception (e.g., log it)
                                null
                            }
                        }
                        isLoading = false
                    }
                }
            ) {
                Text("Click to see texts")
            }
        } else {
            alarms?.let { alarmList ->
                if (alarmList.isNotEmpty()) {
                    alarmList.forEach { alarm ->
                        Text("Alarm ID: ${alarm.id}, First Value: ${alarm.first_value}, Second Value: ${alarm.second_value}, Frequency: ${alarm.freq_in_min} mins, Completed: ${alarm.isReadyToUse}")
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else {
                    Text("No alarms found.")
                }
            }
            Button(onClick = { isLoading = true }) {
                Text("Make it stop")
            }
        }
    }
}

@Composable
fun AlarmContainer(AlarmDao:AlarmDao) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val fontSize = (screenHeight * 0.05f).value.sp
    val coroutineScope = rememberCoroutineScope()
    var alarms by remember { mutableStateOf<List<AlarmData>?>(null) }
    var isAlarmFetchedShowAlarms by remember { mutableStateOf<Boolean>(false) }

    logD("In the alarm container")

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            alarms = withContext(Dispatchers.IO) {
                try {
                    AlarmDao.getAllAlarms()
                } catch (e: Exception) {
                    // Handle the exception (e.g., log it)
                    logD("Opps something went  wrong when getting all the alarms -->$e")
                    null
                }
            }
            logD("got all the alarms-->${alarms.toString()}")
            isAlarmFetchedShowAlarms = true
        }
    }
    LazyColumn{
        if(isAlarmFetchedShowAlarms != true && alarms == null ){
            //display the icon  that we do not have alarms
        }
        else{
            //lets show the alarms

            // add a db field that gives me the time of both the alarm so that I do not need
            // to calculate it right now,and also add date in the card

            alarms?.forEach {
                    individualAlarm ->
               item{
                   ElevatedCard(
                       elevation = CardDefaults.cardElevation(
                           defaultElevation = 18.dp,
                       ),
                       modifier = Modifier
                           .size(width = screenWidth, height = 270.dp)
                           .background(color = Color.LightGray)
                           .padding(13.dp)
                   ) {
                       Text(
                           text = "${individualAlarm.start_hour_for_display}:${individualAlarm.start_min_for_display} --> ${individualAlarm.end_hour_for_display}:${individualAlarm.end_min_for_display}",
                           modifier = Modifier
                               .padding(16.dp),
                           textAlign = TextAlign.Center,
                           fontSize = fontSize /2,
                       )
                   }
               }

            }

        }
    }


}
