package com.coolApps.MultipleAlarmClock.presentation.picker

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.data.local.AlarmDataValidationResult
import com.coolApps.MultipleAlarmClock.data.paywall.PaywallTriggerType
import com.coolApps.MultipleAlarmClock.presentation.AlarmContainerBoundsTransform
import com.coolApps.MultipleAlarmClock.presentation.AlarmTimeBoundsTransform
import com.coolApps.MultipleAlarmClock.presentation.logD
import com.coolApps.MultipleAlarmClock.presentation.util.Permissions.AlarmPermissionDialog
import java.util.Calendar

@OptIn(
	ExperimentalMaterial3Api::class,
	ExperimentalMaterial3ExpressiveApi::class,
	ExperimentalSharedTransitionApi::class
)
@Composable
fun EditAlarmScreen(
		alarmSetProceed: () -> Unit,
		onAlarmDeleted: () -> Unit,
		settingAlarmCancelled: () -> Unit,
		onNavigateToSoundList: () -> Unit,
		onNavigateToMissionList: () -> Unit,
		onShowPaywall: (PaywallTriggerType) -> Unit,
		alarmData: AlarmData,
		viewModel: AlarmPickerViewModel,
		sharedTransitionScope: SharedTransitionScope? = null,
		animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
	val selectedSound by viewModel.selectedAlarmSound.collectAsStateWithLifecycle()
	val view = LocalView.current
	val context = LocalContext.current

	LaunchedEffect(uiState, isPremium) {
		logD("ui state:$uiState")
		logD("isPremium:$isPremium")
	}
	LaunchedEffect(uiState.completedAction) {
		when (uiState.completedAction) {
			is AlarmCompletionAction.Saved -> {
				viewModel.consumeAlarmOperationCompleted()
				alarmSetProceed()
			}
			AlarmCompletionAction.Deleted -> {
				viewModel.consumeAlarmOperationCompleted()
				onAlarmDeleted()
			}
			null -> Unit
		}
	}
	LaunchedEffect(uiState.paywallTriggerType) {
		uiState.paywallTriggerType?.let { trigger ->
			onShowPaywall(trigger)
			viewModel.consumePaywallTrigger()
		}
	}

	LifecycleResumeEffect(Unit) {
		viewModel.checkPermissions(context)
		onPauseOrDispose { }
	}

	if (uiState.showPermissionDialog) {
		AlarmPermissionDialog(
			uiState.missingSteps,
			onAllCriticalGranted = { viewModel.dismissPermissionDialog() },
			onDismiss = { viewModel.dismissPermissionDialog() },
			onTrackEvent = { event, prop -> viewModel.captureEvent(event, prop) }
		)
	}

	var showCalendar by remember { mutableStateOf(false) }
	if (showCalendar) {
		DatePickerModal(
			onDateSelected = { date ->
				if (date != null) {
					val cal = Calendar.getInstance().apply { timeInMillis = date }
					viewModel.updateDate(cal)
				}
				showCalendar = false
			},
			onDismiss = { showCalendar = false }
		)
	}

	val horizontalPadding = rememberAdaptiveHorizontalPadding()

	// Build the optional sharedBounds scaffold modifier
	val scaffoldModifier: Modifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
		with(sharedTransitionScope) {
			Modifier.sharedBounds(
				sharedContentState = rememberSharedContentState(key = "alarm_card_${alarmData.id}"),
				animatedVisibilityScope = animatedVisibilityScope,
				clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(0.dp)),
				boundsTransform = AlarmContainerBoundsTransform,
				resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
			)
		}
	} else {
		Modifier
	}

	Scaffold(
		modifier = scaffoldModifier,
		contentWindowInsets = WindowInsets.safeDrawing,
		topBar = {
			ExpressiveAlarmTopBar(
				onBackClick = { settingAlarmCancelled() }
			)
		},
		bottomBar = {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.background(colorScheme.background)
					.navigationBarsPadding()
					.padding(16.dp)
					.padding(bottom = 20.dp),
				contentAlignment = Alignment.Center
			) {
				Row(
					modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceBetween,
					verticalAlignment = Alignment.CenterVertically
				) {
					CancelAndDeleteButton(
						isNewAlarm = false,
						onClick = { viewModel.onDeleteClicked() }
					)

					val isInactiveEdit = uiState.initialAlarm?.isReadyToUse == false
					val canSetAlarm = uiState.validationResult == AlarmDataValidationResult.Success || isInactiveEdit
					PrimaryActionButton(
						uiState = uiState,
						onAction = {
							if (canSetAlarm) {
								viewModel.onSetAlarmClicked()
								view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
							}
						}
					)
				}
			}
		}
	) { screenPadding ->
		AlarmEditorBody(
			modifier = Modifier
				.fillMaxSize()
				.padding(screenPadding)
				.consumeWindowInsets(screenPadding)
				.padding(horizontal = horizontalPadding),
			uiState = uiState,
			viewModel = viewModel,
			selectedSoundTitle = selectedSound?.title,
			alarmId = alarmData.id,
			showCalendarRequest = { showCalendar = true },
			onNavigateToSoundList = onNavigateToSoundList,
			onNavigateToMissionList = onNavigateToMissionList,
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			animateScroll = false
		)
	}
}

