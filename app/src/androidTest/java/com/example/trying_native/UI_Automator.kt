package com.example.trying_native


import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.rule.ActivityTestRule
import org.junit.Assert.*
import android.content.Context
import android.content.Intent

@RunWith(AndroidJUnit4::class)
class AlarmActivityTest {
    private lateinit var device: UiDevice
    private lateinit var context: Context
    private val LAUNCH_TIMEOUT = 5000L
    private val PACKAGE_NAME = "your.package.name" // Replace with your app's package name

    @get:Rule
    val activityRule = ActivityTestRule(AlarmActivity::class.java, true, false)

    @Before
    fun setUp() {
        // Initialize UiDevice instance
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        context = InstrumentationRegistry.getInstrumentation().targetContext

        // Start from home screen
        device.pressHome()

        // Wait for launcher
        val launcherPackage = device.launcherPackageName
        assertNotNull(launcherPackage)
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT)
    }


    @Test
    fun testAlarmActivityLaunch() {
        // Launch the activity
        val intent = Intent(context, AlarmActivity::class.java)
        activityRule.launchActivity(intent)

        // Verify the activity is launched and screen is turned on
        assertTrue(device.isScreenOn)

//        // Wait for the time display to be visible
//        val timeDisplay = device.wait(
//            Until.findObject(By.res(PACKAGE_NAME, "time_display")),
//            LAUNCH_TIMEOUT
//        )
//        assertNotNull("Time display should be visible", timeDisplay)
        val timeDisplay = device.findObject(UiSelector().description("time_display_text"))
        assertTrue("text that shows the time should be visible ", timeDisplay.exists())
        // Check if cancel button exists
        val cancelButton = device.findObject(UiSelector().text("Cancel alarm"))
        assertTrue("Cancel button should be visible", cancelButton.exists())

        // Test cancel button click
        cancelButton.click()

        // Verify activity is finished
        Thread.sleep(1000) // Give time for activity to finish
        val currentPackage = device.currentPackageName
        assertNotEquals("Activity should be finished", PACKAGE_NAME, currentPackage)
    }

//
//    @Test
//    fun testWakeLockAndScreenFlags() {
//        val intent = Intent(context, AlarmActivity::class.java)
//        activityRule.launchActivity(intent)
//
//        // Verify screen is on and locked
//        device.sleep()
//        Thread.sleep(3000)
//        logD(device.toString()+"----${device.isScreenOn}")
//
//        assertTrue("Screen should be on", device.isScreenOn)
//
//        // Try to put device to sleep
//        device.sleep()
//        Thread.sleep(3000)
//
//        // Verify screen stays on
//        assertTrue("Screen should stay on due to wake lock", device.isScreenOn)
//    }

    @Test
    fun testMediaPlayback() {
        val intent = Intent(context, AlarmActivity::class.java)
        activityRule.launchActivity(intent)

        // Wait for media playback to start
        Thread.sleep(1000)

        // Check if audio is playing (requires MODIFY_AUDIO_SETTINGS permission)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        assertTrue("Media should be playing", audioManager.isMusicActive)

        // Click cancel buttoxn
        val cancelButton = device.findObject(UiSelector().text("Cancel alarm"))
        cancelButton.click()

        // Wait for cleanup
        Thread.sleep(1000)

        // Verify audio stopped
        assertFalse("Media should stop playing", audioManager.isMusicActive)
    }
}