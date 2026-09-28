package com.coolApps.MultipleAlarmClock

import android.app.Application
import android.content.Context
import android.content.res.Resources
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import com.coolApps.MultipleAlarmClock.presentation.util.resolveColorScheme
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class AppColorSchemeTest {

	private val contextWithMissingSystemColors: Context = mockk(relaxed = true) {
		every { resources.getColor(any(), any()) } throws Resources.NotFoundException("missing system color")
	}

	@Test
	fun `light theme falls back to static scheme when system colors are missing`() {
		val scheme = resolveColorScheme(contextWithMissingSystemColors, darkTheme = false)

		assertThat(scheme.primary).isEqualTo(lightColorScheme().primary)
		assertThat(scheme.background).isEqualTo(lightColorScheme().background)
	}

	@Test
	fun `dark theme falls back to static scheme when system colors are missing`() {
		val scheme = resolveColorScheme(contextWithMissingSystemColors, darkTheme = true)

		assertThat(scheme.primary).isEqualTo(darkColorScheme().primary)
		assertThat(scheme.background).isEqualTo(darkColorScheme().background)
	}
}
