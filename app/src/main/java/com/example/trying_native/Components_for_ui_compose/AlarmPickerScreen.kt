package com.example.trying_native.Components_for_ui_compose

import android.icu.util.Calendar
import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmAdd
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trying_native.dataBase.AlarmData

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable fun AlarmPickerScreen(alarm: AlarmData?, onDismissRequestFun: () -> Unit){
    //if the alarm is null then it's for a new alarm else we are editing an alarm
    val coroutineScope = rememberCoroutineScope()
    var startTime by remember { mutableStateOf(Calendar.getInstance() ) }
    var endTime by remember { mutableStateOf(
        // time is +45 min current if more than the day then leave it unchanged
        Calendar.getInstance().apply {
            add(Calendar.MINUTE ,45)
            if (get(Calendar.DAY_OF_YEAR) != startTime.get(Calendar.DAY_OF_YEAR)) { timeInMillis = startTime.timeInMillis }
        }
    ) }
    var frequency by remember { mutableStateOf<Int?>(1) }
    var message by remember { mutableStateOf<String>("") }
    val listOfDays = remember { mutableStateListOf('M', 'T', 'W', 'T', 'F', 'S', 'S') }
    var accentColor by remember {mutableStateOf(Color(0xFF1A73E8)) }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
//            .imePadding(),
    ) { contentPadding->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F131A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F131A)) // Deep dark background
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
                    .imePadding()
                     .padding(horizontal = 20.dp)
                    .padding(bottom = contentPadding.calculateBottomPadding() + 10.dp)
                    .padding(top = contentPadding.calculateTopPadding() + 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally

            ) {
                // --- Header ---
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).padding(start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Cancel", color = Color.Gray, fontSize = 16.sp, )
                    Text(if (alarm == null)"New Schedule" else "Edit Alarm" , color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Save", color = Color(0xFF3F8CFF), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
                // --- Time Range Selector ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimeBox("START TIME", startTime, isSelected = true, onNewTimeSelected = {newSelectedTime-> startTime = newSelectedTime})
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    TimeBox("END TIME", endTime, isSelected = true, onNewTimeSelected = {newSelectedTime-> endTime = newSelectedTime})
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Repeats / Day Picker ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Repeats", color = Color.Gray)
                    Text("Weekdays", color = Color(0xFF3F8CFF))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOfDays.forEach { day ->
                        var isSelected = true
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable {isSelected = !isSelected}
                                .background(if (isSelected) Color(0xFF1A73E8) else Color(0xFF1C222B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(day.toString(), color = if (isSelected) Color.White else Color.Gray)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                // --- frequency Section ---
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF1C222B)
                ) {
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
                                Text("Interval Value", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(25.dp))
                                        .background(accentColor),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(onClick = { frequency?.let { if (it > 1) frequency = it - 1  } }) {
                                        Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White)
                                    }
                                    BasicTextField(
                                        value = frequency?.toString() ?: "",
                                        onValueChange = { newValue ->
                                            val filteredValue = newValue.filter { it.isDigit() }
                                            if (filteredValue.isNotEmpty()) {
                                                val intValue = filteredValue.toIntOrNull()
                                                if (intValue != null && intValue > 0 && intValue < 700) {
                                                    frequency = intValue
                                                }
                                            } else if (newValue.isEmpty()) {
                                                frequency = null
                                            }
                                        },
                                        modifier = Modifier.width(226.dp),
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
                                        val freqTmp = frequency
                                        frequency = if (freqTmp != null) freqTmp + 1 else 1
                                    }) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        if (frequency == null){
                            Text("Please enter the frequency value", color = Color.Gray, fontSize = 12.sp)
                        } else{
                            Text(
                                "Alarm will ring every $frequency minutes between ${getTimeFormatted(startTime)} AM and ${getTimeFormatted(endTime)} AM.",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )

                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- Message Section ---
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(25.dp),
                    color = Color(0xFF1C222B)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, tint = accentColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Message", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        BasicTextField(
                            value = message,
                            onValueChange = { message = it },
                            modifier = Modifier
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
                                    if (message.isEmpty()) {
                                        Text(
                                            "Alarm message ....",
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
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { /* Set Alarms Logic */ },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(33.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AlarmAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Set Alarms", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

        }


    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeBox(label: String, time: Calendar, isSelected: Boolean, onNewTimeSelected: (Calendar) -> Unit) {
    val borderColor = if (isSelected) Color(0xFF1A73E8) else Color(0xFF1C222B)
    val timePickerState = rememberTimePickerState(
        initialHour = time.get(Calendar.HOUR_OF_DAY),
        initialMinute = time.get(Calendar.MINUTE),
        is24Hour = false,
    )
    var calendar by remember { mutableStateOf(time) }
//    var amPmString by remember {  derivedStateOf {getTimeFormatted(calendar, "a")} }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(width = 149.dp, height = 100.dp)
                .border(2.dp, borderColor, RoundedCornerShape(24.dp))
                .background(Color(0xFF0F131A), RoundedCornerShape(24.dp))
                .clickable { showTimePicker = true },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (showTimePicker){
                    AlertDialog(
                        onDismissRequest = {showTimePicker = false},
                        title = { Text(text = "Select ${label.lowercase()}") },
                        text = {
                            TimePicker(state = timePickerState)
                        },
                        confirmButton = {
                            Button(onClick = {
                                showTimePicker = false
                               calendar = calendar.apply {set(Calendar.HOUR_OF_DAY, timePickerState.hour); set(Calendar.MINUTE, timePickerState.minute) }
                                onNewTimeSelected(time)
                            }) {
                                Text("OK")
                            }
                        },
                        dismissButton = {
                            Button(onClick = {showTimePicker = false}) { Text("Cancel") }
                        }
                    )
                }

                Text(label, color = if (isSelected) Color(0xFF3F8CFF) else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(getTimeFormatted(time), color = Color.White, fontSize = 35.sp, fontWeight = FontWeight.Bold)
                Text(getTimeFormatted(calendar, "a"), color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

fun getTimeFormatted(cal: Calendar, formatter:String = "hh:mm"): String{
    return DateFormat.format(formatter, cal.timeInMillis).toString()
}
