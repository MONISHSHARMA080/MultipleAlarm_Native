package com.example.MultipleAlarmClock.Ui.utils

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
	onNavigateBack: () -> Unit,
	onSubmitFeedback: (String) -> Unit
) {
	var feedbackText by rememberSaveable { mutableStateOf("") }

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("Settings") },
				navigationIcon = {
					IconButton(onClick = onNavigateBack) {
						Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
					}
				}
			)
		}
	) { paddingValues ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(paddingValues)
				.padding(16.dp)
				.verticalScroll(rememberScrollState())
		) {
			FeedbackCardContent(
				feedbackText = feedbackText,
				onFeedbackChange = { feedbackText = it },
				onSubmit = {
					onSubmitFeedback(feedbackText.trim())
					feedbackText = "" // Clear after submit
				},
				onDismiss = { feedbackText = "" }, // Let's use dismiss to clear it here
				modifier = Modifier.fillMaxWidth(),
				autoFocus = false, // We don't want the keyboard popping up as soon as they open settings
				showMaybeLaterButton = false
			)
		}
	}
}