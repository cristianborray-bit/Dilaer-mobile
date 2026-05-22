package com.dilaer.mobile.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF1D7A46),
    onPrimary = Color.White,
    secondary = Color(0xFF2D5A7B),
    background = Color(0xFFF6F7F9),
    surface = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7BD49B),
    onPrimary = Color(0xFF003920),
    secondary = Color(0xFFA8C8E1),
    background = Color(0xFF101418),
    surface = Color(0xFF1B1F23),
)

@Composable
fun DilaerTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
