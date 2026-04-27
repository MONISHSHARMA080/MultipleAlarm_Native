package com.coolApps.MultipleAlarmClock.Components_for_ui_compose.alarmPicker

import android.Manifest
import android.content.pm.PackageManager
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coolApps.MultipleAlarmClock.dataBase.AlarmData
import com.coolApps.MultipleAlarmClock.dataBase.AlarmErrorField
import com.coolApps.MultipleAlarmClock.dataBase.AlarmObject
import com.coolApps.MultipleAlarmClock.dataBase.ValidationResult
import com.coolApps.MultipleAlarmClock.logD
import com.example.MultipleAlarmClock.Ui.Permissions.AlarmPermissionDialog
import com.example.MultipleAlarmClock.Ui.Permissions.PermissionStep
import com.example.MultipleAlarmClock.Ui.alarmPicker.AlarmPickerEvent
import com.example.MultipleAlarmClock.Ui.alarmPicker.AlarmPickerViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class AccentColor(val value:Color) {
     Ok(Color(0xFF1A73E8)),
    Problem(Color(0xFFde0707))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalPermissionsApi::class)
@Composable
fun AlarmPickerScreen(
    alarm: AlarmData?,
    alarmSetGoBack: () -> Unit,
    viewModel: AlarmPickerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val allPermissionsGrantedInStore by viewModel.allPermissionsGranted.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(alarm) {
        viewModel.initialize(alarm)
    }



    var showPermissionDialog by remember { mutableStateOf(false) }
    var missingSteps by remember { mutableStateOf<List<PermissionStep>>(emptyList()) }


    val alarmObject = uiState.alarmObject
    val validationResult = uiState.validationResult
    val currentError = validationResult as? ValidationResult.Failure

    val validationOk = validationResult is ValidationResult.Success
    val isPermissionsOk = uiState.areAllPermissionsGranted
    val weGood = validationOk && isPermissionsOk
	val freqText = if (alarmObject.freqGottenAfterCallback < 1) "" else viewModel.getFrequencyPreviewText()


    val accentColor by animateColorAsState(
        targetValue = if (weGood) AccentColor.Ok.value else AccentColor.Problem.value,
        animationSpec = tween(durationMillis = 190),
        label = "accent_color_animation"
    )

    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(Unit) {
        viewModel.checkPermissions(context)
    }
    LaunchedEffect(weGood,uiState ) {
        val isNotificationsEnabled = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        viewModel.captureEvent("is alarmObject value valid changed", mapOf(
            "weGood" to weGood,
            "alarmObject" to alarmObject.toString(),
            "are all permission granted" to uiState.areAllPermissionsGranted,
            "validation error message" to (currentError?.message ?: ""),
            "alarmData" to alarm.toString(),
            "ui_state" to uiState,
            "notification permission granted" to isNotificationsEnabled
        ))
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            logD("got event: $event")
            when (event) {
                is AlarmPickerEvent.NavigateBack -> alarmSetGoBack()
                is AlarmPickerEvent.ShowPermissionDialog -> {
                    missingSteps = event.missingSteps
                    showPermissionDialog = true
                }
                AlarmPickerEvent.UpdateDataStoreGranted -> { /* handled in VM */ }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(showPermissionDialog) {
        viewModel.captureEvent("ask for permission dialog opened", mapOf())
    }

    logD("showPermissionDialog:$showPermissionDialog  missingStep.isNotEmpty():${missingSteps.isNotEmpty()}")
    if (showPermissionDialog) {
        AlarmPermissionDialog(
            missingSteps ,
            onAllCriticalGranted = {
                showPermissionDialog = false
                viewModel.checkPermissions(context)
            },
            onDismiss = {
                showPermissionDialog = false
                viewModel.checkPermissions(context)
            },
            onTrackEvent = {event, prop -> viewModel.captureEvent(event, prop)}
        )
    }

    // btw I want to see the dataStore.allPermissionsGranted = true if it is then check and if the check results in false then update it and update it too after writing to it

    Scaffold(
        contentWindowInsets = WindowInsets.safeContent,
        modifier = Modifier.fillMaxSize()
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F131A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F131A))
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .animateContentSize()
                    .padding(horizontal = 20.dp)
                    .padding(top = contentPadding.calculateTopPadding())
                    .padding(bottom = contentPadding.calculateBottomPadding() + 25.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .padding(start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (alarm == null) "New alarm" else "Edit Alarm",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Time Range Card
                CardContainer {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccessTimeFilled,
                                contentDescription = null,
                                tint = if (currentError?.field == AlarmErrorField.Time)
                                    AccentColor.Problem.value else AccentColor.Ok.value,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Time", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(13.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TimeBox(
                                label = "Start time",
                                time = alarmObject.startTime,
                                modifier = Modifier.weight(1f),
                                accentColor = if (currentError?.field == AlarmErrorField.Time)
                                    AccentColor.Problem.value else AccentColor.Ok.value,
                                onNewTimeSelected = { viewModel.updateStartTime(it) }
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            TimeBox(
                                label = "End time",
                                time = alarmObject.endTime,
                                modifier = Modifier.weight(1f),
                                accentColor = if (currentError?.field == AlarmErrorField.Time)
                                    AccentColor.Problem.value else AccentColor.Ok.value,
                                onNewTimeSelected = { viewModel.updateEndTime(it) }
                            )
                        }
                        ShowErrorMessageIfError(currentError, AlarmErrorField.Time)
                    }
                }

                // Date Card
                CardContainer {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = if (currentError?.field == AlarmErrorField.DATE)
                                    AccentColor.Problem.value else AccentColor.Ok.value,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Date", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        DateList(
                            startDateIndex = alarm?.startTime,
                            weGood = !(currentError?.field == AlarmErrorField.DATE),
                            onSelect = { viewModel.updateDate(it) }
                        )
                        ShowErrorMessageIfError(currentError, AlarmErrorField.DATE)
                    }
                }

                // Frequency Card
                CardContainer {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (currentError?.field == AlarmErrorField.FREQUENCY)
                                    AccentColor.Problem.value else AccentColor.Ok.value,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Repeat every", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier
                                        .height(48.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(25.dp))
                                        .background(
                                            if (currentError?.field == AlarmErrorField.FREQUENCY)
                                                AccentColor.Problem.value else AccentColor.Ok.value
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(onClick = { viewModel.decrementFrequency() }) {
                                        Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White)
                                    }
                                    BasicTextField(
                                        value = if (alarmObject.freqGottenAfterCallback !in 1..710) ""
                                        else alarmObject.freqGottenAfterCallback.toString(),
                                        onValueChange = { newValue ->
                                            val filteredValue = newValue.filter { it.isDigit() }
                                            if (filteredValue.isEmpty()) {
                                                viewModel.updateFrequency(0)
                                            } else {
                                                val intValue = filteredValue.toLongOrNull()
                                                if (intValue != null && intValue in 1..709) {
                                                    viewModel.updateFrequency(intValue)
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .bringIntoViewRequester(bringIntoViewRequester)
                                            .onFocusEvent {
                                                if (it.isFocused) {
                                                    coroutineScope.launch {
                                                        bringIntoViewRequester.bringIntoView()
                                                    }
                                                }
                                            },
                                        textStyle = TextStyle(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            textAlign = TextAlign.Center
                                        ),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true
                                    )
                                    IconButton(onClick = { viewModel.incrementFrequency() }) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        if (alarmObject.freqGottenAfterCallback < 1) {
                            Text("Please enter the frequency value", color = Color.White, fontSize = 12.sp)
                        } else {
                            Text(freqText, color = Color.Gray, fontSize = 12.sp)
                        }
                        ShowErrorMessageIfError(currentError, AlarmErrorField.FREQUENCY)
                    }
                }

                // Message Card
                CardContainer {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.Message,
                                contentDescription = null,
                                tint = AccentColor.Ok.value
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Message", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        BasicTextField(
                            value = alarmObject.message,
                            onValueChange = { viewModel.updateMessage(it) },
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier
                                .bringIntoViewRequester(bringIntoViewRequester)
                                .onFocusEvent {
                                    if (it.isFocused) {
                                        coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                                    }
                                }
                                .fillMaxWidth()
                                .height(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F131A))
                                .padding(12.dp),
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (alarmObject.message.isEmpty()) {
                                        Text("Alarm message......", color = Color.DarkGray, fontSize = 14.sp)
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }
                // Set Alarm Button
                Button(
                    onClick = {
                        if (validationOk) {
                            // All business logic is now inside the ViewModel
                            viewModel.onSetAlarmClicked(alarm, alarmObject)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(33.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedContent(
                            targetState = Triple(validationOk, isPermissionsOk, currentError?.field),
                        ) { (isValid, hasPermissions, errorField )->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                when{
                                    !isValid -> {
                                        Icon(Icons.Default.AlarmOff, contentDescription = null, tint = Color.White)
                                        Spacer(Modifier.width(8.dp))
                                        val errorText =
                                            if (errorField == AlarmErrorField.AlarmIsNotDiff) {
                                                "New alarm must be different"
                                            } else {
                                                "Fix the input to set alarm"
                                            }
                                        Text(errorText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    // Scenario 2: Form is valid, but system permissions are missing (Priority 2) \
                                    !hasPermissions -> {
                                        Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = Color.White)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Grant required permissions", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    // Scenario 3: Everything is ready (Success state)
                                    else -> {
                                        Icon(Icons.Default.AlarmAdd, contentDescription = null, tint = Color.White)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Set Alarm", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun TimeBox(label: String, time: Calendar, accentColor: Color, modifier: Modifier = Modifier, onNewTimeSelected: (Calendar) -> Unit) {
    var calendar by remember(time) { mutableStateOf(time) }
    var showTimePicker by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f) // Keeps the box height relative to its width
                .border(2.dp, accentColor, RoundedCornerShape(24.dp))
                .background(Color(0xFF0F131A), RoundedCornerShape(24.dp))
                .clickable { showTimePicker = true },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (showTimePicker) {
                    val timePickerState = rememberTimePickerState(
                        initialHour = calendar.get(Calendar.HOUR_OF_DAY), initialMinute = calendar.get(Calendar.MINUTE),
                        is24Hour = false,
                    )
                    AlertDialog(
                        onDismissRequest = { showTimePicker = false },
                        title = { Text(text = "Select ${label.lowercase()}") }, text = {
                            TimePicker(state = timePickerState)
                        },
                        confirmButton = {
                            Button(onClick = {
                                // here we need to update the reference of the calendar object
                                calendar = Calendar.getInstance().apply {
                                    timeInMillis = calendar.timeInMillis
                                    set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                    set(Calendar.MINUTE, timePickerState.minute)
                                    set(Calendar.SECOND, 0)
                                }
                                onNewTimeSelected(calendar)
                                showTimePicker = false
                            }) {
                                Text("OK")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showTimePicker = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                Text(label, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(getTimeFormatted(calendar), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text(getTimeFormatted(calendar, "a"), color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable fun CardContainer(modifier: Modifier = Modifier, endSpaceHeight: Dp = 16.dp, shape: Shape = RoundedCornerShape(26.dp), color: Color = Color(0xFF1C222B), content: @Composable (() -> Unit)) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = color
    ) { content()}
    Spacer(Modifier.height(endSpaceHeight))
}

@Composable fun ShowErrorMessageIfError( currentError: ValidationResult.Failure?, alarmErrorField: AlarmErrorField){
    AnimatedVisibility(
        visible = currentError != null && currentError.field == alarmErrorField,
        enter = fadeIn() + expandHorizontally(),
        exit = fadeOut() + shrinkHorizontally()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(7.dp))
            Text(currentError?.message ?: "",color = Color.White, fontSize = 12.sp )
        }
    }

}

fun getTimeFormatted(cal: Calendar, formatter:String = "hh:mm"): String{
    return DateFormat.format(formatter, cal.timeInMillis).toString()
}

fun getPreviewAlarms(alarm: AlarmObject, numberOfAlarmPreviewToReturn:Int = 3): String{
    val alarmObj = alarm.deepCopy()
    val stringBuilder= StringBuilder()
    val timeFormat = SimpleDateFormat("h:mm", Locale.getDefault())
    var index = 0

    while (!alarmObj.startTime.after(alarmObj.endTime) && index < numberOfAlarmPreviewToReturn) {
        stringBuilder.append(timeFormat.format(alarmObj.startTime.time))
        alarmObj.startTime.timeInMillis += alarmObj.getFreqInMillisecond()
        if (alarmObj.freqGottenAfterCallback <= 0) break
        index ++
        if (index < numberOfAlarmPreviewToReturn && !alarmObj.startTime.after(alarmObj.endTime)) {
            stringBuilder.append(", ")
        }
    }

    return if(alarmObj.startTime.after(alarmObj.endTime)){
        stringBuilder.toString().trim()
    }else{
        stringBuilder.append(".....${timeFormat.format(alarmObj.endTime.time)}").toString().trim()
    }
}

