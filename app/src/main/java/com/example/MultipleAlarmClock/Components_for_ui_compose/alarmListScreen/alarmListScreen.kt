package com.coolApps.MultipleAlarmClock.Components_for_ui_compose.alarmListScreen

import android.app.AlarmManager
import android.content.ClipData
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coolApps.MultipleAlarmClock.AlarmLogic.AlarmsController
import com.coolApps.MultipleAlarmClock.Components_for_ui_compose.alarmListScreen.AlarmCard
import com.coolApps.MultipleAlarmClock.FirstLaunchAskForPermission.FirstLaunchAskForPermission
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.coolApps.MultipleAlarmClock.dataBase.AlarmDao
import com.coolApps.MultipleAlarmClock.dataBase.AlarmData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

@Composable fun AlarmListScreen(
	 alarmDao: AlarmDao, alarmsController: AlarmsController = AlarmsController(), alarmManager: AlarmManager, onAlarmDelete:(AlarmData) -> Unit, onAlarmStop:(AlarmData) -> Unit,
	 onAlarmReset:(AlarmData) -> Unit, uncancellableScope: CoroutineScope, activityContext: ComponentActivity,onNavigateToEdit: (AlarmData) -> Unit, onNavigateToCreate: () -> Unit
){
	val screenHeight = LocalConfiguration.current.screenHeightDp.dp
	val snackBarHostState = remember { SnackbarHostState() }
	val clipBoard =LocalClipboard.current
	val alarmsFlow = remember { alarmDao.getAllAlarmsFlow().flowOn(Dispatchers.IO) }
	val alarms by alarmsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
	val accentColor = Color.Blue
	val coroutineScope = rememberCoroutineScope()
	val analytics by lazy{ Analytics(activityContext) }
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
				modifier = Modifier.fillMaxSize() ,
				contentPadding = edgeToEdgePadding
			) {
				itemsIndexed(
					alarms,
					key = { _, alarm -> alarm.id }
				) { _, individualAlarm ->
					AlarmCard(
						individualAlarm, onEdit = {alarmData -> onNavigateToEdit(alarmData)}, onStop = { alarmData -> onAlarmStop(alarmData) },
						onDelete = {alarmData -> onAlarmDelete(alarmData)}, onReset = {alarmData -> onAlarmReset(alarmData)}, onLongPress = { alarmData ->
								uncancellableScope.launch {
									launch {
										analytics.captureEvent("user long pressed the alarm", mapOf(
											"copying the alarm message" to true,
											"is message empty" to alarmData.message.isEmpty(),
											"showing snackBar" to alarmData.message.isNotEmpty()
											)
										)
									}
									if (alarmData.message.isNotEmpty()) {
										clipBoard.setClipEntry(ClipEntry(ClipData.newPlainText("alarm message", alarmData.message)))
										snackBarHostState.showSnackbar("Message copied to clipboard")
									}else{
										snackBarHostState.showSnackbar("Message message not present")
									}
								}
						},
						modifier = Modifier.animateItem()
					)
				}
			}
			Box(
				modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = screenHeight / 15)
			) {
				RoundPlusIcon(
					size = screenHeight / 10,context = activityContext,
					backgroundColor = accentColor,
					onClick = {

						onNavigateToCreate()
						coroutineScope.launch {
							analytics.captureEvent("Plus Icon clicked", mapOf("round plus icon " to "new alarm"))
						}
				  },
				)
			}
		}
	}

}

@Composable fun RoundPlusIcon(modifier: Modifier = Modifier, size: Dp , backgroundColor: Color, onClick: () -> Unit, context:Context) {
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

