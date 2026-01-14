package com.example.trying_native

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.unit.dp
import com.example.trying_native.ui.theme.Trying_nativeTheme
import androidx.room.Room
import com.example.trying_native.components_for_ui_compose.AlarmContainer
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmDatabase

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

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Trying_nativeTheme {
                      AlarmContainer(getAlarmDao(), alarmManager, this@MainActivity, askUserForPermissionToScheduleAlarm = { permissionToScheduleAlarm() } )
                    }
        }
    }


    private  fun getAlarmDao(): AlarmDao {
    val db = Room.databaseBuilder(
        applicationContext,
        AlarmDatabase::class.java, "alarm-database"
    ).build()
    return db.alarmDao()
}


    private fun permissionToScheduleAlarm() {
        // Check for SYSTEM_ALERT_WINDOW permission
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
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