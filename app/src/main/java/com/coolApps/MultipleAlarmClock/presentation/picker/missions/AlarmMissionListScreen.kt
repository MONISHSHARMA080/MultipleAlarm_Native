package com.coolApps.MultipleAlarmClock.presentation.picker.missions

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.presentation.ExpressiveAlarmTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlarmMissionListScreen(
	onBack: () -> Unit,
	onTrackEvent: (String, Map<String, Any>) -> Unit,
	showTopBar: Boolean = true,
) {
	val view = LocalView.current

	var selectedMission by rememberSaveable { mutableStateOf<AlarmMission?>(null) }
	var comingSoonMission by rememberSaveable { mutableStateOf<AlarmMission?>(null) }

	val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

	comingSoonMission?.let { mission ->
		MissionComingSoonDialog(
			mission = mission,
			onDismiss = { comingSoonMission = null },
			onConfirm = {
				comingSoonMission = null
				onBack()
			}
		)
	}

	Scaffold(
		containerColor = MaterialTheme.colorScheme.surface,
		contentWindowInsets = WindowInsets.safeDrawing,
		modifier = Modifier.let {
			if (showTopBar) it.nestedScroll(scrollBehavior.nestedScrollConnection) else it
		},
		topBar = {
			if (showTopBar) {
				LargeFlexibleTopAppBar(
					title = {
						Text(
							text = stringResource(R.string.alarm_missions_title),
							fontWeight = FontWeight.ExtraBold
						)
					},
					subtitle = {
						Text(
							text = stringResource(R.string.alarm_missions_subtitle),
							maxLines = 2,
							overflow = TextOverflow.Ellipsis
						)
					},
					navigationIcon = {
						IconButton(
							onClick = {
								view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
								onBack()
							}
						) {
							Icon(
								imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
								contentDescription = stringResource(R.string.alarm_missions_back_desc)
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
			val buttonHeight = ButtonDefaults.MediumContainerHeight
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.navigationBarsPadding()
					.padding(horizontal = 20.dp, vertical = 16.dp),
				contentAlignment = Alignment.Center
			) {
				Button(
					onClick = {
						val mission = selectedMission ?: return@Button
						view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
						onTrackEvent(
							"mission_select_clicked",
							mapOf(
								"mission_id" to mission.id,
								"mission_name" to mission.analyticsName
							)
						)
						comingSoonMission = mission
					},
					enabled = selectedMission != null,
					shapes = ButtonDefaults.shapesFor(buttonHeight),
					contentPadding = ButtonDefaults.contentPaddingFor(buttonHeight),
					colors = ButtonDefaults.buttonColors(
						containerColor = MaterialTheme.colorScheme.primary,
						contentColor = MaterialTheme.colorScheme.onPrimary
					),
					modifier = Modifier
						.widthIn(max = 600.dp)
						.fillMaxWidth()
						.height(buttonHeight)
				) {
					val ctaTextEffectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
					AnimatedContent(
						targetState = selectedMission,
						transitionSpec = {
							fadeIn(ctaTextEffectsSpec) togetherWith
								fadeOut(ctaTextEffectsSpec)
						},
						label = "select_mission_cta_label"
					) { mission ->
						Text(
							text = if (mission != null) {
								stringResource(
									R.string.alarm_missions_select_mission,
									stringResource(mission.titleRes)
								)
							} else {
								stringResource(R.string.alarm_missions_select_button)
							},
							style = MaterialTheme.typography.titleMediumEmphasized
						)
					}
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
			LazyVerticalGrid(
				columns = GridCells.Adaptive(minSize = 152.dp),
				modifier = Modifier
					.widthIn(max = 640.dp)
					.fillMaxSize(),
				contentPadding = PaddingValues(
					start = 16.dp,
					end = 16.dp,
					top = 8.dp,
					bottom = 96.dp
				),
				horizontalArrangement = Arrangement.spacedBy(12.dp),
				verticalArrangement = Arrangement.spacedBy(12.dp)
			) {
				if (!showTopBar) {
					item(
						key = "missions_subtitle",
						span = { GridItemSpan(maxLineSpan) }
					) {
						Text(
							text = stringResource(R.string.alarm_missions_subtitle),
							style = MaterialTheme.typography.bodyMedium,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
							modifier = Modifier.padding(bottom = 4.dp)
						)
					}
				}

				items(
					items = AlarmMission.entries,
					key = { it.id }
				) { mission ->
					val isSelected = selectedMission == mission
					MissionCard(
						mission = mission,
						isSelected = isSelected,
						onClick = {
							view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
							selectedMission = if (selectedMission == mission) null else mission
						}
					)
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MissionCard(
	mission: AlarmMission,
	isSelected: Boolean,
	onClick: () -> Unit,
	modifier: Modifier = Modifier
) {
	val interactionSource = remember { MutableInteractionSource() }
	val isPressed by interactionSource.collectIsPressedAsState()

	val fastSpatialDpSpec = MaterialTheme.motionScheme.fastSpatialSpec<Dp>()
	val fastSpatialFloatSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
	val defaultSpatialFloatSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
	val colorEffectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()

	val cornerRadius by animateDpAsState(
		targetValue = when {
			isPressed -> 16.dp
			isSelected -> 32.dp
			else -> 24.dp
		},
		animationSpec = fastSpatialDpSpec,
		label = "missionCardCorner"
	)
	val scale by animateFloatAsState(
		targetValue = if (isPressed) 0.97f else 1f,
		animationSpec = fastSpatialFloatSpec,
		label = "missionCardScale"
	)
	val backdropScale by animateFloatAsState(
		targetValue = if (isSelected) 1.08f else 1f,
		animationSpec = defaultSpatialFloatSpec,
		label = "missionBackdropScale"
	)
	val backdropRotation by animateFloatAsState(
		targetValue = if (isSelected) 8f else 0f,
		animationSpec = defaultSpatialFloatSpec,
		label = "missionBackdropRotation"
	)
	val containerColor by animateColorAsState(
		targetValue = if (isSelected) {
			MaterialTheme.colorScheme.primaryContainer
		} else {
			MaterialTheme.colorScheme.surfaceContainerLow
		},
		animationSpec = colorEffectsSpec,
		label = "missionCardContainer"
	)
	val contentColor by animateColorAsState(
		targetValue = if (isSelected) {
			MaterialTheme.colorScheme.onPrimaryContainer
		} else {
			MaterialTheme.colorScheme.onSurface
		},
		animationSpec = colorEffectsSpec,
		label = "missionCardContent"
	)
	val stageColor by animateColorAsState(
		targetValue = if (isSelected) {
			MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.72f)
		} else {
			MaterialTheme.colorScheme.surfaceContainerHigh
		},
		animationSpec = colorEffectsSpec,
		label = "missionStageColor"
	)

	val accentShape = mission.expressiveBackdropShape()
	val baseAccentColor = mission.expressiveAccentContainerColor()
	val accentBackdropColor by animateColorAsState(
		targetValue = if (isSelected) {
			baseAccentColor
		} else {
			baseAccentColor.copy(alpha = 0.65f)
		},
		animationSpec = colorEffectsSpec,
		label = "missionAccentBackdropColor"
	)
	val checkBadgeShape = MaterialShapes.Cookie9Sided.toShape()

	Surface(
		onClick = onClick,
		interactionSource = interactionSource,
		shape = RoundedCornerShape(cornerRadius),
		color = containerColor,
		contentColor = contentColor,
		border = if (isSelected) {
			BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
		} else {
			null
		},
		modifier = modifier
			.fillMaxWidth()
			.semantics {
				selected = isSelected
				role = Role.RadioButton
			}
			.graphicsLayer {
				scaleX = scale
				scaleY = scale
			}
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(12.dp)
		) {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.height(120.dp)
					.clip(RoundedCornerShape(18.dp))
					.background(stageColor),
				contentAlignment = Alignment.Center
			) {
				Box(
					modifier = Modifier
						.size(92.dp)
						.graphicsLayer {
							scaleX = backdropScale
							scaleY = backdropScale
							rotationZ = backdropRotation
						}
						.background(accentBackdropColor, accentShape)
				)

				MissionIllustration(
					mission = mission,
					isSelected = isSelected,
					modifier = Modifier.fillMaxSize()
				)

				androidx.compose.animation.AnimatedVisibility(
					visible = isSelected,
					enter = scaleIn(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()) +
						fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
					exit = scaleOut(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()) +
						fadeOut(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
					modifier = Modifier
						.align(Alignment.TopEnd)
						.padding(8.dp)
				) {
					Surface(
						color = MaterialTheme.colorScheme.primary,
						contentColor = MaterialTheme.colorScheme.onPrimary,
						shape = checkBadgeShape,
						modifier = Modifier.size(30.dp)
					) {
						Box(contentAlignment = Alignment.Center) {
							Icon(
								imageVector = Icons.Rounded.Check,
								contentDescription = null,
								modifier = Modifier.size(16.dp)
							)
						}
					}
				}
			}

			Spacer(modifier = Modifier.height(12.dp))

			Text(
				text = stringResource(mission.titleRes),
				style = if (isSelected) {
					MaterialTheme.typography.titleMediumEmphasized
				} else {
					MaterialTheme.typography.titleMedium
				},
				color = contentColor,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)

			Spacer(modifier = Modifier.height(4.dp))

			Text(
				text = stringResource(mission.descriptionRes),
				style = MaterialTheme.typography.bodySmall,
				color = contentColor.copy(alpha = 0.78f),
				maxLines = 2,
				minLines = 2,
				overflow = TextOverflow.Ellipsis
			)
		}
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MissionComingSoonDialog(
	mission: AlarmMission,
	onDismiss: () -> Unit,
	onConfirm: () -> Unit,
	modifier: Modifier = Modifier
) {
	var showDialog by remember { mutableStateOf(false) }
	LaunchedEffect(Unit) { showDialog = true }

	val heroBackdropShape = mission.expressiveBackdropShape()
	val heroBackdropColor = mission.expressiveAccentContainerColor()
	val infiniteTransition = rememberInfiniteTransition(label = "coming_soon_hero_spin")
	val heroRotation by infiniteTransition.animateFloat(
		initialValue = 0f,
		targetValue = 360f,
		animationSpec = infiniteRepeatable(
			animation = tween(durationMillis = 16000, easing = LinearEasing),
			repeatMode = RepeatMode.Restart
		),
		label = "coming_soon_rotation"
	)

	Dialog(
		onDismissRequest = onDismiss,
		properties = DialogProperties(
			dismissOnBackPress = true,
			dismissOnClickOutside = true,
			usePlatformDefaultWidth = false,
			decorFitsSystemWindows = false
		)
	) {
		BoxWithConstraints(
			modifier = modifier
				.fillMaxSize()
				.padding(horizontal = 24.dp, vertical = 24.dp),
			contentAlignment = Alignment.Center
		) {
			val dialogMaxWidth = if (maxWidth < 600.dp) 420.dp else 460.dp

			AnimatedVisibility(
				visible = showDialog,
				enter = scaleIn(
					animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
					initialScale = 0.88f
				) + fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
				exit = scaleOut(
					animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
					targetScale = 0.92f
				) + fadeOut(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec())
			) {
				Surface(
					modifier = Modifier
						.widthIn(max = dialogMaxWidth)
						.fillMaxWidth()
						.wrapContentHeight(),
					shape = RoundedCornerShape(32.dp),
					color = MaterialTheme.colorScheme.surfaceContainerHigh,
					tonalElevation = 6.dp
				) {
					Column(
						modifier = Modifier
							.fillMaxWidth()
							.padding(24.dp)
							.verticalScroll(rememberScrollState()),
						horizontalAlignment = Alignment.CenterHorizontally
					) {
						Box(
							modifier = Modifier.size(124.dp),
							contentAlignment = Alignment.Center
						) {
							Box(
								modifier = Modifier
									.fillMaxSize()
									.graphicsLayer { rotationZ = heroRotation }
									.background(heroBackdropColor, heroBackdropShape)
							)
							MissionIllustration(
								mission = mission,
								isSelected = true,
								modifier = Modifier.size(width = 112.dp, height = 96.dp)
							)
						}

						Spacer(modifier = Modifier.height(16.dp))

						Surface(
							shape = CircleShape,
							color = MaterialTheme.colorScheme.tertiaryContainer,
							contentColor = MaterialTheme.colorScheme.onTertiaryContainer
						) {
							Text(
								text = stringResource(R.string.mission_coming_soon_badge),
								style = MaterialTheme.typography.labelMediumEmphasized,
								modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
							)
						}

						Spacer(modifier = Modifier.height(12.dp))

						Text(
							text = stringResource(
								R.string.mission_coming_soon_title,
								stringResource(mission.titleRes)
							),
							style = MaterialTheme.typography.headlineSmallEmphasized,
							color = MaterialTheme.colorScheme.onSurface,
							textAlign = TextAlign.Center
						)

						Spacer(modifier = Modifier.height(8.dp))

						Text(
							text = stringResource(R.string.mission_coming_soon_body),
							style = MaterialTheme.typography.bodyMedium,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
							textAlign = TextAlign.Center
						)

						Spacer(modifier = Modifier.height(24.dp))

						val ctaHeight = ButtonDefaults.MediumContainerHeight
						Button(
							onClick = onConfirm,
							shapes = ButtonDefaults.shapesFor(ctaHeight),
							contentPadding = ButtonDefaults.contentPaddingFor(ctaHeight),
							modifier = Modifier
								.fillMaxWidth()
								.height(ctaHeight)
						) {
							Text(
								text = stringResource(R.string.mission_coming_soon_cta),
								style = MaterialTheme.typography.titleMediumEmphasized
							)
						}
					}
				}
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun AlarmMissionListScreenLightPreview() {
	ExpressiveAlarmTheme(darkTheme = false) {
		AlarmMissionListScreen(
			onBack = {},
			onTrackEvent = { _, _ -> }
		)
	}
}

@Preview(showBackground = true)
@Composable
private fun AlarmMissionListScreenDarkPreview() {
	ExpressiveAlarmTheme(darkTheme = true) {
		AlarmMissionListScreen(
			onBack = {},
			onTrackEvent = { _, _ -> }
		)
	}
}
