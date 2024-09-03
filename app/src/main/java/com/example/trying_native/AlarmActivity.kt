package com.example.trying_native

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.trying_native.ui.theme.Trying_nativeTheme

class AlarmActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("AA","in the alarm activity")
        setContent {
            Trying_nativeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    Text("Hello", modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}