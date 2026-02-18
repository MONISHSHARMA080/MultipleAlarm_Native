package com.example.trying_native.Components_for_ui_compose

import android.app.AlarmManager
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

import androidx.navigation3.ui.NavDisplay
import androidx.room.Room
import com.example.trying_native.AlarmLogic.AlarmsController
import com.example.trying_native.components_for_ui_compose.AlarmContainer

import com.example.trying_native.dataBase.AlarmData
import com.example.trying_native.dataBase.AlarmDatabase
import com.example.trying_native.logD
import com.example.trying_native.notification.NotificationBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.Calendar


sealed interface Screen : NavKey {

	@Serializable
	data object AlarmContainer : Screen


	@Serializable
	data class AlarmPicker(val alarmData: AlarmData? = null) : Screen
}
@Composable
fun NavigationStack(activityContext: ComponentActivity) {
	val backStack = rememberNavBackStack(Screen.AlarmContainer)
	val alarmDao = Room.databaseBuilder(activityContext.applicationContext, AlarmDatabase::class.java, "alarm-database").build().alarmDao()
	val context = LocalContext.current
	val alarmManager = remember { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }
	val alarmsController = AlarmsController()

	val coroutineScope = activityContext.lifecycleScope
	val uncancellableScope = CoroutineScope(coroutineScope.coroutineContext + NonCancellable)

	NavDisplay(
		backStack=backStack,
		onBack = { backStack.removeLastOrNull()},
		transitionSpec = {
			slideInHorizontally (initialOffsetX = { it },) togetherWith slideOutHorizontally (targetOffsetX = { -it })
		},
		popTransitionSpec = {
			slideInHorizontally(initialOffsetX = { -it } ) togetherWith slideOutHorizontally(targetOffsetX = { it }, )
		},
		predictivePopTransitionSpec = {
			slideInHorizontally(initialOffsetX = { -it },  ) togetherWith slideOutHorizontally(targetOffsetX = { it })
		},

		entryProvider = {key ->
			when(key){
				is Screen.AlarmContainer -> NavEntry(key){
					AlarmContainer(
						activityContext = activityContext,
						onNavigateToEdit = { alarm -> backStack.add(Screen.AlarmPicker(alarm))},
						onNavigateToCreate = { backStack.add(Screen.AlarmPicker( null)) },
						alarmDao = alarmDao,alarmManager=alarmManager
					)
				}
				is Screen.AlarmPicker -> NavEntry(key){
					/**[ onAlarmSet] - here [ AlarmData] is the alarm passed in the function if it is same to the alarmObject one then do not set the alarm, as user might have miss clicked it*/
					AlarmPickerScreen(key.alarmData, { alarmObject, alarmData->
						uncancellableScope.launch {
							logD("the alarm data confirmed is $alarmObject")
							val exception = alarmsController.scheduleMultipleAlarms(
								alarmManager,
								alarmDao = alarmDao,
								messageForDB = alarmObject.message,
								calendarForStartTime = alarmObject.startTime,
								calendarForEndTime = alarmObject.endTime,
								freqAfterTheCallback = alarmObject.freqGottenAfterCallback.toInt(),
								activityContext = activityContext,
								dateInLong = alarmObject.date,
							)
							exception.fold(
								onSuccess = { return@launch },
								onFailure = { excp ->
									NotificationBuilder(
										context = activityContext,
										title = "there is a error/Exception in making new alarm",
										notificationText = excp.message ?:"Can't set you alarm please retry"
									).showNotification()
									logD("there is a error/Exception in making new alarm-->${excp}")
								}
							)
						}
				}
					, alarmSetGoBack = {backStack.removeLastOrNull()}

				)}
				else -> { NavEntry(key){
					AlarmContainer(
						activityContext,
						onNavigateToEdit = { alarm -> backStack.add(Screen.AlarmPicker(alarm)) },
						alarmDao = alarmDao,alarmManager=alarmManager,
					    onNavigateToCreate = { backStack.add(Screen.AlarmPicker( null)) }
					) }
				}
			}
		}
	)
}
