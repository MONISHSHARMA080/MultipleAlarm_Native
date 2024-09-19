package com.example.trying_native

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.trying_native.Components_for_ui_compose.AlarmContainer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TestWithEspresso {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun clickRoundPlusIcon_showsDialog() {
        // Set the Compose content


        // Perform click on the RoundPlusIcon
//        composeTestRule.onNodeWithTag("RoundPlusIcon").performClick()
//
//        // Verify if the dialog appears
//        composeTestRule.onNodeWithTag("DialogToAskUserAboutAlarm").assertIsDisplayed()
    }
}
