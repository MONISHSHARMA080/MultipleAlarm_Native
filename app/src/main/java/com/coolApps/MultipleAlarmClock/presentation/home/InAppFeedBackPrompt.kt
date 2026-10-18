@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.coolApps.MultipleAlarmClock.presentation.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.presentation.ExpressiveAlarmTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun InAppFeedbackPrompt(
	state: InAppFeedbackPromptUiState,
	onRatingSelected: (Int) -> Unit,
	onFeedbackSubmitted: (String) -> Unit,
	onDismiss: () -> Unit,
	modifier: Modifier = Modifier,
) {
	var feedback by rememberSaveable { mutableStateOf("") }
	var showDialog by remember { mutableStateOf(false) }

	LaunchedEffect(Unit) { showDialog = true }

	Dialog(
		onDismissRequest = onDismiss,
		properties = DialogProperties(
			dismissOnBackPress = true,
			dismissOnClickOutside = true,
			usePlatformDefaultWidth = false,
		),
	) {
		BoxWithConstraints(
			modifier = modifier
				.fillMaxSize()
				.imePadding()
				.padding(horizontal = 20.dp, vertical = 24.dp),
			contentAlignment = Alignment.Center,
		) {
			val isCompactWidth = maxWidth < 360.dp
			val cardPadding = if (isCompactWidth) 16.dp else 24.dp
			val dialogMaxWidth = if (maxWidth < 600.dp) 440.dp else 480.dp

			AnimatedVisibility(
				visible = showDialog,
				enter = scaleIn(
					animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
					initialScale = 0.88f,
				) + fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
				exit = scaleOut(
					animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
					targetScale = 0.92f,
				) + fadeOut(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
			) {
				Surface(
					modifier = Modifier
						.widthIn(max = dialogMaxWidth)
						.fillMaxWidth()
						.wrapContentHeight(),
					shape = RoundedCornerShape(32.dp),
					color = MaterialTheme.colorScheme.surfaceContainerHigh,
					shadowElevation = 8.dp,
					tonalElevation = 6.dp,
				) {
					Column(
						modifier = Modifier
							.fillMaxWidth()
							.padding(cardPadding)
							.verticalScroll(rememberScrollState()),
						horizontalAlignment = Alignment.CenterHorizontally,
					) {
//						PromptHeroIcon(isCompactWidth = isCompactWidth)

//						Spacer(Modifier.height(16.dp))

						val spatialOffsetSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
						val spatialSizeSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
						val spatialFloatSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
						val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

						AnimatedContent(
							targetState = state.step,
							transitionSpec = {
								if (targetState == InAppFeedbackPromptStep.Review) {
									(fadeIn(animationSpec = effectsSpec) +
										scaleIn(animationSpec = spatialFloatSpec, initialScale = 0.94f) +
										slideInVertically(animationSpec = spatialOffsetSpec, initialOffsetY = { it / 6 })
									).togetherWith(
										fadeOut(animationSpec = effectsSpec) +
											scaleOut(animationSpec = spatialFloatSpec, targetScale = 0.96f) +
											slideOutVertically(animationSpec = spatialOffsetSpec, targetOffsetY = { -it / 6 })
									).using(SizeTransform(clip = false) { _, _ -> spatialSizeSpec })
								} else {
									(fadeIn(animationSpec = effectsSpec) +
										scaleIn(animationSpec = spatialFloatSpec, initialScale = 0.96f) +
										slideInVertically(animationSpec = spatialOffsetSpec, initialOffsetY = { -it / 6 })
									).togetherWith(
										fadeOut(animationSpec = effectsSpec) +
											scaleOut(animationSpec = spatialFloatSpec, targetScale = 0.94f) +
											slideOutVertically(animationSpec = spatialOffsetSpec, targetOffsetY = { it / 6 })
									).using(SizeTransform(clip = false) { _, _ -> spatialSizeSpec })
								}
							},
							label = "feedbackPromptStep",
							modifier = Modifier.fillMaxWidth(),
						) { step ->
							when (step) {
								InAppFeedbackPromptStep.Rating -> RatingStep(
									isCompactWidth = isCompactWidth,
									onRatingSelected = onRatingSelected,
									onDismiss = onDismiss,
								)
								InAppFeedbackPromptStep.Review -> ReviewStep(
									rating = state.rating,
									feedback = feedback,
									onFeedbackChange = { feedback = it },
									onSubmit = { onFeedbackSubmitted(feedback.trim()) },
									onDismiss = onDismiss,
									isCompactWidth = isCompactWidth,
								)
							}
						}
					}
				}
			}
		}
	}
}

@Composable
private fun PromptHeroIcon(
	isCompactWidth: Boolean,
	modifier: Modifier = Modifier,
) {
	val badgeSize = if (isCompactWidth) 52.dp else 60.dp
	val badgeCorner = if (isCompactWidth) 16.dp else 20.dp
	val iconSize = if (isCompactWidth) 26.dp else 30.dp

	Box(
		modifier = modifier
			.size(badgeSize)
			.clip(RoundedCornerShape(badgeCorner))
			.background(
				Brush.radialGradient(
					listOf(
						MaterialTheme.colorScheme.primaryContainer,
						MaterialTheme.colorScheme.tertiaryContainer,
					),
				),
			),
		contentAlignment = Alignment.Center,
	) {
		Icon(
			imageVector = Icons.Filled.Star,
			contentDescription = null,
			tint = MaterialTheme.colorScheme.onPrimaryContainer,
			modifier = Modifier.size(iconSize),
		)
	}
}

@Composable
private fun RatingStep(
	isCompactWidth: Boolean,
	onRatingSelected: (Int) -> Unit,
	onDismiss: () -> Unit,
	modifier: Modifier = Modifier,
) {
	val starSize = if (isCompactWidth) 40.dp else 48.dp
	val starIconSize = if (isCompactWidth) 26.dp else 32.dp

	Column(
		modifier = modifier.fillMaxWidth(),
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		Text(
			text = stringResource(R.string.in_app_review_rating_title),
			style = if (isCompactWidth) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
			fontWeight = FontWeight.ExtraBold,
			color = MaterialTheme.colorScheme.onSurface,
			textAlign = TextAlign.Center,
		)

		Spacer(Modifier.height(8.dp))

		Text(
			text = stringResource(R.string.in_app_review_rating_description),
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			textAlign = TextAlign.Center,
		)

		Spacer(Modifier.height(24.dp))

		Surface(
			shape = RoundedCornerShape(24.dp),
			color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
			modifier = Modifier.padding(horizontal = 4.dp),
		) {
			Row(
				modifier = Modifier.padding(
					horizontal = if (isCompactWidth) 6.dp else 10.dp,
					vertical = 6.dp,
				),
				horizontalArrangement = Arrangement.spacedBy(if (isCompactWidth) 2.dp else 4.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				(1..5).forEach { rating ->
					val interactionSource = remember { MutableInteractionSource() }
					val isPressed by interactionSource.collectIsPressedAsState()
					val scale by animateFloatAsState(
						targetValue = if (isPressed) 0.82f else 1f,
						animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
						label = "ratingStarScale",
					)
					val cornerRadius by animateDpAsState(
						targetValue = if (isPressed) 8.dp else 16.dp,
						animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
						label = "ratingStarCorner",
					)

					Surface(
						onClick = { onRatingSelected(rating) },
						interactionSource = interactionSource,
						shape = RoundedCornerShape(cornerRadius),
						color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(
							alpha = if (isPressed) 0.8f else 0f,
						),
						modifier = Modifier
							.size(starSize)
							.graphicsLayer {
								scaleX = scale
								scaleY = scale
							},
					) {
						Box(contentAlignment = Alignment.Center) {
							Icon(
								imageVector = Icons.Outlined.StarBorder,
								contentDescription = stringResource(R.string.in_app_review_star_description, rating),
								modifier = Modifier.size(starIconSize),
								tint = MaterialTheme.colorScheme.primary,
							)
						}
					}
				}
			}
		}

		Spacer(Modifier.height(24.dp))

		val dismissInteractionSource = remember { MutableInteractionSource() }
		val isDismissPressed by dismissInteractionSource.collectIsPressedAsState()
		val dismissCorner by animateDpAsState(
			targetValue = if (isDismissPressed) 12.dp else 24.dp,
			animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
			label = "dismissCorner",
		)
		val dismissScale by animateFloatAsState(
			targetValue = if (isDismissPressed) 0.95f else 1f,
			animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
			label = "dismissScale",
		)

		FilledTonalButton(
			onClick = onDismiss,
			interactionSource = dismissInteractionSource,
			shape = RoundedCornerShape(dismissCorner),
			modifier = Modifier
				.height(44.dp)
				.graphicsLayer {
					scaleX = dismissScale
					scaleY = dismissScale
				},
		) {
			Text(
				text = stringResource(R.string.in_app_review_not_now),
				style = MaterialTheme.typography.labelLarge,
				fontWeight = FontWeight.SemiBold,
			)
		}
	}
}

@Composable
private fun ReviewStep(
	rating: Int?,
	feedback: String,
	onFeedbackChange: (String) -> Unit,
	onSubmit: () -> Unit,
	onDismiss: () -> Unit,
	isCompactWidth: Boolean,
	modifier: Modifier = Modifier,
) {
	val maxChars = 500
	val focusRequester = remember { FocusRequester() }

	LaunchedEffect(Unit) {
		delay(200.milliseconds)
		focusRequester.requestFocus()
	}

	Column(
		modifier = modifier.fillMaxWidth(),
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		Text(
			text = stringResource(R.string.in_app_review_feedback_title),
			style = if (isCompactWidth) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
			fontWeight = FontWeight.ExtraBold,
			color = MaterialTheme.colorScheme.onSurface,
			textAlign = TextAlign.Center,
		)

		Spacer(Modifier.height(8.dp))

		Text(
			text = stringResource(R.string.in_app_review_feedback_description),
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			textAlign = TextAlign.Center,
		)

		if (rating != null && rating > 0) {
			Spacer(Modifier.height(14.dp))
			Surface(
				shape = RoundedCornerShape(16.dp),
				color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
			) {
				Row(
					modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
					horizontalArrangement = Arrangement.spacedBy(4.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					(1..5).forEach { starIndex ->
						val isFilled = starIndex <= rating
						Icon(
							imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.StarBorder,
							contentDescription = null,
							tint = if (isFilled) {
								MaterialTheme.colorScheme.primary
							} else {
								MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
							},
							modifier = Modifier.size(18.dp),
						)
					}
				}
			}
		}

		Spacer(Modifier.height(16.dp))

		OutlinedTextField(
			value = feedback,
			onValueChange = { if (it.length <= maxChars) onFeedbackChange(it) },
			modifier = Modifier
				.fillMaxWidth()
				.height(132.dp)
				.focusRequester(focusRequester),
			placeholder = {
				Text(
					text = stringResource(R.string.in_app_review_feedback_placeholder),
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
				)
			},
			textStyle = MaterialTheme.typography.bodyMedium.copy(
				color = MaterialTheme.colorScheme.onSurface,
			),
			shape = RoundedCornerShape(18.dp),
			colors = OutlinedTextFieldDefaults.colors(
				focusedBorderColor = MaterialTheme.colorScheme.primary,
				unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
				focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
				unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
			),
			keyboardOptions = KeyboardOptions(
				capitalization = KeyboardCapitalization.Sentences,
				imeAction = ImeAction.Default,
			),
			minLines = 3,
			maxLines = 5,
		)

		Spacer(Modifier.height(20.dp))

		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.spacedBy(if (isCompactWidth) 8.dp else 12.dp, Alignment.End),
			verticalAlignment = Alignment.CenterVertically,
		) {
			val dismissInteractionSource = remember { MutableInteractionSource() }
			val isDismissPressed by dismissInteractionSource.collectIsPressedAsState()
			val dismissCorner by animateDpAsState(
				targetValue = if (isDismissPressed) 10.dp else 20.dp,
				animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
				label = "reviewDismissCorner",
			)
			val dismissScale by animateFloatAsState(
				targetValue = if (isDismissPressed) 0.95f else 1f,
				animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
				label = "reviewDismissScale",
			)

			FilledTonalButton(
				onClick = onDismiss,
				interactionSource = dismissInteractionSource,
				shape = RoundedCornerShape(dismissCorner),
				modifier = Modifier
					.height(44.dp)
					.graphicsLayer {
						scaleX = dismissScale
						scaleY = dismissScale
					},
			) {
				Text(
					text = stringResource(R.string.in_app_review_not_now),
					style = MaterialTheme.typography.labelLarge,
					fontWeight = FontWeight.SemiBold,
				)
			}

			val submitInteractionSource = remember { MutableInteractionSource() }
			val isSubmitPressed by submitInteractionSource.collectIsPressedAsState()
			val submitCorner by animateDpAsState(
				targetValue = if (isSubmitPressed) 10.dp else 24.dp,
				animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
				label = "submitCorner",
			)
			val submitScale by animateFloatAsState(
				targetValue = if (isSubmitPressed) 0.96f else 1f,
				animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
				label = "submitScale",
			)

			Button(
				onClick = onSubmit,
				enabled = feedback.isNotBlank(),
				interactionSource = submitInteractionSource,
				shape = RoundedCornerShape(submitCorner),
				colors = ButtonDefaults.buttonColors(
					containerColor = MaterialTheme.colorScheme.primary,
					contentColor = MaterialTheme.colorScheme.onPrimary,
					disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
					disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
				),
				modifier = Modifier
					.height(44.dp)
					.graphicsLayer {
						scaleX = submitScale
						scaleY = submitScale
					},
			) {
				Icon(
					imageVector = Icons.AutoMirrored.Outlined.Send,
					contentDescription = null,
					modifier = Modifier.size(18.dp),
				)
				Spacer(Modifier.size(8.dp))
				Text(
					text = stringResource(R.string.in_app_review_submit),
					style = MaterialTheme.typography.labelLarge,
					fontWeight = FontWeight.Bold,
				)
			}
		}
	}
}

private const val PreviewBackground = 0xFFF9F9FF

@Preview(name = "Compact phone", widthDp = 320, heightDp = 640, showBackground = true, backgroundColor = PreviewBackground)
@Preview(name = "Phone", device = Devices.PHONE, showBackground = true, backgroundColor = PreviewBackground)
@Preview(name = "Foldable", device = Devices.FOLDABLE, showBackground = true, backgroundColor = PreviewBackground)
@Preview(name = "Tablet", device = Devices.TABLET, showBackground = true, backgroundColor = PreviewBackground)
@Preview(name = "Desktop", device = Devices.DESKTOP, showBackground = true, backgroundColor = PreviewBackground)
@Composable
private fun InAppFeedbackRatingPromptPreview() {
	ExpressiveAlarmTheme {
		InAppFeedbackPrompt(
			state = InAppFeedbackPromptUiState(
				isVisible = true,
				step = InAppFeedbackPromptStep.Rating,
			),
			onRatingSelected = {},
			onFeedbackSubmitted = {},
			onDismiss = {},
		)
	}
}

@Preview(name = "Compact phone", widthDp = 320, heightDp = 640, showBackground = true, backgroundColor = PreviewBackground)
@Preview(name = "Phone", device = Devices.PHONE, showBackground = true, backgroundColor = PreviewBackground)
@Preview(name = "Foldable", device = Devices.FOLDABLE, showBackground = true, backgroundColor = PreviewBackground)
@Preview(name = "Tablet", device = Devices.TABLET, showBackground = true, backgroundColor = PreviewBackground)
@Preview(name = "Desktop", device = Devices.DESKTOP, showBackground = true, backgroundColor = PreviewBackground)
@Composable
private fun InAppFeedbackTextPromptPreview() {
	ExpressiveAlarmTheme {
		InAppFeedbackPrompt(
			state = InAppFeedbackPromptUiState(
				isVisible = true,
				rating = 5,
				step = InAppFeedbackPromptStep.Review,
			),
			onRatingSelected = {},
			onFeedbackSubmitted = {},
			onDismiss = {},
		)
	}
}
