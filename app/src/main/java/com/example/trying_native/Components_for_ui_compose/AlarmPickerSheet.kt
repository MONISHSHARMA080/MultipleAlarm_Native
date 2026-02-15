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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation.Companion.keyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.trying_native.dataBase.AlarmData
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.trying_native.components_for_ui_compose.InputPickerField
import com.example.trying_native.components_for_ui_compose.InputPickerType
import com.example.trying_native.logD
import com.google.common.math.LinearTransformation.horizontal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds


// -------------- add the nav link and make it into a screen I give up on the bottoms sheet



















//
//@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
//@Composable
//fun AlarmPickerSheet(
//    alarm: AlarmData?,
//    onDismissRequestFun: () -> Unit,
//    onSaveAlarm: (AlarmData) -> Unit = {}
//) {
//    val sheetState = rememberModalBottomSheetState(
//        skipPartiallyExpanded = false // Start with partial expansion
//    )
//    val scrollState = rememberScrollState()
//    val coroutineScope = rememberCoroutineScope()
//    val density = LocalDensity.current
//
//    // State variables
//    var startTime by remember { mutableStateOf(Calendar.getInstance()) }
//    var endTime by remember {
//        mutableStateOf(
//            Calendar.getInstance().apply {
//                add(Calendar.MINUTE, 45)
//                if (get(Calendar.DAY_OF_YEAR) != startTime.get(Calendar.DAY_OF_YEAR)) {
//                    timeInMillis = startTime.timeInMillis
//                }
//            }
//        )
//    }
//    var frequency by remember { mutableStateOf<Int?>(1) }
//    var alarmMessage by remember { mutableStateOf("") }
//    val selectedDays = remember { mutableStateListOf<Char>().apply { addAll(listOf('M', 'T', 'W', 'T', 'F')) } }
//    var showBottomSheet by remember { mutableStateOf(true) }
//
//    // Keyboard management
//    val focusRequester = remember { FocusRequester() }
//    val focusManager = LocalFocusManager.current
//
//    // Track keyboard visibility
//    val ime = WindowInsets.ime
//    val imeBottom = ime.getBottom(density)
//    val isKeyboardVisible = imeBottom > 0
//    logD("0000")
//
//    // Expand sheet and scroll when keyboard appears
//    LaunchedEffect(isKeyboardVisible) {
//        if (isKeyboardVisible) {
//            sheetState.expand()
//            delay(300) // Wait for sheet to expand
//            scrollState.animateScrollTo(scrollState.maxValue)
//        }
//    }
//
//    if (showBottomSheet) {
//        ModalBottomSheet(
//            windowInsets = WindowInsets(0, 0, 0, 0),
//            onDismissRequest = {
//                focusManager.clearFocus()
//                onDismissRequestFun()
//                showBottomSheet = false
//            },
//            containerColor = Color(0xFF0F131A),
//            sheetState = sheetState,
//        ) {
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .imePadding()
//                    .verticalScroll(scrollState)
//                    .padding(horizontal = 20.dp)
//                    .padding(bottom = 24.dp),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                // Header
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(vertical = 16.dp),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically,
//                ) {
//                    Text(
//                        "Cancel",
//                        color = Color.Gray,
//                        fontSize = 16.sp,
//                        modifier = Modifier.clickable {
//                            focusManager.clearFocus()
//                            onDismissRequestFun()
//                        }
//                    )
//                    Text(
//                        if (alarm == null) "New Schedule" else "Edit Alarm",
//                        color = Color.White,
//                        fontWeight = FontWeight.Bold,
//                        fontSize = 18.sp
//                    )
//                    Text(
//                        "Save",
//                        color = Color(0xFF3F8CFF),
//                        fontWeight = FontWeight.Bold,
//                        fontSize = 16.sp,
//                        modifier = Modifier.clickable {
//                            focusManager.clearFocus()
//                            onDismissRequestFun()
//                        }
//                    )
//                }
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Time Range Selector
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceEvenly,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    TimeBox("START TIME", startTime,  isSelected = true, onNewTimeSelected = { startTime = it })
//                    Icon(
//                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
//                        contentDescription = null,
//                        tint = Color.Gray,
//                        modifier = Modifier.size(24.dp)
//                    )
//                    TimeBox("END TIME", endTime,  isSelected = true, onNewTimeSelected = { endTime = it })
//                }
//
//                Spacer(modifier = Modifier.height(24.dp))
//
//                // Repeats / Day Picker
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    Text("Repeats", color = Color.Gray)
//                    Text("Weekdays", color = Color(0xFF3F8CFF))
//                }
//
//                Spacer(modifier = Modifier.height(12.dp))
//
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    listOf('M', 'T', 'W', 'T', 'F', 'S', 'S').forEach { day ->
//                        val isSelected = selectedDays.contains(day)
//                        Box(
//                            modifier = Modifier
//                                .size(40.dp)
//                                .clip(CircleShape)
//                                .clickable {
//                                    if (isSelected) selectedDays.remove(day)
//                                    else selectedDays.add(day)
//                                }
//                                .background(if (isSelected) Color(0xFF1A73E8) else Color(0xFF1C222B)),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Text(day.toString(), color = if (isSelected) Color.White else Color.Gray)
//                        }
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(24.dp))
//
//                // Frequency Section
//                Surface(
//                    modifier = Modifier.fillMaxWidth(),
//                    shape = RoundedCornerShape(28.dp),
//                    color = Color(0xFF1C222B)
//                ) {
//                    Column(modifier = Modifier.padding(16.dp)) {
//                        Row(verticalAlignment = Alignment.CenterVertically) {
//                            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF3F8CFF), modifier = Modifier.size(20.dp))
//                            Spacer(modifier = Modifier.width(8.dp))
//                            Text("Frequency", color = Color.White, fontWeight = FontWeight.Bold)
//                        }
//
//                        Spacer(modifier = Modifier.height(16.dp))
//
//                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
//                            Column(modifier = Modifier.weight(1f)) {
//                                Text("Interval Value", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
//                                Row(
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        .height(48.dp)
//                                        .clip(RoundedCornerShape(25.dp))
//                                        .background(Color(0xFF1A73E8)),
//                                    verticalAlignment = Alignment.CenterVertically,
//                                    horizontalArrangement = Arrangement.SpaceBetween
//                                ) {
//                                    IconButton(onClick = {
//                                        frequency?.let { if (it > 1) frequency = it - 1 }
//                                    }) {
//                                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color.White)
//                                    }
//
//                                    BasicTextField(
//                                        value = frequency?.toString() ?: "",
//                                        onValueChange = { newValue ->
//                                            val filteredValue = newValue.filter { it.isDigit() }
//                                            if (filteredValue.isNotEmpty()) {
//                                                frequency = filteredValue.toIntOrNull()?.takeIf { it > 0 }
//                                            } else {
//                                                frequency = null
//                                            }
//                                        },
//                                        modifier = Modifier
//                                            .weight(1f)
//                                            .focusRequester(focusRequester)
//                                            .onFocusChanged { focusState ->
//                                                if (focusState.isFocused) {
//                                                    coroutineScope.launch {
//                                                        sheetState.expand()
//                                                    }
//                                                }
//                                            },
//                                        textStyle = TextStyle(
//                                            color = Color.White,
//                                            fontWeight = FontWeight.Bold,
//                                            fontSize = 16.sp,
//                                            textAlign = TextAlign.Center
//                                        ),
//                                        keyboardOptions = KeyboardOptions(
//                                            keyboardType = KeyboardType.Number,
//                                            imeAction = ImeAction.Done
//                                        ),
//                                        keyboardActions = KeyboardActions(
//                                            onDone = { focusManager.clearFocus() }
//                                        ),
//                                        singleLine = true,
//                                        decorationBox = { innerTextField ->
//                                            Box(
//                                                modifier = Modifier.fillMaxWidth(),
//                                                contentAlignment = Alignment.Center
//                                            ) {
//                                                if (frequency == null) {
//                                                    Text("0", color = Color.White.copy(alpha = 0.3f), fontWeight = FontWeight.Bold, fontSize = 16.sp)
//                                                }
//                                                innerTextField()
//                                            }
//                                        }
//                                    )
//
//                                    IconButton(onClick = { frequency = (frequency ?: 0) + 1 }) {
//                                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White)
//                                    }
//                                }
//                            }
//                        }
//
//                        Spacer(modifier = Modifier.height(12.dp))
//
//                        if (frequency == null) {
//                            Text("Please enter the frequency value", color = Color.Gray, fontSize = 12.sp)
//                        } else {
//                            Text(
//                                "Alarm will ring every $frequency minutes between ${getTimeFormatted(startTime)} and ${getTimeFormatted(endTime)}.",
//                                color = Color.Gray,
//                                fontSize = 12.sp
//                            )
//                        }
//                    }
//                }
//
//                // Hide these when keyboard is visible
//                if (!isKeyboardVisible) {
//                    Spacer(modifier = Modifier.height(16.dp))
//
//                    // Message Section
//                    Surface(
//                        modifier = Modifier.fillMaxWidth(),
//                        shape = RoundedCornerShape(24.dp),
//                        color = Color(0xFF1C222B)
//                    ) {
//                        Column(modifier = Modifier.padding(16.dp)) {
//                            Row(verticalAlignment = Alignment.CenterVertically) {
//                                Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, tint = Color(0xFF3F8CFF))
//                                Spacer(modifier = Modifier.width(8.dp))
//                                Text("Message", color = Color.White, fontWeight = FontWeight.Bold)
//                            }
//                            Spacer(modifier = Modifier.height(8.dp))
//                            BasicTextField(
//                                value = alarmMessage,
//                                onValueChange = { alarmMessage = it },
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .height(80.dp)
//                                    .clip(RoundedCornerShape(12.dp))
//                                    .background(Color(0xFF0F131A))
//                                    .padding(12.dp),
//                                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
//                                decorationBox = { innerTextField ->
//                                    Box(modifier = Modifier.fillMaxSize()) {
//                                        if (alarmMessage.isEmpty()) {
//                                            Text("Alarm message ....", color = Color.DarkGray, fontSize = 14.sp)
//                                        }
//                                        innerTextField()
//                                    }
//                                }
//                            )
//                        }
//                    }
//
//                    Spacer(modifier = Modifier.height(24.dp))
//
//                    Button(
//                        onClick = {
//                            focusManager.clearFocus()
//                            onDismissRequestFun()
//                        },
//                        modifier = Modifier.fillMaxWidth().height(64.dp),
//                        shape = RoundedCornerShape(33.dp),
//                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))
//                    ) {
//                        Row(verticalAlignment = Alignment.CenterVertically) {
//                            Icon(Icons.Default.AlarmAdd, contentDescription = null)
//                            Spacer(modifier = Modifier.width(8.dp))
//                            Text("Set Alarms", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
//                        }
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(16.dp))
//            }
//        }
//    }
//}







