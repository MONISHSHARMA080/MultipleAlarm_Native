package com.example.trying_native
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AA","in the alarm receiver func and here is the intent --> $intent")
        val intent_1 = Intent(context, AlarmActivity::class.java)
        intent_1.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent_1)
    }
}