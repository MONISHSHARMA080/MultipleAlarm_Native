package com.coolApps.MultipleAlarmClock.presentation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val ExpressiveAlarmShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
    extraExtraLarge = RoundedCornerShape(48.dp),
)

private val DefaultTypography = Typography()

val ExpressiveAlarmTypography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(fontWeight = FontWeight.Black),
    displayMedium = DefaultTypography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
    displaySmall = DefaultTypography.displaySmall.copy(fontWeight = FontWeight.Bold),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = DefaultTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = DefaultTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = DefaultTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = DefaultTypography.bodyLarge,
    bodyMedium = DefaultTypography.bodyMedium,
    bodySmall = DefaultTypography.bodySmall,
    labelLarge = DefaultTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = DefaultTypography.labelMedium.copy(fontWeight = FontWeight.Medium),
    labelSmall = DefaultTypography.labelSmall.copy(fontWeight = FontWeight.Medium),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveAlarmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = if (darkTheme) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = ExpressiveAlarmShapes,
        typography = ExpressiveAlarmTypography,
        content = content,
    )
}
