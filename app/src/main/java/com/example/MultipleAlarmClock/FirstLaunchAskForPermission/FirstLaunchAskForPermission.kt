package com.coolApps.MultipleAlarmClock.FirstLaunchAskForPermission

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import com.coolApps.MultipleAlarmClock.logD
import androidx.core.content.edit

class FirstLaunchAskForPermission(private val context: Context) {
    private val prefsName = "AppPreferences"
    private val isFirstLaunchKey = "IS_FIRST_LAUNCH"
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }

    fun checkAndRequestPermissions() {
        logD("here in the check andRequest func")
//        if (isFirstLaunch()) {
//            askForNotificationPermission()
//            ifMiuiGetBgAutoStartPermission().onFailure { exception -> logD("Error in checking if device is XIAOMI and it is $exception");return }
//            if (this.doWeHavePermissionForNotification()) setFirstLaunchComplete()
//        }
        logD("here in the check andRequest func")

        // Ask for permission if it's first launch OR we don't have permission
        if (isFirstLaunch() || !doWeHavePermissionForNotification()) {
            askForNotificationPermission()
            ifMiuiGetBgAutoStartPermission().onFailure { exception ->
                logD("Error in checking if device is XIAOMI and it is $exception")
            }

            // Mark first launch as complete only on first launch
            if (isFirstLaunch()) {
                setFirstLaunchComplete()
            }
        }

    }

    fun checkIfWeHaveNotificationPermissionElseMarkitFalse(){
        if (!this.doWeHavePermissionForNotification()){
            this.prefs.edit { putBoolean(isFirstLaunchKey, false) }
        }
    }

    private fun ifMiuiGetBgAutoStartPermission(): Result<Unit>{
        return runCatching {
            val brand = Build.BRAND ?: return Result.failure(Exception("Can't find brand of the device "))
            val brandLowercase = brand.lowercase()
            logD("brand we have is $brandLowercase ($brand)")
            if (brandLowercase == "xiaomi" || brandLowercase == "redmi" || brandLowercase == "poco") {
                logD(" we have a miui device($brand) and we need to ask for bg autostart")
                val intent =Intent().apply {
                    component = ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                }
                context.startActivity(intent)
            }
        }
    }

    private fun doWeHavePermissionForNotification(): Boolean{
        val notificationPermission =ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        logD("here in the notification func and the post notification permission is -->$notificationPermission")
         return  notificationPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun askForNotificationPermission() {
        if (!this.doWeHavePermissionForNotification()) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1
            )
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        logD("checking for the full screen intent")
        // FSI management was introduced/tightened in API 34+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (!notificationManager.canUseFullScreenIntent()) {
                logD("FSI Permission denied. Redirecting user to settings.")
                // Intent to take the user directly to the "Manage Full Screen Intents" settings page
                val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.fromParts("package", context.packageName, null)
//                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                logD("asked and launched the FSI intent ")
            }else{
                logD("WE can use full screen intent")
            }
        }
    }

     fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(isFirstLaunchKey, true)
    }

    private fun setFirstLaunchComplete() {
        prefs.edit { putBoolean(isFirstLaunchKey, false) }
    }
}