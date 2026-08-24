package com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmContainer

import android.app.Activity
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmContainer.utils.FeedbackPopUpCard
import com.coolApps.MultipleAlarmClock.logD
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.launch


@Composable fun AlarmContainer(
	onNavigateToEdit: (AlarmData) -> Unit, onNavigateToCreate: () -> Unit, onNavigateToSettings:()->Unit
){
	val alarmContainerViewModel :AlarmContainerViewModel = hiltViewModel()
	val snackBarHostState = remember { SnackbarHostState() }

	val uiState by alarmContainerViewModel.alarmControllerUi.collectAsStateWithLifecycle()
	val alarmList = uiState.alarmList
	var selectedAlarmId by remember { mutableStateOf<Int?>(null) }
	ReportDrawnWhen { alarmList != null }
	val colorScheme = colorScheme
	val showFeedbackCard by alarmContainerViewModel.showFeedbackUIState.collectAsStateWithLifecycle()
	val inAppReviewState = uiState.showReviewUi
	val context = LocalContext.current

	LaunchedEffect(inAppReviewState) {
		val activity = context as? Activity
		logD("asking for  review inAppReviewState:$inAppReviewState, :) ")
		if (inAppReviewState  && (activity != null)) {
			val manager = ReviewManagerFactory.create(context)
			val request = manager.requestReviewFlow()
			logD("asking for  review inAppReviewState:$inAppReviewState ")
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
		contentWindowInsets = WindowInsets.safeDrawing
	) { edgeToEdgePadding ->
		Box(modifier = Modifier.fillMaxSize()) {
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
				columns = GridCells.Adaptive(minSize = 300.dp),
				modifier = Modifier.fillMaxSize(),
				contentPadding = PaddingValues(
					start = edgeToEdgePadding.calculateStartPadding(LocalLayoutDirection.current),
					top = edgeToEdgePadding.calculateTopPadding(),
					end = edgeToEdgePadding.calculateEndPadding(LocalLayoutDirection.current),
					bottom = edgeToEdgePadding.calculateBottomPadding() + 155.dp
				),
				horizontalArrangement = Arrangement.spacedBy(0.dp),
				verticalArrangement = Arrangement.spacedBy(0.dp)
			) {
				item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(bottom = 23.dp, end = 10.dp),
						horizontalArrangement = Arrangement.End,
						verticalAlignment = Alignment.CenterVertically
					) {
						FilledTonalIconButton(
							onClick = onNavigateToSettings,
							modifier = Modifier.size(45.dp),
							shape = RoundedCornerShape(18.dp)
						) {
							Icon(
								imageVector = Icons.Outlined.Settings,
								contentDescription = stringResource(R.string.settings_content_description),
								modifier = Modifier.size(24.dp)
							)
						}
					}
				}

				alarmList?.let { list ->
					items(
						items = list,
						key = { it.id }
					) { individualAlarm ->
						AlarmCard(
							alarmData = individualAlarm,
							onEdit = { alarmData -> onNavigateToEdit(alarmData) },
							onToggle = { alarmData, isChecked ->
								if (isChecked) alarmContainerViewModel.resetAlarm(alarmData)
								else alarmContainerViewModel.stopAlarm(alarmData)
							},
							onDelete = { alarmData -> alarmContainerViewModel.deleteAlarm(alarmData) },
							onLongPress = { alarmData ->
								selectedAlarmId = if (selectedAlarmId == alarmData.id) null else alarmData.id
							},
							modifier = Modifier.animateItem()
						)
					}
				}
			}

			if (alarmList?.isEmpty() == true) {
				EmptyState(
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
				AddAlarmButton(
					onClick = {
						onNavigateToCreate()
				  },
				)
			}
		}
	}
}


@Composable
fun EmptyState(modifier: Modifier = Modifier) {
	Column(
		modifier = modifier,
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Icon(
			imageVector = Icons.Outlined.Alarm,
			contentDescription = null,
			modifier = Modifier.size(120.dp),
			tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
		)
		Spacer(modifier = Modifier.height(24.dp))
		Text(
			text = stringResource(R.string.empty_state_title),
			style = MaterialTheme.typography.headlineMedium,
			fontWeight = FontWeight.Bold,
			color = MaterialTheme.colorScheme.onSurface
		)
		Spacer(modifier = Modifier.height(8.dp))
		Text(
			text = stringResource(R.string.empty_state_subtitle),
			style = MaterialTheme.typography.bodyMedium,
			textAlign = TextAlign.Center,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			modifier = Modifier.padding(horizontal = 48.dp)
		)
	}
}

@Composable
fun AddAlarmButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
	val coroutineScope = rememberCoroutineScope()
	val interactionSource = remember { MutableInteractionSource() }
	val isPressed by interactionSource.collectIsPressedAsState()
	val scale by animateFloatAsState(
		targetValue = if (isPressed) 0.93f else 1f,
		animationSpec = spring(),
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
				.padding(bottom = 29.dp, end = 16.dp)
				.scale(scale)
				.height(75.dp)
				.widthIn(min = 178.dp)
				.zIndex(5f),
			interactionSource = interactionSource,
			shape = RoundedCornerShape(45.dp),
			containerColor = colorScheme.tertiaryContainer,
			contentColor = colorScheme.onTertiaryContainer,
			elevation = FloatingActionButtonDefaults.elevation(
				defaultElevation = 6.dp,
				pressedElevation = 6.dp
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
					fontWeight = FontWeight.Bold,
					letterSpacing = 0.1.sp,
					maxLines = 1
				)
			}
		)
	}
}