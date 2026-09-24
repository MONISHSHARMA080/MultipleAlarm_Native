import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePickerColors

@OptIn(ExperimentalMaterial3Api::class)
fun checkMethods(colors: TimePickerColors) {
    // We will just let the compiler tell us what is available or we can use reflection if it was a real Kotlin environment.
    // Wait, let's just grep through the decompiled TimePickerColors if we can.
}
