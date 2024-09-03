package com.example.trying_native

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.trying_native.ui.theme.Trying_nativeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Trying_nativeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Button_for_alarm("Click Me", Modifier.padding(innerPadding), ::log_ran) // Pass log_ran as a reference
                }
            }
        }
    }
}

fun log_ran() {
    Log.d("MM", "hi there ----")
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Composable
fun Button_for_alarm(
    name: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = { onClick() }, // Trigger the passed function when the button is clicked
        modifier = modifier
    ) {
        Text(text = name)
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Trying_nativeTheme {
        Greeting("Android")
    }
}
