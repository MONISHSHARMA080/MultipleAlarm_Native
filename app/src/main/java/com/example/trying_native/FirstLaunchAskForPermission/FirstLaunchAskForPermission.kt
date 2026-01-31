package com.example.trying_native.FirstLaunchAskForPermission

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.trying_native.BackGroundAutoStartHelper.BackGroundAutostartPermissionHelper
import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.trying_native.logD
import io.reactivex.Notification

class FirstLaunchAskForPermission(private val context: Context) {
    private val prefsName = "AppPreferences"
    private val isFirstLaunchKey = "IS_FIRST_LAUNCH"
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }

    fun checkAndRequestPermissions() {
        logD("here in the check andRequest func")
        if (isFirstLaunch()) {
            askForNotificationPermission()
            BackGroundAutostartPermissionHelper.getAutoStartPermission(context)
            BackGroundAutostartPermissionHelper.requestDisableBatteryOptimization(context)
            setFirstLaunchComplete()
            logD("about to get out ")
        }
    }

    private fun askForNotificationPermission() {
        logD("here in the notification func and the post notification permission is -->${
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        }")
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
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
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }else{
                logD("WE can use full screen intent")
            }
        }
    }

    private fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(isFirstLaunchKey, true)
    }

    private fun setFirstLaunchComplete() {
        prefs.edit().putBoolean(isFirstLaunchKey, false).apply()
    }
}
