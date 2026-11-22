package com.coolApps.MultipleAlarmClock.presentation.trigger

import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.IntentCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.coolApps.MultipleAlarmClock.BuildConfig
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.data.ads.AdManager
import com.coolApps.MultipleAlarmClock.presentation.ExpressiveAlarmTheme
import com.coolApps.MultipleAlarmClock.presentation.ads.AdMobBanner
import com.coolApps.MultipleAlarmClock.presentation.logD
import com.coolApps.MultipleAlarmClock.receiver.AlarmService
import com.coolApps.MultipleAlarmClock.util.Analytics
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {
    private var wakeLock: PowerManager.WakeLock? = null
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var intentReceived: Intent
    private val defaultAlarmDismissDuration = 2.minutes
    private var dismissIntent : Intent? = null
	private var shouldDismissAlarm = false
	@Inject lateinit var analytics: Analytics
	@Inject lateinit var adManager: AdManager


    override fun onCreate(savedInstanceState: Bundle?) {
		logD("about to create a new alarm")
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
		this.intentReceived = intent
        setContent {
            ExpressiveAlarmTheme {
                val message = remember { parseTheIntent()?.message.orEmpty() }
                LaunchedEffect(Unit) {
                    if (dismissIntent == null) dismissIntent = makeDismissIntent()
                }
                val shouldShowAds by adManager.shouldShowAds.collectAsStateWithLifecycle()
                val keyguardManager = remember { getSystemService(KeyguardManager::class.java) }
                var isDeviceLocked by remember { mutableStateOf(keyguardManager?.isKeyguardLocked == true) }
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    isDeviceLocked = keyguardManager?.isKeyguardLocked == true
                }
                TimeDisplay(
                    onFinish = { stopAlarmAndFinish() },
                    message = message,
                    shouldShowAds = shouldShowAds && !isDeviceLocked,
                )
            }
        }
        lifecycleScope.launch {
			delay(parseTheIntent()?.ringDurationInMin?.minutes ?: defaultAlarmDismissDuration)
			stopAlarmAndFinish()
		}
        lifecycleScope.launch(Dispatchers.Main) {keepScreenON()  }
    }

    /** this  function will get the message form the intent and will set it on the mutable State  that is  passed in */
    private fun parseTheIntent(): AlarmActivityIntentData?{
        val parsedIntentData = IntentCompat.getParcelableExtra(intent, "intentData", AlarmActivityIntentData::class.java)
        logD("the message present in the intent is $parsedIntentData")
        return parsedIntentData
    }

    override fun onNewIntent(intent: Intent) {
		logD("New Intent received in AlarmActivity")
		super.onNewIntent(intent)
		setIntent(intent)
		this.intentReceived = intent
		dismissIntent = null
		// release wakelock if needed
		wakeLock?.let { if (it.isHeld) it.release() }
		// just finish and restart cleanly
		finish()
		startActivity(intent)
    }

    override fun onDestroy() {
        runCatching {
            super.onDestroy()
            try {
                wakeLock?.let {
                    if (it.isHeld) {
                        it.release()
                    }
                }
            } catch (e: Exception) {
                logD("Error releasing WakeLock in onDestroy: ${e.message}")
            }
            if (shouldDismissAlarm) {
                dismissTheAlarm()
            }
            activityScope.cancel()
        }.fold(onSuccess = {}, onFailure = {exception ->
            logD("there is a exception while destroying the AlarmActivity ->  ${exception.message}\n-->$exception")
        })
    }
	private fun makeDismissIntent(): Intent {
		return Intent(this.intentReceived).apply {
			action = AlarmService.ACTION_DISMISS_ALARM
			setClass(this@AlarmActivity, AlarmService::class.java)
		}
	}
    private fun dismissTheAlarm(){
        if (dismissIntent == null) dismissIntent = makeDismissIntent()
        startService(dismissIntent)
    }

	private fun stopAlarmAndFinish() {
		shouldDismissAlarm = true
		finishAndRemoveTask()
	}

    private fun keepScreenON() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        setShowWhenLocked(true)
        setTurnScreenOn(true)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable fun TimeDisplay(
    onFinish: () -> Unit,
    message: String,
    modifier: Modifier = Modifier,
    shouldShowAds: Boolean = false,
) {
    var currentTime by remember { mutableStateOf(getCurrentTime()) }
    var amPm by remember { mutableStateOf(getAmPm()) }
	val colorScheme = MaterialTheme.colorScheme
	val view = LocalView.current

    val stopInteractionSource = remember { MutableInteractionSource() }
    val isStopPressed by stopInteractionSource.collectIsPressedAsState()
    val stopButtonScale by animateFloatAsState(
        targetValue = if (isStopPressed) 0.94f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "stopButtonScale"
    )
    val stopButtonCorner by animateDpAsState(
        targetValue = if (isStopPressed) 22.dp else 44.dp,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "stopButtonCorner"
    )

	LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentTime()
            delay(1.seconds)
        }
    }
	LaunchedEffect(Unit) {
		while (true) {
			amPm = getAmPm()
			delay(30.seconds)
		}
	}
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeContent,
        containerColor = colorScheme.surface,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = {
						view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
						onFinish()
				  	},
                    interactionSource = stopInteractionSource,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primaryContainer,
                        contentColor = colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(stopButtonCorner),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp
                    ),
                    modifier = Modifier
                        .scale(stopButtonScale)
                        .height(74.dp)
                        .widthIn(max = 520.dp)
                        .fillMaxWidth(0.94f)
                ) {
					Icon(
						imageVector = Icons.Filled.Cancel,
						modifier = Modifier.size(28.dp),
						contentDescription = null
					)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.stop_button),
                        style = typography.headlineSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            val isCompactHeight = maxHeight < 460.dp
			val textMeasurer = rememberTextMeasurer()
			val density = LocalDensity.current
			val baseStyle = typography.displayLarge.copy(letterSpacing = (-2).sp, fontFeatureSettings = "tnum")

			val timeFontSize = remember(maxWidth, maxHeight) {
				val availableWidthPx = with(density) { (minOf(maxWidth, 640.dp) - 16.dp).toPx() }
				val refSize = 100.sp
				val measured = textMeasurer.measure(
					text = "00:00:00 AM",              // widest realistic string
					style = baseStyle.copy(fontSize = refSize),
					maxLines = 1, softWrap = false
				).size.width
				val scaled = refSize.value * availableWidthPx / measured
				minOf(scaled, maxHeight.value * 0.38f).coerceIn(32f, 168f).sp
			}
            val amPmFontSize = (timeFontSize.value * 0.22f).coerceIn(16f, 34f).sp
            val defaultSpatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
            val defaultEffectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

            AnimatedContent(
                targetState = message.isEmpty(),
                transitionSpec = {
                    (scaleIn(
                        initialScale = 0.95f,
                        animationSpec = defaultSpatialSpec
                    ) + fadeIn(
                        animationSpec = defaultEffectsSpec
                    )) togetherWith (scaleOut(
                        targetScale = 0.95f,
                        animationSpec = defaultSpatialSpec
                    ) + fadeOut(
                        animationSpec = defaultEffectsSpec
                    ))
                },
                label = "AlarmHeroContent"
            ) { messageIsEmpty ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp)
                        .padding(top = if (isCompactHeight) 12.dp else 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = if (messageIsEmpty) Arrangement.Center else Arrangement.Top
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = currentTime,
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false,
                            style = typography.displayLarge.copy(
                                fontSize = timeFontSize,
                                lineHeight = timeFontSize,
//                                fontWeight = FontWeight.Normal,
                                letterSpacing = (-2).sp,
                                fontFeatureSettings = "tnum"
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = amPm,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(bottom = (timeFontSize.value * 0.12f).dp),
                            style = typography.headlineMedium.copy(fontSize = amPmFontSize)
                        )
                    }

                    if (shouldShowAds) {
                        Spacer(modifier = Modifier.height(if (messageIsEmpty) 24.dp else 16.dp))
                        AdMobBanner(
                            adUnitId = BuildConfig.ADMOB_BANNER_ALARM_AD_UNIT_ID,
                            placement = AdManager.PLACEMENT_ALARM_RING,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }

                    if (!messageIsEmpty) {
                        Spacer(modifier = Modifier.height(if (shouldShowAds) 12.dp else 20.dp))
                        // Expressive Tonal Message Card
						Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
							Column(
								modifier = Modifier
									.fillMaxSize()
									.verticalScroll(rememberScrollState())
									.padding(vertical = 20.dp),
								horizontalAlignment = Alignment.CenterHorizontally
							) {
								Text(
									text = message,
									color = colorScheme.onSurface.copy(alpha = 0.85f),
									style = typography.titleLarge,
									fontWeight = FontWeight.Normal,
									textAlign = TextAlign.Center
								)
								Spacer(modifier = Modifier.height(24.dp))
							}
							Box(
								modifier = Modifier
									.align(Alignment.BottomCenter)
									.fillMaxWidth()
									.height(36.dp)
									.background(
										brush = Brush.verticalGradient(
											colors = listOf(
												Color.Transparent,
												colorScheme.surface
										)
									)
								)
							)
						}
					}
				}
			}
		}
	}
}

fun getCurrentTime(): String {
	return SimpleDateFormat("h:mm:ss", Locale.getDefault()).format(Date())
}

fun getAmPm(): String {
	return SimpleDateFormat("a", Locale.getDefault()).format(Date())
}
