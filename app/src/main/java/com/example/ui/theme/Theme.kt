package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.AppSettings
import com.example.data.ShapeStyle
import com.example.data.ThemeMode

private fun ShapesForStyle(style: ShapeStyle): Shapes = when (style) {
    ShapeStyle.ROUNDED -> Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(22.dp),
        extraLarge = RoundedCornerShape(28.dp)
    )
    ShapeStyle.EXPRESSIVE -> Shapes(
        extraSmall = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(16.dp),
        medium = RoundedCornerShape(24.dp),
        large = RoundedCornerShape(30.dp),
        extraLarge = RoundedCornerShape(36.dp)
    )
    ShapeStyle.TECHNICAL -> Shapes(
        extraSmall = RoundedCornerShape(2.dp),
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(8.dp),
        large = RoundedCornerShape(10.dp),
        extraLarge = RoundedCornerShape(14.dp)
    )
}

private fun ColorScheme.asAmoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0C0E),
    surfaceContainer = Color(0xFF101316),
    surfaceContainerHigh = Color(0xFF171B1F),
    surfaceContainerHighest = Color(0xFF1F2429)
)

@Composable
fun AdbDeckTheme(
    settings: AppSettings,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }

    val context = LocalContext.current
    val baseScheme = when {
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDark -> buildDarkColorScheme(settings.seedPalette)
        else -> buildLightColorScheme(settings.seedPalette)
    }

    val finalScheme = if (settings.themeMode == ThemeMode.AMOLED) {
        baseScheme.asAmoled()
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = finalScheme,
        typography = AdbDeckTypography,
        shapes = ShapesForStyle(settings.shapeStyle),
        content = content
    )
}
