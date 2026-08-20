package com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding.components

import android.R.attr.onClick
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.coolApps.MultipleAlarmClock.R
import androidx.compose.ui.unit.sp


@Composable fun GreetingScreen(onClickNext:()->Unit) {

	val infiniteTransition = rememberInfiniteTransition(
		label = "wave_animation"
	)

	val rotation by infiniteTransition.animateFloat(
		initialValue = -18f,
		targetValue = 20f,
		animationSpec = infiniteRepeatable(
			animation = tween(durationMillis = 450, easing = LinearEasing),
			repeatMode = RepeatMode.Reverse
		),
		label = "handRotation"
	)

	val handRotation by infiniteTransition.animateFloat(
		initialValue = 0f,
		targetValue = 0f,
		animationSpec = infiniteRepeatable(
			animation = keyframes {
				durationMillis = 1990
				0f at 0
				-18f at 150
				18f at 300
				-18f at 450
				15f at 600
				0f at 750
				0f at 1600
			},
			repeatMode = RepeatMode.Restart
		),
		label = "hand_rotation"
	)

	Scaffold(
		bottomBar = {
			Box(
				modifier =
					Modifier.fillMaxWidth()
						.background(colorScheme.background)
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
						onClick = onClickNext,
						modifier = Modifier
							.fillMaxWidth()
							.height(56.dp),
						shape = shapes.extraLarge,
						colors = ButtonDefaults.buttonColors(
							containerColor = colorScheme.primaryContainer,
							contentColor = colorScheme.onPrimaryContainer
						)
					) {
						Text(
							text = stringResource(R.string.onboarding_greeting_next),
							style = typography.titleMedium
						)
					}


				}
			}
		}
	) { padding->
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(colorScheme.background),
			contentAlignment = Alignment.Center
		) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.Center
			) {
				Text(
					text = stringResource(R.string.onboarding_greeting_hello),
					style = MaterialTheme.typography.displayLarge,
					fontWeight = FontWeight.SemiBold,
					color = colorScheme.onBackground
				)
				Spacer(modifier = Modifier.width(10.dp))
				Text(
					text = "👋",
					fontSize = 44.sp,
					modifier = Modifier.graphicsLayer {
						rotationZ = handRotation
						// pivot near the wrist so it swings like a real wave
						transformOrigin = TransformOrigin(0.74f, 0.75f)
					}
				)
			}
		}

	}

}