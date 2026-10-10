package com.coolApps.MultipleAlarmClock.presentation.onboarding

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OnboardingScreen(onNavigateToPaywall: (Boolean) -> Unit = {}) {
	val viewModel: OnboardingViewModel = hiltViewModel()
	val remoteDocument by viewModel.remoteDocument.collectAsStateWithLifecycle()
	val remoteDocumentError by viewModel.remoteDocumentError.collectAsStateWithLifecycle()

	LaunchedEffect(Unit) {
		viewModel.fetchRemoteDocument("https://d138545829.cloudfront.net/ui/onboarding.rc")
	}

	Scaffold { innerPadding ->
		Box(
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding)
		) {
			RemoteContent(
				document = remoteDocument,
				errorMessage = remoteDocumentError
			)
		}
	}
}
