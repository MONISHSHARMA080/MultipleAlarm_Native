package com.example.trying_native

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.trying_native.ui.theme.Trying_nativeTheme

class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (Settings.canDrawOverlays(this)) {
                // Permission granted, schedule the alarm
                scheduleAlarmInternal()
            } else {
                Log.d("AA", "Overlay permission denied")
            }
        }
    }

    private val exactAlarmPermissionLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        // Check the result to see if the permission was granted
        scheduleAlarmInternal() // Schedule the alarm anyway, as the system might still allow it
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Trying_nativeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    Button_for_alarm("Click Me", Modifier.padding(it)) { scheduleAlarm() }
                }
            }
        }
    }

    private fun scheduleAlarm() {
        // Check for SYSTEM_ALERT_WINDOW permission
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            overlayPermissionLauncher.launch(intent)
            return
        }

        // Check for SCHEDULE_EXACT_ALARM permission (only required for Android 12+)


        // If both permissions are granted, proceed with scheduling the alarm
        scheduleAlarmInternal()
    }

    private fun scheduleAlarmInternal() {
        Log.d("AA", "Clicked on the schedule alarm func")
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60000, pendingIntent)
    }
}

fun log_ran() {
    Log.d("MM", "hi there ----")
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Composable
fun Button_for_alarm(
    name: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = { onClick() }, // Trigger the passed function when the button is clicked
        modifier = modifier
    ) {
        Text(text = name)
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Trying_nativeTheme {
        Greeting("Android")
    }
}
