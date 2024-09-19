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