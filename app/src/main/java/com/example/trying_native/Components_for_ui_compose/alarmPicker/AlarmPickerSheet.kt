package com.example.trying_native.Components_for_ui_compose.alarmPicker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trying_native.dataBase.AlarmData
import com.example.trying_native.dataBase.AlarmObject
import com.example.trying_native.logD
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable fun AlarmPickerBottomSheet(
	alarm: AlarmData?, onAlarmSet: (AlarmObject) -> Unit,
){
	//if the alarm is null then it's for a new alarm else we are editing an alarm
	val coroutineScope = rememberCoroutineScope()
	var alarmObject by remember { mutableStateOf<AlarmObject>(
		alarm?.toAlarmObject() ?: AlarmObject(
			startTime = Calendar.getInstance(),
			endTime =Calendar.getInstance().apply {
				add(Calendar.MINUTE ,45)
				if (get(Calendar.DAY_OF_YEAR) != Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) {
//                    timeInMillis = Calendar.getInstance().timeInMillis
					// go to the end of the day
					set(Calendar.HOUR_OF_DAY, 23)
					set(Calendar.MINUTE, 59)
				}
			},
			date = Calendar.getInstance().timeInMillis,
			message = "",
			freqGottenAfterCallback = 1
		)
	) }
	val weGood by remember { derivedStateOf { alarmObject.isOk(alarm)  } }
	val accentColor by remember { derivedStateOf { logD("weGood: $weGood"); if (weGood) AccentColor.Ok.value else AccentColor.Problem.value  } }
	val listOfDays = remember { mutableStateListOf('M', 'T', 'W', 'T', 'F', 'S', 'S') }
	val bringIntoViewRequester = remember { BringIntoViewRequester() }
	val sheetState = rememberModalBottomSheetState()
	var showBottomSheet by remember { mutableStateOf(true) }
	if (showBottomSheet){
		ModalBottomSheet(onDismissRequest = {logD("alarmSheet dismissed")},sheetState = sheetState , shape = RoundedCornerShape(30.dp)) {
			Scaffold(modifier = Modifier.fillMaxSize()) { contentPadding->
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
							.padding(horizontal = 20.dp)
							.padding(bottom = contentPadding.calculateBottomPadding() + 10.dp)
							.padding(top = contentPadding.calculateTopPadding() + 12.dp),
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
						Row(
							modifier = Modifier.fillMaxWidth(),
							horizontalArrangement = Arrangement.SpaceEvenly,
							verticalAlignment = Alignment.CenterVertically
						) {
							TimeBox(
								"START TIME",
								alarmObject.startTime,
								accentColor,
								onNewTimeSelected = { newSelectedTime ->
									alarmObject = alarmObject.copy(startTime = newSelectedTime)
								})
							Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
							TimeBox(
								"END TIME",
								alarmObject.endTime,
								accentColor,
								onNewTimeSelected = { newSelectedTime ->
									alarmObject = alarmObject.copy(endTime = newSelectedTime)
								})
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
										.background(accentColor),
									contentAlignment = Alignment.Center
								) {
									Text(day.toString(), color = if (isSelected) Color.White else Color.Gray)
								}
							}
						}
						Spacer(modifier = Modifier.height(26.dp))
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
												value = alarmObject.freqGottenAfterCallback.toString() ?: "",
												onValueChange = { newValue ->
													val filteredValue = newValue.filter { it.isDigit() }
													if (filteredValue.isNotEmpty()) {
														val intValue = filteredValue.toLongOrNull()
														if (intValue != null && intValue > 0 && intValue < 710) {
															alarmObject= alarmObject.copy(freqGottenAfterCallback = intValue )
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
										"Alarm will ring every ${alarmObject.freqGottenAfterCallback} minutes between ${
											getTimeFormatted(
												alarmObject.startTime
											)
										}  and ${getTimeFormatted(alarmObject.endTime)} AM.",
										color = Color.Gray,
										fontSize = 12.sp
									)
								}
							}
						}
						Spacer(modifier = Modifier.height(26.dp))
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
									value = alarmObject.message,
									onValueChange = { alarmObject= alarmObject.copy(message = it) },
									modifier = Modifier
										.bringIntoViewRequester(bringIntoViewRequester)
										.onFocusEvent {
											if (it.isFocused) {
												coroutineScope.launch {
													bringIntoViewRequester.bringIntoView()
												}
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
						Spacer(modifier = Modifier.height(54.dp))
						Button(
							onClick = {
								if (alarmObject.isOk(alarm)) onAlarmSet(alarmObject)
							},
							modifier = Modifier.fillMaxWidth().height(64.dp),
							shape = RoundedCornerShape(33.dp),
							colors = ButtonDefaults.buttonColors(containerColor = accentColor)
						) {
							Row(verticalAlignment = Alignment.CenterVertically) {
								Icon(Icons.Default.AlarmAdd, contentDescription = null, tint = Color.White)
								Spacer(modifier = Modifier.width(8.dp))
								Text("Set Alarm", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
							}
						}
					}
				}
			}

		}
	}
}
