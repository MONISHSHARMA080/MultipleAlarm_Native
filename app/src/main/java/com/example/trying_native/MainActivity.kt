package com.example.trying_native

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.trying_native.ui.theme.Trying_nativeTheme
import androidx.room.Room
import com.example.trying_native.BackGroundAutoStartHelper.BackGroundAutostartPermissionHelper
import com.example.trying_native.components_for_ui_compose.AlarmContainer
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmDatabase
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig

class MainActivity : ComponentActivity() {

    companion object {
        const val POSTHOG_API_KEY = "phc_HKK1ZuTn3bjPFLZs0yA8ApLpgPt48JvrB614zbOduFQ"
        const val POSTHOG_HOST = "https://us.i.posthog.com"
    }

    private val overlayPermissionLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (Settings.canDrawOverlays(this)) {
                // Permission granted, schedule the alarm

                permissionToScheduleAlarm()
            } else {
                logD( "Overlay permission denied")
            }
        }
    }


    private val exactAlarmPermissionLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        // Check the result to see if the permission was granted
        permissionToScheduleAlarm() // Schedule the alarm anyway, as the system might still allow it
    }



    var startHour_after_the_callback: Int? = null
    var startMin_after_the_callback: Int? = null
    var endHour_after_the_callback: Int? = null
    var endMin_after_the_callback: Int? = null
    var date_after_the_callback: Long? = null
    var freq_after_the_callback: Long? = null
//    var selected_date_for_display :String? = null


    private lateinit var alarmDao: AlarmDao

//    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "AlarmData")
//    val database = DatabaseManager.getInstance(applicationContext)
 //-form docsd

val activity_context = this

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        val config = PostHogAndroidConfig(
            apiKey = POSTHOG_API_KEY,
            host = POSTHOG_HOST
        )
        config.sessionReplay = true
        // choose whether to mask images or text
        config.sessionReplayConfig.maskAllImages = false
        config.sessionReplayConfig.maskAllTextInputs = true
        // screenshot is disabled by default
        // The screenshot may contain sensitive information, use with caution
        config.sessionReplayConfig.screenshot = true

        PostHogAndroid.setup(this, config)

//        lifecycleScope.launch(Dispatchers.IO) {

        super.onCreate(savedInstanceState)
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        setContent {
            var a by remember { mutableStateOf(false) }
            Trying_nativeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
                    Column(modifier = Modifier.padding(paddingValues)) {

                      AlarmContainer(getAlarmDao(), alarmManager, activity_context, askUserForPermissionToScheduleAlarm = { permissionToScheduleAlarm() } )
                    }
                }
            }
        }
    }

    private fun areAllFieldsNotFilled(): Boolean {
        var freq= freq_after_the_callback
        logD("changing freq to null->$freq_after_the_callback")

        if (freq?.toInt() == 0){
            logD("changing freq to null->$freq")
            freq = null
        }else{
            freq = null
        }
        return startHour_after_the_callback == null && startMin_after_the_callback == null &&
                endHour_after_the_callback == null && endMin_after_the_callback == null &&
                date_after_the_callback == null && freq == null
    }
    private fun areSomeFieldNotFilled(): Boolean {
        return startHour_after_the_callback == null || startMin_after_the_callback == null ||
                endHour_after_the_callback == null || endMin_after_the_callback == null ||
                date_after_the_callback == null || freq_after_the_callback == null
    }

    private fun emptyFieldAndTheirName():( String){
        // this func is used when all the field are not null, so maybe we should return which field is null
       if(startHour_after_the_callback == null){
           return "Start time"
       }
        else if(startMin_after_the_callback == null){
            return "Start time"
        }
        else if(endHour_after_the_callback == null){
          return "End time"
       }
        else if (endMin_after_the_callback == null){
            return "End time"
       }
        else if (date_after_the_callback == null){
            return  "Date"
       }
        else{
            return "Frequency"
        }
    }

    private  fun getAlarmDao(): AlarmDao {
    val db = Room.databaseBuilder(
        applicationContext,
        AlarmDatabase::class.java, "alarm-database"
    ).build()
    return db.alarmDao()
}

    private fun scheduleAlarm(triggerTime: Long, alarmManager:AlarmManager) {
        logD( "Clicked on the schedule alarm func")
        var triggerTime_1 = triggerTime
        val intent = Intent(this, AlarmReceiver::class.java)
        intent.putExtra("last_alarm_info1","from the schedule alarm function")
        logD("Trigger time in the scheduleAlarm func is --> $triggerTime_1 ")
        intent.putExtra("triggerTime", triggerTime_1)
        val pendingIntent = PendingIntent.getBroadcast(this, triggerTime.toInt(), intent, PendingIntent.FLAG_MUTABLE)
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime_1, pendingIntent)
    }
    // this should fix it as I changed FLAG_IMUTABLE to FLAG_MUTABLE

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
    Log.d("AAA","-->$message")
}