//@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
//@Composable fun AlarmPickerSheet(alarm: AlarmData?, onDismissRequestFun: () -> Unit){
//    //if the alarm is null then it's for a new alarm else we are editing an alarm
//    var sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
//    val scrollState = rememberScrollState()
//    val coroutineScope = rememberCoroutineScope()
//    var startTime by remember { mutableStateOf(Calendar.getInstance() ) }
//    var endTime by remember { mutableStateOf(
//        // time is +45 min current if more than the day then leave it unchanged
//        Calendar.getInstance().apply {
//            add(Calendar.MINUTE ,45)
//            if (get(Calendar.DAY_OF_YEAR) != startTime.get(Calendar.DAY_OF_YEAR)) { timeInMillis = startTime.timeInMillis }
//        }
//    ) }
//    var frequency by remember { mutableStateOf<Int?>(1) }
//    val selectedDays = remember { mutableStateListOf('M', 'T', 'W', 'T', 'F', 'S', 'S') }
//    var showBottomSheet by remember { mutableStateOf(true) }
//
//    LaunchedEffect(showBottomSheet) {
//        showBottomSheet = true;
//        logD("showing the bottom sheet, showBottomSheet:$showBottomSheet")
//        delay(0.1.seconds)
//    }
//
//    if (showBottomSheet) {
//        ModalBottomSheet(
////            windowInsets = WindowInsets.ime,
////            windowInsets = BottomSheetDefaults.windowInsets,
////            windowInsets = WindowInsets(0,0,0,0),
//            onDismissRequest = {
//                onDismissRequestFun();
//                showBottomSheet = false
//            },
//            containerColor = Color(0xFF0F131A),
//            sheetState = sheetState,
//        ) {
//            Column(
//                modifier = Modifier
////                    .windowInsetsPadding(WindowInsets.navigationBars)
//                    .fillMaxWidth()
//                    .background(Color(0xFF0F131A)) // Deep dark background
////                    .verticalScroll(scrollState) // Use the scrollState here
//                    .imePadding()
//                    .padding(horizontal = 20.dp)
//                    .padding(bottom = 24.dp),
//                horizontalAlignment = Alignment.CenterHorizontally
//
//            ) {
//                // --- Header ---
//                Row(
//                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically,
//                ) {
//                    Text("Cancel", color = Color.Gray, fontSize = 16.sp)
//                    Text(if (alarm == null)"New Schedule" else "Edit Alarm" , color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
//                    Text("Save", color = Color(0xFF3F8CFF), fontWeight = FontWeight.Bold, fontSize = 16.sp)
//                }
//
//                Spacer(modifier = Modifier.height(16.dp))
//                // --- Time Range Selector ---
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceEvenly,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    TimeBox("START TIME", startTime, "AM", isSelected = true, onNewTimeSelected = {newSelectedTime-> startTime = newSelectedTime})
//                    Icon(
//                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
//                        contentDescription = null,
//                        tint = Color.Gray,
//                        modifier = Modifier.size(24.dp)
//                    )
//                    TimeBox("END TIME", endTime, "AM", isSelected = true, onNewTimeSelected = {newSelectedTime-> endTime = newSelectedTime})
//                }
//
//                Spacer(modifier = Modifier.height(24.dp))
//
//                // --- Repeats / Day Picker ---
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    Text("Repeats", color = Color.Gray)
//                    Text("Weekdays", color = Color(0xFF3F8CFF))
//                }
//
//                Spacer(modifier = Modifier.height(12.dp))
//
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    listOf('M', 'T', 'W', 'T', 'F', 'S', 'S').forEach { day ->
//
//                        var isSelected = true
////                        var isSelected = if (Random.nextFloat() > 0.55) true else false
//                        Box(
//                            modifier = Modifier
//                                .size(40.dp)
//                                .clip(CircleShape)
//                                .clickable {isSelected = !isSelected}
//                                .background(if (isSelected) Color(0xFF1A73E8) else Color(0xFF1C222B)),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Text(day.toString(), color = if (isSelected) Color.White else Color.Gray)
//                        }
//                    }
//                }
//                Spacer(modifier = Modifier.height(24.dp))
//                // --- frequency Section ---
//                Surface(
//                    modifier = Modifier.fillMaxWidth(),
//                    shape = RoundedCornerShape(28.dp),
//                    color = Color(0xFF1C222B)
//                ) {
//                    Column(modifier = Modifier.padding(16.dp)) {
//                        Row(verticalAlignment = Alignment.CenterVertically) {
//                            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF3F8CFF), modifier = Modifier.size(20.dp))
//                            Spacer(modifier = Modifier.width(8.dp))
//                            Text("Frequency", color = Color.White, fontWeight = FontWeight.Bold)
//                        }
//
//                        Spacer(modifier = Modifier.height(16.dp))
//
//                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
//                            // Interval Value Stepper
//                            Column(modifier = Modifier.weight(1f)
//                                .clickable{
//                                    coroutineScope.launch {sheetState.expand() }
//                                }
//                            ) {
//                                Text("Interval Value", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
//                                Row(
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        .height(48.dp)
//                                        .clip(RoundedCornerShape(25.dp))
//                                        .background(Color(0xFF1A73E8)),
//                                    verticalAlignment = Alignment.CenterVertically,
//                                    horizontalArrangement = Arrangement.SpaceBetween
//                                ) {
//                                    IconButton(onClick = { frequency?.let { if (it > 1) frequency = it - 1  } }) {
//                                        Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White)
//                                    }
//                                    BasicTextField(
//                                        value = frequency?.toString() ?: "",
//                                        onValueChange = { newValue ->
//                                            val filteredValue = newValue.filter { it.isDigit() }
//                                            if (filteredValue.isNotEmpty()) {
//                                                val intValue = filteredValue.toIntOrNull()
//                                                if (intValue != null && intValue > 0) {
//                                                    frequency = intValue
//                                                }
//                                            } else if (newValue.isEmpty()) {
//                                                frequency = null
//                                            }
//                                        },
//                                        modifier = Modifier.width(226.dp),
//                                        textStyle = TextStyle(
//                                            color = Color.White,
//                                            fontWeight = FontWeight.Bold,
//                                            fontSize = 16.sp,
//                                            textAlign = TextAlign.Center
//                                        ),
//                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//                                        singleLine = true
//                                    )
//                                    IconButton(onClick = { frequency?.let { frequency = it + 1 } }) {
//                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
//                                    }
//                                }
//                            }
//                        }
//
//                        Spacer(modifier = Modifier.height(12.dp))
//                        if (frequency == null){
//                            Text("Please enter the frequency value", color = Color.Gray, fontSize = 12.sp)
//                        } else{
//                            Text(
//                                "Alarm will ring every $frequency minutes between ${getTimeFormatted(startTime)} AM and ${getTimeFormatted(endTime)} AM.",
//                                color = Color.Gray,
//                                fontSize = 12.sp
//                            )
//
//                        }
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // --- Message Section ---
//                Surface(
//                    modifier = Modifier.fillMaxWidth(),
//                    shape = RoundedCornerShape(24.dp),
//                    color = Color(0xFF1C222B)
//                ) {
//                    Column(modifier = Modifier.padding(16.dp)) {
//                        Row(verticalAlignment = Alignment.CenterVertically) {
//                            Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, tint = Color(0xFF3F8CFF))
//                            Spacer(modifier = Modifier.width(8.dp))
//                            Text("Message", color = Color.White, fontWeight = FontWeight.Bold)
//                        }
//                        Spacer(modifier = Modifier.height(8.dp))
//                        Box(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .height(80.dp)
//                                .clip(RoundedCornerShape(12.dp))
//                                .background(Color(0xFF0F131A))
//                                .padding(12.dp)
//                        ) {
//                            Text("Alarm message ....", color = Color.DarkGray)
//                        }
//                    }
//                }
//                Spacer(modifier = Modifier.height(24.dp))
//                Button(
//                    onClick = { /* Set Alarms Logic */ },
//                    modifier = Modifier.fillMaxWidth().height(64.dp),
//                    shape = RoundedCornerShape(33.dp),
//                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))
//                ) {
//                    Row(verticalAlignment = Alignment.CenterVertically) {
//                        Icon(Icons.Default.AlarmAdd, contentDescription = null)
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Text("Set Alarms", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
//                    }
//                }
//            }
//        }
//    }
//}

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun TimeBox(label: String, time: Calendar, isSelected: Boolean, onNewTimeSelected: (Calendar) -> Unit) {
//    val borderColor = if (isSelected) Color(0xFF1A73E8) else Color(0xFF1C222B)
//    val timePickerState = rememberTimePickerState(
//        initialHour = time.get(Calendar.HOUR_OF_DAY),
//        initialMinute = time.get(Calendar.MINUTE),
//        is24Hour = false,
//    )
//    var showTimePicker by remember { mutableStateOf(false) }
//
//    Column(horizontalAlignment = Alignment.CenterHorizontally) {
//        Box(
//            modifier = Modifier
//                .size(width = 149.dp, height = 100.dp)
//                .border(2.dp, borderColor, RoundedCornerShape(24.dp))
//                .background(Color(0xFF0F131A), RoundedCornerShape(24.dp))
//                .clickable { showTimePicker = true },
//            contentAlignment = Alignment.Center,
//        ) {
//            Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                if (showTimePicker){
//                    AlertDialog(
//                        onDismissRequest = {showTimePicker = false},
//                        title = { Text(text = "Select ${label.lowercase()}") },
//                        text = {
//                            TimePicker(state = timePickerState)
//                        },
//                        confirmButton = {
//                            Button(onClick = {
//                                showTimePicker = false
//                                time.apply {set(Calendar.HOUR, timePickerState.hour); set(Calendar.MINUTE, timePickerState.minute) }
//                                onNewTimeSelected(time)
//                            }) {
//                                Text("OK")
//                            }
//                        },
//                        dismissButton = {
//                            Button(onClick = {showTimePicker = false}) {
//                                Text("Cancel")
//                            }
//                        }
//                    )
//                }
//
//                Text(label, color = if (isSelected) Color(0xFF3F8CFF) else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
//                Text(getTimeFormatted(time), color = Color.White, fontSize = 35.sp, fontWeight = FontWeight.Bold)
//                Text(getTimeFormatted( time, "a"), color = Color.Gray, fontSize = 12.sp)
//            }
//        }
//    }
//}
//
//fun getTimeFormatted(cal: Calendar, formatter:String = "hh:mm"): String{
//    return DateFormat.format(formatter, cal.timeInMillis).toString()
//}