@OptIn(
	ExperimentalMaterial3Api::class,
	ExperimentalMaterial3ExpressiveApi::class,
	ExperimentalSharedTransitionApi::class
)
@Composable
fun NewAlarmScreen(
		alarmSetProceed: (AlarmData) -> Unit,
		settingAlarmCancelled: () -> Unit,
		onNavigateToSoundList: () -> Unit,
		onNavigateToMissionList: () -> Unit,
		onShowPaywall: (PaywallTriggerType) -> Unit,
		linearProgressBar: (@Composable () -> Unit)? = null,
		viewModel: AlarmPickerViewModel,
) {
	val fromOnboarding = linearProgressBar != null

	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
	val selectedSound by viewModel.selectedAlarmSound.collectAsStateWithLifecycle()
	val view = LocalView.current
	val context = LocalContext.current

	LaunchedEffect(uiState, isPremium) {
		logD("ui state:$uiState")
		logD("isPremium:$isPremium")
	}
	LaunchedEffect(uiState.completedAction) {
		when (val action = uiState.completedAction) {
			is AlarmCompletionAction.Saved -> {
				viewModel.consumeAlarmOperationCompleted()
				alarmSetProceed(action.alarmData)
			}
			else -> Unit
		}
	}
	LaunchedEffect(uiState.paywallTriggerType) {
		uiState.paywallTriggerType?.let { trigger ->
			onShowPaywall(trigger)
			viewModel.consumePaywallTrigger()
		}
	}

	LifecycleResumeEffect(Unit) {
		viewModel.checkPermissions(context)
		onPauseOrDispose { }
	}

	if (uiState.showPermissionDialog) {
		AlarmPermissionDialog(
			uiState.missingSteps,
			onAllCriticalGranted = { viewModel.dismissPermissionDialog() },
			onDismiss = { viewModel.dismissPermissionDialog() },
			onTrackEvent = { event, prop -> viewModel.captureEvent(event, prop) }
		)
	}

	var showCalendar by remember { mutableStateOf(false) }
	if (showCalendar) {
		DatePickerModal(
			onDateSelected = { date ->
				if (date != null) {
					val cal = Calendar.getInstance().apply { timeInMillis = date }
					viewModel.updateDate(cal)
				}
				showCalendar = false
			},
			onDismiss = { showCalendar = false }
		)
	}

	val horizontalPadding = rememberAdaptiveHorizontalPadding()

	Scaffold(
		contentWindowInsets = if (fromOnboarding) WindowInsets(0) else WindowInsets.safeDrawing,
		topBar = {
			if (!fromOnboarding) {
				ExpressiveAlarmTopBar(
					onBackClick = { settingAlarmCancelled() }
				)
			}
		},
		bottomBar = {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.background(colorScheme.background)
					.then(if (fromOnboarding) Modifier else Modifier.navigationBarsPadding())
					.padding(16.dp)
					.padding(bottom = 20.dp),
				contentAlignment = Alignment.Center
			) {
				Row(
					modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceBetween,
					verticalAlignment = Alignment.CenterVertically
				) {
					CancelAndDeleteButton(
						isNewAlarm = true,
						fromOnboarding = fromOnboarding,
						onClick = settingAlarmCancelled
					)

					val isInactiveEdit = uiState.initialAlarm?.isReadyToUse == false
					val canSetAlarm = uiState.validationResult == AlarmDataValidationResult.Success || isInactiveEdit
					PrimaryActionButton(
						uiState = uiState,
						onAction = {
							if (canSetAlarm) {
								viewModel.onSetAlarmClicked()
								view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
							}
						}
					)
				}
			}
		}
	) { screenPadding ->
		AlarmEditorBody(
			modifier = Modifier
				.fillMaxSize()
				.padding(screenPadding)
				.consumeWindowInsets(screenPadding)
				.padding(horizontal = horizontalPadding),
			uiState = uiState,
			viewModel = viewModel,
			selectedSoundTitle = selectedSound?.title,
			alarmId = null,
			showCalendarRequest = { showCalendar = true },
			onNavigateToSoundList = onNavigateToSoundList,
			onNavigateToMissionList = onNavigateToMissionList,
			sharedTransitionScope = null,
			animatedVisibilityScope = null,
			animateScroll = true
		)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpressiveAlarmTopBar(
	onBackClick: () -> Unit,
	title: String = ""
) {
	val view = LocalView.current

	CenterAlignedTopAppBar(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 8.dp),
		colors = TopAppBarDefaults.topAppBarColors(
			containerColor = Color.Transparent,
			scrolledContainerColor = Color.Transparent
		),
		title = {
			if (title.isNotEmpty()) {
				Text(
					text = title,
					style = typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
					color = colorScheme.onSurface
				)
			}
		},
		navigationIcon = {
			IconButton(
				onClick = {
					view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
					onBackClick()
				}
			) {
				Icon(
					imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
					contentDescription = stringResource(R.string.alarm_picker_back_desc)
				)
			}
		}
	)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun AlarmEditorBody(
		modifier: Modifier = Modifier,
		uiState: AlarmPickerUiState,
		viewModel: AlarmPickerViewModel,
		selectedSoundTitle: String?,
		alarmId: Int?,
		showCalendarRequest: () -> Unit,
		onNavigateToSoundList: () -> Unit,
		onNavigateToMissionList: () -> Unit,
		sharedTransitionScope: SharedTransitionScope? = null,
		animatedVisibilityScope: AnimatedVisibilityScope? = null,
		animateScroll: Boolean
) {
	val view = LocalView.current

	Box(
		modifier = modifier,
		contentAlignment = Alignment.TopCenter
	) {
		Column(
			modifier = Modifier
				.widthIn(max = 600.dp)
				.fillMaxSize(),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Spacer(modifier = Modifier.weight(0.44f))
			TimeRow(
				uiState = uiState,
				onStartTimeChange = { viewModel.updateStartTime(it) },
				onEndTimeChange = { viewModel.updateEndTime(it) },
				onDisabledTimeSelected = { onDisabledTimeSelected(view) },
				sharedTransitionScope = sharedTransitionScope,
				animatedVisibilityScope = animatedVisibilityScope,
				alarmId = alarmId,
				boundsTransform = AlarmTimeBoundsTransform
			)
			Spacer(modifier = Modifier.weight(0.45f))
			SettingsCard(
				uiState = uiState,
				updateFrequency = { viewModel.updateFrequency(it) },
				updateRingDuration = { viewModel.updateRingDuration(it) },
				messageValueChanged = { viewModel.updateMessage(it) },
				updateAlarmVolume = { viewModel.updateAlarmVolume(it) },
				calenderButtonClicked = showCalendarRequest,
				selectSoundButtonClicked = onNavigateToSoundList,
				selectMissionButtonClicked = {
					viewModel.captureEvent("mission_row_clicked", mapOf("is_edit" to (alarmId != null)))
					onNavigateToMissionList()
				},
				repeatDayToggled = { day -> viewModel.onRepeatDayClicked(day) },
				selectedSoundName = selectedSoundTitle ?: stringResource(R.string.alarm_picker_sound_random),
				modifier = Modifier.weight(1f, fill = false),
				animateScroll = animateScroll
			)
			Spacer(modifier = Modifier.weight(0.04f))
		}
	}
}

// ─── Shared bottom-bar buttons (unchanged logic) ──────────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CancelAndDeleteButton(
	isNewAlarm: Boolean,
	modifier: Modifier = Modifier,
	fromOnboarding: Boolean = false,
	onClick: () -> Unit,
) {
	val interactionSource = remember { MutableInteractionSource() }
	val isPressed by interactionSource.collectIsPressedAsState()
	val cornerRadius by animateDpAsState(
		targetValue = if (isPressed) 16.dp else 28.dp,
		animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
		label = "cancel_delete_corner",
	)
	val scale by animateFloatAsState(
		targetValue = if (isPressed) 0.96f else 1f,
		animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
		label = "cancel_delete_scale",
	)

	if (!isNewAlarm) {
		TextButton(
			onClick = onClick,
			interactionSource = interactionSource,
			modifier = modifier
				.height(56.dp)
				.graphicsLayer {
					scaleX = scale
					scaleY = scale
				},
			contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
			shape = RoundedCornerShape(cornerRadius),
			colors = ButtonDefaults.textButtonColors(
				containerColor = colorScheme.errorContainer.copy(alpha = 0.45f),
				contentColor = colorScheme.error,
			),
		) {
			Text(
				text = stringResource(R.string.alarm_picker_delete_alarm),
				style = typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
			)
		}
	} else {
		OutlinedButton(
			onClick = onClick,
			interactionSource = interactionSource,
			modifier = modifier
				.height(56.dp)
				.graphicsLayer {
					scaleX = scale
					scaleY = scale
				},
			contentPadding = PaddingValues(horizontal = 28.dp, vertical = 0.dp),
			shape = RoundedCornerShape(cornerRadius),
		) {
			Text(
				text = stringResource(
					if (fromOnboarding) R.string.alarm_picker_previous else R.string.alarm_picker_cancel,
				),
				style = typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
			)
		}
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PrimaryActionButton(
	uiState: AlarmPickerUiState,
	onAction: () -> Unit,
	modifier: Modifier = Modifier,
) {
	val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
	val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()

	val interactionSource = remember { MutableInteractionSource() }
	val isPressed by interactionSource.collectIsPressedAsState()
	val cornerRadius by animateDpAsState(
		targetValue = if (isPressed) 16.dp else 28.dp,
		animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
		label = "primary_cta_corner",
	)
	val scale by animateFloatAsState(
		targetValue = if (isPressed) 0.96f else 1f,
		animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
		label = "primary_cta_scale",
	)

	val isInactiveEdit = uiState.initialAlarm?.isReadyToUse == false
	val canSetAlarm = uiState.validationResult == AlarmDataValidationResult.Success || isInactiveEdit
	val containerColor by animateColorAsState(
		targetValue = when {
			canSetAlarm -> colorScheme.primary
			uiState.validationResult.isFailure() -> colorScheme.errorContainer
			else -> colorScheme.primary
		},
		animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
		label = "primary_cta_container_color",
	)
	val contentColor by animateColorAsState(
		targetValue = when {
			canSetAlarm -> colorScheme.onPrimary
			uiState.validationResult.isFailure() -> colorScheme.onErrorContainer
			else -> colorScheme.onPrimary
		},
		animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
		label = "primary_cta_content_color",
	)

	Button(
		onClick = onAction,
		interactionSource = interactionSource,
		colors = ButtonDefaults.buttonColors(
			containerColor = containerColor,
			contentColor = contentColor,
		),
		modifier = modifier
			.height(56.dp)
			.graphicsLayer {
				scaleX = scale
				scaleY = scale
			},
		contentPadding = PaddingValues(horizontal = 36.dp, vertical = 0.dp),
		shape = RoundedCornerShape(cornerRadius),
	) {
		AnimatedContent(
			targetState = canSetAlarm,
			transitionSpec = {
				fadeIn(effectsSpec) togetherWith fadeOut(effectsSpec) using SizeTransform { _, _ -> spatialSpec }
			},
			label = "button_text",
		) { isValid ->
			Text(
				text = stringResource(if (isValid) R.string.alarm_picker_btn_set else R.string.alarm_picker_btn_fix),
				style = typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
			)
		}
	}
}

fun onDisabledTimeSelected(view: View) {
	view.performHapticFeedback(HapticFeedbackConstants.REJECT)
}

@Composable fun rememberAdaptiveHorizontalPadding(percent: Float = 0.0066f, min: Dp = 14.dp, max: Dp = 30.dp): Dp {
  val density = LocalDensity.current
  val screenWidthDp = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
  return (screenWidthDp * percent).coerceIn(min, max)
}
