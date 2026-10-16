package com.coolApps.MultipleAlarmClock.presentation.settings

import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.presentation.logD

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
		onNavigateBack: () -> Unit,
		onNavigateToPaywall: (Boolean) -> Unit,
		onNavigateToCustomerCenter: () -> Unit,
) {
	var feedbackText by rememberSaveable { mutableStateOf("") }

	val settingsViewModel: SettingsViewModel = hiltViewModel()
	val isPro by settingsViewModel.isPremium.collectAsState()
	val context = LocalContext.current
	val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

	Scaffold(
		modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
		containerColor = MaterialTheme.colorScheme.surface,
		contentWindowInsets = WindowInsets.safeDrawing,
		topBar = {
			val backInteractionSource = remember { MutableInteractionSource() }
			val isBackPressed by backInteractionSource.collectIsPressedAsState()
			val backCorner by animateDpAsState(
				targetValue = if (isBackPressed) 8.dp else 14.dp,
				animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
				label = "backCorner"
			)

			TopAppBar(
				title = {
					Text(
						text = stringResource(R.string.settings_title),
						style = MaterialTheme.typography.headlineSmall,
						fontWeight = FontWeight.ExtraBold,
					)
				},
				navigationIcon = {
					FilledTonalIconButton(
						onClick = onNavigateBack,
						interactionSource = backInteractionSource,
						shape = RoundedCornerShape(backCorner),
						colors = IconButtonDefaults.filledTonalIconButtonColors(
							containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
							contentColor = MaterialTheme.colorScheme.onSurface
						),
						modifier = Modifier
							.padding(start = 8.dp)
							.size(42.dp)
					) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = stringResource(R.string.settings_back_desc)
						)
					}
				},
				colors = TopAppBarDefaults.topAppBarColors(
					containerColor = Color.Transparent,
					scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
				),
				scrollBehavior = scrollBehavior
			)
		}
	) { paddingValues ->
		Box(
			modifier = Modifier
				.fillMaxSize()
				.padding(paddingValues),
			contentAlignment = Alignment.TopCenter
		) {
			LazyColumn(
				modifier = Modifier
					.widthIn(max = 640.dp)
					.fillMaxSize(),
				contentPadding = PaddingValues(
					start = 20.dp,
					end = 20.dp,
					top = 12.dp,
					bottom = 36.dp
				),
				verticalArrangement = Arrangement.spacedBy(24.dp)
			) {

				// ---------------------------------------------------------
				// PREMIUM
				// ---------------------------------------------------------

				item {
					SettingsSection(
						title = stringResource(R.string.settings_section_premium)
					) {
						SettingsListItem(
							icon = Icons.Outlined.WorkspacePremium,
							title = if (isPro) {
								stringResource(R.string.settings_premium_active)
							} else {
								stringResource(R.string.settings_premium_go_pro)
							},
							subtitle = if (isPro) {
								stringResource(R.string.settings_premium_manage_subscription)
							} else {
								stringResource(R.string.settings_premium_unlock_features)
							},
							onClick = {
								if (isPro) {
									onNavigateToCustomerCenter()
								} else {
									onNavigateToPaywall(true)
								}
							},
							trailing = {
								Surface(
									shape = RoundedCornerShape(10.dp),
									color = if (!isPro) {
										MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
									} else {
										MaterialTheme.colorScheme.surfaceContainerHighest
									},
									modifier = Modifier.size(32.dp)
								) {
									Box(contentAlignment = Alignment.Center) {
										Icon(
											imageVector = Icons.Filled.ChevronRight,
											contentDescription = null,
											modifier = Modifier.size(20.dp)
										)
									}
								}
							},
							emphasized = !isPro,
							topCorner = 24.dp,
							bottomCorner = 24.dp
						)
					}
				}

				// ---------------------------------------------------------
				// FEEDBACK
				// ---------------------------------------------------------

				item {
					SettingsSection(
						title = stringResource(R.string.settings_section_feedback)
					) {
						Surface(
							modifier = Modifier.fillMaxWidth(),
							shape = RoundedCornerShape(24.dp),
							color = MaterialTheme.colorScheme.surfaceContainerLow,
							tonalElevation = 0.dp
						) {
							Column(
								modifier = Modifier
									.fillMaxWidth()
									.padding(20.dp),
								verticalArrangement = Arrangement.spacedBy(14.dp)
							) {
								Row(
									verticalAlignment = Alignment.CenterVertically,
									horizontalArrangement = Arrangement.spacedBy(14.dp)
								) {
									Box(
										modifier = Modifier
											.size(44.dp)
											.clip(RoundedCornerShape(12.dp))
											.background(MaterialTheme.colorScheme.secondaryContainer),
										contentAlignment = Alignment.Center
									) {
										Icon(
											imageVector = Icons.AutoMirrored.Outlined.Send,
											contentDescription = null,
											tint = MaterialTheme.colorScheme.onSecondaryContainer,
											modifier = Modifier.size(22.dp)
										)
									}

									Column(modifier = Modifier.weight(1f)) {
										Text(
											text = stringResource(R.string.settings_feedback_improve_app),
											style = MaterialTheme.typography.titleMedium,
											fontWeight = FontWeight.Bold,
											color = MaterialTheme.colorScheme.onSurface
										)
										Spacer(Modifier.height(2.dp))
										Text(
											text = stringResource(R.string.feedback_description),
											style = MaterialTheme.typography.bodyMedium,
											color = MaterialTheme.colorScheme.onSurfaceVariant
										)
									}
								}

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
									shape = RoundedCornerShape(16.dp),
									maxLines = 6,
									supportingText = {
										Text(
											text = stringResource(
												R.string.feedback_char_count,
												feedbackText.length,
												500
											),
											modifier = Modifier.fillMaxWidth(),
											textAlign = TextAlign.End
										)
									},
									colors = OutlinedTextFieldDefaults.colors(
										focusedContainerColor =
											MaterialTheme.colorScheme.surfaceContainerLowest,
										unfocusedContainerColor =
											MaterialTheme.colorScheme.surfaceContainerLowest,
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
									val sendInteractionSource = remember { MutableInteractionSource() }
									val isSendPressed by sendInteractionSource.collectIsPressedAsState()
									val sendCorner by animateDpAsState(
										targetValue = if (isSendPressed) 10.dp else 18.dp,
										animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
										label = "sendCorner"
									)
									val sendScale by animateFloatAsState(
										targetValue = if (isSendPressed) 0.96f else 1f,
										animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
										label = "sendScale"
									)

									FilledTonalButton(
										onClick = {
											settingsViewModel.submitFeedback(
												feedbackText
											)
											feedbackText = ""
										},
										interactionSource = sendInteractionSource,
										enabled = feedbackText.isNotBlank(),
										shape = RoundedCornerShape(sendCorner),
										modifier = Modifier.graphicsLayer {
											scaleX = sendScale
											scaleY = sendScale
										}
									) {
										Icon(
											imageVector = Icons.AutoMirrored.Outlined.Send,
											contentDescription = null,
											modifier = Modifier.size(18.dp)
										)

										Spacer(
											modifier = Modifier.width(8.dp)
										)

										Text(
											text = stringResource(R.string.settings_feedback_send),
											fontWeight = FontWeight.Bold
										)
									}
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
						title = stringResource(R.string.about_section_title)
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
							modifier = Modifier.fillMaxWidth(),
							verticalArrangement = Arrangement.spacedBy(3.dp)
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
									Surface(
										shape = RoundedCornerShape(10.dp),
										color = MaterialTheme.colorScheme.surfaceContainerHighest,
										modifier = Modifier.size(32.dp)
									) {
										Box(contentAlignment = Alignment.Center) {
											Icon(
												imageVector = Icons.Outlined.ChevronRight,
												contentDescription = null,
												modifier = Modifier.size(20.dp)
											)
										}
									}
								},
								topCorner = 24.dp,
								bottomCorner = 6.dp
							)

							SettingsListItem(
								icon = Icons.Outlined.Info,
								title = stringResource(
									R.string.about_version_title
								),
								subtitle = appVersion,
								onClick = null,
								trailing = {
									Surface(
										shape = RoundedCornerShape(50),
										color = MaterialTheme.colorScheme.secondaryContainer,
										contentColor = MaterialTheme.colorScheme.onSecondaryContainer
									) {
										Text(
											text = appVersion,
											style = MaterialTheme.typography.labelMedium,
											fontWeight = FontWeight.Bold,
											modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
										)
									}
								},
								topCorner = 6.dp,
								bottomCorner = 24.dp
							)
						}
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
		context.getString(R.string.settings_version_format, versionName, versionCode)
	} catch (e: Exception) {
		"v1.0.0"
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsListItem(
		icon: ImageVector,
		title: String,
		subtitle: String,
		onClick: (() -> Unit)?,
		trailing: @Composable (() -> Unit)? = null,
		emphasized: Boolean = false,
		topCorner: Dp = 24.dp,
		bottomCorner: Dp = 24.dp,
) {
	val interactionSource = remember { MutableInteractionSource() }
	val isPressed by interactionSource.collectIsPressedAsState()

	val animatedTopCorner by animateDpAsState(
		targetValue = if (isPressed && onClick != null) 16.dp else topCorner,
		animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
		label = "topCorner"
	)
	val animatedBottomCorner by animateDpAsState(
		targetValue = if (isPressed && onClick != null) 16.dp else bottomCorner,
		animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
		label = "bottomCorner"
	)
	val scale by animateFloatAsState(
		targetValue = if (isPressed && onClick != null) 0.98f else 1f,
		animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
		label = "itemScale"
	)

	val background by animateColorAsState(
		targetValue = if (emphasized) {
			MaterialTheme.colorScheme.primaryContainer
		} else {
			MaterialTheme.colorScheme.surfaceContainerLow
		},
		animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
		label = "itemBg"
	)

	val contentColor by animateColorAsState(
		targetValue = if (emphasized) {
			MaterialTheme.colorScheme.onPrimaryContainer
		} else {
			MaterialTheme.colorScheme.onSurface
		},
		animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
		label = "itemContent"
	)

	val shape = RoundedCornerShape(
		topStart = animatedTopCorner,
		topEnd = animatedTopCorner,
		bottomStart = animatedBottomCorner,
		bottomEnd = animatedBottomCorner
	)

	Surface(
		modifier = Modifier
			.fillMaxWidth()
			.graphicsLayer {
				scaleX = scale
				scaleY = scale
			}
			.clip(shape)
			.then(
				if (onClick != null) {
					Modifier.clickable(
						interactionSource = interactionSource,
						indication = null,
						onClick = onClick
					)
				} else {
					Modifier
				}
			),
		shape = shape,
		color = background,
		border = if (emphasized) {
			BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
		} else {
			null
		},
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
					.size(46.dp)
					.clip(RoundedCornerShape(14.dp))
					.background(
						if (emphasized) {
							MaterialTheme.colorScheme.primary
						} else {
							MaterialTheme.colorScheme.secondaryContainer
						}
					),
				contentAlignment = Alignment.Center
			) {
				Icon(
					imageVector = icon,
					contentDescription = null,
					tint = if (emphasized) {
						MaterialTheme.colorScheme.onPrimary
					} else {
						MaterialTheme.colorScheme.onSecondaryContainer
					},
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
					style = MaterialTheme.typography.titleMedium,
					color = contentColor,
					fontWeight = if (emphasized) FontWeight.ExtraBold else FontWeight.Bold
				)

				Spacer(
					modifier = Modifier.height(3.dp)
				)

				Text(
					text = subtitle,
					style = MaterialTheme.typography.bodyMedium,
					color = contentColor.copy(alpha = 0.76f)
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
		verticalArrangement = Arrangement.spacedBy(10.dp)
	) {
		Text(
			text = title.uppercase(),
			style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.8.sp),
			color = MaterialTheme.colorScheme.primary,
			fontWeight = FontWeight.ExtraBold,
			modifier = Modifier.padding(
				start = 8.dp,
				bottom = 2.dp
			)
		)

		content()
	}
}