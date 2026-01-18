package com.example.trying_native

import android.app.AlarmManager
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.core.net.toUri
import com.example.trying_native.components_for_ui_compose.AlarmContainer

class MainActivity : ComponentActivity() {

  private val alarmManager by lazy { getSystemService(ALARM_SERVICE) as AlarmManager }

  private val overlayPermissionLauncher =
          registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
              if (Settings.canDrawOverlays(this)) {
                // Permission granted, schedule the alarm
                permissionToScheduleAlarm()
              } else {
                logD("Overlay permission denied")
              }
            }
          }


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    try {
      enableEdgeToEdge()
      setContent {
        MaterialTheme(colorScheme = dynamicDarkColorScheme(this)) {
          AlarmContainer(
                  alarmManager,
                  this@MainActivity,
                  askUserForPermissionToScheduleAlarm = { permissionToScheduleAlarm() }
          )
        }
      }
    } catch (e: Exception) {
      logD(" \n\n\n\n\n\n [FATAL] --> error occurred in the onCreate, and it is ${e}\n}")
    }
  }

  private fun permissionToScheduleAlarm() {
    // Check for SYSTEM_ALERT_WINDOW permission
    if (!Settings.canDrawOverlays(this)) {
      val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())
      overlayPermissionLauncher.launch(intent)
      return
    }
    // Check for SCHEDULE_EXACT_ALARM permission (only required for Android 12+)
    // If both permissions are granted, proceed with scheduling the alarm
    // scheduleAlarmInternal()
  }
}

fun logD(message: String): Unit {
  Log.d("AAAAA", message)
}

