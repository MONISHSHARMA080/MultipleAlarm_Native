package com.example.trying_native.FirstLaunchAskForPermission

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.trying_native.BackGroundAutoStartHelper.BackGroundAutostartPermissionHelper
import android.Manifest
import androidx.fragment.app.FragmentActivity
import com.example.trying_native.logD
import kotlin.math.log


class FirstLaunchAskForPermission(private val context: Context) {
    private val prefsName = "AppPreferences"
    private val isFirstLaunchKey = "IS_FIRST_LAUNCH"

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    fun checkAndRequestPermissions() {
        logD("here in the checkandRequest func")
        if (isFirstLaunch()) {
            // Request autostart permission
            BackGroundAutostartPermissionHelper.getAutoStartPermission(context)

            // Request battery optimization permission
            BackGroundAutostartPermissionHelper.requestDisableBatteryOptimization(context)

            askFornotificationPermission()

            // Mark first launch as complete
            setFirstLaunchComplete()
        }
    }

    private fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(isFirstLaunchKey, true)
    }
    private  fun askFornotificationPermission(){
        logD("here in the notification func")
        when (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        )) {
            PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted, do nothing
                logD("the notification permission is already given")
            }
            else -> {
                // Request the permission but don't wait for result
                if (context is FragmentActivity) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

    }

    private fun setFirstLaunchComplete() {
        prefs.edit().putBoolean(isFirstLaunchKey, false).apply()
    }

}