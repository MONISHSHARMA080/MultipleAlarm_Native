package com.example.trying_native.Components_for_ui_compose

import android.app.AlarmManager
import android.content.ClipData
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.trying_native.AlarmLogic.AlarmsController
import com.example.trying_native.FirstLaunchAskForPermission.FirstLaunchAskForPermission
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmData
import com.example.trying_native.logD
import com.example.trying_native.notification.NotificationBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable fun AlarmListScreen(
	alarms:List<AlarmData>, alarmDao: AlarmDao,
	alarmsController: AlarmsController = AlarmsController(), alarmManager: AlarmManager,
	uncancellableScope: CoroutineScope, activityContext: ComponentActivity,onNavigateToEdit: (AlarmData) -> Unit, onNavigateToCreate: () -> Unit
){
	val coroutineScope = rememberCoroutineScope()
	val screenHeight = LocalConfiguration.current.screenHeightDp.dp
	val fontSize = (screenHeight * 0.05f).value.sp
	val snackBarHostState = remember { SnackbarHostState() }
	val clipBoard =LocalClipboard.current


	Scaffold(contentWindowInsets = WindowInsets.systemBars) { edgeToEdgePadding ->
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
			) { snackBarData ->
				Snackbar(
					snackbarData = snackBarData,
					shape = RoundedCornerShape(45.dp),
					containerColor = Color.Blue,
					contentColor = Color.White,
					modifier = Modifier.fillMaxWidth()
				)
			}
			LazyColumn(
				modifier = Modifier.fillMaxSize(),
				contentPadding = edgeToEdgePadding
			) {
				itemsIndexed(
					alarms,
					key = { _, alarm -> alarm.id }
				) { indexOfIndividualAlarmInAlarm, individualAlarm ->
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
								.pointerInput(Unit) {
									detectTapGestures(
										onTap = { onNavigateToEdit(individualAlarm) },
										onLongPress = { offset ->
											coroutineScope.launch {
												logD("long press copying to clipboard")
												val clip = ClipData.newPlainText("Alarm message", individualAlarm.message)
												clipBoard.setClipEntry(ClipEntry(clip))
												snackBarHostState.showSnackbar("Message copied")
											}
										}
									)
								}
								.background(
									color = if (!individualAlarm.isReadyToUse) {
										Color(0xFF666b75)
									} else {
										Color(0xFF0D388C)
									}
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
										text = individualAlarm.getTimeFormatted(
											individualAlarm.startTime
										),
										fontSize = (fontSize / 1.2),
										fontWeight = FontWeight.Black,
										modifier = Modifier.padding(end = 4.dp)
									)
									Text(
										text = individualAlarm.getFormattedAmPm(
											individualAlarm.startTime
										),
										fontSize = (fontSize / 2.2),
										modifier = Modifier.padding(bottom = 2.dp)
									)
								}
								Column(horizontalAlignment = Alignment.CenterHorizontally) {
									Icon(
										imageVector = Icons.AutoMirrored.Filled.ArrowForward,
										contentDescription = "to",
										modifier = Modifier.size(38.dp),
									)
								}

								Row(verticalAlignment = Alignment.Bottom) {
									Text(
										text = individualAlarm.getTimeFormatted(
											individualAlarm.endTime
										),
										fontSize = (fontSize / 1.2),
										fontWeight = FontWeight.Black,
										modifier = Modifier.padding(end = 4.dp)
									)
									Text(
										text = individualAlarm.getFormattedAmPm(
											individualAlarm.endTime
										),
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
										alarmDao.let { dao ->
											logD("++==${individualAlarm.id} ---- $indexOfIndividualAlarmInAlarm")
											coroutineScope.launch(Dispatchers.IO) {
												alarmsController.cancelAlarmByCancelingPendingIntent(
													context_of_activity = activityContext,
													startTime = individualAlarm.startTime,
													endTime = individualAlarm.endTime,
													frequencyInMin = individualAlarm.getFreqInMillisecond(),
													alarmDao = dao,
													alarmManager = alarmManager,
													delete_the_alarm_from_db = true,
													alarmData = individualAlarm
												)
											}
										}
									},
									colors = ButtonDefaults.buttonColors(Color(0xFF0eaae3))
								) {
									Text("delete")
								}

								Text(
									text = "On: ${individualAlarm.getDateFormatted(individualAlarm.startTime)}",
									textAlign = TextAlign.Right, fontSize = (fontSize / 2.43), fontWeight = FontWeight.W600,
									modifier = Modifier.padding(vertical = screenHeight / 74),
								)
								Button(
									onClick = {
										alarmDao.let { dao ->
											if (individualAlarm.isReadyToUse) {
												coroutineScope.launch(Dispatchers.IO) {
													alarmsController.cancelAlarmByCancelingPendingIntent(
														context_of_activity = activityContext,
														startTime = individualAlarm.startTime,
														endTime = individualAlarm.endTime,
														frequencyInMin = individualAlarm.getFreqInMillisecond(),
														alarmDao = dao,
														alarmManager = alarmManager,
														delete_the_alarm_from_db = false,
														alarmData = individualAlarm
													)
												}
											} else {
												uncancellableScope.launch {
													logD("about to reset the alarm-+")
													val exception = alarmsController.resetAlarms(
														alarmData = individualAlarm,
														alarmManager = alarmManager,
														activityContext = activityContext,
														alarmDao = dao,
													)
													logD("the exception from the resetAlarm is ->$exception")
													exception.fold(
														onSuccess = {},
														onFailure = {
															NotificationBuilder(
																activityContext,
																title = "error returned in creating multiple alarm",
																notificationText = "execution returned exception in schedule multiple alarm -->${exception}"
															).showNotification()
															logD("error in the schedule multiple -->${exception}")
														}
													)
												}
											}
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
			Box(
				modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = screenHeight / 15)
			) {
				RoundPlusIcon(
					size = screenHeight / 10,context = activityContext,
					onClick = {
//						showTheDialogToTheUserToAskForPermission = !showTheDialogToTheUserToAskForPermission
						onNavigateToCreate()
				   },
				)
			}
		}
	}

}

@Composable fun RoundPlusIcon(modifier: Modifier = Modifier, size: Dp , backgroundColor: Color = Color.Blue, onClick: () -> Unit, context:Context) {
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

