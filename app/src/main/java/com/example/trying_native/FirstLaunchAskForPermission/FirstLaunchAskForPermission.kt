package com.example.trying_native.FirstLaunchAskForPermission

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.trying_native.BackGroundAutoStartHelper.BackGroundAutostartPermissionHelper
import android.Manifest

import androidx.core.app.ActivityCompat
import com.example.trying_native.logD

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
    }

    private fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(isFirstLaunchKey, true)
    }

    private fun setFirstLaunchComplete() {
        prefs.edit().putBoolean(isFirstLaunchKey, false).apply()
    }
}
