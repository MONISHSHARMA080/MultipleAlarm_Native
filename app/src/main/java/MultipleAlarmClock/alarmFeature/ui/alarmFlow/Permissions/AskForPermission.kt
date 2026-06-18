package com.example.MultipleAlarmClock.Ui.Permissions

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState

/**
 * Shows all missing permissions at once.
 * Critical ones block proceeding; Xiaomi autostart is advisory.
 *
 * @param missingSteps      The list of missing PermissionSteps to display
 * @param onAllCriticalGranted  Called when user has handled all critical permissions
 * @param onDismiss         Called when user dismisses / skips
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AlarmPermissionDialog(
	missingSteps: List<PermissionStep>,
	onAllCriticalGranted: () -> Unit,
	onDismiss: () -> Unit,
	onTrackEvent: (String, Map<String, Any>) -> Unit
) {
	val context = LocalContext.current

	val notificationPermState = if (missingSteps.contains(PermissionStep.PostNotification)) {
		rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
	} else null

	var actedSteps by remember { mutableStateOf(setOf<PermissionStep>()) }

	var allCriticalNowGranted by remember {
		mutableStateOf(PermissionUtils.allCriticalPermissionsGranted(context))
	}

	LaunchedEffect(allCriticalNowGranted) {
		onTrackEvent(
			"permission_dialog_shown",
			mapOf(
				"permission name" to missingSteps.map { it.title },
				"permission count" to missingSteps.size,
			)
		)
	}

	val lifecycleOwner = LocalLifecycleOwner.current
	DisposableEffect(lifecycleOwner) {
		val observer = LifecycleEventObserver { _, event ->
			if (event == Lifecycle.Event.ON_RESUME) {
				allCriticalNowGranted = PermissionUtils.allCriticalPermissionsGranted(context)
			}
		}
		lifecycleOwner.lifecycle.addObserver(observer)
		onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
	}

	AlertDialog(
		onDismissRequest = onDismiss,
		// Optional M3 Expressive touch: Adds an icon to the top of the dialog
		icon = {
			Icon(
				imageVector = Icons.Default.Security,
				contentDescription = null,
				tint = MaterialTheme.colorScheme.primary
			)
		},
		title = {
			Text(
				text = "Permissions needed",
				style = MaterialTheme.typography.headlineSmall,
				color = MaterialTheme.colorScheme.onSurface
			)
		},
		text = {
			Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
				Text(
					text = "To ensure your alarm works, the following permissions are needed:",
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)

				missingSteps.forEach { step ->
					val isActedOn = actedSteps.contains(step)

					PermissionStepRow(
						step = step,
						isActedOn = isActedOn,
						onAction = {
							when (step) {
								PermissionStep.PostNotification -> {
									notificationPermState?.launchPermissionRequest()
									actedSteps = actedSteps + step
								}
								PermissionStep.ExactAlarm -> {
									context.startActivity(
										Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
											data = Uri.fromParts("package", context.packageName, null)
										}
									)
									actedSteps = actedSteps + step
								}
								PermissionStep.FullScreenIntent -> {
									if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
										context.startActivity(
											Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
												data = Uri.fromParts("package", context.packageName, null)
											}
										)
									}
									actedSteps = actedSteps + step
								}
								PermissionStep.XiaomiAutostart -> {
									PermissionUtils.launchXiaomiSettings(context)
									actedSteps = actedSteps + step
								}
							}
						}
					)
				}

				if (missingSteps.any { it == PermissionStep.XiaomiAutostart } && missingSteps.size == 1) {
					// Only autostart missing ? it's advisory, allow skip
					Text(
						text = "Autostart is advisory. Your alarm will still work, but may not survive a reboot on Xiaomi devices.",
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
					text = if (allCriticalNowGranted) "Done" else "Cancel",
					// Use primary for "Done", and secondary/outline color for "Cancel"
					color = if (allCriticalNowGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
				)
			}
		},
		// M3 automatically handles container shape (ExtraLarge) and surface elevation colors.
	)
}

@Composable
private fun PermissionStepRow(
	step: PermissionStep,
	isActedOn: Boolean,
	onAction: () -> Unit
) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.SpaceBetween
	) {
		Column(modifier = Modifier.weight(1f)) {
			Text(
				text = step.title,
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.onSurface,
				fontWeight = FontWeight.SemiBold
			)
			Spacer(modifier = Modifier.height(2.dp))
			Text(
				text = step.rationale,
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
		Spacer(Modifier.width(16.dp))

		if (isActedOn) {
			Icon(
				imageVector = Icons.Default.CheckCircle,
				contentDescription = "Done",
				tint = MaterialTheme.colorScheme.primary,
				modifier = Modifier.size(24.dp)
			)
		} else {
			Button(onClick = {onAction()}, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) {
				Text(
					text = if (step.action != null || step == PermissionStep.XiaomiAutostart) "Open settings" else "Allow",
				)
			}
		}
	}
}
