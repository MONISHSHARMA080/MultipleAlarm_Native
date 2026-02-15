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
import androidx.core.view.WindowCompat
import com.example.trying_native.Components_for_ui_compose.AlarmPickerScreen
import com.example.trying_native.components_for_ui_compose.AlarmContainer

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    try {
      enableEdgeToEdge()
      setContent {
        MaterialTheme(colorScheme = dynamicDarkColorScheme(this)) {
          AlarmPickerScreen(null) {}
//          AlarmContainer( alarmManager, this@MainActivity, )
        }
      }
    } catch (e: Exception) {
      logD(" \n\n\n\n\n\n [FATAL] --> error occurred in the onCreate, and it is ${e}\n}")
    }
  }

}

fun logD(message: String): Unit {
  Log.d("AAAAA", message)
}

