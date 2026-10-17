package com.coolApps.MultipleAlarmClock.presentation.picker.listAlarmRingtone

import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.presentation.picker.AlarmPickerViewModel
import com.coolApps.MultipleAlarmClock.presentation.picker.AlarmSound


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable fun ListAlarmSoundScreen(
	vm: AlarmPickerViewModel,
	selectedUri: Uri?,
	previewingUri: Uri?,
	onBack: () -> Unit,
	onProceed: (AlarmSound?) -> Unit,
	linearProgressBar: (@Composable () -> Unit)? = null,
	onNavigateToPaywall: (Boolean) -> Unit = {}
) {
	val fromOnboarding = linearProgressBar != null
	val view = LocalView.current
	val listOfAlarms by vm.listOfAlarms.collectAsStateWithLifecycle()
	val randomPreviewing by vm.previewingRandom.collectAsStateWithLifecycle()
	val selectedAlarmSound by vm.selectedAlarmSound.collectAsStateWithLifecycle()
	val uiState by vm.uiState.collectAsStateWithLifecycle()

	LaunchedEffect(uiState.showPaywall) {
		if (uiState.showPaywall) {
			onNavigateToPaywall(true)
			vm.navigationToPaywallComplete()
		}
	}
	
	LaunchedEffect(uiState.soundSelectionCompletedGoBack) {
		if (uiState.soundSelectionCompletedGoBack) {
			vm.consumeSoundSelectionCompleted()
			onBack()
		}
	}

	var soundToSelect by remember { mutableStateOf(selectedAlarmSound) }
	var isSoundToSelectRandom by remember { mutableStateOf(selectedUri == null) }

	val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

	Scaffold(
		containerColor = MaterialTheme.colorScheme.surface,
		modifier = Modifier
			.let { if (!fromOnboarding) it.nestedScroll(scrollBehavior.nestedScrollConnection) else it },
		topBar = {
			if (!fromOnboarding) {
				val backInteractionSource = remember { MutableInteractionSource() }
				val isBackPressed by backInteractionSource.collectIsPressedAsState()
				val backCorner by animateDpAsState(
					targetValue = if (isBackPressed) 8.dp else 14.dp,
					animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
					label = "backCorner"
				)

				LargeTopAppBar(
					title = {
						Text(
							text = stringResource(R.string.alarm_sound_title),
							fontWeight = FontWeight.ExtraBold
						)
					},
					navigationIcon = {
						FilledTonalIconButton(
							onClick = {
								view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
								onBack()
							},
							interactionSource = backInteractionSource,
							shape = RoundedCornerShape(backCorner),
							colors = IconButtonDefaults.filledTonalIconButtonColors(
								containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
								contentColor = MaterialTheme.colorScheme.onSurface
							),
							modifier = Modifier
								.padding(start = 8.dp)
								.size(44.dp)
						) {
							Icon(
								imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
								contentDescription = "Back"
							)
						}
					},
					colors = TopAppBarDefaults.topAppBarColors(
						containerColor = MaterialTheme.colorScheme.surface,
						scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
					),
					scrollBehavior = scrollBehavior
				)
			}
		},
		bottomBar = {
			val selectInteractionSource = remember { MutableInteractionSource() }
			val isSelectPressed by selectInteractionSource.collectIsPressedAsState()
			val selectCorner by animateDpAsState(
				targetValue = if (isSelectPressed) 16.dp else 28.dp,
				animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
				label = "selectCorner"
			)
			val selectScale by animateFloatAsState(
				targetValue = if (isSelectPressed) 0.97f else 1f,
				animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
				label = "selectScale"
			)

			Box(
				modifier = Modifier
					.fillMaxWidth()
					.navigationBarsPadding()
					.padding(horizontal = 20.dp, vertical = 16.dp),
				contentAlignment = Alignment.Center
			) {
				Button(
					onClick = {
						view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
						onProceed(if (isSoundToSelectRandom) null else soundToSelect)
					},
					interactionSource = selectInteractionSource,
					colors = ButtonDefaults.buttonColors(
						containerColor = MaterialTheme.colorScheme.primary,
						contentColor = MaterialTheme.colorScheme.onPrimary
					),
					shape = RoundedCornerShape(selectCorner),
					modifier = Modifier
						.widthIn(max = 600.dp)
						.fillMaxWidth()
						.height(58.dp)
						.graphicsLayer {
							scaleX = selectScale
							scaleY = selectScale
						}
				) {
					Text(
						text = "Select",
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.ExtraBold
					)
				}
			}
		}
	) { padding ->
		Box(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding),
			contentAlignment = Alignment.TopCenter
		) {
			LazyColumn(
				modifier = Modifier
					.widthIn(max = 640.dp)
					.fillMaxSize(),
				contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
				verticalArrangement = Arrangement.spacedBy(3.dp)
			) {
				item(key = "random_hero_sound") {
					SoundCard(
						sound = null,
						selected = selectedUri == null,
						isPlaying = randomPreviewing,
						isHero = true,
						topCorner = 24.dp,
						bottomCorner = 24.dp,
						onClick = {
							view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
							isSoundToSelectRandom = true
							soundToSelect = null
							vm.previewSound(null)
						},
						imageVector = Icons.Rounded.Shuffle
					)
					Spacer(Modifier.height(20.dp))
				}

				itemsIndexed(
					items = listOfAlarms,
					key = { _, it -> it.soundUri }
				) { index, sound ->
					val isFirst = index == 0
					val isLast = index == listOfAlarms.lastIndex
					val baseTopCorner = if (isFirst) 24.dp else 6.dp
					val baseBottomCorner = if (isLast) 24.dp else 6.dp

					SoundCard(
						sound = sound,
						selected = selectedUri == sound.soundUri,
						isPlaying = previewingUri == sound.soundUri && !randomPreviewing,
						isHero = false,
						topCorner = baseTopCorner,
						bottomCorner = baseBottomCorner,
						onClick = {
							view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
							isSoundToSelectRandom = false
							soundToSelect = sound
							vm.previewSound(sound)
						}
					)
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SoundCard(
	sound: AlarmSound?,
	selected: Boolean,
	isPlaying: Boolean,
	isHero: Boolean = false,
	topCorner: Dp = 20.dp,
	bottomCorner: Dp = 20.dp,
	onClick: () -> Unit,
	imageVector: ImageVector = Icons.Rounded.Audiotrack
) {
	val interactionSource = remember { MutableInteractionSource() }
	val isPressed by interactionSource.collectIsPressedAsState()

	val spatialSpec = MaterialTheme.motionScheme.fastSpatialSpec<Dp>()
	val floatSpatialSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
	val colorEffectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()

	val targetTopCorner = when {
		isPressed -> 16.dp
		isHero -> 26.dp
		selected || isPlaying -> 22.dp
		else -> topCorner
	}
	val targetBottomCorner = when {
		isPressed -> 16.dp
		isHero -> 26.dp
		selected || isPlaying -> 22.dp
		else -> bottomCorner
	}

	val animatedTopCorner by animateDpAsState(
		targetValue = targetTopCorner,
		animationSpec = spatialSpec,
		label = "topCorner"
	)
	val animatedBottomCorner by animateDpAsState(
		targetValue = targetBottomCorner,
		animationSpec = spatialSpec,
		label = "bottomCorner"
	)

	val scale by animateFloatAsState(
		targetValue = if (isPressed) 0.98f else 1f,
		animationSpec = floatSpatialSpec,
		label = "cardScale"
	)

	val containerColor by animateColorAsState(
		targetValue = when {
			isPlaying -> MaterialTheme.colorScheme.secondaryContainer
			selected -> MaterialTheme.colorScheme.primaryContainer
			isHero -> MaterialTheme.colorScheme.surfaceContainerHigh
			else -> MaterialTheme.colorScheme.surfaceContainerLow
		},
		animationSpec = colorEffectsSpec,
		label = "containerColor"
	)
	val contentColor by animateColorAsState(
		targetValue = when {
			isPlaying -> MaterialTheme.colorScheme.onSecondaryContainer
			selected -> MaterialTheme.colorScheme.onPrimaryContainer
			else -> MaterialTheme.colorScheme.onSurface
		},
		animationSpec = colorEffectsSpec,
		label = "contentColor"
	)

	val badgeCorner by animateDpAsState(
		targetValue = if (isPlaying || selected) 24.dp else 14.dp,
		animationSpec = spatialSpec,
		label = "badgeCorner"
	)

	val badgeColor by animateColorAsState(
		targetValue = when {
			isPlaying -> MaterialTheme.colorScheme.secondary
			selected -> MaterialTheme.colorScheme.primary
			isHero -> MaterialTheme.colorScheme.tertiaryContainer
			else -> MaterialTheme.colorScheme.surfaceContainerHighest
		},
		animationSpec = colorEffectsSpec,
		label = "badgeColor"
	)

	val badgeContentColor by animateColorAsState(
		targetValue = when {
			isPlaying -> MaterialTheme.colorScheme.onSecondary
			selected -> MaterialTheme.colorScheme.onPrimary
			isHero -> MaterialTheme.colorScheme.onTertiaryContainer
			else -> MaterialTheme.colorScheme.onSurfaceVariant
		},
		animationSpec = colorEffectsSpec,
		label = "badgeContentColor"
	)

	val verticalMargin by animateDpAsState(
		targetValue = if ((selected || isPlaying) && !isHero) 4.dp else 0.dp,
		animationSpec = spatialSpec,
		label = "verticalMargin"
	)

	Surface(
		onClick = onClick,
		interactionSource = interactionSource,
		shape = RoundedCornerShape(
			topStart = animatedTopCorner.coerceAtLeast(0.dp),
			topEnd = animatedTopCorner.coerceAtLeast(0.dp),
			bottomStart = animatedBottomCorner.coerceAtLeast(0.dp),
			bottomEnd = animatedBottomCorner.coerceAtLeast(0.dp)
		),
		color = containerColor,
		contentColor = contentColor,
		border = if (selected && !isPlaying) {
			BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
		} else {
			null
		},
		modifier = Modifier
			.fillMaxWidth()
			.padding(vertical = verticalMargin.coerceAtLeast(0.dp))
			.graphicsLayer {
				scaleX = scale
				scaleY = scale
			}
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 18.dp, vertical = if (isHero) 20.dp else 16.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Surface(
				color = badgeColor,
				contentColor = badgeContentColor,
				shape = RoundedCornerShape(badgeCorner)
			) {
				Box(
					modifier = Modifier.size(48.dp),
					contentAlignment = Alignment.Center
				) {
					Crossfade(
						targetState = isPlaying,
						modifier = Modifier.fillMaxSize(),
						animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
						label = "icon_swap"
					) { isPlayingState ->
						Box(
							modifier = Modifier.fillMaxSize(),
							contentAlignment = Alignment.Center
						) {
							if (isPlayingState) {
								EqualizerBars(
									color = LocalContentColor.current
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
			}

			Spacer(Modifier.width(16.dp))

			Column(modifier = Modifier.weight(1f)) {
				Text(
					text = sound?.title ?: stringResource(R.string.Sound_screen_Random),
					style = if (isHero) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
					fontWeight = if (selected || isPlaying || isHero) FontWeight.ExtraBold else FontWeight.SemiBold,
					color = contentColor
				)
				Spacer(Modifier.height(2.dp))
				Text(
					text = when {
						sound == null && selected -> stringResource(R.string.Sound_screen_Random_sound_choosen)
						sound == null -> stringResource(R.string.alarm_picker_sound_random)
						selected -> stringResource(R.string.Sound_screen_selected)
						else -> stringResource(R.string.Sound_screen_preview)
					},
					style = MaterialTheme.typography.bodyMedium,
					color = contentColor.copy(alpha = 0.78f)
				)
			}

			AnimatedVisibility(
				visible = selected,
				enter = scaleIn(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()) +
						fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
				exit = scaleOut(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()) +
						fadeOut(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec())
			) {
				Surface(
					color = MaterialTheme.colorScheme.primary,
					contentColor = MaterialTheme.colorScheme.onPrimary,
					shape = CircleShape,
					modifier = Modifier.size(32.dp)
				) {
					Box(contentAlignment = Alignment.Center) {
						Icon(
							imageVector = Icons.Rounded.Check,
							contentDescription = null,
							modifier = Modifier.size(18.dp)
						)
					}
				}
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
		modifier = modifier.height(14.dp),
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

