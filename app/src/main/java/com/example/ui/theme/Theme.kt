package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DjCyan,
    onPrimary = Color.Black,
    secondary = DjMagenta,
    onSecondary = Color.White,
    tertiary = DjGreen,
    onTertiary = Color.Black,
    background = DjDarkBg,
    onBackground = Color.White,
    surface = DjDarkSurface,
    onSurface = Color.White,
    surfaceVariant = DjDarkCard,
    onSurfaceVariant = Color.LightGray
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
