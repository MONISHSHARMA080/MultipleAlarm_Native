package com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.Permissions

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.coolApps.MultipleAlarmClock.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
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

	AlertDialog(
		onDismissRequest = onDismiss,
		icon = {
			Icon(
				imageVector = Icons.Default.Security,
				contentDescription = null,
				tint = MaterialTheme.colorScheme.primary
			)
		},
		title = {
			Column(horizontalAlignment = Alignment.CenterHorizontally) {
				Text(
					text = stringResource(R.string.permission_dialog_title),
					style = MaterialTheme.typography.headlineSmall,
					color = MaterialTheme.colorScheme.onSurface
				)
			}
		},
		text = {
			Column(
				verticalArrangement = Arrangement.spacedBy(12.dp)
			) {
				currentMissingSteps.forEach { step ->
					val isGranted = when (step) {
						PermissionStep.PostNotification -> notificationPermState.status.isGranted
						else -> !currentMissingSteps.contains(step)
					}

					PermissionStepRow(
						step = step,
						isGranted = isGranted,
						isPermanentlyDenied = step == PermissionStep.PostNotification && notificationPermanentlyDenied,
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
								PermissionStep.XiaomiAutostart -> {
									PermissionUtils.launchXiaomiSettings(context)
								}
							}
						}
					)
				}

				if (currentMissingSteps.any { it == PermissionStep.XiaomiAutostart } && currentMissingSteps.size == 1) {
					Text(
						text = stringResource(R.string.permission_dialog_autostart_advisory),
						style = MaterialTheme.typography.labelMedium,
						fontStyle = FontStyle.Italic,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}
		},
		confirmButton = {
			TextButton(
				onClick = { if (allCriticalNowGranted) onAllCriticalGranted() else onDismiss() },
			) {
				Text(
					text = if (allCriticalNowGranted) stringResource(R.string.permission_dialog_btn_done) else stringResource(R.string.permission_dialog_btn_cancel),
					color = if (allCriticalNowGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
				)
			}
		},
	)
}

@Composable
private fun PermissionStepRow(
	step: PermissionStep,
	isGranted: Boolean,
	isPermanentlyDenied: Boolean,
	onAction: () -> Unit
) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.SpaceBetween
	) {
		Column(modifier = Modifier.weight(1f)) {
			Text(
				text = stringResource(step.titleRes),
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.onSurface,
				fontWeight = FontWeight.SemiBold
			)
			Spacer(modifier = Modifier.height(2.dp))
			Text(
				text = stringResource(step.rationaleRes),
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
		Spacer(Modifier.width(16.dp))

		if (isGranted) {
			Icon(
				imageVector = Icons.Default.CheckCircle,
				contentDescription = stringResource(R.string.permission_dialog_done_desc),
				tint = MaterialTheme.colorScheme.primary,
				modifier = Modifier.size(24.dp)
			)
		} else {
			Button(
				onClick = onAction,
				colors = ButtonDefaults.buttonColors(
					containerColor = MaterialTheme.colorScheme.primaryContainer,
					contentColor = MaterialTheme.colorScheme.onPrimaryContainer
				)
			) {
				Text(
					text = stringResource(
						when {
							isPermanentlyDenied -> R.string.permission_dialog_btn_open_settings
							step.action != null || step == PermissionStep.XiaomiAutostart -> R.string.permission_dialog_btn_open_settings
							else -> R.string.permission_dialog_btn_allow
						}
					)
				)
			}
		}
	}
}
