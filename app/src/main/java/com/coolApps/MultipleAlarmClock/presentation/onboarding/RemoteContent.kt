package com.coolApps.MultipleAlarmClock.presentation.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp

@Composable
fun RemoteContent(
    document: CoreDocument?,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            errorMessage != null -> {
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Welcome to Multiple Alarm Clock",
                        style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Let's set up your first alarm.",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            document != null -> {
                // Render the parsed CoreDocument natively
                RemoteDocumentPlayer(
                    document = document,
                    documentWidth = 400,
                    documentHeight = 800,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp) // Adaptive constraints instead of fixed size
                )
            }
            else -> {
                CircularProgressIndicator()
            }
        }
    }
}
