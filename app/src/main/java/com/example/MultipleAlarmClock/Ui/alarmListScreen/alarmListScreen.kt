package com.coolApps.MultipleAlarmClock.Components_for_ui_compose.alarmListScreen

import android.content.ClipData
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coolApps.MultipleAlarmClock.FirstLaunchAskForPermission.FirstLaunchAskForPermission
import com.coolApps.MultipleAlarmClock.dataBase.AlarmData
import com.example.MultipleAlarmClock.Ui.alarmListScreen.AlarmContainerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch

@Composable fun AlarmContainer(
//	 activityContext: ComponentActivity,
	 onNavigateToEdit: (AlarmData) -> Unit, onNavigateToCreate: () -> Unit
){
	val alarmContainerViewModel :AlarmContainerViewModel = viewModel()
	val uncancellableScope = CoroutineScope(NonCancellable + Dispatchers.Default)

	val screenHeight = LocalConfiguration.current.screenHeightDp.dp
	val snackBarHostState = remember { SnackbarHostState() }
	val clipBoard =LocalClipboard.current
	val alarms by alarmContainerViewModel.alarms.collectAsStateWithLifecycle()
	ReportDrawnWhen { alarms != null }	// for the startUp profile
	val accentColor = Color.Blue
	val coroutineScope = rememberCoroutineScope()
	Scaffold(contentWindowInsets = WindowInsets.safeContent) { edgeToEdgePadding ->
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
				contentPadding = PaddingValues(
					start = edgeToEdgePadding.calculateStartPadding(LocalLayoutDirection.current),
					top = edgeToEdgePadding.calculateTopPadding(),
					end = edgeToEdgePadding.calculateEndPadding(LocalLayoutDirection.current),
					bottom = edgeToEdgePadding.calculateBottomPadding() + 145.dp
				)
			) {
				val alarmList = alarms
				if (alarmList != null){
					itemsIndexed(
						items = alarmList,
						key = { _, alarm -> alarm.id }
					) { _, individualAlarm ->
						AlarmCard(
							individualAlarm, onEdit = {alarmData -> onNavigateToEdit(alarmData)}, onStop = { alarmData -> alarmContainerViewModel.stopAlarm(alarmData) },
							onDelete = {alarmData ->alarmContainerViewModel.deleteAlarm(alarmData)}, onReset = {alarmData -> alarmContainerViewModel.resetAlarm(alarmData)}, onLongPress = { alarmData ->
								uncancellableScope.launch {
									launch {
										alarmContainerViewModel.captureEvent("user long pressed the alarm", mapOf(
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
			}
			Box(
				modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = screenHeight / 15)
			) {
				AddAlarmButton(
					backgroundColor = accentColor,
					onClick = {
						onNavigateToCreate()
						coroutineScope.launch {
							alarmContainerViewModel.captureEvent("Plus Icon clicked", mapOf("round plus icon " to "new alarm"))
						}
				  },
					viewModel = alarmContainerViewModel
				)
			}
		}
	}
}

@Composable fun AddAlarmButton(modifier: Modifier = Modifier,  backgroundColor: Color, onClick: () -> Unit, viewModel: AlarmContainerViewModel) {
	val coroutineScope = rememberCoroutineScope()
	val interactionSource = remember { MutableInteractionSource() }
	val isPressed by interactionSource.collectIsPressedAsState()
	val scale by animateFloatAsState(
		targetValue = if (isPressed) 0.94f else 1f,
		animationSpec = spring(),
	)
	ExtendedFloatingActionButton(
		onClick = {
			coroutineScope.launch {
				FirstLaunchAskForPermission(viewModel.context).checkAndRequestPermissions(viewModel.analytics, coroutineScope)
			}
			coroutineScope.launch {
				onClick()
			}
		},
		modifier = modifier
			.padding(bottom = 29.dp, end = 16.dp)
			.scale(scale)
			.size(width = 180.dp, height = 74.dp)
			.zIndex(5f),
		interactionSource = interactionSource,
		shape = MaterialTheme.shapes.extraLarge,
		containerColor = Color(0xFF0a446e),
		contentColor = Color.White,
		elevation = FloatingActionButtonDefaults.elevation(
			defaultElevation = 6.dp,
			pressedElevation = 6.dp
		),
		icon = {
			Icon(
				imageVector = Icons.Default.AlarmAdd, contentDescription = null,
				modifier = Modifier.size(28.dp)
			)
		},
		text = {
			Text(
				text = "Add alarm",
				style = MaterialTheme.typography.labelLarge,
				fontSize = 17.sp,
				fontWeight = FontWeight.SemiBold
			)
		}
	)
}