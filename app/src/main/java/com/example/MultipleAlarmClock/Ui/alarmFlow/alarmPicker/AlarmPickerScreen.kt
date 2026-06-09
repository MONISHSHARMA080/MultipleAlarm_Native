package com.coolApps.MultipleAlarmClock.Components_for_ui_compose.alarmPicker

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAlarm
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coolApps.MultipleAlarmClock.dataBase.AlarmData
import com.coolApps.MultipleAlarmClock.dataBase.AlarmErrorField
import com.coolApps.MultipleAlarmClock.dataBase.ValidationResult
import com.example.MultipleAlarmClock.Ui.Permissions.AlarmPermissionDialog
import com.example.MultipleAlarmClock.Ui.Permissions.PermissionStep
import com.example.MultipleAlarmClock.Ui.alarmPicker.AlarmPickerEvent
import com.example.MultipleAlarmClock.Ui.alarmPicker.AlarmPickerViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// AlarmPickerScreen.kt  –  Material 3 Expressive redesign
//
// Design decisions
// ────────────────
// • HERO: the time-range arc (start → end) is enormous – DisplayLarge + custom
//   pill container – it owns ~40 % of the visual height so the eye lands there first.
// • Shape language: all containers use extra-large rounded corners (28 dp) or
//   pill shapes (50 % radius). The CTA is a full-width pill.
// • Color roles used deliberately:
//     primaryContainer   → time hero card
//     secondaryContainer → date + sound cards (supporting)
//     tertiaryContainer  → frequency card (accent variety)
//     surfaceVariant     → message field
//     primary            → CTA button + active accent
//     error              → validation states only
// • Typography scale: DisplayLarge for times, TitleMedium for labels,
//   BodyMedium for helper text, LabelSmall for metadata.
// • Motion: animateColorAsState on the CTA background + container tint,
//   animateFloatAsState on the frequency counter for a spring-pop feel.
// • Breathing room: 20 dp horizontal padding, 16 dp vertical gap between cards.
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(
	ExperimentalMaterial3Api::class,
	ExperimentalFoundationApi::class,
	ExperimentalPermissionsApi::class
)
@Composable
fun AlarmPickerScreen(
	alarm: AlarmData?,
	alarmSetGoBack: () -> Unit,
	onNavigateToSoundList: () -> Unit,
	viewModel: AlarmPickerViewModel,
) {
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	val selectedAlarmSound by viewModel.selectedAlarmSound.collectAsStateWithLifecycle()
	val context = LocalContext.current
	val lifecycleOwner = LocalLifecycleOwner.current

	val alarmObject = uiState.alarmObject
	val validationResult = uiState.validationResult
	val currentError = validationResult as? ValidationResult.Failure
	val validationOk = validationResult is ValidationResult.Success
	val isPermissionsOk = uiState.areAllPermissionsGranted
	val weGood = validationOk && isPermissionsOk

	var showPermissionDialog by remember { mutableStateOf(false) }
	var missingSteps by remember { mutableStateOf<List<PermissionStep>>(emptyList()) }

	// ── Animated accent that bleeds from heroic-ok to alert-error ────────────
	val colorScheme = MaterialTheme.colorScheme
	val heroContainerColor by animateColorAsState(
		targetValue = if (weGood) colorScheme.primaryContainer
		else colorScheme.errorContainer,
		animationSpec = tween(durationMillis = 320),
		label = "heroContainer"
	)
	val heroContentColor by animateColorAsState(
		targetValue = if (weGood) colorScheme.onPrimaryContainer
		else colorScheme.onErrorContainer,
		animationSpec = tween(durationMillis = 320),
		label = "heroContent"
	)
	val ctaColor by animateColorAsState(
		targetValue = if (weGood) colorScheme.primary else colorScheme.surfaceVariant,
		animationSpec = tween(durationMillis = 300),
		label = "cta"
	)
	val ctaContentColor by animateColorAsState(
		targetValue = if (weGood) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
		animationSpec = tween(durationMillis = 300),
		label = "ctaContent"
	)

	// ── Frequency spring-pop ──────────────────────────────────────────────────
	val freqScale by animateFloatAsState(
		targetValue = 1f,
		animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
		label = "freqScale"
	)

	// ── Time-picker sheet state ───────────────────────────────────────────────
	var showStartTimePicker by remember { mutableStateOf(false) }
	var showEndTimePicker   by remember { mutableStateOf(false) }
	var showDatePicker      by remember { mutableStateOf(false) }

	// ── Lifecycle / event handling (unchanged logic) ──────────────────────────
	LaunchedEffect(Unit) { viewModel.checkPermissions(context) }
	LaunchedEffect(Unit) {
		viewModel.events.collect { event ->
			when (event) {
				is AlarmPickerEvent.NavigateBack -> alarmSetGoBack()
				is AlarmPickerEvent.ShowPermissionDialog -> {
					missingSteps = event.missingSteps
					showPermissionDialog = true
				}
				AlarmPickerEvent.UpdateDataStoreGranted -> Unit
			}
		}
	}
	DisposableEffect(lifecycleOwner) {
		val observer = LifecycleEventObserver { _, event ->
			if (event == Lifecycle.Event.ON_RESUME) viewModel.checkPermissions(context)
		}
		lifecycleOwner.lifecycle.addObserver(observer)
		onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
	}

	// ── Root scaffold ─────────────────────────────────────────────────────────
	Scaffold(
		topBar = {
			TopAppBar(
				title = {
					Text(
						text = if (alarm == null) "New alarm" else "Edit alarm",
						style = MaterialTheme.typography.titleLarge,
						fontWeight = FontWeight.SemiBold,
					)
				},
				navigationIcon = {
					IconButton(onClick = alarmSetGoBack) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = "Back"
						)
					}
				},
				colors = TopAppBarDefaults.topAppBarColors(
					containerColor = colorScheme.surface,
					titleContentColor = colorScheme.onSurface,
				),
			)
		},
		containerColor = colorScheme.surface,
	) { innerPadding ->

		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding)
				.verticalScroll(rememberScrollState())
				.padding(horizontal = 20.dp)
				.padding(bottom = 32.dp),
			verticalArrangement = Arrangement.spacedBy(16.dp),
		) {

			Spacer(Modifier.height(4.dp))

			// ══════════════════════════════════════════════════════════════════
			// HERO CARD – Time Range (primary focus of the screen)
			// ══════════════════════════════════════════════════════════════════
			Card(
				shape = RoundedCornerShape(32.dp),
				colors = CardDefaults.cardColors(containerColor = heroContainerColor),
				modifier = Modifier.fillMaxWidth(),
			) {
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 24.dp, vertical = 28.dp),
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.spacedBy(4.dp),
				) {

					Text(
						text = "When should it ring?",
						style = MaterialTheme.typography.labelLarge,
						color = heroContentColor.copy(alpha = 0.7f),
						letterSpacing = 0.5.sp,
					)

					Spacer(Modifier.height(8.dp))

					// Start + End time in one expressive row
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.Center,
						verticalAlignment = Alignment.CenterVertically,
					) {
						// ── Start time chip ──────────────────────────────────
						TimeChip(
							time = alarmObject.startTime,
							label = "FROM",
							contentColor = heroContentColor,
							chipColor = heroContentColor.copy(alpha = 0.12f),
							onClick = { showStartTimePicker = true },
						)

						// ── Arrow divider ────────────────────────────────────
						Box(
							modifier = Modifier.padding(horizontal = 12.dp),
							contentAlignment = Alignment.Center,
						) {
							Icon(
								imageVector = Icons.Default.ArrowForward,
								contentDescription = null,
								tint = heroContentColor.copy(alpha = 0.5f),
								modifier = Modifier.size(20.dp)
							)
						}

						// ── End time chip ────────────────────────────────────
						TimeChip(
							time = alarmObject.endTime,
							label = "UNTIL",
							contentColor = heroContentColor,
							chipColor = heroContentColor.copy(alpha = 0.12f),
							onClick = { showEndTimePicker = true },
						)
					}

					Spacer(Modifier.height(12.dp))

					// Validation / preview text
					val helperText = when {
						currentError?.field == AlarmErrorField.Time -> currentError.message
						validationOk -> viewModel.getFrequencyPreviewText()
							.takeIf { it.isNotBlank() } ?: "Tap a time to edit"
						else -> "Tap a time to edit"
					}

					AnimatedContent(
						targetState = helperText,
						transitionSpec = {
							fadeIn(tween(200)) togetherWith fadeOut(tween(150))
						},
						label = "helperText"
					) { text ->
						Text(
							text = text,
							style = MaterialTheme.typography.bodySmall,
							color = heroContentColor.copy(alpha = 0.75f),
							textAlign = TextAlign.Center,
							modifier = Modifier.fillMaxWidth(),
						)
					}
				}
			}

			// ══════════════════════════════════════════════════════════════════
			// DATE CARD  (secondary emphasis)
			// ══════════════════════════════════════════════════════════════════
			Card(
				shape = RoundedCornerShape(24.dp),
				colors = CardDefaults.cardColors(containerColor = colorScheme.secondaryContainer),
				modifier = Modifier.fillMaxWidth(),
				onClick = { showDatePicker = true },
			) {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 20.dp, vertical = 18.dp),
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.SpaceBetween,
				) {
					Column {
						Text(
							text = "DATE",
							style = MaterialTheme.typography.labelSmall,
							color = colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
							letterSpacing = 1.sp,
						)
						Spacer(Modifier.height(2.dp))
						Text(
							text = formatDate(alarmObject.date),
							style = MaterialTheme.typography.titleLarge,
							fontWeight = FontWeight.SemiBold,
							color = colorScheme.onSecondaryContainer,
						)
					}
					Surface(
						shape = CircleShape,
						color = colorScheme.onSecondaryContainer.copy(alpha = 0.10f),
						modifier = Modifier.size(44.dp),
					) {
						Box(contentAlignment = Alignment.Center) {
							Icon(
								imageVector = Icons.Default.CalendarToday,
								contentDescription = "Pick date",
								tint = colorScheme.onSecondaryContainer,
								modifier = Modifier.size(22.dp),
							)
						}
					}
				}
			}

			// ══════════════════════════════════════════════════════════════════
			// FREQUENCY CARD  (tertiary – accent variety)
			// ══════════════════════════════════════════════════════════════════
			Card(
				shape = RoundedCornerShape(24.dp),
				colors = CardDefaults.cardColors(containerColor = colorScheme.tertiaryContainer),
				modifier = Modifier.fillMaxWidth(),
			) {
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 20.dp, vertical = 18.dp),
				) {
					Text(
						text = "REPEAT EVERY",
						style = MaterialTheme.typography.labelSmall,
						color = colorScheme.onTertiaryContainer.copy(alpha = 0.6f),
						letterSpacing = 1.sp,
					)
					Spacer(Modifier.height(12.dp))
					Row(
						modifier = Modifier.fillMaxWidth(),
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.SpaceBetween,
					) {
						// Decrement
						FreqButton(
							icon = Icons.Default.Remove,
							tint = colorScheme.onTertiaryContainer,
							bgColor = colorScheme.onTertiaryContainer.copy(alpha = 0.12f),
							onClick = { viewModel.decrementFrequency() },
						)

						// Big number
						Column(horizontalAlignment = Alignment.CenterHorizontally) {
							Text(
								text = alarmObject.freqGottenAfterCallback
									.coerceAtLeast(1).toString(),
								style = MaterialTheme.typography.displayMedium,
								fontWeight = FontWeight.Bold,
								color = colorScheme.onTertiaryContainer,
								modifier = Modifier.graphicsLayer {
									scaleX = freqScale
									scaleY = freqScale
								},
							)
							Text(
								text = if (alarmObject.freqGottenAfterCallback == 1L) "minute" else "minutes",
								style = MaterialTheme.typography.bodyMedium,
								color = colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
							)
						}

						// Increment
						FreqButton(
							icon = Icons.Default.Add,
							tint = colorScheme.onTertiaryContainer,
							bgColor = colorScheme.onTertiaryContainer.copy(alpha = 0.12f),
							onClick = { viewModel.incrementFrequency() },
						)
					}

					// Frequency error
					if (currentError?.field == AlarmErrorField.FREQUENCY) {
						Spacer(Modifier.height(8.dp))
						Text(
							text = currentError.message,
							style = MaterialTheme.typography.bodySmall,
							color = colorScheme.error,
						)
					}
				}
			}

			// ══════════════════════════════════════════════════════════════════
			// SOUND ROW  (secondary container, tappable)
			// ══════════════════════════════════════════════════════════════════
			Card(
				shape = RoundedCornerShape(24.dp),
				colors = CardDefaults.cardColors(containerColor = colorScheme.secondaryContainer),
				modifier = Modifier.fillMaxWidth(),
				onClick = onNavigateToSoundList,
			) {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 20.dp, vertical = 18.dp),
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.SpaceBetween,
				) {
					Row(
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.spacedBy(14.dp),
					) {
						Surface(
							shape = CircleShape,
							color = colorScheme.onSecondaryContainer.copy(alpha = 0.10f),
							modifier = Modifier.size(44.dp),
						) {
							Box(contentAlignment = Alignment.Center) {
								Icon(
									imageVector = Icons.Default.MusicNote,
									contentDescription = null,
									tint = colorScheme.onSecondaryContainer,
									modifier = Modifier.size(22.dp),
								)
							}
						}
						Column {
							Text(
								text = "ALARM SOUND",
								style = MaterialTheme.typography.labelSmall,
								color = colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
								letterSpacing = 1.sp,
							)
							Spacer(Modifier.height(2.dp))
							Text(
								text = selectedAlarmSound?.title ?: "Random",
								style = MaterialTheme.typography.titleMedium,
								fontWeight = FontWeight.Medium,
								color = colorScheme.onSecondaryContainer,
								maxLines = 1,
								overflow = TextOverflow.Ellipsis,
							)
						}
					}
					Icon(
						imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
						contentDescription = "Choose sound",
						tint = colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
					)
				}
			}

			// ══════════════════════════════════════════════════════════════════
			// MESSAGE FIELD  (surface variant – lowest emphasis)
			// ══════════════════════════════════════════════════════════════════
			OutlinedTextField(
				value = alarmObject.message,
				onValueChange = { viewModel.updateMessage(it) },
				placeholder = {
					Text(
						"Add a label  (optional)",
						style = MaterialTheme.typography.bodyLarge,
						color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
					)
				},
				leadingIcon = {
					Icon(
						imageVector = Icons.AutoMirrored.Filled.Label,
						contentDescription = null,
						tint = colorScheme.onSurfaceVariant,
					)
				},
				shape = RoundedCornerShape(20.dp),
				colors = OutlinedTextFieldDefaults.colors(
					focusedBorderColor = colorScheme.primary,
					unfocusedBorderColor = colorScheme.outlineVariant,
					focusedContainerColor = colorScheme.surfaceVariant.copy(alpha = 0.4f),
					unfocusedContainerColor = colorScheme.surfaceVariant.copy(alpha = 0.4f),
				),
				singleLine = true,
				modifier = Modifier.fillMaxWidth(),
				textStyle = MaterialTheme.typography.bodyLarge,
			)

			Spacer(Modifier.height(8.dp))

			// ══════════════════════════════════════════════════════════════════
			// PRIMARY CTA  – full-width pill, animated color
			// ══════════════════════════════════════════════════════════════════
			Button(
				onClick = {
					viewModel.onSetAlarmClicked(alarm, alarmObject)
				},
				modifier = Modifier
					.fillMaxWidth()
					.height(60.dp),
				shape = CircleShape,
				colors = ButtonDefaults.buttonColors(
					containerColor = ctaColor,
					contentColor = ctaContentColor,
					disabledContainerColor = colorScheme.surfaceVariant,
					disabledContentColor = colorScheme.onSurfaceVariant,
				),
				elevation = ButtonDefaults.buttonElevation(
					defaultElevation = if (weGood) 4.dp else 0.dp
				),
			) {
				Row(
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(10.dp),
				) {
					Icon(
						imageVector = if (alarm == null) Icons.Default.AddAlarm
						else Icons.Default.Edit,
						contentDescription = null,
						modifier = Modifier.size(22.dp),
					)
					Text(
						text = if (alarm == null) "Set alarm" else "Save changes",
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.SemiBold,
						letterSpacing = 0.3.sp,
					)
				}
			}

			// Permissions note (de-emphasized, surfaces only when needed)
			if (!isPermissionsOk) {
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.Center,
					verticalAlignment = Alignment.CenterVertically,
				) {
					Icon(
						imageVector = Icons.Default.Warning,
						contentDescription = null,
						tint = colorScheme.error,
						modifier = Modifier.size(14.dp),
					)
					Spacer(Modifier.width(6.dp))
					Text(
						text = "Some permissions are missing — tap Set alarm to grant them",
						style = MaterialTheme.typography.bodySmall,
						color = colorScheme.error,
						textAlign = TextAlign.Center,
					)
				}
			}
		}
	}

	// ── Permission dialog (unchanged – just forwarded) ────────────────────────
	if (showPermissionDialog && missingSteps.isNotEmpty()) {
		AlarmPermissionDialog(
			missingSteps ,
			onAllCriticalGranted = {
				showPermissionDialog = false
				viewModel.checkPermissions(context)
			},
			onDismiss = {
				showPermissionDialog = false
				viewModel.checkPermissions(context)
			},
			onTrackEvent = {event, prop -> viewModel.captureEvent(event, prop)}
		)
	}

	// ── Time pickers (modal bottom sheet style) ───────────────────────────────
	if (showStartTimePicker) {
		TimePickerSheet(
			initial = alarmObject.startTime,
			title = "Start time",
			onConfirm = { viewModel.updateStartTime(it); showStartTimePicker = false },
			onDismiss = { showStartTimePicker = false },
		)
	}
	if (showEndTimePicker) {
		TimePickerSheet(
			initial = alarmObject.endTime,
			title = "End time",
			onConfirm = { viewModel.updateEndTime(it); showEndTimePicker = false },
			onDismiss = { showEndTimePicker = false },
		)
	}
	if (showDatePicker) {
		DatePickerSheet(
			initial = Calendar.getInstance().apply { timeInMillis = alarmObject.date },
			onConfirm = { viewModel.updateDate(it); showDatePicker = false },
			onDismiss = { showDatePicker = false },
		)
	}
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-components
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Hero time chip  –  large pill container with a label above and the formatted
 * time in DisplayLarge weight. Tapping opens the time picker.
 */
