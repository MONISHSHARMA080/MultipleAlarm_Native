package com.example.trying_native

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trying_native.components_for_ui_compose.ALARM_ACTION
import com.example.trying_native.testHelperFile.E2ETestHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger


class TestAlarmReceiver : BroadcastReceiver() {

    companion object {
        private val intentReceived = AtomicInteger(0)
    }
    override fun onReceive(context: Context?, intent: Intent?) {
        logD("Intent received: ${intent?.action}")
        if (intent?.action == ALARM_ACTION) {
            intentReceived.incrementAndGet()
            logD("Intent count now: ${intentReceived.get()}")
        }
    }
    fun getIntentsReceived():Int{
        return  intentReceived.get()
    }

    fun resetIntents() {
        intentReceived.set(0)
    }
}

@RunWith(AndroidJUnit4::class)
class SeeIfBroadcastReceiverIsReceivingIntentTest {
    private val helperClass = E2ETestHelper()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val coroutineScope = TestScope()
    private val mockAlarmReceiverForTest = TestAlarmReceiver()
    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

//    @Before
//    fun setup() {
//        Dispatchers.setMain(testDispatcher)
//        mockAlarmReceiverForTest.resetIntents()
//    }
//
//    @After
//    fun teardown() {
//        Dispatchers.resetMain()
//    }

    @Test
    fun seeIfMyBroadCastReceiverGetsAppropriateIntents() = runTest {
        // Test parameters
        val startInMin = 0
        val endMin = 3
        val freqToSkipAlarm = 1
        val expectedAlarms = helperClass.expectedAlarmToBePlayed(endMin, startInMin, freqToSkipAlarm)

        logD("Expected alarms: $expectedAlarms")

        // Schedule and trigger alarms
        val exception = helperClass.scheduleMultipleAlarmHelper(
            composeTestRule.activity,
            context,
            startInMin,
            endMin,
            freqToSkipAlarm,
            coroutineScope,
            broadcastReceiverClass = mockAlarmReceiverForTest
        )

        assertNull("Error scheduling alarms: $exception", exception)

        helperClass.triggerPendingAlarms(context, startInMin, endMin, freqToSkipAlarm)

        // Use a combination of instrumentation idle sync and coroutine delay
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        delay(4000)
        // Advance virtual time if using TestScope
        testDispatcher.scheduler.advanceUntilIdle()

        logD(" intents received in the broadcast receiver: ${mockAlarmReceiverForTest.getIntentsReceived()}")
        assertEquals(
            "Expected $expectedAlarms alarms but received ${mockAlarmReceiverForTest.getIntentsReceived()}",
            expectedAlarms,
            mockAlarmReceiverForTest.getIntentsReceived()
        )
    }
}
