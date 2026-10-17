package com.coolApps.MultipleAlarmClock.presentation.util.Permissions

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.coolApps.MultipleAlarmClock.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlarmPermissionDialog(
	missingSteps: List<PermissionStep>,
	onAllCriticalGranted: () -> Unit,
	onDismiss: () -> Unit,
	onTrackEvent: (String, Map<String, Any>) -> Unit
) {
	val context = LocalContext.current
	val lifecycleOwner = LocalLifecycleOwner.current

	// Dynamically track remaining missing steps when coming back from settings
	var currentMissingSteps by remember(missingSteps) {
		mutableStateOf(PermissionUtils.getRequiredPermissionSteps(context))
	}

	val notificationPermState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)

	var notificationRequested by rememberSaveable { mutableStateOf(false) }

	val notificationPermanentlyDenied = notificationRequested &&
			!notificationPermState.status.isGranted &&
			!notificationPermState.status.shouldShowRationale

	var allCriticalNowGranted by remember {
		mutableStateOf(PermissionUtils.allCriticalPermissionsGranted(context))
	}

	// Refresh permission statuses on resume (e.g. after returning from Settings)
	DisposableEffect(lifecycleOwner) {
		val observer = LifecycleEventObserver { _, event ->
			if (event == Lifecycle.Event.ON_RESUME) {
				currentMissingSteps = PermissionUtils.getRequiredPermissionSteps(context)
				allCriticalNowGranted = PermissionUtils.allCriticalPermissionsGranted(context)
			}
		}
		lifecycleOwner.lifecycle.addObserver(observer)
		onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
	}

	// Auto-dismiss or call callback if all critical permissions were granted while in Settings
	LaunchedEffect(allCriticalNowGranted) {
		if (allCriticalNowGranted) {
			onAllCriticalGranted()
		}
	}

	LaunchedEffect(Unit) {
		onTrackEvent(
			"permission_dialog_shown", mapOf(
				"permission name" to currentMissingSteps.map { it.id },
				"permission count" to currentMissingSteps.size,
			)
		)
	}

	val confirmInteraction = remember { MutableInteractionSource() }
	val isConfirmPressed by confirmInteraction.collectIsPressedAsState()
	val confirmCorner by animateDpAsState(
		targetValue = if (isConfirmPressed) 12.dp else 20.dp,
		animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
		label = "confirmCorner"
	)
	val confirmScale by animateFloatAsState(
		targetValue = if (isConfirmPressed) 0.96f else 1f,
		animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
		label = "confirmScale"
	)
	val confirmContainerColor by animateColorAsState(
		targetValue = if (allCriticalNowGranted) {
			MaterialTheme.colorScheme.primary
		} else {
			MaterialTheme.colorScheme.surfaceContainerHighest
		},
		animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
		label = "confirmContainerColor"
	)
	val confirmContentColor by animateColorAsState(
		targetValue = if (allCriticalNowGranted) {
			MaterialTheme.colorScheme.onPrimary
		} else {
			MaterialTheme.colorScheme.onSurface
		},
		animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
		label = "confirmContentColor"
	)

	AlertDialog(
		onDismissRequest = onDismiss,
		modifier = Modifier.widthIn(max = 480.dp),
		shape = RoundedCornerShape(32.dp),
		containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
		tonalElevation = 6.dp,
		icon = {
			Surface(
				shape = RoundedCornerShape(22.dp),
				color = MaterialTheme.colorScheme.primaryContainer,
				modifier = Modifier.size(60.dp)
			) {
				Box(contentAlignment = Alignment.Center) {
					Icon(
						imageVector = Icons.Default.Security,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.onPrimaryContainer,
						modifier = Modifier.size(30.dp)
					)
				}
			}
		},
		title = {
			Column(
				modifier = Modifier.fillMaxWidth(),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Text(
					text = stringResource(R.string.permission_dialog_title),
					style = MaterialTheme.typography.headlineSmall,
					fontWeight = FontWeight.ExtraBold,
					textAlign = TextAlign.Center,
					color = MaterialTheme.colorScheme.onSurface
				)
			}
		},
		text = {
			Column(
				verticalArrangement = Arrangement.spacedBy(4.dp)
			) {
				currentMissingSteps.forEachIndexed { index, step ->
					val isGranted = when (step) {
						PermissionStep.PostNotification -> notificationPermState.status.isGranted
						else -> !currentMissingSteps.contains(step)
					}
					val isFirst = index == 0
					val isLast = index == currentMissingSteps.lastIndex
					val topCorner = if (isFirst) 20.dp else 8.dp
					val bottomCorner = if (isLast) 20.dp else 8.dp

					PermissionStepRow(
						step = step,
						isGranted = isGranted,
						isPermanentlyDenied = step == PermissionStep.PostNotification && notificationPermanentlyDenied,
						topCorner = topCorner,
						bottomCorner = bottomCorner,
						onAction = {
							when (step) {
								PermissionStep.PostNotification -> {
									if (notificationPermState.status.isGranted) {
										// Update state immediately if already granted
										currentMissingSteps = PermissionUtils.getRequiredPermissionSteps(context)
									} else if (notificationPermanentlyDenied) {
										context.startActivity(
											Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
												data = Uri.fromParts("package", context.packageName, null)
											}
										)
									} else {
										notificationPermState.launchPermissionRequest()
										notificationRequested = true
									}
								}
								PermissionStep.ExactAlarm -> {
									context.startActivity(
										Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
											data = Uri.fromParts("package", context.packageName, null)
										}
									)
								}
								PermissionStep.FullScreenIntent -> {
									if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
										context.startActivity(
											Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
												data = Uri.fromParts("package", context.packageName, null)
											}
										)
									}
								}
							}
						}
					)
				}
			}
		},
		confirmButton = {
			FilledTonalButton(
				onClick = { if (allCriticalNowGranted) onAllCriticalGranted() else onDismiss() },
				interactionSource = confirmInteraction,
				shape = RoundedCornerShape(confirmCorner),
				colors = ButtonDefaults.filledTonalButtonColors(
					containerColor = confirmContainerColor,
					contentColor = confirmContentColor
				),
				modifier = Modifier.graphicsLayer {
					scaleX = confirmScale
					scaleY = confirmScale
				}
			) {
				Text(
					text = if (allCriticalNowGranted) stringResource(R.string.permission_dialog_btn_done) else stringResource(R.string.permission_dialog_btn_cancel),
					fontWeight = FontWeight.Bold
				)
			}
		},
	)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PermissionStepRow(
	step: PermissionStep,
	isGranted: Boolean,
	isPermanentlyDenied: Boolean,
	topCorner: Dp = 20.dp,
	bottomCorner: Dp = 20.dp,
	onAction: () -> Unit
) {
	val containerColor by animateColorAsState(
		targetValue = if (isGranted) {
			MaterialTheme.colorScheme.secondaryContainer
		} else {
			MaterialTheme.colorScheme.surfaceContainerLowest
		},
		animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
		label = "permContainerColor"
	)

	val actionInteraction = remember { MutableInteractionSource() }
	val isActionPressed by actionInteraction.collectIsPressedAsState()
	val actionCorner by animateDpAsState(
		targetValue = if (isActionPressed) 10.dp else 16.dp,
		animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
		label = "actionCorner"
	)
	val actionScale by animateFloatAsState(
		targetValue = if (isActionPressed) 0.96f else 1f,
		animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
		label = "actionScale"
	)

	val statusEffectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
	val statusSpatialSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()

	Surface(
		modifier = Modifier.fillMaxWidth(),
		shape = RoundedCornerShape(
			topStart = topCorner,
			topEnd = topCorner,
			bottomStart = bottomCorner,
			bottomEnd = bottomCorner
		),
		color = containerColor
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 16.dp, vertical = 14.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			Column(modifier = Modifier.weight(1f)) {
				Text(
					text = stringResource(step.titleRes),
					style = MaterialTheme.typography.titleMedium,
					color = MaterialTheme.colorScheme.onSurface,
					fontWeight = FontWeight.Bold
				)
				Spacer(modifier = Modifier.height(3.dp))
				Text(
					text = stringResource(step.rationaleRes),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
			Spacer(Modifier.width(12.dp))

			AnimatedContent(
				targetState = isGranted,
				transitionSpec = {
					(fadeIn(animationSpec = statusEffectsSpec) +
						scaleIn(animationSpec = statusSpatialSpec, initialScale = 0.8f))
						.togetherWith(
							fadeOut(animationSpec = statusEffectsSpec) +
								scaleOut(animationSpec = statusSpatialSpec, targetScale = 0.8f)
						)
				},
				label = "permissionStepStatusTransition"
			) { granted ->
				if (granted) {
					Surface(
						shape = CircleShape,
						color = MaterialTheme.colorScheme.primaryContainer,
						modifier = Modifier.size(36.dp)
					) {
						Box(contentAlignment = Alignment.Center) {
							Icon(
								imageVector = Icons.Default.CheckCircle,
								contentDescription = stringResource(R.string.permission_dialog_done_desc),
								tint = MaterialTheme.colorScheme.primary,
								modifier = Modifier.size(22.dp)
							)
						}
					}
				} else {
					Button(
						onClick = onAction,
						interactionSource = actionInteraction,
						shape = RoundedCornerShape(actionCorner),
						contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
						colors = ButtonDefaults.buttonColors(
							containerColor = MaterialTheme.colorScheme.primary,
							contentColor = MaterialTheme.colorScheme.onPrimary
						),
						modifier = Modifier.graphicsLayer {
							scaleX = actionScale
							scaleY = actionScale
						}
					) {
						Text(
							text = stringResource(
								when {
									isPermanentlyDenied -> R.string.permission_dialog_btn_open_settings
									step.action != null -> R.string.permission_dialog_btn_open_settings
									else -> R.string.permission_dialog_btn_allow
								}
							),
							style = MaterialTheme.typography.labelLarge,
							fontWeight = FontWeight.Bold
						)
					}
				}
			}
		}
	}
}
