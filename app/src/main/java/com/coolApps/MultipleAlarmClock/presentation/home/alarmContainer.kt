package com.coolApps.MultipleAlarmClock.presentation.home

import android.app.Activity
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.ResultEffect
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.presentation.logD
import com.coolApps.MultipleAlarmClock.presentation.settings.FeedbackPopUpCard
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.launch
import java.util.Calendar


@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable fun AlarmContainer(
	onNavigateToEdit: (AlarmData) -> Unit, onNavigateToCreate: () -> Unit, onNavigateToSettings:()->Unit,
	sharedTransitionScope: SharedTransitionScope, animatedVisibilityScope: AnimatedVisibilityScope
){
	val alarmContainerViewModel :AlarmContainerViewModel = hiltViewModel()
	val snackBarHostState = remember { SnackbarHostState() }
	val view = LocalView.current
	ResultEffect<Boolean> { isNewAlarm ->
		if (isNewAlarm) {
			alarmContainerViewModel.onPositiveUserAction()
			logD("new_alarm event received in the alarm container and called, alarmContainerViewModel.onPositiveUserAction() ")
		}
	}

	val uiState by alarmContainerViewModel.alarmControllerUi.collectAsStateWithLifecycle()
	val alarmList = uiState.alarmList
	var selectedAlarmId by remember { mutableStateOf<Int?>(null) }
	ReportDrawnWhen { alarmList != null }
	val colorScheme = colorScheme
	val showFeedbackCard by alarmContainerViewModel.showFeedbackUIState.collectAsStateWithLifecycle()
	val inAppReviewState = uiState.showReviewUi
	val context = LocalContext.current

	// Computed once per composition — the start of today at midnight (local timezone).
	// Passed to every AlarmCard so all cards share the same day boundary.
	val todayStartMs = remember {
		Calendar.getInstance().apply {
			set(Calendar.HOUR_OF_DAY, 0)
			set(Calendar.MINUTE, 0)
			set(Calendar.SECOND, 0)
			set(Calendar.MILLISECOND, 0)
		}.timeInMillis
	}

	LaunchedEffect(inAppReviewState) {
		val activity = context as? Activity

		logD("asking for  review inAppReviewState:$inAppReviewState, :) ")
		if (inAppReviewState  && (activity != null)) {
			val manager = ReviewManagerFactory.create(context)
			val request = manager.requestReviewFlow()
//			logD("asking for  review inAppReviewState:$inAppReviewState ")
			request.addOnCompleteListener { task ->
				if (task.isSuccessful) {
					val reviewInfo = task.result
					val flow = manager.launchReviewFlow(activity, reviewInfo)
					logD("review successful, $task")
					flow.addOnCompleteListener {
						alarmContainerViewModel.setInAppReviewConsumed( task)
					}
				} else {
					logD("review unSuccessful, $task")
					alarmContainerViewModel.setInAppReviewConsumed(task)
				}
			}
		}
	}

	Scaffold(
		containerColor = colorScheme.surface,
		contentWindowInsets = WindowInsets.safeDrawing,
		topBar = {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
					.padding(end = 10.dp, top = 8.dp, bottom = 8.dp),
				horizontalArrangement = Arrangement.End,
				verticalAlignment = Alignment.CenterVertically
			) {
				val settingsInteractionSource = remember { MutableInteractionSource() }
				val isSettingsPressed by settingsInteractionSource.collectIsPressedAsState()
				val settingsCorner by animateDpAsState(
					targetValue = if (isSettingsPressed) 10.dp else 16.dp,
					animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
					label = "settingsCorner"
				)
				val settingsScale by animateFloatAsState(
					targetValue = if (isSettingsPressed) 0.92f else 1f,
					animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
					label = "settingsScale"
				)

				FilledTonalIconButton(
					onClick = {
						view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
						onNavigateToSettings()
					},
					interactionSource = settingsInteractionSource,
					modifier = Modifier
						.size(48.dp)
						.graphicsLayer {
							scaleX = settingsScale
							scaleY = settingsScale
						},
					shape = RoundedCornerShape(settingsCorner)
				) {
					Icon(
						imageVector = Icons.Outlined.Settings,
						contentDescription = stringResource(R.string.settings_content_description),
						modifier = Modifier.size(24.dp)
					)
				}
			}
		}
	) { edgeToEdgePadding ->
		Box(modifier = Modifier.fillMaxSize()) {
			SnackbarHost(
				hostState = snackBarHostState,
				modifier = Modifier
					.align(Alignment.BottomCenter)
					.padding(bottom = 112.dp, start = 16.dp, end = 16.dp)
					.widthIn(max = 560.dp)
					.zIndex(10f)
			) { snackBarData ->
				Snackbar(
					snackbarData = snackBarData,
					shape = MaterialTheme.shapes.large,
					containerColor = colorScheme.inverseSurface,
					contentColor = colorScheme.inverseOnSurface,
					modifier = Modifier.fillMaxWidth()
				)
			}
			if (showFeedbackCard) {
				FeedbackPopUpCard(
					onReviewGiven = { review ->
						logD("Feedback given is $review")
						alarmContainerViewModel.captureFeedback(review)
					},
					onDismiss = { alarmContainerViewModel.dismissFeedback() }
				)
				LaunchedEffect(Unit) {
					alarmContainerViewModel.captureEvent("FeedBackPopUp card shown", mapOf())
				}
			}

			LazyVerticalGrid(
				columns = GridCells.Adaptive(minSize = 360.dp),
				modifier = Modifier.fillMaxSize(),
				contentPadding = PaddingValues(
					start = edgeToEdgePadding.calculateStartPadding(LocalLayoutDirection.current) + 6.dp,
					top = edgeToEdgePadding.calculateTopPadding() + 8.dp,
					end = edgeToEdgePadding.calculateEndPadding(LocalLayoutDirection.current) + 6.dp,
					bottom = edgeToEdgePadding.calculateBottomPadding() + 155.dp
				),
				horizontalArrangement = Arrangement.spacedBy(8.dp),
				verticalArrangement = Arrangement.spacedBy(4.dp)
			) {
				alarmList?.let { list ->
					items(
						items = list,
						key = { it.id }
					) { individualAlarm ->
						AlarmCard(
							alarmData = individualAlarm,
							todayStartMs = todayStartMs,
							onEdit = { alarmData -> onNavigateToEdit(alarmData) },
							onToggle = { alarmData, isChecked ->
								if (isChecked){
									alarmContainerViewModel.resetAlarm(alarmData)
									alarmContainerViewModel.onPositiveUserAction()
									if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
										view.performHapticFeedback(HapticFeedbackConstants.TOGGLE_OFF)
									}
								}
								else {
									alarmContainerViewModel.stopAlarm(alarmData)
									if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
										view.performHapticFeedback(HapticFeedbackConstants.TOGGLE_OFF)
									}
								}
							},
							onDelete = { alarmData -> alarmContainerViewModel.deleteAlarm(alarmData) },
							onLongPress = { alarmData ->
								selectedAlarmId = if (selectedAlarmId == alarmData.id) null else alarmData.id
							},
							sharedTransitionScope = sharedTransitionScope,
							animatedVisibilityScope = animatedVisibilityScope,
							modifier = Modifier.animateItem(
								fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
								fadeOutSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
								placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
							)
						)
					}
				}
			}

			if (alarmList?.isEmpty() == true) {
				EmptyAlarmsView(
					modifier = Modifier
						.fillMaxSize()
						.padding(edgeToEdgePadding)
						.padding(bottom = 100.dp)
				)
			}

			Box(
				modifier = Modifier
					.align(Alignment.BottomCenter)
					.padding(bottom = edgeToEdgePadding.calculateBottomPadding() + 20.dp)
			) {
				AnimatedVisibility(
					visible = alarmList != null,
					enter = scaleIn(
						initialScale = 0.8f,
						animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
					) + fadeIn(
						animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
					),
					exit = scaleOut(
						targetScale = 0.8f,
						animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
					) + fadeOut(
						animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
					)
				) {
					AddAlarmButton(
						onClick = {
							view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
							onNavigateToCreate()
						},
					)
				}
			}
		}
	}
}


