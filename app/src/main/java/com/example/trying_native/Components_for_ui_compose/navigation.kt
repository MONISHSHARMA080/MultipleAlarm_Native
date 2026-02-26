package com.example.trying_native.Components_for_ui_compose

import android.app.AlarmManager
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack

import androidx.navigation3.ui.NavDisplay
import androidx.room.Room
import com.example.trying_native.AlarmLogic.AlarmsController
import com.example.trying_native.Components_for_ui_compose.alarmListScreen.AlarmListScreen
import com.example.trying_native.Components_for_ui_compose.alarmPicker.AlarmPickerScreen
import com.example.trying_native.dataBase.AlarmDao

import com.example.trying_native.dataBase.AlarmData
import com.example.trying_native.dataBase.AlarmDatabase
import com.example.trying_native.logD
import com.example.trying_native.notification.NotificationBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable


sealed interface Screen : NavKey {
	@Serializable
	data object AlarmContainer : Screen
	@Serializable
	data class AlarmPicker(val alarmData: AlarmData? = null) : Screen
}

@Composable fun NavigationStack(activityContext: ComponentActivity) {
	val backStack = rememberNavBackStack(Screen.AlarmContainer)
	val alarmDao = remember { Room.databaseBuilder(activityContext.applicationContext, AlarmDatabase::class.java, "alarm-database").build().alarmDao() }
	val context = LocalContext.current
	val alarmManager = remember { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }
	val alarmsController = AlarmsController()

	val coroutineScope = activityContext.lifecycleScope
	val uncancellableScope = CoroutineScope(coroutineScope.coroutineContext + NonCancellable)
	Scaffold(contentWindowInsets = WindowInsets.systemBars) { _->
		NavDisplay(
			backStack=backStack,
			onBack = { backStack.removeLastOrNull()},
			transitionSpec = {
				slideInHorizontally(
					animationSpec = tween(200, easing = FastOutSlowInEasing), initialOffsetX = { it }
				) + fadeIn(tween(150, easing = LinearEasing)) togetherWith
						slideOutHorizontally(animationSpec = tween(200, easing = FastOutSlowInEasing), targetOffsetX = { -it }) +
						fadeOut(tween(100, easing = LinearEasing))
			},
			popTransitionSpec = {
				slideInHorizontally(
					animationSpec = tween(200, easing = FastOutSlowInEasing), initialOffsetX = { -it }) +
						fadeIn(tween(150, easing = LinearEasing)) togetherWith
						slideOutHorizontally(animationSpec = tween(200, easing = FastOutSlowInEasing), targetOffsetX = { it }) +
						fadeOut(tween(100, easing = LinearEasing))
			},
			predictivePopTransitionSpec = {
				slideInHorizontally(animationSpec = tween(180, easing = FastOutSlowInEasing), initialOffsetX = { (-it * 0.3f).toInt() }) +
						fadeIn(tween(130, easing = LinearEasing)) togetherWith
						slideOutHorizontally(animationSpec = tween(180, easing = FastOutSlowInEasing), targetOffsetX = { it }) +
						fadeOut(tween(90, easing = LinearEasing))
			},

			entryProvider = { key ->
				when(key) {
					is Screen.AlarmContainer -> NavEntry(key) {
							AlarmContainer(
								activityContext = activityContext,
								onNavigateToEdit = { alarm -> backStack.add(Screen.AlarmPicker(alarm)) },
								onNavigateToCreate = { backStack.add(Screen.AlarmPicker(null)) },
								alarmDao = alarmDao,
								alarmManager = alarmManager, onAlarmStop = { alarmData ->
									uncancellableScope.launch {
										alarmsController.cancelAlarmHandler(alarmData,  context, alarmManager, alarmDao).fold(onSuccess = {}, onFailure = {exception ->
											NotificationBuilder(
												activityContext,
												title = "error in cancelling the alarm",
												notificationText = "got error while trying to cancel the alarm, exception: $exception"
											).showNotification()
										})
									}

								}, onAlarmReset = { alarmData ->
									uncancellableScope.launch {
										logD("about to reset the alarm-+")
										val exception = alarmsController.resetAlarms(
											alarmData = alarmData,
											alarmManager = alarmManager,
											activityContext = activityContext,
											alarmDao =alarmDao,
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
								}, onAlarmDelete = {alarmData ->
									uncancellableScope.launch {
										logD("deleting the alarm $alarmData")
										alarmsController.deleteAlarmHandler(alarmData,  context, alarmDao,  alarmManager).fold(onSuccess = {}, onFailure = { exception ->
											logD("there is a error in deleting the alarm  that is $exception ")
											NotificationBuilder(
												activityContext,
												title = "error returned in deleting alarm",
												notificationText = "error in deleting alarm was: $exception"
											).showNotification()

										})
									}
								}
							)
						}
					is Screen.AlarmPicker ->						NavEntry(key) {
						/**[ onAlarmSet] - here [ AlarmData] is the alarm passed in the function if it is same to the alarmObject one then do not set the alarm, as user might have miss clicked it*/
						AlarmPickerScreen(key.alarmData, { newAlarmObject, alarmData ->
							when (alarmData) {
								null -> {
									// alarmData was not there so setting a new alarm
									uncancellableScope.launch {
										logD(
											"the alarm data confirmed is $newAlarmObject, and is alarmData == newAlarmObject -> ${
												newAlarmObject.isOk(
													alarmData
												)
											} "
										)
										val exception = alarmsController.scheduleMultipleAlarms(
											alarmManager,
											alarmDao = alarmDao,
											messageForDB = newAlarmObject.message,
											calendarForStartTime = newAlarmObject.startTime,
											calendarForEndTime = newAlarmObject.endTime,
											freqAfterTheCallback = newAlarmObject.freqGottenAfterCallback.toInt(),
											activityContext = activityContext,
											dateInLong = newAlarmObject.date,
										)
										exception.fold(
											onSuccess = { },
											onFailure = { excp ->
												NotificationBuilder(
													context = activityContext,
													title = "there is a error/Exception in making new alarm",
													notificationText = excp.message
														?: "Can't set you alarm please retry"
												).showNotification()
												logD("there is a error/Exception in making new alarm-->${excp}")
											}
										)
									}
								}
								else -> {
									// alarmData was there so editing an existing alarm
									uncancellableScope.launch {
										logD("deleting the alarm $alarmData")
										alarmsController.deleteAlarmHandler(alarmData,  context, alarmDao,  alarmManager).fold(onSuccess = {}, onFailure = { exception ->
											logD("there is a error in deleting the alarm  that is $exception ")
											NotificationBuilder(
												activityContext,
												title = "error returned in deleting alarm",
												notificationText = "error in deleting alarm was: $exception"
											).showNotification()
										})
										val exception = alarmsController.scheduleMultipleAlarms(
											alarmManager,
											alarmDao = alarmDao,
											messageForDB = newAlarmObject.message,
											calendarForStartTime = newAlarmObject.startTime,
											calendarForEndTime = newAlarmObject.endTime,
											freqAfterTheCallback = newAlarmObject.freqGottenAfterCallback.toInt(),
											activityContext = activityContext,
											dateInLong = newAlarmObject.date,
										)
										exception.fold(
											onSuccess = { },
											onFailure = { excp ->
												NotificationBuilder(
													context = activityContext,
													title = "there is a error/Exception in making new alarm",
													notificationText = excp.message
														?: "Can't set you alarm please retry"
												).showNotification()
												logD("there is a error/Exception in making new alarm-->${excp}")
											}
										)
									}
								}
							}
						}, alarmSetGoBack = { backStack.removeLastOrNull() })
					}
					else ->
						NavEntry(key) {
							AlarmContainer(
								activityContext = activityContext,
								onNavigateToEdit = { alarm -> backStack.add(Screen.AlarmPicker(alarm)) },
								onNavigateToCreate = { backStack.add(Screen.AlarmPicker(null)) },
								alarmDao = alarmDao,
								alarmManager = alarmManager, onAlarmStop = { alarmData ->
									uncancellableScope.launch {
										alarmsController.cancelAlarmHandler(alarmData,  context, alarmManager, alarmDao).fold(onSuccess = {}, onFailure = {exception ->
											NotificationBuilder(
												activityContext,
												title = "error in cancelling the alarm",
												notificationText = "got error while trying to cancel the alarm, exception: $exception"
											).showNotification()
										})
									}

								}, onAlarmReset = { alarmData ->
									uncancellableScope.launch {
										logD("about to reset the alarm-+")
										val exception = alarmsController.resetAlarms(
											alarmData = alarmData,
											alarmManager = alarmManager,
											activityContext = activityContext,
											alarmDao =alarmDao,
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
								}, onAlarmDelete = {alarmData ->
									uncancellableScope.launch {
										logD("deleting the alarm $alarmData")
										alarmsController.deleteAlarmHandler(alarmData,  context, alarmDao,  alarmManager).fold(onSuccess = {}, onFailure = { exception ->
											logD("there is a error in deleting the alarm  that is $exception ")
											NotificationBuilder(
												activityContext,
												title = "error returned in deleting alarm",
												notificationText = "error in deleting alarm was: $exception"
											).showNotification()

										})
									}
								}
							)
						}
				}
			}
		)
	}
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmContainer(activityContext: ComponentActivity, alarmDao: AlarmDao, alarmManager: AlarmManager,
				   onNavigateToEdit: (AlarmData) -> Unit, onNavigateToCreate: () -> Unit , onAlarmDelete:(AlarmData) ->Unit, onAlarmStop:(AlarmData) -> Unit, onAlarmReset:(AlarmData) -> Unit
) {
	val coroutineScope = activityContext.lifecycleScope
	val uncancellableScope = CoroutineScope(coroutineScope.coroutineContext + NonCancellable)
	AlarmListScreen(
		alarmManager = alarmManager,
		alarmDao = alarmDao,
		onNavigateToEdit = onNavigateToEdit,
		onNavigateToCreate = onNavigateToCreate,
		uncancellableScope = uncancellableScope,
		activityContext = activityContext,
		onAlarmDelete = {alarmData -> onAlarmDelete(alarmData)},
		onAlarmStop = {alarmData ->onAlarmStop(alarmData)},
		onAlarmReset = {alarmData ->onAlarmReset(alarmData)},
	)
}
