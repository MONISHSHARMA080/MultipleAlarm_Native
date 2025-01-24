package com.example.trying_native

import android.app.AlarmManager
import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.trying_native.testHelperFile.E2ETestHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SeeIfAlarmsAreCancelledTest{
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val coroutineScope = TestScope()
    private val helperClass = E2ETestHelper()

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()


    @Test fun  seeIfCancellingAlarmWorks() = runTest{
        val activityContext = composeTestRule.activity

        // Test parameters
        val startInMin = 15
        val endMin =35
        val freqToSkipAlarm = 3
        val expectedAlarms = helperClass.expectedAlarmToBePlayed(endMin, startInMin, freqToSkipAlarm)

        logD("expected alarm to played is $expectedAlarms")
        // Schedule the alarms

        val exception = helperClass.scheduleMultipleAlarmHelper(activityContext, context, startInMin, endMin, freqToSkipAlarm, coroutineScope)
        assertNull("Error scheduling alarms: $exception", exception)
        delay( 4000L)

        var pendingIntentList =helperClass.getAllPendingIntentsOfAllAlarms(startInMin, endMin, freqToSkipAlarm, context, false)
        helperClass.printPendingIntents(pendingIntentList)
        logD("freq in the long is ${freqToSkipAlarm.toLong()}")
        logD("the pending intents list is ${pendingIntentList.size}, expected alarms are $expectedAlarms")
        assertTrue("the pending alarm list should have been 0 as all the alarms are cancelled", pendingIntentList.size == expectedAlarms)

//        assertNotNull("wait here",null,)

        cancelAlarmByCancelingPendingIntent(
            startTime = helperClass.getTheCalenderInstanceAndSkipTheMinIn(startInMin).timeInMillis,
            endTime =  helperClass.getTheCalenderInstanceAndSkipTheMinIn(endMin).timeInMillis,
            frequency_in_min = freqToSkipAlarm.toLong() *  60000 , alarmDao = helperClass.getAlarmDao(context),
            alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager, context_of_activity =composeTestRule.activity ,
            delete_the_alarm_from_db = true
        )
        logD("\n\n --- after cancelling the alarm ---\n\n")
        pendingIntentList =helperClass.getAllPendingIntentsOfAllAlarms(startInMin, endMin, freqToSkipAlarm, context, true)
        helperClass.printPendingIntents(pendingIntentList)
        logD("the pending intents list is ${pendingIntentList.size}, expected alarms are $expectedAlarms")
        assertTrue("the pending alarm list should have been 0 as all the alarms are cancelled", pendingIntentList.isEmpty())
//        run the cancel func and see before and after that if the pending intent is cancelled
    }

}