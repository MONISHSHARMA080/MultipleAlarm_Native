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

    enum class AutoStartState {
        ENABLED,
        DISABLED,
        NO_INFO,
        UNEXPECTED_RESULT
    }

    // Check if the device is Xiaomi/MIUI
    fun isXiaomiDevice(): Boolean {
        return Build.MANUFACTURER.lowercase() == "xiaomi" ||
                Build.BRAND.lowercase() == "xiaomi" ||
                Build.BRAND.lowercase() == "redmi"
    }

    // Get autostart state
    fun getAutoStartState(): AutoStartState {
        if (!isXiaomiDevice()) {
            return AutoStartState.NO_INFO
        }

        try {
            val pm = context.packageManager
            val intent = Intent()
            val miuiSettingsComponent = ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )

            intent.component = miuiSettingsComponent

            // Check if MIUI security center exists
            val securityCenterExists = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()

            if (!securityCenterExists) {
                return AutoStartState.NO_INFO
            }

            // Use contentResolver to check autostart status
            val uri = Uri.parse("content://com.miui.securitycenter.autostart/state")
            val projection = arrayOf("package_name", "state")
            val selection = "package_name=?"
            val selectionArgs = arrayOf(context.packageName)

            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val stateIndex = cursor.getColumnIndex("state")
                    if (stateIndex != -1) {
                        val state = cursor.getInt(stateIndex)
                        return if (state == 1) {
                            AutoStartState.ENABLED
                        } else {
                            AutoStartState.DISABLED
                        }
                    }
                }
            }

            return AutoStartState.UNEXPECTED_RESULT
        } catch (e: Exception) {
            e.printStackTrace()
            return AutoStartState.NO_INFO
        }
    }

    // Check if autostart is enabled
    fun isAutoStartEnabled(checkBatteryOptimization: Boolean = true): Boolean {
        if (!isXiaomiDevice()) {
            return true
        }

        val state = getAutoStartState()

        if (state == AutoStartState.NO_INFO || state == AutoStartState.UNEXPECTED_RESULT) {
            // Fallback to checking battery optimization
            return if (checkBatteryOptimization) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                powerManager.isIgnoringBatteryOptimizations(context.packageName)
            } else {
                false
            }
        }

        return state == AutoStartState.ENABLED
    }

    // Request autostart permission
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

    fun askForBackgroundActivityPermissionOnMIUI(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            context.ProtoDataStore.data.collect { preferences ->
                logD("Background permission is: ${preferences.backgroundAutostartPremission}")
                val autoStartEnabled = isAutoStartEnabled()
                logD("Autostart permission status: $autoStartEnabled")

                if (!preferences.backgroundAutostartPremission) {
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
