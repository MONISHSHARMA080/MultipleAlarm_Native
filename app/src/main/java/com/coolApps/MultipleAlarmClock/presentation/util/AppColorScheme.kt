package com.coolApps.MultipleAlarmClock.presentation.util

import android.content.Context
import android.content.res.Resources
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.coolApps.MultipleAlarmClock.presentation.logD

@Composable
fun rememberAppColorScheme(): ColorScheme {
	val context = LocalContext.current
	val darkTheme = isSystemInDarkTheme()
	return remember(context, darkTheme) { resolveColorScheme(context, darkTheme) }
}

/** Some Android 14 devices do not have all the system color resources that the dynamic scheme reads, so we fall back to the static scheme. */
internal fun resolveColorScheme(context: Context, darkTheme: Boolean): ColorScheme =
	try {
		if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
	} catch (e: Resources.NotFoundException) {
		logD("dynamic color scheme is not available, using the static scheme: $e")
		if (darkTheme) darkColorScheme() else lightColorScheme()
	}
