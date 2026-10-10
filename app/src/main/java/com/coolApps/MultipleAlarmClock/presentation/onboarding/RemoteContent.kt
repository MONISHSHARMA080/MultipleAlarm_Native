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
                Text(text = "Error loading remote UI: $errorMessage")
            }
            document != null -> {
                // Render the parsed CoreDocument natively
                RemoteDocumentPlayer(
                    document = document,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp) // Adaptive constraints instead of fixed size
                )
            }
            else -> {
                CircularProgressIndicator()
            }
        }
    }
}
