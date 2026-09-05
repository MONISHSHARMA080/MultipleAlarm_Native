package com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmContainer.utils

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.logD

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
	onNavigateBack: () -> Unit,
	onNavigateToPaywall: () -> Unit,
	onNavigateToCustomerCenter: () -> Unit,
) {
	var feedbackText by rememberSaveable { mutableStateOf("") }
	val settingsViewModel: SettingsViewModel = hiltViewModel()
	val isPro by settingsViewModel.isPro.collectAsState()
	val context = LocalContext.current

	Scaffold(
		contentWindowInsets = WindowInsets.safeDrawing,
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
				.verticalScroll(rememberScrollState()),
			verticalArrangement = Arrangement.spacedBy(16.dp)
		) {
			PremiumSection(
				isPro = isPro,
				onGoProClick = onNavigateToPaywall,
				onManageSubscriptionClick = onNavigateToCustomerCenter,
				modifier = Modifier.fillMaxWidth()
			)

			FeedbackCardContent(
				feedbackText = feedbackText,
				onFeedbackChange = { feedbackText = it },
				onSubmit = {
					settingsViewModel.submitFeedback(feedbackText)
					feedbackText = "" // Clear after submit
				},
				onDismiss = { feedbackText = "" },
				modifier = Modifier.fillMaxWidth(),
				autoFocus = false,
				showMaybeLaterButton = false
			)

			AboutSection(
				onRateAppClick = {
					openPlayStore(context)
					settingsViewModel.captureEvent("Rate App Clicked", mapOf("screen" to "Setting"))
	     		 },
				modifier = Modifier.fillMaxWidth()
			)

		}
	}
}

@Composable
fun PremiumSection(
	isPro: Boolean,
	onGoProClick: () -> Unit,
	onManageSubscriptionClick: () -> Unit,
	modifier: Modifier = Modifier
) {
	Card(
		modifier = modifier.fillMaxWidth(),
		shape = RoundedCornerShape(20.dp),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
		),
		elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
	) {
		Column(
			modifier = Modifier.padding(20.dp),
			verticalArrangement = Arrangement.spacedBy(4.dp)
		) {
			Text(
				text = "Premium",
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.primary,
				modifier = Modifier.padding(bottom = 8.dp)
			)

			if (!isPro) {
				SettingsRow(
					icon = Icons.Outlined.WorkspacePremium,
					title = "Go Pro",
					subtitle = "Unlock all features and remove ads",
					onClick = onGoProClick
				)
			} else {
				SettingsRow(
					icon = Icons.Outlined.WorkspacePremium,
					title = "Premium Active",
					subtitle = "Manage your subscription",
					onClick = onManageSubscriptionClick
				)
			}
		}
	}
}

@Composable
fun AboutSection(
	onRateAppClick: () -> Unit,
	modifier: Modifier = Modifier
) {
	val context = LocalContext.current
	val appName = stringResource(R.string.app_name)
	val rateAppTitle = stringResource(R.string.rate_app_title, appName)
	val appVersion = remember(context) { getAppVersion(context) }

	Card(
		modifier = modifier.fillMaxWidth(),
		shape = RoundedCornerShape(20.dp),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
		),
		elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
	) {
		Column(
			modifier = Modifier.padding(20.dp),
			verticalArrangement = Arrangement.spacedBy(4.dp)
		) {
			Text(
				text = stringResource(R.string.about_section_title),
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.primary,
				modifier = Modifier.padding(bottom = 8.dp)
			)

			// Rate App item
			SettingsRow(
				icon = Icons.Outlined.Star,
				title = rateAppTitle,
				subtitle = stringResource(R.string.rate_app_subtitle),
				onClick = onRateAppClick
			)

			HorizontalDivider(
				modifier = Modifier.padding(vertical = 8.dp),
				color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
			)

			// App Version item
			SettingsRow(
				icon = Icons.Outlined.Info,
				title = stringResource(R.string.about_version_title),
				subtitle = appVersion,
				onClick = null
			)
		}
	}
}

@Composable
fun SettingsRow(
	icon: ImageVector,
	title: String,
	subtitle: String,
	onClick: (() -> Unit)?,
	modifier: Modifier = Modifier
) {
	Row(
		modifier = modifier
			.fillMaxWidth()
			.then(
				if (onClick != null) Modifier.clickable(onClick = onClick)
				else Modifier
			)
			.padding(vertical = 8.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Icon(
			imageVector = icon,
			contentDescription = null,
			tint = MaterialTheme.colorScheme.onSurfaceVariant,
			modifier = Modifier.size(24.dp)
		)
		Spacer(modifier = Modifier.width(16.dp))
		Column(modifier = Modifier.weight(1f)) {
			Text(
				text = title,
				style = MaterialTheme.typography.bodyLarge,
				color = MaterialTheme.colorScheme.onSurface
			)
			Text(
				text = subtitle,
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	}
}

private fun openPlayStore(context: Context) {
	val packageName = context.packageName
	try {
		val intent = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()).apply {
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		}
		context.startActivity(intent)
	} catch (e: Exception) {
		val webIntent = Intent(Intent.ACTION_VIEW,
			"https://play.google.com/store/apps/details?id=$packageName".toUri()).apply {
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		}
		context.startActivity(webIntent)
	}
}

private fun getAppVersion(context: Context): String {
	return try {
		val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
		logD("app version is ${packageInfo.versionName}")
		val versionName = packageInfo.versionName
		val versionCode = packageInfo.longVersionCode
		"v$versionName ($versionCode)"
	} catch (e: Exception) {
		"v1.0.0"
	}
}