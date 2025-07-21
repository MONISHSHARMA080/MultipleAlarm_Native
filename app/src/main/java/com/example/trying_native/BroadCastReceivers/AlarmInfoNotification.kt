package com.example.trying_native.BroadCastReceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.trying_native.notification.NotificationBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmInfoNotification: BroadcastReceiver()  {
    public fun DisplayAlarmsMetadataInNotification(){

    }

    override fun onReceive(context: Context?, intent: Intent?) {

    }

    private  fun getTimeInHumanReadableFormatProtectFrom0Included(t:Long): String{
        if (t == 0L) return "--the time here(probablyFromTheIntent) is 0--"
        return SimpleDateFormat("yyyy-MM-dd h:mm:ss a", Locale.getDefault()).format(Date(t))
    }

}