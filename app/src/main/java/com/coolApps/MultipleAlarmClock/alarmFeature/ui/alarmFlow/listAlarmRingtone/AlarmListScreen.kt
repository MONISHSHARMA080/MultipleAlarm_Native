package com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.listAlarmRingtone

import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker.AlarmPickerViewModel
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker.data.AlarmSound


@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ListAlarmSoundScreen(
	vm: AlarmPickerViewModel,
	selectedUri: Uri?,
	previewingUri: Uri?,
	onBack: () -> Unit,
	onProceed: (AlarmSound?) -> Unit,
) {
	val view = LocalView.current
	val listOfAlarms by vm.listOfAlarms.collectAsStateWithLifecycle()
	val randomPreviewing by vm.previewingRandom.collectAsStateWithLifecycle()
	val selectedAlarmSound by vm.selectedAlarmSound.collectAsStateWithLifecycle()

	// Track the temporarily selected sound (the one the user clicked on, but hasn't proceeded with yet)
	var pendingSelectedSound by remember { mutableStateOf(selectedAlarmSound) }
	// We also need to track if random was selected as pending, which means pendingSelectedSound is null, 
	// but initially if selectedUri is null, then random is the pending one.
	var isPendingRandom by remember { mutableStateOf(selectedUri == null) }

	Scaffold(
		modifier = Modifier.padding(2.dp),
		topBar = {
			LargeTopAppBar(
				title = {
					Text(stringResource(R.string.Sound_screen_heading))
				},
				navigationIcon = {
					IconButton(onClick = {
						view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
						onBack()
					}) {
						Icon(
							imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
							contentDescription = "Back"
						)
					}
				}
			)
		},
		bottomBar = {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.padding(16.dp),
				contentAlignment = Alignment.Center
			) {
				Button(
					onClick = {
						view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
						onProceed(if (isPendingRandom) null else pendingSelectedSound)
					},
					modifier = Modifier.fillMaxWidth()
				) {
					Text("Select")
				}
			}
		}
	) { padding ->
		LazyColumn(
			modifier = Modifier.fillMaxSize().padding(padding),
			contentPadding = PaddingValues(start = 15.dp, end = 15.dp, top = 0.dp, bottom = 80.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp)
		) {
			item {
				SoundCard(
					sound = null,
					selected = isPendingRandom,
					isPlaying = randomPreviewing,
					onClick = {
						view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
						isPendingRandom = true
						pendingSelectedSound = null
						vm.previewSound(null)
					},
					imageVector = Icons.Rounded.Shuffle
				)
				Spacer(Modifier.padding(bottom = 25.dp))
			}

			items(
				items = listOfAlarms,
				key = { it.soundUri }
			) { sound ->
				SoundCard(
					sound = sound,
					selected = !isPendingRandom && pendingSelectedSound?.soundUri == sound.soundUri,
					isPlaying = previewingUri == sound.soundUri,
					onClick = {
						view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
						isPendingRandom = false
						pendingSelectedSound = sound
						vm.previewSound(sound)
					}
				)
			}
		}
	}
}

@Composable
private fun SoundCard(
	sound: AlarmSound?,
	selected: Boolean,
	isPlaying: Boolean,
	onClick: () -> Unit,
	imageVector: ImageVector = Icons.Rounded.Audiotrack
) {
	val containerColor = if (selected)
		MaterialTheme.colorScheme.secondaryContainer
	else
		MaterialTheme.colorScheme.surfaceContainer
	val contentColor = if (selected)
		MaterialTheme.colorScheme.onSecondaryContainer
	else
		MaterialTheme.colorScheme.onSurface

	ElevatedCard(
		onClick = onClick,
		shape = MaterialTheme.shapes.extraLarge,
		colors = CardDefaults.elevatedCardColors(
			containerColor = containerColor,
			contentColor = contentColor
		)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 20.dp, vertical = 18.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Surface(
				color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainerHighest,
				shape = CircleShape
			) {
				Box(
					modifier = Modifier.size(46.dp),
					contentAlignment = Alignment.Center
				) {
					Crossfade(
						targetState = isPlaying,
						animationSpec = tween(200),
						label = "icon_swap"
					) { isPlaying ->
						if (isPlaying) {
							EqualizerBars(
								color = MaterialTheme.colorScheme.onSecondary
							)
						} else {
							Icon(
								imageVector = imageVector,
								contentDescription = null,
							)
						}
					}
				}
			}

			Spacer(Modifier.width(16.dp))

			Column(modifier = Modifier.weight(1f)) {
				Text(
					text = sound?.title ?: stringResource(R.string.Sound_screen_Random),
					style = MaterialTheme.typography.titleMedium
				)
				Text(
					text = when {
						sound == null && selected ->stringResource(R.string.Sound_screen_Random_sound_choosen)
						sound == null ->stringResource(R.string.alarm_picker_sound_random)
						selected ->stringResource(R.string.Sound_screen_selected)
						else ->stringResource(R.string.Sound_screen_preview)
					},
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}

			AnimatedVisibility(selected) {
				Icon(
					imageVector = Icons.Rounded.Check,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.secondary
				)
			}
		}
	}
}


@Composable
private fun EqualizerBars(modifier: Modifier = Modifier, color: Color) {
	val infiniteTransition = rememberInfiniteTransition(label = "eq")
	val heights = listOf(
		infiniteTransition.animateFloat(
			initialValue = 4f, targetValue = 14f, label = "bar1",
			animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse)
		),
		infiniteTransition.animateFloat(
			initialValue = 14f, targetValue = 4f, label = "bar2",
			animationSpec = infiniteRepeatable(tween(700, delayMillis = 120, easing = FastOutSlowInEasing), RepeatMode.Reverse)
		),
		infiniteTransition.animateFloat(
			initialValue = 8f, targetValue = 14f, label = "bar3",
			animationSpec = infiniteRepeatable(tween(700, delayMillis = 60, easing = FastOutSlowInEasing), RepeatMode.Reverse)
		),
	)
	Row(
		modifier = modifier.size(20.dp),
		horizontalArrangement = Arrangement.spacedBy(2.5.dp),
		verticalAlignment = Alignment.Bottom
	) {
		heights.forEach { height ->
			Box(
				modifier = Modifier
					.width(3.dp)
					.height(height.value.dp)
					.clip(RoundedCornerShape(2.dp))
					.background(color)
			)
		}
	}
}
