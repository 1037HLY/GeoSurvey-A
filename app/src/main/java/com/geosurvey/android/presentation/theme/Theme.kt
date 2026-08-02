package com.geosurvey.android.presentation.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 毛玻璃效果颜色
val GlassColor = Color.White.copy(alpha = 0.7f)
val GlassColorDark = Color(0xFF1A2332).copy(alpha = 0.6f)
val GlassBorder = Color.White.copy(alpha = 0.2f)

// 主色调
val PrimaryBlue = Color(0xFF0EA5E9)
val PrimaryBlueDark = Color(0xFF0284C7)
val PrimaryBlueLight = Color(0xFF7DD3FC)

val SecondaryGreen = Color(0xFF10B981)
val SecondaryGreenDark = Color(0xFF059669)

val AccentPurple = Color(0xFF8B5CF6)
val AccentOrange = Color(0xFFF59E0B)
val ErrorRed = Color(0xFFEF4444)

// 毛玻璃阴影
val GlassShadow = Color.Black.copy(alpha = 0.1f)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    secondary = SecondaryGreen,
    onSecondary = Color.White,
    tertiary = AccentPurple,
    surface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurface = Color(0xFF0F172A),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    error = ErrorRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    secondary = SecondaryGreen,
    onSecondary = Color.White,
    tertiary = AccentPurple,
    surface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun GeoSurveyTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
