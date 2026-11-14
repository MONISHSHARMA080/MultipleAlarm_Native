package com.coolApps.MultipleAlarmClock.presentation.picker

import android.app.AlarmManager
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.coolApps.MultipleAlarmClock.ErrorHandling.ErrorHandler
import com.coolApps.MultipleAlarmClock.data.billing.EntitlementManager
import com.coolApps.MultipleAlarmClock.data.paywall.PaywallTriggerType
import com.coolApps.MultipleAlarmClock.data.repository.AlarmSoundRepository
import com.coolApps.MultipleAlarmClock.domain.usecase.AlarmsController
import com.coolApps.MultipleAlarmClock.util.Analytics
import com.coolApps.MultipleAlarmClock.util.FeatureFlagsData
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AlarmPickerViewModelFlagsTest {

	@Before
	fun setUp() {
		Dispatchers.setMain(UnconfinedTestDispatcher())
	}

	@After
	fun tearDown() {
		Dispatchers.resetMain()
	}

	private val sound = AlarmSound(title = "Test", soundUri = Uri.parse("content://media/1"))

	private class Harness(
		val viewModel: AlarmPickerViewModel,
		val premium: MutableStateFlow<Boolean>,
	)

	private fun createViewModel(
		isSnoozePremium: Boolean,
		isSoundPickerPremium: Boolean,
		premium: MutableStateFlow<Boolean> = MutableStateFlow(false),
	): Harness {
		val context: Context = ApplicationProvider.getApplicationContext()
		val analytics: Analytics = mockk {
			every { featureFlagsData } returns MutableStateFlow(
				FeatureFlagsData(
					isSnoozePremium = isSnoozePremium,
					isSoundPickerPremium = isSoundPickerPremium,
				)
			)
			every { captureEvent(any(), any()) } just Runs
			every { screen(any(), any()) } just Runs
		}
		val alarmsController: AlarmsController = mockk {
			every { getTimeInHumanReadableFormatProtectFrom0Included(any()) } returns "12:00"
		}
		val entitlementManager: EntitlementManager = mockk {
			every { isPremium } returns premium
		}
		val alarmSoundRepository: AlarmSoundRepository = mockk {
			every { getAlarmSoundsStream() } returns flowOf(emptyList())
		}
		val viewModel = AlarmPickerViewModel(
			analytics = analytics,
			alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager,
			alarmsController = alarmsController,
			entitlementManager = entitlementManager,
			errorHandler = mockk(),
			alarmSoundRepository = alarmSoundRepository,
			context = context,
			alarmData = null,
		)
		return Harness(viewModel, premium)
	}

	private val defaultRingDuration get() = 2L

	// ─── Snooze (ring duration) ───

	@Test
	fun `snooze flag on and free user sees paywall and pending is stored`() {
		val harness = createViewModel(isSnoozePremium = true, isSoundPickerPremium = true)

		harness.viewModel.updateRingDuration(5)

		val state = harness.viewModel.uiState.value
		assertThat(state.paywallTriggerType).isEqualTo(PaywallTriggerType.PremiumFeature)
		assertThat(state.pendingRingDuration).isEqualTo(5)
		assertThat(state.alarmData.ringDurationInMin).isEqualTo(defaultRingDuration)
	}

	@Test
	fun `snooze flag on and premium user applies directly`() {
		val harness = createViewModel(
			isSnoozePremium = true,
			isSoundPickerPremium = true,
			premium = MutableStateFlow(true),
		)

		harness.viewModel.updateRingDuration(5)

		val state = harness.viewModel.uiState.value
		assertThat(state.paywallTriggerType).isNull()
		assertThat(state.pendingRingDuration).isNull()
		assertThat(state.alarmData.ringDurationInMin).isEqualTo(5)
	}

	@Test
	fun `snooze flag off and free user applies directly`() {
		val harness = createViewModel(isSnoozePremium = false, isSoundPickerPremium = true)

		harness.viewModel.updateRingDuration(5)

		val state = harness.viewModel.uiState.value
		assertThat(state.paywallTriggerType).isNull()
		assertThat(state.pendingRingDuration).isNull()
		assertThat(state.alarmData.ringDurationInMin).isEqualTo(5)
	}

	@Test
	fun `snooze flag off and premium user applies directly`() {
		val harness = createViewModel(
			isSnoozePremium = false,
			isSoundPickerPremium = true,
			premium = MutableStateFlow(true),
		)

		harness.viewModel.updateRingDuration(5)

		val state = harness.viewModel.uiState.value
		assertThat(state.paywallTriggerType).isNull()
		assertThat(state.alarmData.ringDurationInMin).isEqualTo(5)
	}

	@Test
	fun `snooze pending duration applies once premium unlocks`() {
		val harness = createViewModel(isSnoozePremium = true, isSoundPickerPremium = true)
		harness.viewModel.updateRingDuration(7)
		assertThat(harness.viewModel.uiState.value.pendingRingDuration).isEqualTo(7)

		harness.premium.value = true

		val state = harness.viewModel.uiState.value
		assertThat(state.alarmData.ringDurationInMin).isEqualTo(7)
		assertThat(state.pendingRingDuration).isNull()
		assertThat(state.paywallTriggerType).isNull()
	}

	// ─── Sound picker ───

	@Test
	fun `sound picker flag on and free user sees paywall and pending sound is stored`() {
		val harness = createViewModel(isSnoozePremium = false, isSoundPickerPremium = true)

		harness.viewModel.onAlarmSoundSelected(sound)

		val state = harness.viewModel.uiState.value
		assertThat(state.paywallTriggerType).isEqualTo(PaywallTriggerType.PremiumFeature)
		assertThat(state.pendingSound).isEqualTo(sound)
		assertThat(state.alarmData.sound).isNull()
	}

	@Test
	fun `sound picker flag on and premium user applies directly`() {
		val harness = createViewModel(
			isSnoozePremium = false,
			isSoundPickerPremium = true,
			premium = MutableStateFlow(true),
		)

		harness.viewModel.onAlarmSoundSelected(sound)

		val state = harness.viewModel.uiState.value
		assertThat(state.paywallTriggerType).isNull()
		assertThat(state.pendingSound).isNull()
		assertThat(state.alarmData.sound).isEqualTo(sound.soundUri.toString())
	}

	@Test
	fun `sound picker flag off and free user applies directly`() {
		val harness = createViewModel(isSnoozePremium = false, isSoundPickerPremium = false)

		harness.viewModel.onAlarmSoundSelected(sound)

		val state = harness.viewModel.uiState.value
		assertThat(state.paywallTriggerType).isNull()
		assertThat(state.pendingSound).isNull()
		assertThat(state.alarmData.sound).isEqualTo(sound.soundUri.toString())
		assertThat(state.soundSelectionCompletedGoBack).isTrue()
	}

	@Test
	fun `sound picker flag off and premium user applies directly`() {
		val harness = createViewModel(
			isSnoozePremium = false,
			isSoundPickerPremium = false,
			premium = MutableStateFlow(true),
		)

		harness.viewModel.onAlarmSoundSelected(sound)

		val state = harness.viewModel.uiState.value
		assertThat(state.paywallTriggerType).isNull()
		assertThat(state.alarmData.sound).isEqualTo(sound.soundUri.toString())
	}

	@Test
	fun `sound picker pending sound applies once premium unlocks`() {
		val harness = createViewModel(isSnoozePremium = false, isSoundPickerPremium = true)
		harness.viewModel.onAlarmSoundSelected(sound)
		assertThat(harness.viewModel.uiState.value.pendingSound).isEqualTo(sound)

		harness.premium.value = true

		val state = harness.viewModel.uiState.value
		assertThat(state.alarmData.sound).isEqualTo(sound.soundUri.toString())
		assertThat(state.pendingSound).isNull()
		assertThat(state.paywallTriggerType).isNull()
	}
}