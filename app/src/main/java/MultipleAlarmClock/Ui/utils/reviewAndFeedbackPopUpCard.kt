package com.example.MultipleAlarmClock.Ui.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds


@Composable
fun FeedbackCardContent(
	feedbackText: String,
	onFeedbackChange: (String) -> Unit,
	onSubmit: () -> Unit,
	onDismiss: () -> Unit,
	modifier: Modifier = Modifier,
	autoFocus: Boolean = false,
	showMaybeLaterButton: Boolean
) {
	val maxChars = 500
	val isSubmitEnabled = feedbackText.isNotBlank()
	val focusRequester = remember { FocusRequester() }

	if (autoFocus) {
		LaunchedEffect(Unit) {
			delay(200.milliseconds)
			focusRequester.requestFocus()
		}
	}

	Card(
		modifier = modifier.fillMaxWidth(),
		shape = RoundedCornerShape(28.dp),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
		),
		elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
	) {
		Column(
			modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
			verticalArrangement = Arrangement.spacedBy(0.dp),
			horizontalAlignment = Alignment.Start,
		) {
			// ── Icon badge ──
			Box(
				modifier = Modifier
					.size(56.dp)
					.clip(CircleShape)
					.background(
						brush = Brush.radialGradient(
							colors = listOf(
								MaterialTheme.colorScheme.primaryContainer,
								MaterialTheme.colorScheme.secondaryContainer,
							),
						),
					),
				contentAlignment = Alignment.Center,
			) {
				Icon(
					imageVector = Icons.Filled.RateReview,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.onPrimaryContainer,
					modifier = Modifier.size(28.dp),
				)
			}

			Spacer(modifier = Modifier.height(20.dp))

			// ── Title ──
			Text(
				text = "Feedback please",
				style = MaterialTheme.typography.headlineSmall,
				color = MaterialTheme.colorScheme.onSurface,
			)

			Spacer(modifier = Modifier.height(8.dp))

			// ── Description ──
			Text(
				text = "Tell us what you love and what needs work. From design tweaks to feature requests and bug fixes, help us improve the app for you and everyone",
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)

			Spacer(modifier = Modifier.height(24.dp))

			// ── Text field ──
			OutlinedTextField(
				value = feedbackText,
				onValueChange = { if (it.length <= maxChars) onFeedbackChange(it) },
				modifier = Modifier
					.fillMaxWidth()
					.height(140.dp)
					.focusRequester(focusRequester),
				placeholder = {
					Text(
						text = "Tell us what you think…",
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
					)
				},
				textStyle = MaterialTheme.typography.bodyMedium.copy(
					color = MaterialTheme.colorScheme.onSurface,
				),
				shape = RoundedCornerShape(16.dp),
				colors = OutlinedTextFieldDefaults.colors(
					focusedBorderColor = MaterialTheme.colorScheme.primary,
					unfocusedBorderColor = MaterialTheme.colorScheme.outline,
					focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
					unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
				),
				keyboardOptions = KeyboardOptions(
					capitalization = KeyboardCapitalization.Sentences,
					imeAction = ImeAction.Default,
				),
				maxLines = 6,
				singleLine = false,
				supportingText = {
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.End,
					) {
						Text(
							text = "${feedbackText.length} / $maxChars",
							style = MaterialTheme.typography.bodySmall,
							color = if (feedbackText.length >= maxChars)
								MaterialTheme.colorScheme.error
							else
								MaterialTheme.colorScheme.onSurfaceVariant,
						)
					}
				},
			)

			Spacer(modifier = Modifier.height(24.dp))

			// ── Actions ──
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
				verticalAlignment = Alignment.CenterVertically,
			) {
				if (showMaybeLaterButton){
					TextButton(
						onClick = onDismiss,
						shape = RoundedCornerShape(50),
					) {
						Text(
							text = "Maybe later",
							style = MaterialTheme.typography.labelLarge,
							color = MaterialTheme.colorScheme.onSurface,
						)
					}
				}

				Button(
					onClick = onSubmit,
					enabled = isSubmitEnabled,
					shape = RoundedCornerShape(50),
					colors = ButtonDefaults.buttonColors(
						containerColor = MaterialTheme.colorScheme.primary,
						contentColor = MaterialTheme.colorScheme.onPrimary,
						disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
						disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
					),
				) {
					Text(text = "Submit", style = MaterialTheme.typography.labelLarge)
				}
			}
		}
	}
}



@Composable
fun FeedbackPopUpCard(
	onReviewGiven: (String) -> Unit,
	onDismiss: () -> Unit = {},
) {
	var feedbackText by rememberSaveable { mutableStateOf("") }
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
		AnimatedVisibility(
			visible = showDialog,
			enter = scaleIn(
				animationSpec = spring(
					dampingRatio = Spring.DampingRatioMediumBouncy,
					stiffness = Spring.StiffnessMediumLow,
				),
				initialScale = 0.85f,
			) + fadeIn(animationSpec = tween(durationMillis = 200)),
			exit = scaleOut(targetScale = 0.92f) + fadeOut(animationSpec = tween(150)),
		) {
			// Call our extracted UI here
			FeedbackCardContent(
				feedbackText = feedbackText,
				onFeedbackChange = { feedbackText = it },
				onSubmit = { onReviewGiven(feedbackText.trim()) },
				onDismiss = onDismiss,
				modifier = Modifier.padding(horizontal = 24.dp).wrapContentHeight(),
				autoFocus = true, // Enable auto-focus for the dialog
				showMaybeLaterButton = true
			)
		}
	}
}