//package com.example.trying_native
//
//import android.annotation.SuppressLint
//import android.app.AlarmManager
//import android.content.Context
//import android.content.Context.ALARM_SERVICE
//import androidx.activity.compose.setContent
//import androidx.compose.material3.Scaffold
//import androidx.compose.ui.test.junit4.createAndroidComposeRule
//import androidx.compose.ui.test.onChild
//import androidx.compose.ui.test.onNodeWithContentDescription
//import androidx.compose.ui.test.onNodeWithTag
//import androidx.compose.ui.test.onNodeWithText
//import androidx.compose.ui.test.onRoot
//import androidx.compose.ui.test.performClick
//import androidx.compose.ui.test.performTextInput
//import androidx.compose.ui.test.printToLog
//import androidx.room.Room
//import androidx.test.espresso.contrib.PickerActions
//import androidx.test.ext.junit.runners.AndroidJUnit4
//import com.example.trying_native.Components_for_ui_compose.AlarmContainer
//import com.example.trying_native.dataBase.AlarmDatabase
//import com.example.trying_native.ui.theme.Trying_nativeTheme
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.runBlocking
//import org.junit.Rule
//import org.junit.Test
//import org.junit.runner.RunWith
//import java.text.SimpleDateFormat
//import java.util.Calendar
//import java.util.Locale
//
//
//@RunWith(AndroidJUnit4::class)
//class MainActivityTest {
//
//    // This creates a rule to launch MainActivity
//    @get:Rule
//    val composeTestRule = createAndroidComposeRule<MainActivity>()
//
//    @Test
//    fun appOpensSuccessfullyAndAlarmContainerExists() {
//        logD("entered the ")
//        composeTestRule.onNodeWithTag("AlarmContainer").assertExists()
//    }
//
//    @Test
//    fun clickOnPlusIconToEnterAnAlarm(){
//        var  time = Calendar.getInstance().apply { set(Calendar.MINUTE, Calendar.MINUTE+10) }
//        var hourToClickOn = time.get(Calendar.HOUR)
//        composeTestRule.onNodeWithTag("RoundPlusIcon").performClick()
//
//        logD("clicked on ")
//
//        // runBlocking { delay(3000)  }
//        composeTestRule.onNodeWithTag("DialogToAskUserAboutAlarm").assertExists()
//        logD("about to exit --")
////        PickerActions.setTime(12, time.get(Calendar.MINUTE))
////        composeTestRule.onNodeWithTag("TimePickerTestTag").performTextInput("12:30")
//
//        // choose the current time as start time
//        runBlocking { delay(1000) }
//
//        composeTestRule.onNodeWithTag("NextButtonInTimePicker").assertExists()
//        composeTestRule.onNodeWithTag("NextButtonInTimePicker").performClick()
//
//        // choose the end time as 10 min after
//
//        logD("hourToClickOn-->$hourToClickOn")
//        PickerActions.setTime(12, time.get(Calendar.MINUTE))
//        composeTestRule.onNodeWithText("Ending time").assertExists()
//        //        logD("---------${composeTestRule.onRoot()}-----------")
//        composeTestRule.onNodeWithTag("TimePickerTestTag").printToLog("TimePickerHierarchy")
//        logD("--------------------")
//        runBlocking { delay(1000) }
////        var a = composeTestRule.onNodeWithContentDescription("${hourToClickOn+2} o'clock")
//        var a = time.get(Calendar.HOUR) +2
//        logD("||---$a o'clock|[[]]][[[][][[]")
//
//        composeTestRule.onNodeWithContentDescription("$a o'clock").performClick()
//
//        // clicking on the minute
//        var minute_now = Calendar.getInstance().get(Calendar.MINUTE)
//        composeTestRule.onNodeWithText(minute_now.toString())
//
////
////        val amPm = time.get(Calendar.AM_PM)
////
////        val result = if (amPm == Calendar.AM) "AM" else "PM"
////        composeTestRule.onNodeWithText("${result}").assertExists()
////        composeTestRule.onNodeWithTag("${result}").performClick()
////        composeTestRule.onNodeWithTag("${time.get(Calendar.MINUTE)}").performClick()
////        composeTestRule.onNodeWithTag("${time.get(Calendar.MINUTE)}").assertExists()
//
//        composeTestRule.onNodeWithTag("NextButtonInTimePicker").assertExists()
//        composeTestRule.onNodeWithTag("NextButtonInTimePicker").performClick()
//        composeTestRule.onNodeWithTag("datePicker").assertExists()
//        logD("date is ${time.get(Calendar.DATE)}")
//
//        composeTestRule.onNodeWithTag("datePicker").printToLog("datePickerH")
//        val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.ENGLISH)
//        val formattedDate = dateFormat.format(time.time)
//        logD("++--++"+formattedDate)
//        composeTestRule.onNodeWithTag("datePicker").onChild
//            onNodeWithText("${time.get(Calendar.DAY_OF_WEEK)}, ${time.get(Calendar.MONTH)} ${time.get(Calendar.DATE)}, ${time.get(Calendar.YEAR)}").performClick()
//
//        runBlocking { delay(32000)  }
//        logD("$hourToClickOn")
//
//    }
//
//}


