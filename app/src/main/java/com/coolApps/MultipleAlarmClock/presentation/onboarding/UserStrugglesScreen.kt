package com.coolApps.MultipleAlarmClock.presentation.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import androidx.compose.ui.res.stringResource
import com.coolApps.MultipleAlarmClock.R

@Composable
fun UserStrugglesScreen(
    onButtonStateChange: (ButtonState) -> Unit,
    onStrugglesSelected: (Set<String>) -> Unit
) {
	onButtonStateChange(ButtonState.Enabled)
	val struggles = listOf(
        stringResource(id = R.string.onboarding_struggle_waking_up),
        stringResource(id = R.string.onboarding_struggle_being_on_time),
        stringResource(id = R.string.onboarding_struggle_heavy_sleeper),
        stringResource(id = R.string.onboarding_struggle_ignoring_alarms)
    )

    var selectedStruggles by remember { mutableStateOf(emptySet<String>()) }

    LaunchedEffect(selectedStruggles) {
//        onButtonStateChange(if (selectedStruggles.isNotEmpty()) ButtonState.Enabled else ButtonState.Disabled)
        onStrugglesSelected(selectedStruggles)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(id = R.string.onboarding_struggles_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.onboarding_struggles_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(struggles) { struggle ->
                val isSelected = selectedStruggles.contains(struggle)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    ),
                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    ),
                    onClick = {
                        selectedStruggles = if (isSelected) {
                            selectedStruggles - struggle
                        } else {
                            selectedStruggles + struggle
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = struggle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null // handled by card click
                        )
                    }
                }
            }
        }
    }
}
