package com.example.trying_native.permission

import android.app.ActivityManager
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.trying_native.data.ProtoDataStore
import com.example.trying_native.logD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AskBackgroundAutoStartPermissionMI(private val context: Context) {
    fun isXiaomiDevice(): Boolean {
        return Build.MANUFACTURER.lowercase() == "xiaomi" ||
                Build.BRAND.lowercase() == "xiaomi" ||
                Build.BRAND.lowercase() == "redmi"
    }

    fun isAutoStartEnabled(checkBatteryOptimization: Boolean = true): Boolean {
        if (!isXiaomiDevice()) {
            return true
        }

        return try {
            // Try to check through package manager first
            val pm = context.packageManager
            val intent = Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            }

            val securityCenterExists = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()
            if (!securityCenterExists) {
                return checkBatteryOptimization && checkBatteryOptimization()
            }

            // If we can't directly query the content provider (due to permissions),
            // check the app's background state
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningProcesses = activityManager.runningAppProcesses

            val isBackgroundRestricted = runningProcesses?.any {
                it.processName == context.packageName &&
                        it.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE
            } ?: false

            if (isBackgroundRestricted) {
                return false
            }

            // If all else fails, check battery optimization
            checkBatteryOptimization && checkBatteryOptimization()
        } catch (e: Exception) {
            e.printStackTrace()
            checkBatteryOptimization && checkBatteryOptimization()
        }
    }

    private fun checkBatteryOptimization(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    // Your original implementation
    fun requestAutostartPermission(): Boolean {
        if (!isXiaomiDevice()) {
            return false
        }
        return try {
            // Main intent for MIUI Security Center
            val mainIntent = Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            // Try to open MIUI autostart settings
            try {
                context.startActivity(mainIntent)
                return true
            } catch (e: Exception) {
                // If MIUI settings fails, try battery optimization settings
                val batterySaverIntent = Intent().apply {
                    action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(batterySaverIntent)
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Your original implementation
    fun askForBackgroundActivityPermissionOnMIUI(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            context.ProtoDataStore.data.collect { preferences ->
                logD("Background permission is: ${preferences.backgroundAutostartPremission}")
                val autoStartEnabled = isAutoStartEnabled()
                logD("Autostart permission status: $autoStartEnabled")

                if (!autoStartEnabled) {
                    val requested = requestAutostartPermission()
                    logD("Updating backgroundAutostartPermission to: $requested")
                    context.ProtoDataStore.updateData { currentData ->
                        currentData.toBuilder()
                            .setBackgroundAutostartPremission(requested)
                            .build()
                    }
                }
            }
        }
    }
}
