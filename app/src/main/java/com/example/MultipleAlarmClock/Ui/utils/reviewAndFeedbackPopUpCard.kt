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

/**
 * Material 3 Expressive feedback pop-up card.
 *
 * Follows the M3 Expressive spec:
 *  - Color roles  : primaryContainer / onPrimaryContainer for the icon badge,
 *                   surfaceContainerHigh for the card surface,
 *                   primary for the CTA button.
 *  - Shape        : Extra-large corner radius (28 dp) on the card — M3 Expressive
 *                   favours rounder, friendlier containers.  The icon badge uses a
 *                   full-circle.  The CTA button uses the M3 "full" pill shape.
 *  - Typography   : headlineSmall for the title, bodyMedium for the description,
 *                   bodySmall for the character counter.
 *  - Motion       : Dialog entrance uses a spring-based scaleIn + fadeIn (M3
 *                   Expressive motion emphasises overshoot / springiness).
 *
 * @param onReviewGiven Called with the trimmed feedback string when the user
 *                      taps **Submit**.  The dialog closes automatically.
 * @param onDismiss     Called when the user taps **Maybe later** or taps
 *                      outside the card.  Defaults to a no-op so callers that
 *                      only care about the happy path don't have to supply it.
 */
@Composable fun FeedbackPopUpCard(
	onReviewGiven: (String) -> Unit,
	onDismiss: () -> Unit = {},
) {
	var feedbackText by rememberSaveable { mutableStateOf("") }
	val maxChars = 500
	val isSubmitEnabled = feedbackText.isNotBlank()
	var showDialog by remember { mutableStateOf(false) }
	val focusRequester = remember { FocusRequester() }
	LaunchedEffect(Unit) {showDialog = true; delay(200.milliseconds); focusRequester.requestFocus() }

	Dialog(
		onDismissRequest = onDismiss,
		properties = DialogProperties(
			dismissOnBackPress = true,
			dismissOnClickOutside = true,
			usePlatformDefaultWidth = false,   // we control width ourselves
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
			Card(
				modifier = Modifier
					.padding(horizontal = 24.dp)
					.fillMaxWidth()
					.wrapContentHeight(),
				shape = RoundedCornerShape(28.dp),
				colors = CardDefaults.cardColors(
					containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
				),
				elevation = CardDefaults.cardElevation(
					defaultElevation = 6.dp,
				),
			) {
				Column(
					modifier = Modifier
						.padding(horizontal = 24.dp, vertical = 28.dp),
					verticalArrangement = Arrangement.spacedBy(0.dp),
					horizontalAlignment = Alignment.Start,
				) {

					// ── Icon badge ──────────────────────────────────────────
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

					// ── Title ────────────────────────────────────────────────
					Text(
						text = "Feedback please",
						style = MaterialTheme.typography.headlineSmall,
						color = MaterialTheme.colorScheme.onSurface,
					)

					Spacer(modifier = Modifier.height(8.dp))

					// ── Description ──────────────────────────────────────────
					Text(
						text = "Tell us what you love and what needs a work. From design tweaks to feature requests and bug fixes, help us improve the app for you and everyone",
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)

					Spacer(modifier = Modifier.height(24.dp))

					// ── Text field ───────────────────────────────────────────
					OutlinedTextField(
						value = feedbackText,
						onValueChange = { if (it.length <= maxChars) feedbackText = it },
						modifier = Modifier
							.fillMaxWidth()
							.height(140.dp)
							.focusRequester(focusRequester)
						,
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
						shape = RoundedCornerShape(16.dp),   // M3 Expressive: rounded inputs
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
							// Character counter — right-aligned
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

					// ── Actions ──────────────────────────────────────────────
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
						verticalAlignment = Alignment.CenterVertically,
					) {
						// Dismiss — text button (low emphasis)
						TextButton(
							onClick = onDismiss,
							shape = RoundedCornerShape(50),  // pill
						) {
							Text(
								text = "Maybe later",
								style = MaterialTheme.typography.labelLarge,
								color = MaterialTheme.colorScheme.onSurface,
							)
						}

						Button(
							onClick = {
								if (isSubmitEnabled) {
									onReviewGiven(feedbackText.trim())
								}
							},
							enabled = isSubmitEnabled,
							shape = RoundedCornerShape(50),  // M3 Expressive: pill CTA
							colors = ButtonDefaults.buttonColors(
								containerColor = MaterialTheme.colorScheme.primary,
								contentColor = MaterialTheme.colorScheme.onPrimary,
								disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
								disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
							),
						) {
							Text(
								text = "Submit",
								style = MaterialTheme.typography.labelLarge,
							)
						}
					}
				}
			}
		}
	}
}