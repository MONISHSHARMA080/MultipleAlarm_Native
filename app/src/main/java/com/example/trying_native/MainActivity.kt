package com.example.trying_native

import android.app.AlarmManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.room.Room
import com.example.trying_native.components_for_ui_compose.AlarmContainer
import com.example.trying_native.dataBase.AlarmDatabase
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val alarmManager by lazy {getSystemService(ALARM_SERVICE) as AlarmManager}

    private val overlayPermissionLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            if (Settings.canDrawOverlays(this)) {
                // Permission granted, schedule the alarm
                permissionToScheduleAlarm()
            } else {
                logD( "Overlay permission denied")
            }
        }
    }

    private  val alarmDB by lazy {
         Room.databaseBuilder(
            applicationContext,
            AlarmDatabase::class.java, "alarm-database"
        ).build()
    }
    private  val alarmDao by lazy { alarmDB.alarmDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
            setContent {
                MaterialTheme(colorScheme = dynamicDarkColorScheme(this)){
                    AlarmContainer(alarmDao, alarmManager, this@MainActivity, askUserForPermissionToScheduleAlarm = { permissionToScheduleAlarm() } )
                }
            }
        }catch (e: Exception){
            logD(" \n\n\n\n\n\n [FATAL] --> error occurred in the onCreate, and it is ${e}\n}")
        }
    }


    private fun permissionToScheduleAlarm() {
        // Check for SYSTEM_ALERT_WINDOW permission
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri())
            overlayPermissionLauncher.launch(intent)
            return
        }
        // Check for SCHEDULE_EXACT_ALARM permission (only required for Android 12+)
        // If both permissions are granted, proceed with scheduling the alarm
        // scheduleAlarmInternal()
    }
}



fun logD(message:String):Unit{
    Log.d("AAAAA",message)
}