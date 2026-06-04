package com.example.MultipleAlarmClock.Ui.alarmPicker.listAlarmRingtone

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.MultipleAlarmClock.Ui.alarmPicker.data.AlarmSound


@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ListAlarmScreen(
	sounds: List<AlarmSound>,
	selectedUri: Uri?,
	onSelected: (AlarmSound?) -> Unit,
	modifier: Modifier = Modifier,
) {
//	/** [onSelected]  2 states on onSelected; one is random alarm leave the intent field as null and other is uri id etc, put it in db */
	Scaffold(
		modifier = modifier,
		topBar = {
			LargeTopAppBar(
				title = {
					Text("Alarm sound")
				}
			)
		}
	) { padding ->

		LazyColumn(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding),
			contentPadding = PaddingValues(
				horizontal = 24.dp,
				vertical = 16.dp
			),
			verticalArrangement = Arrangement.spacedBy(12.dp)
		) {

			item {

				Text(
					text = "Choose a sound",
					style = MaterialTheme.typography.displaySmall,
					color = MaterialTheme.colorScheme.onSurface
				)

				Spacer(Modifier.height(8.dp))

				Text(
					text = "Used when the alarm rings.",
					style = MaterialTheme.typography.bodyLarge,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}

			items(
				items = sounds,
				key = { it.soundUri}
			) { sound, index ->
				// here get the index too
				val selected = sound.soundUri == selectedUri

				SoundCard(
					sound = sound,
					selected = selected,
					onClick = {
						onSelected(sound)
					}
				)
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSoundsScreen(
	sounds: List<AlarmSound>,
	selectedUri: Uri?,
	onSoundSelected: (AlarmSound) -> Unit,
	modifier: Modifier = Modifier
) {
	Scaffold(
		modifier = modifier,
		topBar = {
			LargeTopAppBar(
				title = {
					Text("Choose a sound")
				}
			)
		}
	) { padding ->

		LazyColumn(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding),
			contentPadding = PaddingValues(
				horizontal = 24.dp,
				vertical = 16.dp
			),
			verticalArrangement = Arrangement.spacedBy(12.dp)
		) {

			item {
				SoundCard(null, selectedUri == null )
			}

			items(
				items = sounds,
				key = { it.soundUri.toString() }
			) { sound ->

				val selected = sound.soundUri== selectedUri

				SoundCard(
					sound = sound,
					selected = selected,
					onClick = {
						onSoundSelected(sound)
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

	val containerColor =
		if (selected)
			MaterialTheme.colorScheme.secondaryContainer
		else
			MaterialTheme.colorScheme.surfaceContainer

	val contentColor =
		if (selected)
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
				.padding(
					horizontal = 20.dp,
					vertical = 18.dp
				),
			verticalAlignment = Alignment.CenterVertically
		) {

			Surface(
				color =
					if (selected)
						MaterialTheme.colorScheme.secondary
					else
						MaterialTheme.colorScheme.surfaceContainerHighest,
				shape = CircleShape
			) {
				Icon(
					imageVector = Icons.Rounded.Audiotrack,
					contentDescription = null,
					modifier = Modifier.padding(13.dp)
				)
			}

			Spacer(Modifier.width(16.dp))

			Column(
				modifier = Modifier.weight(1f)
			) {

				Text(
					text = sound?.title ?: "Random",
					style = MaterialTheme.typography.titleMedium
				)

				Text(
					text = "Alarm tone",
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