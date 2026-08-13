package MultipleAlarmClock.alarmFeature.ui.onboarding.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.coolApps.MultipleAlarmClock.R
import com.example.MultipleAlarmClock.Ui.Permissions.PermissionStep
import com.example.MultipleAlarmClock.Ui.Permissions.PermissionUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionScreen(
    missingSteps: List<PermissionStep>,
    refreshPermissionUiState: () -> Unit,
    onNext: () -> Unit,
    allCriticalGranted: Boolean
) {

	val context = LocalContext.current
	val lifecycleOwner = LocalLifecycleOwner.current

	val notificationPermState = rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)

	// Track if we've requested notification permission at least once
	var notificationRequested by rememberSaveable {
		mutableStateOf(false)
	}

	val notificationPermanentlyDenied = notificationRequested && !notificationPermState.status.isGranted && !notificationPermState.status.shouldShowRationale

	DisposableEffect(lifecycleOwner) {
		val observer = LifecycleEventObserver { _, event ->
			if (event == Lifecycle.Event.ON_RESUME) {
				refreshPermissionUiState()
			}
		}
		lifecycleOwner.lifecycle.addObserver(observer)
		onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
	}


    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding()
                    .padding(26.dp)
                    .padding(bottom = 20.dp)
                    .animateContentSize(),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onNext,
                        enabled = allCriticalGranted,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.height(56.dp),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Text(
                            text = if (allCriticalGranted) "Continue" else "Grant permissions",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
			Spacer(modifier = Modifier.weight(0.2f))

			AnimatedContent(
				targetState = allCriticalGranted,
				transitionSpec = {
					(
							fadeIn(
								animationSpec = tween(
									durationMillis = 220,
									delayMillis = 100,
									easing = FastOutSlowInEasing
								)
							) +
									scaleIn(
										initialScale = 0.75f,
										animationSpec = spring(
											dampingRatio = Spring.DampingRatioMediumBouncy,
											stiffness = Spring.StiffnessMedium
										)
									)
							) togetherWith (
							fadeOut(
								animationSpec = tween(
									durationMillis = 120,
									easing = FastOutLinearInEasing
								)
							) +
									scaleOut(
										targetScale = 0.75f,
										animationSpec = tween(
											durationMillis = 120,
											easing = FastOutLinearInEasing
										)
									)
							)
				},
				label = "permission_icon_transition"
			) { granted ->

				Icon(
					imageVector = if (granted) {
						Icons.Default.Check
					} else {
						Icons.Default.Security
					},
					contentDescription = null,
					modifier = Modifier.size(64.dp),
					tint = MaterialTheme.colorScheme.primary
				)
			}

			Spacer(modifier = Modifier.height(24.dp))

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text =if (allCriticalGranted) "All set" else "Permissions" ,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(12.dp))

			if (!allCriticalGranted){
				Text(
					text = stringResource(R.string.onboarding_permission_reason),
					style = MaterialTheme.typography.bodySmall,
					textAlign = TextAlign.Center,
					color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
					modifier = Modifier.fillMaxWidth(0.9f)
				)

			}

            Spacer(modifier = Modifier.height(32.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                modifier = Modifier
					.fillMaxWidth()
					.weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(missingSteps) { step ->
					val isPermanentlyDenied = step == PermissionStep.PostNotification && notificationPermanentlyDenied

                    PermissionItem(
                        step = step,
						isPermanentlyDenied = isPermanentlyDenied,
                        onAction = {
							when (step) {
								PermissionStep.PostNotification -> {
									if (notificationPermanentlyDenied) {
										context.startActivity(
											Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
												data = Uri.fromParts("package", context.packageName, null)
											}
										)
									} else {
										notificationPermState.launchPermissionRequest()
										notificationRequested = true
									}
								}
								PermissionStep.ExactAlarm -> {
									context.startActivity(
										Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
											data = Uri.fromParts("package", context.packageName, null)
										}
									)
								}
								PermissionStep.FullScreenIntent -> {
									if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
										context.startActivity(
											Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
												data = Uri.fromParts("package", context.packageName, null)
											}
										)
									}
								}
								PermissionStep.XiaomiAutostart -> {
									PermissionUtils.launchXiaomiSettings(context)
								}
							}

						}
                    )
                }
            }
            Spacer(modifier = Modifier.weight(0.2f))
        }
    }
}

@Composable
private fun PermissionItem(
    step: PermissionStep,
	isPermanentlyDenied: Boolean = false,
    onAction: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(step.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(step.rationaleRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isPermanentlyDenied) "Settings" else if (step.action != null) "Open" else "Allow",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
