package com.example.trying_native
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        logD("in the alarm receiver func and here is the intent --> $intent")
        val intent_1 = Intent(context, AlarmActivity::class.java)
        val time_By_Me = intent.getExtras()?.getLong("triggerTime",-1000)
        val isMessagePresent = intent.getBooleanExtra("isMessagePresent", false)
        if (isMessagePresent==false){
            intent_1.putExtra("message", "")
            intent_1.putExtra("isMessagePresent", false)
        }else{
            intent.putExtra("isMessagePresent", true)
            val message = intent.getStringExtra("message")
            if (message ==""){
                intent_1.putExtra("message", "")
                intent_1.putExtra("isMessagePresent", false)
            }
            intent_1.putExtra("message", message)
            intent_1.putExtra("isMessagePresent", true)
        }
        val triggerTime = intent.getLongExtra("triggerTime", 0)
        logD("Trigger time in the alarm receiver's func is -->$triggerTime; intent -->${intent.extras} ; time_By_Me -->$time_By_Me ")
        intent_1.putExtra("triggerTime", triggerTime)
        intent_1.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        context.startActivity(intent_1)
    }
}
