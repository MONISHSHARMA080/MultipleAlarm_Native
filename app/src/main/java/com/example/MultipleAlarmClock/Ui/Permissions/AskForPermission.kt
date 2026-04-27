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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

	val lifecycleOwner =LocalLifecycleOwner.current
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
		containerColor = Color(0xFF1C2333),
		title = {
			Text(
				"Permissions needed",
				color = Color.White,
				fontWeight = FontWeight.Bold,
				fontSize = 18.sp
			)
		},
		text = {
			Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
				Text(
					"To ensure your alarm works, following permissions are needed:",
					color = Color.Gray,
					fontSize = 13.sp
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

				if ( missingSteps.any { it == PermissionStep.XiaomiAutostart } && missingSteps.size == 1 ) {
					// Only autostart missing — it's advisory, allow skip
					Text(
						"Autostart is advisory. Your alarm will still work, but may not survive a reboot on Xiaomi devices.",
						color = Color.Gray,
						fontSize = 11.sp,
						fontStyle = FontStyle.Italic
					)
				}
			}
		},
		confirmButton = {
			TextButton(
				onClick = { if (allCriticalNowGranted) onAllCriticalGranted() else onDismiss() },
			) {
				Text(
					if (allCriticalNowGranted) "Done" else "Cancel",
					color = if (allCriticalNowGranted) Color(0xFF1A73E8) else Color.Gray
				)
			}
		},
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
			Row(verticalAlignment = Alignment.CenterVertically) {
				Text(
					step.title,
					color = Color.White,
					fontWeight = FontWeight.SemiBold,
					fontSize = 14.sp
				)
			}
			Text(
				step.rationale,
				color = Color.Gray,
				fontSize = 12.sp
			)
		}
		Spacer(Modifier.width(8.dp))
		if (isActedOn) {
			Icon(
				Icons.Default.CheckCircle,
				contentDescription = "Done",
				tint = Color(0xFF1A73E8),
				modifier = Modifier.size(20.dp)
			)
		} else {
			TextButton(onClick = onAction) {
				Text(
					if (step.action != null || step == PermissionStep.XiaomiAutostart)
						"Open settings" else "Allow",
					color = Color(0xFF1A73E8),
					fontSize = 13.sp
				)
			}
		}
	}
}
