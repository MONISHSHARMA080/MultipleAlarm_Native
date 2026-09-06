package com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmContainer.utils

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
		containerColor = MaterialTheme.colorScheme.background,
		contentWindowInsets = WindowInsets.safeDrawing,
		topBar = {
			TopAppBar(
				title = {
					Text(
						text = "Settings",
						style = MaterialTheme.typography.headlineSmall,
						fontWeight = FontWeight.SemiBold,
					)
				},
				navigationIcon = {
					IconButton(
						onClick = onNavigateBack
					) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = "Back"
						)
					}
				},
				colors = TopAppBarDefaults.topAppBarColors(
					containerColor = Color.Transparent,
					scrolledContainerColor = MaterialTheme.colorScheme.surface,
				)
			)
		}
	) { paddingValues ->

		LazyColumn(
			modifier = Modifier
				.fillMaxSize()
				.padding(paddingValues),
			contentPadding = PaddingValues(
				start = 20.dp,
				end = 20.dp,
				top = 12.dp,
				bottom = 32.dp
			),
			verticalArrangement = Arrangement.spacedBy(28.dp)
		) {

			// ---------------------------------------------------------
			// PREMIUM
			// ---------------------------------------------------------

			item {
				SettingsSection(
					title = "Premium"
				) {

					SettingsListItem(
						icon = Icons.Outlined.WorkspacePremium,
						title = if (isPro) {
							"Premium Active"
						} else {
							"Go Pro"
						},
						subtitle = if (isPro) {
							"Manage your subscription"
						} else {
							"Unlock all features and remove ads"
						},
						onClick = {
							if (isPro) {
								onNavigateToCustomerCenter()
							} else {
								onNavigateToPaywall()
							}
						},
						trailing = {
							Icon(
								imageVector =
									Icons.Filled.ChevronRight,
								contentDescription = null
							)
						},
						emphasized = !isPro
					)
				}
			}

			// ---------------------------------------------------------
			// FEEDBACK
			// ---------------------------------------------------------

			item {
				SettingsSection(
					title = "Feedback"
				) {

					Column(
						modifier = Modifier.fillMaxWidth(),
						verticalArrangement = Arrangement.spacedBy(12.dp)
					) {

						Text(
							text = "Help improve the app",
							style = MaterialTheme.typography.titleMedium,
							fontWeight = FontWeight.Medium
						)

						Text(
							text = stringResource(R.string.feedback_description),
							style = MaterialTheme.typography.bodyMedium,
							color = MaterialTheme.colorScheme.onSurfaceVariant
						)

						OutlinedTextField(
							value = feedbackText,
							onValueChange = {
								if (it.length <= 500) {
									feedbackText = it
								}
							},
							modifier = Modifier
								.fillMaxWidth()
								.heightIn(min = 120.dp),
							placeholder = {
								Text(
									text = stringResource(
										R.string.feedback_placeholder
									)
								)
							},
							shape = RoundedCornerShape(20.dp),
							maxLines = 6,
							supportingText = {
								Text(
									text = "${feedbackText.length}/500",
									modifier = Modifier.fillMaxWidth(),
									textAlign = TextAlign.End
								)
							},
							colors = OutlinedTextFieldDefaults.colors(
								focusedContainerColor =
									MaterialTheme.colorScheme.surfaceContainerLow,
								unfocusedContainerColor =
									MaterialTheme.colorScheme.surfaceContainerLow,
								focusedBorderColor =
									MaterialTheme.colorScheme.primary,
								unfocusedBorderColor =
									MaterialTheme.colorScheme.outlineVariant
							)
						)

						Row(
							modifier = Modifier.fillMaxWidth(),
							horizontalArrangement = Arrangement.End
						) {
							FilledTonalButton(
								onClick = {
									settingsViewModel.submitFeedback(
										feedbackText
									)
									feedbackText = ""
								},
								enabled = feedbackText.isNotBlank(),
								shape = RoundedCornerShape(16.dp)
							) {
								Icon(
									imageVector = Icons.AutoMirrored.Outlined.Send,
									contentDescription = null,
									modifier = Modifier.size(18.dp)
								)

								Spacer(
									modifier = Modifier.width(8.dp)
								)

								Text("Send")
							}
						}
					}
				}
			}

			// ---------------------------------------------------------
			// ABOUT
			// ---------------------------------------------------------

			item {
				SettingsSection(
					title = "About"
				) {

					val appName =
						stringResource(R.string.app_name)

					val rateAppTitle =
						stringResource(
							R.string.rate_app_title,
							appName
						)

					val appVersion =
						remember(context) {
							getAppVersion(context)
						}

					Column(
						modifier = Modifier.fillMaxWidth()
					) {

						SettingsListItem(
							icon = Icons.Outlined.Star,
							title = rateAppTitle,
							subtitle = stringResource(
								R.string.rate_app_subtitle
							),
							onClick = {
								openPlayStore(context)

								settingsViewModel.captureEvent(
									"Rate App Clicked",
									mapOf(
										"screen" to "Setting"
									)
								)
							},
							trailing = {
								Icon(
									imageVector =
										Icons.Outlined.ChevronRight,
									contentDescription = null
								)
							}
						)

						HorizontalDivider(
							modifier = Modifier.padding(
								start = 56.dp
							)
						)

						SettingsListItem(
							icon = Icons.Outlined.Info,
							title = stringResource(
								R.string.about_version_title
							),
							subtitle = appVersion,
							onClick = null
						)
					}
				}
			}
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

@Composable
private fun SettingsListItem(
		icon: ImageVector,
		title: String,
		subtitle: String,
		onClick: (() -> Unit)?,
		trailing: @Composable (() -> Unit)? = null,
		emphasized: Boolean = false,
) {
	val background =
		if (emphasized) {
			MaterialTheme.colorScheme.primaryContainer
		} else {
			MaterialTheme.colorScheme.surfaceContainerLow
		}

	val contentColor =
		if (emphasized) {
			MaterialTheme.colorScheme.onPrimaryContainer
		} else {
			MaterialTheme.colorScheme.onSurface
		}

	Surface(
		modifier = Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(24.dp))
			.then(
				if (onClick != null) {
					Modifier.clickable(onClick = onClick)
				} else {
					Modifier
				}
			),
		color = background,
		tonalElevation = 0.dp,
		shadowElevation = 0.dp
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(
					horizontal = 18.dp,
					vertical = 16.dp
				),
			verticalAlignment = Alignment.CenterVertically
		) {

			Box(
				modifier = Modifier
					.size(48.dp)
					.clip(CircleShape)
					.background(
						if (emphasized) {
							MaterialTheme.colorScheme.primary.copy(
								alpha = 0.14f
							)
						} else {
							MaterialTheme.colorScheme.surfaceContainerHighest
						}
					),
				contentAlignment = Alignment.Center
			) {
				Icon(
					imageVector = icon,
					contentDescription = null,
					tint = contentColor,
					modifier = Modifier.size(24.dp)
				)
			}

			Spacer(
				modifier = Modifier.width(16.dp)
			)

			Column(
				modifier = Modifier.weight(1f)
			) {
				Text(
					text = title,
					style = MaterialTheme.typography.bodyLarge,
					color = contentColor,
					fontWeight = FontWeight.Medium
				)

				Spacer(
					modifier = Modifier.height(3.dp)
				)

				Text(
					text = subtitle,
					style = MaterialTheme.typography.bodyMedium,
					color = contentColor.copy(alpha = 0.72f)
				)
			}

			if (trailing != null) {
				Spacer(
					modifier = Modifier.width(12.dp)
				)

				CompositionLocalProvider(
					LocalContentColor provides contentColor
				) {
					trailing()
				}
			}
		}
	}
}
@Composable
private fun SettingsSection(
		title: String,
		content: @Composable ColumnScope.() -> Unit
) {
	Column(
		modifier = Modifier.fillMaxWidth(),
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
		Text(
			text = title.uppercase(),
			style = MaterialTheme.typography.labelLarge,
			color = MaterialTheme.colorScheme.primary,
			fontWeight = FontWeight.SemiBold,
			modifier = Modifier.padding(
				start = 4.dp,
				bottom = 2.dp
			)
		)

		content()
	}
}