@Composable
private fun TimeChip(
	time: Calendar,
	label: String,
	contentColor: Color,
	chipColor: Color,
	onClick: () -> Unit,
) {
	Surface(
		shape = RoundedCornerShape(20.dp),
		color = chipColor,
		modifier = Modifier
			.clip(RoundedCornerShape(20.dp))
			.clickable(onClick = onClick),
	) {
		Column(
			modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(2.dp),
		) {
			Text(
				text = label,
				style = MaterialTheme.typography.labelSmall,
				color = contentColor.copy(alpha = 0.55f),
				letterSpacing = 1.sp,
			)
			Text(
				text = formatTime(time),
				style = MaterialTheme.typography.displaySmall,
				fontWeight = FontWeight.Bold,
				color = contentColor,
			)
		}
	}
}

/**
 * Circular +/- button used inside the frequency card.
 */
@Composable
private fun FreqButton(
	icon: ImageVector,
	tint: Color,
	bgColor: Color,
	onClick: () -> Unit,
) {
	Surface(
		shape = CircleShape,
		color = bgColor,
		modifier = Modifier
			.size(52.dp)
			.clip(CircleShape)
			.clickable(onClick = onClick),
	) {
		Box(contentAlignment = Alignment.Center) {
			Icon(
				imageVector = icon,
				contentDescription = null,
				tint = tint,
				modifier = Modifier.size(26.dp),
			)
		}
	}
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun formatTime(cal: Calendar): String {
	val hour   = cal.get(Calendar.HOUR_OF_DAY)
	val minute = cal.get(Calendar.MINUTE)
	val amPm   = if (hour < 12) "AM" else "PM"
	val h      = if (hour % 12 == 0) 12 else hour % 12
	return "%d:%02d %s".format(h, minute, amPm)
}

private fun formatDate(millis: Long): String {
	val sdf = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
	return sdf.format(Date(millis))
}

// ─────────────────────────────────────────────────────────────────────────────
// TimePickerSheet / DatePickerSheet  – you likely have these already; stubs shown
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerSheet(
	initial: Calendar,
	title: String,
	onConfirm: (Calendar) -> Unit,
	onDismiss: () -> Unit,
) {
	val state = rememberTimePickerState(
		initialHour   = initial.get(Calendar.HOUR_OF_DAY),
		initialMinute = initial.get(Calendar.MINUTE),
	)
	ModalBottomSheet(onDismissRequest = onDismiss, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 24.dp)
				.padding(bottom = 24.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			Text(
				text = title,
				style = MaterialTheme.typography.titleLarge,
				fontWeight = FontWeight.SemiBold,
				modifier = Modifier.padding(bottom = 20.dp),
			)
			TimePicker(state = state)
			Spacer(Modifier.height(16.dp))
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(12.dp),
			) {
				OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = CircleShape) {
					Text("Cancel")
				}
				Button(
					onClick = {
						val cal = initial.clone() as Calendar
						cal.set(Calendar.HOUR_OF_DAY, state.hour)
						cal.set(Calendar.MINUTE, state.minute)
						cal.set(Calendar.SECOND, 0)
						onConfirm(cal)
					},
					modifier = Modifier.weight(1f),
					shape = CircleShape,
				) {
					Text("Confirm")
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSheet(
	initial: Calendar,
	onConfirm: (Calendar) -> Unit,
	onDismiss: () -> Unit,
) {
	val state = rememberDatePickerState(initialSelectedDateMillis = initial.timeInMillis)
	ModalBottomSheet(onDismissRequest = onDismiss, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 16.dp)
				.padding(bottom = 24.dp),
		) {
			DatePicker(state = state)
			Spacer(Modifier.height(8.dp))
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 8.dp),
				horizontalArrangement = Arrangement.spacedBy(12.dp),
			) {
				OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = CircleShape) {
					Text("Cancel")
				}
				Button(
					onClick = {
						val millis = state.selectedDateMillis ?: initial.timeInMillis
						val cal = Calendar.getInstance().apply { timeInMillis = millis }
						onConfirm(cal)
					},
					modifier = Modifier.weight(1f),
					shape = CircleShape,
				) {
					Text("Confirm")
				}
			}
		}
	}
}
