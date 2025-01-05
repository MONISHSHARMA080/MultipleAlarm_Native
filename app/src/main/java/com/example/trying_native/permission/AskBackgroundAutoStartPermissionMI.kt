package com.example.trying_native.permission

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.trying_native.data.ProtoDataStore
import com.example.trying_native.logD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AskBackgroundAutoStartPermissionMI(private val context: Context) {
    // Check if the device is Xiaomi/MIUI
    fun isXiaomiDevice(): Boolean {
        return Build.MANUFACTURER.lowercase() == "xiaomi" ||
                Build.BRAND.lowercase() == "xiaomi" ||
                Build.BRAND.lowercase() == "redmi"
    }

    // Check if autostart permission is granted
    fun hasAutostartPermission(): Boolean {
        if (!isXiaomiDevice()) {
            return true // Non-MIUI devices don't need this specific permission
        }

        return try {
            // Check if the app is added to protected apps list
            val pm = context.packageManager
            val intent = Intent()
            intent.component = ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )

            // If the security center activity exists and app can query it
            val securityCenterExists = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()

            if (!securityCenterExists) {
                // If security center doesn't exist, check battery optimization instead
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                return powerManager.isIgnoringBatteryOptimizations(context.packageName)
            }

            // For MIUI devices, check if app is in recent apps list
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val runningApps = am.runningAppProcesses
            val isRunning = runningApps?.any { it.processName == context.packageName } ?: false

            // If app is running in background, it likely has autostart permission
            isRunning
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
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

            // List of possible component paths
            val possibleComponents = listOf(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                ),
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                ),
                ComponentName(
                    "com.miui.security",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            )

            // Try each possible path
            for (component in possibleComponents) {
                try {
                    mainIntent.component = component
                    context.startActivity(mainIntent)
                    return true
                } catch (e: Exception) {
                    continue
                }
            }

            // If all specific intents fail, open battery optimization settings
            val batterySaverIntent = Intent().apply {
                action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(batterySaverIntent)
            true

        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun askForBackgroundActivityPermissionOnMIUI(context: Context){
        CoroutineScope(Dispatchers.Main).launch {
            context.ProtoDataStore.data.collect { preferences ->
                logD( "Background permission is : ${preferences.backgroundAutostartPremission}")
                val requestForBGAutoStart = AskBackgroundAutoStartPermissionMI(context)
                logD("does we have auto start permission --> ${requestForBGAutoStart.hasAutostartPermission()}")
                if ( !preferences.backgroundAutostartPremission){
                    val a =requestForBGAutoStart.requestAutostartPermission()
                    logD(" the  updating the  backgroundAutostartPermission to be ${a}")
                    context.ProtoDataStore.updateData {currentData ->
                        currentData.toBuilder().setBackgroundAutostartPremission(a).build()
                    }
                }
            }
        }
    }
}