@Composable
fun EmptyAlarmsView(modifier: Modifier = Modifier) {
	val infiniteTransition = rememberInfiniteTransition(label = "EmptyStateIconTransition")
	val dy by infiniteTransition.animateFloat(
		initialValue = -6f,
		targetValue = 6f,
		animationSpec = infiniteRepeatable(
			animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
			repeatMode = RepeatMode.Reverse
		),
		label = "EmptyStateIconOffset"
	)
	val haloScale by infiniteTransition.animateFloat(
		initialValue = 0.95f,
		targetValue = 1.05f,
		animationSpec = infiniteRepeatable(
			animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
			repeatMode = RepeatMode.Reverse
		),
		label = "EmptyStateHaloScale"
	)

	Box(
		modifier = modifier,
		contentAlignment = Alignment.Center
	) {
		Column(
			modifier = Modifier
				.widthIn(max = 420.dp)
				.padding(horizontal = 24.dp),
			verticalArrangement = Arrangement.Center,
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Box(
				contentAlignment = Alignment.Center,
				modifier = Modifier.offset { IntOffset(x = 0, y = dy.dp.roundToPx()) }
			) {

				Surface(
					shape = MaterialTheme.shapes.extraLarge,
					color = colorScheme.primaryContainer,
					contentColor = colorScheme.onPrimaryContainer,
					modifier = Modifier
						.size(124.dp)
						.scale(haloScale)
				) {
				Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
					Icon(
						imageVector = Icons.Outlined.Alarm,
						contentDescription = null,
						modifier = Modifier.size(64.dp),
						tint = colorScheme.onPrimaryContainer
					)
				}
				}
			}

			Spacer(modifier = Modifier.height(28.dp))

			Text(
				text = stringResource(R.string.empty_state_title),
				style = MaterialTheme.typography.headlineMedium,
				fontWeight = FontWeight.ExtraBold,
				textAlign = TextAlign.Center,
				color = colorScheme.onSurface
			)

			Spacer(modifier = Modifier.height(10.dp))

			Surface(
				shape = MaterialTheme.shapes.large,
				color = colorScheme.surfaceContainerLow
			) {
				Text(
					text = stringResource(R.string.empty_state_subtitle),
					style = MaterialTheme.typography.bodyLarge,
					textAlign = TextAlign.Center,
					color = colorScheme.onSurfaceVariant,
					modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
				)
			}
		}
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddAlarmButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
	val coroutineScope = rememberCoroutineScope()
	val interactionSource = remember { MutableInteractionSource() }
	val isPressed by interactionSource.collectIsPressedAsState()
	val scale by animateFloatAsState(
		targetValue = if (isPressed) 0.94f else 1f,
		animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
		label = "fabScale"
	)
	val cornerRadius by animateDpAsState(
		targetValue = if (isPressed) 16.dp else 37.dp,
		animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
		label = "fabCornerRadius"
	)
	val colorScheme = colorScheme

	// 🔑 Lock font scale so button looks identical on every device
	CompositionLocalProvider(
		LocalDensity provides Density(density = LocalDensity.current.density, fontScale = 1f)
	) {
		ExtendedFloatingActionButton(
			onClick = {
				coroutineScope.launch { onClick() }
			},
			modifier = modifier
				.padding(bottom = 24.dp, end = 16.dp, start = 16.dp)
				.scale(scale)
				.height(74.dp)
				.widthIn(min = 196.dp)
				.zIndex(5f),
			interactionSource = interactionSource,
			shape = RoundedCornerShape(cornerRadius),
			containerColor = colorScheme.tertiaryContainer,
			contentColor = colorScheme.onTertiaryContainer,
			elevation = FloatingActionButtonDefaults.elevation(
				defaultElevation = 6.dp,
				pressedElevation = 8.dp
			),
			icon = {
				Icon(
					imageVector = Icons.Default.AlarmAdd,
					contentDescription = null,
					modifier = Modifier.size(28.dp)
				)
			},
			text = {
				Text(
					text = stringResource(R.string.add_alarm_button_text),
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.ExtraBold,
					letterSpacing = 0.2.sp,
					maxLines = 1
				)
			}
		)
	}
}