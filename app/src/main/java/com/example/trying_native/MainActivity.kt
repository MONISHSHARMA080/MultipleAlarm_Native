package com.example.trying_native

import android.app.AlarmManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.core.content.ContextCompat
import com.example.trying_native.Components_for_ui_compose.AlarmPickerScreen
import com.example.trying_native.components_for_ui_compose.AlarmContainer

class MainActivity : ComponentActivity() {

  private val alarmManager by lazy { getSystemService(ALARM_SERVICE) as AlarmManager }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    try {
      enableEdgeToEdge()
      setContent {
        MaterialTheme(colorScheme = dynamicDarkColorScheme(this)) {
//          AlarmPickerScreen(null, {alarmObject -> })
          AlarmContainer(  this@MainActivity )
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

