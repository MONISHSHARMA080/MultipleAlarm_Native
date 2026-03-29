package com.coolApps.MultipleAlarmClock.Components_for_ui_compose.alarmPicker

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.CalendarMonth
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.coolApps.MultipleAlarmClock.dataBase.AlarmData
import com.coolApps.MultipleAlarmClock.dataBase.AlarmErrorField
import com.coolApps.MultipleAlarmClock.dataBase.AlarmObject
import com.coolApps.MultipleAlarmClock.dataBase.ValidationResult
import com.coolApps.MultipleAlarmClock.logD
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class AccentColor(val value:Color) {
     Ok(Color(0xFF1A73E8)),
    Problem(Color(0xFFde0707))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
/**[onAlarmSet] - here [AlarmData] is the alarm passed in the function if it is same to the alarmObject one then do not set the alarm, as user might have miss clicked it*/
@Composable fun AlarmPickerScreen(alarm: AlarmData? , onAlarmSet: (AlarmObject, AlarmData?) -> Unit , alarmSetGoBack: () -> Unit, analytics: Analytics){
    // if the alarm is null then it's for a new alarm else we are editing an alarm
    val coroutineScope = rememberCoroutineScope()
    val now = Calendar.getInstance()
    var alarmObject by remember { mutableStateOf(
        alarm?.toAlarmObject() ?: AlarmObject(
            startTime = (now.clone() as Calendar).apply {
                add(Calendar.MINUTE, 1)
                // If adding 1 min pushed us to tomorrow, cap at 23:59 today
                if (get(Calendar.DAY_OF_YEAR) != now.get(Calendar.DAY_OF_YEAR)) {
                    set(Calendar.DAY_OF_YEAR, now.get(Calendar.DAY_OF_YEAR)) // Reset to today
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                }
                set(Calendar.SECOND, 0)
            },
            endTime = (now.clone() as Calendar).apply {
                add(Calendar.MINUTE, 45)
                // If adding 45 mins pushed us to tomorrow, cap at 23:59 today
                if (get(Calendar.DAY_OF_YEAR) != now.get(Calendar.DAY_OF_YEAR)) {
                    set(Calendar.DAY_OF_YEAR, now.get(Calendar.DAY_OF_YEAR)) // Reset to today
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                }
                set(Calendar.SECOND, 0)
            },
            date = Calendar.getInstance().timeInMillis,
            message = alarm?.message ?: "",
            freqGottenAfterCallback = alarm?.frequencyInMin ?: 1
        )
    )
    }

    val validationResult by remember { derivedStateOf {
        alarmObject.validate(alarm)
    } }

    val weGood: Boolean by remember { derivedStateOf {
        when(validationResult){
            is ValidationResult.Success -> true
            is ValidationResult.Failure -> false
        }
    } }

    val validationErrorMessage by remember { derivedStateOf {
        when(val res = validationResult){
            is ValidationResult.Success -> ""
            is ValidationResult.Failure -> res.message
        }
    } }

    val currentError = validationResult as? ValidationResult.Failure
//    val currentError by remember { derivedStateOf {  validationResult as? ValidationResult.Failure } }


    LaunchedEffect(validationErrorMessage) {
        logD("validation error: $validationErrorMessage")
    }

    val freqText by remember { derivedStateOf {
        if (weGood) "your alarm will ring on "+getPreviewAlarms(alarmObject, 4) else ""
    } }
//    val accentColor by remember { derivedStateOf { logD("weGood: $weGood"); if (weGood) AccentColor.Ok.value else AccentColor.Problem.value  } }
    val accentColor by animateColorAsState(
        targetValue = if (weGood) AccentColor.Ok.value else AccentColor.Problem.value ,
        animationSpec = tween(durationMillis = 180),
        label = "accent_color_animation"
    )
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(weGood) {
        analytics.captureEvent("is alarmObject value valid changed", mapOf(
            "weGood" to weGood,
            "alarmObject" to alarmObject.toString(),
            "alarmData" to alarm.toString()
        ))
    }


    Scaffold(
        contentWindowInsets = WindowInsets.safeContent,
        modifier = Modifier.fillMaxSize()
    ) { contentPadding->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F131A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth().background(Color(0xFF0F131A)).verticalScroll(rememberScrollState())
                    .fillMaxSize().navigationBarsPadding().animateContentSize()
                    .padding(horizontal = 20.dp).padding(top = contentPadding.calculateTopPadding() + 12.dp)
                    .padding(bottom = contentPadding.calculateBottomPadding() + 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- Header ---
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).padding(start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (alarm == null)"New alarm" else "Edit Alarm" , color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                // --- Time Range Selector ---
                CardContainer {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
                        ) {
                            TimeBox("Start time", alarmObject.startTime,  accentColor, onNewTimeSelected = {newSelectedTime-> alarmObject = alarmObject.copy(startTime = newSelectedTime) })
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                            TimeBox("End time", alarmObject.endTime,  accentColor, onNewTimeSelected = {newSelectedTime-> alarmObject = alarmObject.copy(endTime = newSelectedTime) })
                        }
                        if (currentError != null && currentError.field == AlarmErrorField.Time){
                            Text(currentError.message)
                        }
                    }
                }
                // --- Repeats / Day Picker ---
                CardContainer {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Date", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        DateList(
                            startDateIndex =alarm?.startTime,
                            weGood = weGood,
                            onSelect = {calVersion ->
                                logD(" updated date is :${getTimeFormatted(calVersion, "hh:mm dd/MM/yyyy")}")
                                val newStartDate = (alarmObject.startTime.clone() as Calendar).apply {
                                    set(Calendar.DAY_OF_YEAR, calVersion.get(Calendar.DAY_OF_YEAR))
                                    set(Calendar.YEAR, calVersion.get(Calendar.YEAR))
                                }
                                val newEndDate = (alarmObject.endTime.clone() as Calendar).apply {
                                    set(Calendar.DAY_OF_YEAR, calVersion.get(Calendar.DAY_OF_YEAR))
                                    set(Calendar.YEAR, calVersion.get(Calendar.YEAR))
                                }
                                alarmObject = alarmObject.copy(
                                    date = calVersion.timeInMillis,
                                    startTime = newStartDate,
                                    endTime = newEndDate
                                )
                                logD("updated the alarmObject for new date and it is $alarmObject")
                            }
                        )
                    }

                }
                // --- frequency Section ---
                CardContainer {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Frequency", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Interval Value Stepper
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(25.dp))
                                        .background(accentColor),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(onClick = { alarmObject = alarmObject.copy(freqGottenAfterCallback = alarmObject.freqGottenAfterCallback - 1) }) {
                                        Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White)
                                    }
                                    BasicTextField(
                                        value = if (alarmObject.freqGottenAfterCallback !in 1..710) "" else alarmObject.freqGottenAfterCallback.toString() ,
                                        onValueChange = { newValue ->
                                            val filteredValue = newValue.filter { it.isDigit() }
                                            if (filteredValue.isEmpty()) {
                                                alarmObject = alarmObject.copy(freqGottenAfterCallback = 0)
                                            } else {
                                                val intValue = filteredValue.toLongOrNull()
                                                if (intValue != null && intValue in 1..709) {
                                                    alarmObject = alarmObject.copy(freqGottenAfterCallback = intValue)
                                                }
                                            }
                                        },
                                        modifier = Modifier.width(226.dp)
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
                                    IconButton(onClick = {
                                        val freqTmp = alarmObject.freqGottenAfterCallback
                                        val finalFreq= if (freqTmp >= 1) freqTmp + 1 else 1
                                        alarmObject = alarmObject.copy( freqGottenAfterCallback = finalFreq)
                                    }) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        if ( alarmObject.freqGottenAfterCallback < 1){
                            Text("Please enter the frequency value", color = Color.Gray, fontSize = 12.sp)
                        } else{
                            Text(
                                freqText,
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                CardContainer {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, tint = accentColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Message", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        BasicTextField(
                            value = alarmObject.message,
                            onValueChange = { alarmObject= alarmObject.copy(message = it) },
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
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 14.sp
                            ),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (alarmObject.message.isEmpty()) {
                                        Text(
                                            "Alarm message......",
                                            color = Color.DarkGray,
                                            fontSize = 14.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }
                Button(
                    onClick = {

                        if (weGood && alarmObject.validate(alarm) == ValidationResult.Success) {
                            alarmObject.startTime.set(Calendar.SECOND, 0)
                            alarmObject.endTime.set(Calendar.SECOND, 0)
                            onAlarmSet(alarmObject, alarm)
                            alarmSetGoBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(33.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedContent(targetState = weGood) { isGood ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isGood) {
                                    Icon(Icons.Default.AlarmAdd, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Set Alarm", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                } else {
                                    Icon(Icons.Default.AlarmOff, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Fix the input to set alarm", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
@Composable fun TimeBox(label: String, time: Calendar, accentColor: Color, onNewTimeSelected: (Calendar) -> Unit) {
    var calendar by remember { mutableStateOf(time) }
    var showTimePicker by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(width = 149.dp, height = 100.dp)
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
                Text(getTimeFormatted(calendar), color = Color.White, fontSize = 35.sp, fontWeight = FontWeight.Bold)
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

