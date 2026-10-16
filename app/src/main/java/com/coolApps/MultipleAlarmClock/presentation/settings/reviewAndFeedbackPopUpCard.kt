package com.coolApps.MultipleAlarmClock.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coolApps.MultipleAlarmClock.R
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
                        delay(230.milliseconds)
                        focusRequester.requestFocus()
                }
        }
        Card(
                modifier = modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
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
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                                // ── Hero Squircle Icon Badge (Optical Roundness: 32dp - 14dp = 18dp) ──
                                Box(
                                        modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(18.dp))
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

                                // ── Title ──
                                Text(
                                        text = stringResource(R.string.feedback_card),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                )
                        }


                        Spacer(modifier = Modifier.height(10.dp))

                        // ── Description ──
                        Text(
                                text = stringResource(R.string.feedback_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // ── Text field (Optical Roundness: 18.dp inside 32.dp card) ──
                        OutlinedTextField(
                                value = feedbackText,
                                onValueChange = { if (it.length <= maxChars) onFeedbackChange(it) },
                                modifier = Modifier
                                        .fillMaxWidth()
                                        .height(144.dp)
                                        .focusRequester(focusRequester),
                                placeholder = {
                                        Text(
                                                text = stringResource(R.string.feedback_placeholder),
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
                                maxLines = 6,
                                singleLine = false,
                                supportingText = {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                        ) {
                                                Surface(
                                                        shape = RoundedCornerShape(50),
                                                        color = if (feedbackText.length >= maxChars)
                                                                MaterialTheme.colorScheme.errorContainer
                                                        else
                                                                MaterialTheme.colorScheme.surfaceContainerHighest,
                                                ) {
                                                        Text(
                                                                text = stringResource(R.string.feedback_char_count, feedbackText.length, maxChars),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = if (feedbackText.length >= maxChars)
                                                                        MaterialTheme.colorScheme.onErrorContainer
                                                                else
                                                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                        )
                                                }
                                        }
                                },
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        // ── Actions ──
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                                verticalAlignment = Alignment.CenterVertically,
                        ) {
                                if (showMaybeLaterButton) {
                                        val dismissInteraction = remember { MutableInteractionSource() }
                                        val isDismissPressed by dismissInteraction.collectIsPressedAsState()
                                        val dismissCorner by animateDpAsState(
                                                targetValue = if (isDismissPressed) 12.dp else 20.dp,
                                                animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                                                label = "dismissCorner"
                                        )
                                        val dismissScale by animateFloatAsState(
                                                targetValue = if (isDismissPressed) 0.96f else 1f,
                                                animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                                                label = "dismissScale"
                                        )

                                        FilledTonalButton(
                                                onClick = onDismiss,
                                                interactionSource = dismissInteraction,
                                                shape = RoundedCornerShape(dismissCorner),
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                        contentColor = MaterialTheme.colorScheme.onSurface
                                                ),
                                                modifier = Modifier.graphicsLayer {
                                                        scaleX = dismissScale
                                                        scaleY = dismissScale
                                                }
                                        ) {
                                                Text(
                                                        text = stringResource(R.string.feedback_btn_maybe_later),
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold,
                                                )
                                        }
                                }

                                val submitInteraction = remember { MutableInteractionSource() }
                                val isSubmitPressed by submitInteraction.collectIsPressedAsState()
                                val submitCorner by animateDpAsState(
                                        targetValue = if (isSubmitPressed) 12.dp else 24.dp,
                                        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                                        label = "submitCorner"
                                )
                                val submitScale by animateFloatAsState(
                                        targetValue = if (isSubmitPressed) 0.96f else 1f,
                                        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                                        label = "submitScale"
                                )

                                Button(
                                        onClick = onSubmit,
                                        enabled = isSubmitEnabled,
                                        interactionSource = submitInteraction,
                                        shape = RoundedCornerShape(submitCorner),
                                        colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
                                                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        ),
                                        modifier = Modifier.graphicsLayer {
                                                scaleX = submitScale
                                                scaleY = submitScale
                                        }
                                ) {
                                        Text(
                                                text = stringResource(R.string.feedback_btn_submit),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold
                                        )
                                }
                        }
                }
        }
}



@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
                Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                ) {
                        AnimatedVisibility(
                                visible = showDialog,
                                enter = scaleIn(
                                        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                                        initialScale = 0.85f,
                                ) + fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
                                exit = scaleOut(
                                        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                                        targetScale = 0.92f
                                ) + fadeOut(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
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
}