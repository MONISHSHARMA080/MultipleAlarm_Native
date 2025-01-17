
import android.app.AlarmManager
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.trying_native.AlarmActivity
import com.example.trying_native.MainActivity
import com.example.trying_native.components_for_ui_compose.scheduleMultipleAlarms
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import com.example.trying_native.testHelperFile.E2ETestHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import org.junit.Assert

@RunWith(AndroidJUnit4::class)
class AlarmContainerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val activityContext = ApplicationProvider.getApplicationContext<ComponentActivity>()
    private val coroutineScope= TestScope()
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val alarmActivityRule = createAndroidComposeRule<AlarmActivity>()

    @Test
    fun testIfTheAlarmSetBySetMultipleAlarmRunsTillTheEnd(){
        // this one does not test the ui for the user, rather it is to see that if the alarms set by fun setMultipleAlarms
        // executes/runs, and see that the class is called appropriate number of times
        val helperClass = E2ETestHelper()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmDao = helperClass.getAlarmDao(context)
        val startInMIn= 3
        val endMin = 20
        val freqToSkipAlarm = 2
         coroutineScope.launch {
             val exception=   scheduleMultipleAlarms(alarmManager =alarmManager, activity_context = activityContext, alarmDao = alarmDao,
                calendar_for_start_time = helperClass.getTheCalenderInstanceAndSkipTheMinIn(startInMIn), calendar_for_end_time = helperClass.getTheCalenderInstanceAndSkipTheMinIn(endMin),
                date_in_long = helperClass.getDateInLong(), coroutineScope = coroutineScope, freq_after_the_callback = freqToSkipAlarm,
                selected_date_for_display = helperClass.getDateString(helperClass.getDateInLong()), is_alarm_ready_to_use = true, new_is_ready_to_use = false
            )
             if (exception != null){
                Assert.fail("error in scheduling alarm , the exception was --> $exception")
             }
        }
        // noe use adb to fast forward to the time and then see how many times the alarm activity gets called
    }

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