import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.trying_native.Components_for_ui_compose.AlarmContainer
import com.example.trying_native.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

@RunWith(AndroidJUnit4::class)
class AlarmContainerTest {
//
//    @get:Rule
//    val composeTestRule = createComposeRule()
//
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testSetAlarmWithSpecificTimeDateAndFrequency() {
        // Setup the content with the composable to test
//        composeTestRule.setContent {
//            AlarmContainer(
//                AlarmDao = FakeAlarmDao(),  // Provide a fake or mock AlarmDao
//                alarmManager = FakeAlarmManager(),  // Provide a mock AlarmManager
//                context_of_activity = FakeActivityContext()  // Provide a fake context for testing
//            )
//        }

        // Find the RoundPlusIcon and click it to open the alarm dialog
        composeTestRule.onNodeWithTag("RoundPlusIcon").performClick()

        // Ensure the alarm dialog is displayed
        composeTestRule.onNodeWithTag("DialogToAskUserAboutAlarm").assertIsDisplayed()

        // Start Time: Set the time to 5 minutes from now
        val currentTime = Calendar.getInstance()
        val startHour = currentTime.get(Calendar.HOUR_OF_DAY)
        val startMinute = (currentTime.get(Calendar.MINUTE) + 5) % 60
        runBlocking { delay(5000) }
        composeTestRule.onNodeWithTag("TimePickerTestTag").assertExists("TimePickerTestTag does not exists")
        composeTestRule.onNodeWithTag("TimePickerTestTag").printToLog("TimePickerHierarchy")
        setTimeInTimePicker(startHour, startMinute)

        // Click "Next" to move to the End Time selection
        composeTestRule.onNodeWithTag("NextButtonInTimePicker").performClick()

        // End Time: Set the time to 1 hour from now
        val endHour = (currentTime.get(Calendar.HOUR_OF_DAY) + 1) % 24
        val endMinute = currentTime.get(Calendar.MINUTE)
        setTimeInTimePicker(endHour, endMinute)

        // Click "Next" to move to the Date selection
        composeTestRule.onNodeWithTag("NextButtonInTimePicker").performClick()

        // Select today's date (DatePicker will already show today's date by default)
        composeTestRule.onNodeWithText("Confirm").performClick()

        // Frequency: Set the frequency to 1 minute
        composeTestRule.onNodeWithTag("EnterFrequencyField").performTextInput("1")

        // Click the "Set the alarm" button to confirm
        composeTestRule.onNodeWithText("Set the alarm").performClick()

        // Optionally, you could add assertions to verify if the alarm has been set with the correct values.
    }

    // Helper function to set the time in TimePicker
    private fun setTimeInTimePicker(hour: Int, minute: Int) {
        // Interact with the time picker (assuming the tag is "TimePickerTestTag")
        composeTestRule.onNodeWithTag("TimePickerTestTag").performSemanticsAction(SemanticsActions.SetProgress) { progress ->
            val timePickerHourProgress = (hour % 12).toFloat() / 12f  // For the hour dial
            val timePickerMinuteProgress = minute.toFloat() / 60f    // For the minute dial
            progress(timePickerHourProgress)  // Set hour
            progress(timePickerMinuteProgress)  // Set minute
        }
    }
}
