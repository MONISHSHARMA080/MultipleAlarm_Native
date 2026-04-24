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
	onDismiss: () -> Unit
) {
	val context = LocalContext.current

	// Only POST_NOTIFICATIONS is a runtime permission; the rest are settings deep-links
	val notificationPermState = if (missingSteps.contains(PermissionStep.PostNotification)) {
		rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
	} else null

	// Track which steps user has acted on (so we can show "✓ Done" feedback)
	var actedSteps by remember { mutableStateOf(setOf<PermissionStep>()) }

	// Re-check on every recomposition triggered by returning from settings
	val criticalMissing = missingSteps.filter { it != PermissionStep.XiaomiAutostart }
	val allCriticalNowGranted = PermissionUtils.allCriticalPermissionsGranted(context)

	LaunchedEffect(allCriticalNowGranted) {
		if (allCriticalNowGranted) onAllCriticalGranted()
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
					"To ensure your alarm works reliably, please grant the following:",
					color = Color.Gray,
					fontSize = 13.sp
				)

				missingSteps.forEach { step ->
					val isAdvisory = step == PermissionStep.XiaomiAutostart
					val isActedOn = actedSteps.contains(step)

					PermissionStepRow(
						step = step,
						isAdvisory = isAdvisory,
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

				if (missingSteps.any { it == PermissionStep.XiaomiAutostart } &&
					missingSteps.size == 1) {
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
			// Only show "Done" if all critical permissions are granted
			// or all that's left is the advisory Xiaomi step
			val canProceed = allCriticalNowGranted
			TextButton(
				onClick = { if (canProceed) onAllCriticalGranted() else onDismiss() },
			) {
				Text(
					if (canProceed) "Done" else "Cancel",
					color = if (canProceed) Color(0xFF1A73E8) else Color.Gray
				)
			}
		},
		dismissButton = {
			if (!allCriticalNowGranted) {
				TextButton(onClick = onDismiss) {
					Text("Not now", color = Color.Gray)
				}
			}
		}
	)
}

@Composable
private fun PermissionStepRow(
	step: PermissionStep,
	isAdvisory: Boolean,
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
				if (isAdvisory) {
					Spacer(Modifier.width(6.dp))
					Text(
						"(optional)",
						color = Color.Gray,
						fontSize = 11.sp
					)
				}
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
