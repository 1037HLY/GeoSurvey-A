package com.geosurvey.android.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0EA5E9),
    secondary = Color(0xFF10B981),
    tertiary = Color(0xFF8B5CF6),
    surface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurface = Color(0xFF0F172A),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A)
)

@Composable
fun GeoSurveyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
