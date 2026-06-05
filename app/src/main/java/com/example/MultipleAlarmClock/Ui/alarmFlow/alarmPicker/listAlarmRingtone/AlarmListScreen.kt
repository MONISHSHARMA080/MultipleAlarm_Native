package com.example.MultipleAlarmClock.Ui.alarmPicker.listAlarmRingtone

import android.net.Uri
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
import androidx.compose.ui.unit.dp
import com.example.MultipleAlarmClock.Ui.alarmPicker.data.AlarmSound


@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ListAlarmScreen(
	sounds: List<AlarmSound>,
	selectedUri: Uri?,
	onBack: () -> Unit,
	onSelected: (AlarmSound?) -> Unit,
	modifier: Modifier = Modifier,
) {
	var isRandomSelected by remember { mutableStateOf(selectedUri == null) }
	Scaffold(
		modifier = modifier,
		topBar = {
			LargeTopAppBar(
				title = {
					Text("Pick a sound")
				},
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(
							imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
							contentDescription = "Back"
						)
					}
				}
			)
		}
	) { padding ->

		LazyColumn(
			modifier = Modifier.fillMaxSize().padding(padding),
			contentPadding = PaddingValues(
				horizontal = 24.dp,
				vertical = 16.dp
			),
			verticalArrangement = Arrangement.spacedBy(12.dp)
		) {

			item {
				SoundCard(null, isRandomSelected, onClick = {isRandomSelected = true; onSelected(null)} )
				Spacer(Modifier.padding(bottom = 25.dp))
			}

			items(
				items = sounds,
				key = { it.soundUri}
			) { sound ->
				val selected = sound.soundUri == selectedUri
				SoundCard(
					sound = sound,
					selected = selected,
					onClick = {
						onSelected(sound)
						isRandomSelected = false
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
	onClick: () -> Unit
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
				color = if (selected)
					MaterialTheme.colorScheme.secondary
				else
					MaterialTheme.colorScheme.surfaceContainerHighest,
				shape = CircleShape
			) {
				Box(
					modifier = Modifier.size(46.dp),
					contentAlignment = Alignment.Center
				) {
					Crossfade(
						targetState = selected,
						animationSpec = tween(200),
						label = "icon_swap"
					) { isPlaying ->
						if (isPlaying) {
							EqualizerBars(
								color = MaterialTheme.colorScheme.onSecondary
							)
						} else {
							Icon(
								imageVector = Icons.Rounded.Audiotrack,
								contentDescription = null,
							)
						}
					}
				}
			}

			Spacer(Modifier.width(16.dp))

			Column(modifier = Modifier.weight(1f)) {
				Text(
					text = sound?.title ?: "Random",
					style = MaterialTheme.typography.titleMedium
				)
				Text(
					text = when {
						sound == null && selected -> "Playing a random sound"
						sound == null -> "A random sound will be chosen on every alarm"
						selected -> "Now playing"
						else -> "Tap to preview"
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
