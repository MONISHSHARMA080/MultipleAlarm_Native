@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.coolApps.MultipleAlarmClock.presentation.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.presentation.ExpressiveAlarmTheme

@Composable
fun InAppFeedbackPrompt(
		state: InAppFeedbackPromptUiState,
		onRatingSelected: (Int) -> Unit,
	onFeedbackSubmitted: (String) -> Unit,
		onDismiss: () -> Unit,
		modifier: Modifier = Modifier,
) {
	var feedback by rememberSaveable { mutableStateOf("") }

	Dialog(
		onDismissRequest = onDismiss,
		properties = DialogProperties(usePlatformDefaultWidth = false),
	) {
		BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
			val isCompactWidth = maxWidth < 360.dp
			val horizontalPadding = if (isCompactWidth) 12.dp else 24.dp
			val contentPadding = if (isCompactWidth) 16.dp else 24.dp

			Box(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = horizontalPadding)
					.imePadding(),
				contentAlignment = Alignment.Center,
			) {
				Surface(
					modifier = Modifier
						.widthIn(max = 480.dp)
						.fillMaxWidth(),
					shape = RoundedCornerShape(28.dp),
					color = MaterialTheme.colorScheme.surfaceContainerHigh,
					shadowElevation = 8.dp,
				) {
					Column(
						modifier = Modifier
							.padding(contentPadding)
							.verticalScroll(rememberScrollState()),
						horizontalAlignment = Alignment.CenterHorizontally,
					) {
						AnimatedContent(
							targetState = state.step,
							label = "feedbackPromptStep",
						) { step ->
							when (step) {
								InAppFeedbackPromptStep.Rating -> RatingStep(
									onRatingSelected = onRatingSelected,
									onDismiss = onDismiss,
								)
								InAppFeedbackPromptStep.Review -> ReviewStep(
									feedback = feedback,
									onFeedbackChange = { feedback = it },
									onSubmit = { onFeedbackSubmitted(feedback.trim()) },
									onDismiss = onDismiss,
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
private fun RatingStep(onRatingSelected: (Int) -> Unit, onDismiss: () -> Unit) {
	ReviewPromptHeader(
		title = stringResource(R.string.in_app_review_rating_title),
		description = stringResource(R.string.in_app_review_rating_description),
	)
	Spacer(Modifier.height(24.dp))
	Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
		(1..5).forEach { rating ->
			val interactionSource = remember { MutableInteractionSource() }
			val isPressed by interactionSource.collectIsPressedAsState()
			val scale by animateFloatAsState(
				targetValue = if (isPressed) 0.84f else 1f,
				animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
				label = "ratingStarScale",
			)
			IconButton(
				onClick = { onRatingSelected(rating) },
				interactionSource = interactionSource,
				modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
			) {
				Icon(
					imageVector = Icons.Outlined.StarBorder,
					contentDescription = stringResource(R.string.in_app_review_star_description, rating),
					modifier = Modifier.size(32.dp),
					tint = MaterialTheme.colorScheme.primary,
				)
			}
		}
	}
	Spacer(Modifier.height(16.dp))
	FilledTonalButton(onClick = onDismiss) {
		Text(stringResource(R.string.in_app_review_not_now))
	}
}

@Composable
private fun ReviewStep(
	feedback: String,
	onFeedbackChange: (String) -> Unit,
	onSubmit: () -> Unit,
	onDismiss: () -> Unit,
) {
	ReviewPromptHeader(
		title = stringResource(R.string.in_app_review_feedback_title),
		description = stringResource(R.string.in_app_review_feedback_description),
	)
	Spacer(Modifier.height(20.dp))
	OutlinedTextField(
		value = feedback,
		onValueChange = onFeedbackChange,
		modifier = Modifier.fillMaxWidth(),
		placeholder = { Text(stringResource(R.string.in_app_review_feedback_placeholder)) },
		shape = RoundedCornerShape(18.dp),
		colors = OutlinedTextFieldDefaults.colors(
			focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
			unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
		),
		minLines = 4,
		maxLines = 6,
	)
	Spacer(Modifier.height(20.dp))
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
	) {
		FilledTonalButton(onClick = onDismiss) {
			Text(stringResource(R.string.in_app_review_not_now))
		}
		Button(
			onClick = onSubmit,
			enabled = feedback.isNotBlank(),
			shape = RoundedCornerShape(24.dp),
			colors = ButtonDefaults.buttonColors(
				containerColor = MaterialTheme.colorScheme.primary,
				contentColor = MaterialTheme.colorScheme.onPrimary,
			),
		) {
			Icon(
				imageVector = Icons.Filled.Star,
				contentDescription = null,
				modifier = Modifier.size(18.dp),
			)
			Text(stringResource(R.string.in_app_review_submit), modifier = Modifier.padding(start = 8.dp))
		}
	}
}

@Composable
private fun ReviewPromptHeader(title: String, description: String) {
	Box(
		modifier = Modifier
			.size(56.dp)
			.clip(RoundedCornerShape(18.dp))
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
			modifier = Modifier.size(28.dp),
		)
	}
	Spacer(Modifier.height(16.dp))
	Text(
		text = title,
		style = MaterialTheme.typography.headlineSmall,
		fontWeight = FontWeight.ExtraBold,
	)
	Spacer(Modifier.height(8.dp))
	Text(
		text = description,
		style = MaterialTheme.typography.bodyMedium,
		color = MaterialTheme.colorScheme.onSurfaceVariant,
	)
}

private const val PreviewBackground = 0xFFF9F9FF

@Preview(name = "Compact phone", widthDp = 320, heightDp = 640, showBackground = true, backgroundColor = PreviewBackground)
@Preview(name = "Phone", device = Devices.PHONE, showBackground = true, backgroundColor = PreviewBackground)
@Preview(name = "Tablet", device = Devices.TABLET, showBackground = true, backgroundColor = PreviewBackground)
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

@Preview(name = "Phone", device = Devices.PHONE, showBackground = true, backgroundColor = PreviewBackground)
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
