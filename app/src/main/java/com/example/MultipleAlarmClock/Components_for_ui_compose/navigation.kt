package com.coolApps.MultipleAlarmClock.Components_for_ui_compose

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
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

import androidx.navigation3.ui.NavDisplay
import androidx.room.Room
import com.coolApps.MultipleAlarmClock.AlarmLogic.AlarmsController
import com.coolApps.MultipleAlarmClock.AlarmLogic.AlarmsController.AlarmValueForAlarmSeries
import com.coolApps.MultipleAlarmClock.Components_for_ui_compose.alarmListScreen.AlarmListScreen
import com.coolApps.MultipleAlarmClock.Components_for_ui_compose.alarmPicker.AlarmPickerScreen
import com.coolApps.MultipleAlarmClock.ErrorHandling.ErrorHandler
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.coolApps.MultipleAlarmClock.dataBase.AlarmDao

import com.coolApps.MultipleAlarmClock.dataBase.AlarmData
import com.coolApps.MultipleAlarmClock.dataBase.AlarmDatabase
import com.coolApps.MultipleAlarmClock.logD
import com.coolApps.MultipleAlarmClock.notification.NotificationHandler
import com.coolApps.MultipleAlarmClock.utils.Result.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.jvm.java


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
	val notificationHandler = NotificationHandler(activityContext)
	val errorHandler = remember { ErrorHandler(notificationHandler, Analytics(context))}
	val analytics = remember {  Analytics(context) }
	val coroutineScope = activityContext.lifecycleScope
	val uncancellableScope = CoroutineScope(coroutineScope.coroutineContext + NonCancellable)



	Scaffold(
		contentWindowInsets = WindowInsets.safeContent,
	) { _->
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
								onNavigateToEdit = { alarm ->
									backStack.add(Screen.AlarmPicker(alarm))
									coroutineScope.launch {
										analytics.screen("AlarmPicker", mapOf("is_to_edit_alarm" to true, "alarmData to edit" to alarm.toString()) )
									}
							   },
								onNavigateToCreate = {
									backStack.add(Screen.AlarmPicker(null))
									coroutineScope.launch {
										analytics.screen("AlarmPicker", mapOf("is_to_create_new_alarm" to true) )
									}
								 },
								alarmDao = alarmDao,
								alarmManager = alarmManager, onAlarmStop = { alarmData ->
									uncancellableScope.launch {
										launch {
											analytics.captureEvent("user stopped the alarm", mapOf(
												"alarmData" to alarmData.toString()
											))
										}
										logD("user asked to stop the alarm $alarmData")
										alarmsController.cancelAlarmHandler(alarmData,  context, alarmManager, alarmDao).fold(onSuccess = {}, onError = {messageToDisplayUser,exception ->
											errorHandler.handleError(Result.Failure(messageToDisplayUser, exception), "Sorry an error occurred while cancelling alarm, Please try again" )
											logD("there is a error/Exception in making new alarm-->${exception.message}")
										})
									}

								}, onAlarmReset = { alarmData ->
									uncancellableScope.launch {
										logD("about to reset the alarm-+")
										launch {
											analytics.captureEvent("user reset the alarm", mapOf(
												"new alarmData" to alarmData.toString()
											))
										}
										val exception = alarmsController.resetAlarms(
											alarmData = alarmData,
											alarmManager = alarmManager,
											activityContext = activityContext,
											alarmDao =alarmDao,
										)
										exception.fold(
											onSuccess = {
												coroutineScope.launch {
													 analytics.captureEvent("alarm successfully reset", mapOf(
														 "alarmData" to alarmData.toString()
													 ))
												}
											},
											onError = { messageToDisplayUser, exception->
												errorHandler.handleError(Result.Failure(messageToDisplayUser, exception), "Sorry an error occurred while resting the  alarm, Please try again" )
												logD("error/exception in the reset alarm -->${exception.message}")
											}
										)
									}
								}, onAlarmDelete = {alarmData ->
									uncancellableScope.launch {
										launch {
											analytics.captureEvent("user deleting the alarm", mapOf(
												"alarmData" to alarmData.toString()
												)
											)
										}
										logD("deleting the alarm $alarmData")
										alarmsController.deleteAlarmHandler(alarmData,  context, alarmDao,  alarmManager).fold(onSuccess = {}, onError = {messageToDisplayUser, exception ->
											logD("there is a error in deleting the alarm  that is $exception ")
											errorHandler.handleError(Result.Failure(messageToDisplayUser, exception), "Sorry an error occurred while deleting the alarm, Please try again" )
										})
									}
								}
							)
							LaunchedEffect(Unit) {
								analytics.screen("AlarmContainer")
							}
						}
					is Screen.AlarmPicker ->						NavEntry(key) {
						/**[ onAlarmSet] - here [ AlarmData] is the alarm passed in the function if it is same to the alarmObject one then do not set the alarm, as user might have miss clicked it*/
						AlarmPickerScreen(key.alarmData, analytics=analytics, onAlarmSet =  { newAlarmObject, oldAlarm ->
							when ( oldAlarm) {
								null -> {
									//  oldAlarm was not there so setting a new alarm
									uncancellableScope.launch {
										launch {
											analytics.captureEvent("user setting new alarm", mapOf(
												"alarmObject" to newAlarmObject.toString()
											))
										}
										logD("the alarm data confirmed is $newAlarmObject, and is  oldAlarm == newAlarmObject -> ${newAlarmObject.isOk( oldAlarm)} ")
										val exception = alarmsController.startAlarmSeriesHandler(
											alarm = AlarmValueForAlarmSeries.AlarmObjectType(newAlarmObject),
											alarmManager = alarmManager,
											activityContext = context,
											alarmDao = alarmDao,
										)
										exception.fold(
											onSuccess = {
												launch {
													analytics.captureEvent("new alarm successfully set", mapOf("alarmObject" to newAlarmObject.toString()))
												}
											},
											onError = {messageToDisplayUser, exception ->
												logD("there is a error in making new alarm  that is $exception ")
												errorHandler.handleError(Result.Failure(messageToDisplayUser, exception), "Sorry an error occurred while making new alarm, Please try again" )
											}
										)
									}
								}
								else -> {
									//  oldAlarm was there so editing an existing alarm
									uncancellableScope.launch {
										logD("deleting the alarm $ oldAlarm")
										alarmsController.updateAlarmStateInDb( oldAlarm, alarmDao).fold(onSuccess = {}, onError = { messageToDisplayUser, exception  ->
											// no such alarm exist in DB so can't update it
												logD("there is a error while editing the alarm and updating it's state in DB and  that is ${exception.message} ")
												errorHandler.handleError(Result.Failure(messageToDisplayUser, exception), "Sorry an error occurred while editing alarm, Please try again" )
											}
										)
										val alarmScheduledResult = alarmsController.startAlarmSeriesHandler(
											alarm = AlarmValueForAlarmSeries.AlarmDataType(newAlarmObject.toAlarmData(oldAlarm.id) ),
											alarmManager, activityContext, alarmDao
										)
										// now the error case is handled there
										alarmScheduledResult.fold(
											onSuccess = { },
											onError = { messageToDisplayUser, exception ->
												errorHandler.handleError(Result.Failure(messageToDisplayUser, exception), "Sorry an error occurred while editing alarm, Please try again" )
												logD("there is a error/Exception in editing new alarm-->${exception.message}")
											}
										)

									}
								}
							}
						}, alarmSetGoBack = { backStack.removeLastOrNull() })
						LaunchedEffect(Unit) {
							analytics.screen("AlarmPicker")
						}
					}
					else ->
						NavEntry(key) {
							AlarmContainer(
								activityContext = activityContext,
								onNavigateToEdit = { alarm ->
									backStack.add(Screen.AlarmPicker(alarm))
									coroutineScope.launch {
										analytics.screen("AlarmPicker", mapOf("is_to_edit_alarm" to true, "alarmData to edit" to alarm.toString()) )
									}
								},
								onNavigateToCreate = {
									backStack.add(Screen.AlarmPicker(null))
									coroutineScope.launch {
										analytics.screen("AlarmPicker", mapOf("is_to_create_new_alarm" to true) )
									}
								},
								alarmDao = alarmDao,
								alarmManager = alarmManager, onAlarmStop = { alarmData ->
									uncancellableScope.launch {
										launch {
											analytics.captureEvent("user stopped the alarm", mapOf(
												"alarmData" to alarmData.toString()
											))
										}
										alarmsController.cancelAlarmHandler(alarmData,  context, alarmManager, alarmDao).fold(onSuccess = {}, onError = {messageToDisplayUser,exception ->
											errorHandler.handleError(Result.Failure(messageToDisplayUser, exception), "Sorry an error occurred while cancelling alarm, Please try again" )
											logD("there is a error/Exception in making new alarm-->${exception.message}")
										})
									}

								}, onAlarmReset = { alarmData ->
									uncancellableScope.launch {
										logD("about to reset the alarm-+")
										launch {
											analytics.captureEvent("user reset the alarm", mapOf(
												"new alarmData" to alarmData.toString()
											))
										}

										val exception = alarmsController.resetAlarms(
											alarmData = alarmData,
											alarmManager = alarmManager,
											activityContext = activityContext,
											alarmDao =alarmDao,
										)
										exception.fold(
											onSuccess = {},
											onError = { messageToDisplayUser, exception->
												errorHandler.handleError(Result.Failure(messageToDisplayUser, exception), "Sorry an error occurred while resting the  alarm, Please try again" )
												logD("error/exception in the reset alarm -->${exception.message}")
											}
										)
									}
								}, onAlarmDelete = {alarmData ->
									uncancellableScope.launch {
										launch {
											analytics.captureEvent("user deleting the alarm", mapOf(
												"new alarmData" to alarmData.toString()
											))
										}

										logD("deleting the alarm $alarmData")
										alarmsController.deleteAlarmHandler(alarmData,  context, alarmDao,  alarmManager).fold(onSuccess = {}, onError = {messageToDisplayUser, exception ->
											logD("there is a error in deleting the alarm  that is $exception ")
											errorHandler.handleError(Result.Failure(messageToDisplayUser, exception), "Sorry an error occurred while deleting the alarm, Please try again" )
										})
									}
								}
							)
							LaunchedEffect(Unit) {
								analytics.screen("AlarmContainer", mapOf("from_else_branch_in_navigation" to true))
							}
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
