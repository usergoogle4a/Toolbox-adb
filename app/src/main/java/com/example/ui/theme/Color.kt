package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.example.data.SeedPalette

data class PalettePreviewColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color
)

fun palettePreview(palette: SeedPalette): PalettePreviewColors = when (palette) {
    SeedPalette.CYBER_TEAL -> PalettePreviewColors(
        primary = Color(0xFF006A67),
        secondary = Color(0xFF4A6362),
        tertiary = Color(0xFF47607A)
    )
    SeedPalette.ELECTRIC_INDIGO -> PalettePreviewColors(
        primary = Color(0xFF4355B9),
        secondary = Color(0xFF5B5D72),
        tertiary = Color(0xFF77536D)
    )
    SeedPalette.AMBER_FORGE -> PalettePreviewColors(
        primary = Color(0xFF8C5000),
        secondary = Color(0xFF735A42),
        tertiary = Color(0xFF586339)
    )
    SeedPalette.CRIMSON_PULSE -> PalettePreviewColors(
        primary = Color(0xFFB3261E),
        secondary = Color(0xFF775652),
        tertiary = Color(0xFF705C2E)
    )
    SeedPalette.EMERALD_MATRIX -> PalettePreviewColors(
        primary = Color(0xFF006D3B),
        secondary = Color(0xFF4F6354),
        tertiary = Color(0xFF3B6470)
    )
    SeedPalette.VIOLET_STUDIO -> PalettePreviewColors(
        primary = Color(0xFF7B41B4),
        secondary = Color(0xFF665A6F),
        tertiary = Color(0xFF805158)
    )
}

fun buildLightColorScheme(palette: SeedPalette): ColorScheme = when (palette) {
    SeedPalette.CYBER_TEAL -> lightColorScheme(
        primary = Color(0xFF006A67),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF9CF1EC),
        onPrimaryContainer = Color(0xFF00201F),
        secondary = Color(0xFF4A6361),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCCE8E5),
        onSecondaryContainer = Color(0xFF051F1E),
        tertiary = Color(0xFF47607A),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFCEE5FF),
        onTertiaryContainer = Color(0xFF001D32),
        background = Color(0xFFF4FBF9),
        onBackground = Color(0xFF161D1C),
        surface = Color(0xFFF4FBF9),
        onSurface = Color(0xFF161D1C),
        surfaceVariant = Color(0xFFDAE5E3),
        onSurfaceVariant = Color(0xFF3F4948),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFEEF5F3),
        surfaceContainer = Color(0xFFE9EFED),
        surfaceContainerHigh = Color(0xFFE3E9E8),
        surfaceContainerHighest = Color(0xFFDDE4E2),
        outline = Color(0xFF6F7978),
        outlineVariant = Color(0xFFBEC9C7)
    )
    SeedPalette.ELECTRIC_INDIGO -> lightColorScheme(
        primary = Color(0xFF4355B9),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDDE1FF),
        onPrimaryContainer = Color(0xFF001159),
        secondary = Color(0xFF5A5D72),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFDFE1F9),
        onSecondaryContainer = Color(0xFF171A2C),
        tertiary = Color(0xFF76546E),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD7F2),
        onTertiaryContainer = Color(0xFF2C1228),
        background = Color(0xFFFBF8FF),
        onBackground = Color(0xFF1B1B21),
        surface = Color(0xFFFBF8FF),
        onSurface = Color(0xFF1B1B21),
        surfaceContainerLow = Color(0xFFF4F2FA),
        surfaceContainer = Color(0xFFEEECF4),
        surfaceContainerHigh = Color(0xFFE8E7EF)
    )
    SeedPalette.AMBER_FORGE -> lightColorScheme(
        primary = Color(0xFF8C5000),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDCBE),
        onPrimaryContainer = Color(0xFF2D1600),
        secondary = Color(0xFF735A42),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFDCBE),
        onSecondaryContainer = Color(0xFF291806),
        tertiary = Color(0xFF596339),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFDDE9B3),
        onTertiaryContainer = Color(0xFF171E00),
        background = Color(0xFFFFF8F5),
        onBackground = Color(0xFF221A13),
        surface = Color(0xFFFFF8F5),
        onSurface = Color(0xFF221A13),
        surfaceContainerLow = Color(0xFFFEF1E8),
        surfaceContainer = Color(0xFFF8EBE2),
        surfaceContainerHigh = Color(0xFFF2E6DC)
    )
    SeedPalette.CRIMSON_PULSE -> lightColorScheme(
        primary = Color(0xFFB3261E),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDAD6),
        onPrimaryContainer = Color(0xFF410002),
        secondary = Color(0xFF775652),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFDAD6),
        onSecondaryContainer = Color(0xFF2C1512),
        tertiary = Color(0xFF715B2E),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFDDFA6),
        onTertiaryContainer = Color(0xFF261900),
        background = Color(0xFFFFF8F7),
        onBackground = Color(0xFF231918),
        surface = Color(0xFFFFF8F7),
        onSurface = Color(0xFF231918),
        surfaceContainerLow = Color(0xFFFFF0EE),
        surfaceContainer = Color(0xFFFCEAE8),
        surfaceContainerHigh = Color(0xFFF6E4E2)
    )
    SeedPalette.EMERALD_MATRIX -> lightColorScheme(
        primary = Color(0xFF006D3B),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF97F6B5),
        onPrimaryContainer = Color(0xFF00210E),
        secondary = Color(0xFF4F6353),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD1E8D4),
        onSecondaryContainer = Color(0xFF0D1F13),
        tertiary = Color(0xFF3B6470),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFBEEAF7),
        onTertiaryContainer = Color(0xFF001F27),
        background = Color(0xFFF5FBF4),
        onBackground = Color(0xFF171D18),
        surface = Color(0xFFF5FBF4),
        onSurface = Color(0xFF171D18),
        surfaceContainerLow = Color(0xFFEFF5EE),
        surfaceContainer = Color(0xFFEAEFE8),
        surfaceContainerHigh = Color(0xFFE4EAE3)
    )
    SeedPalette.VIOLET_STUDIO -> lightColorScheme(
        primary = Color(0xFF7B41B4),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFF0DBFF),
        onPrimaryContainer = Color(0xFF2B0052),
        secondary = Color(0xFF665A6F),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFEDDDF6),
        onSecondaryContainer = Color(0xFF211829),
        tertiary = Color(0xFF805159),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD9DF),
        onTertiaryContainer = Color(0xFF321018),
        background = Color(0xFFFFF7FE),
        onBackground = Color(0xFF1E1A20),
        surface = Color(0xFFFFF7FE),
        onSurface = Color(0xFF1E1A20),
        surfaceContainerLow = Color(0xFFF9F1FA),
        surfaceContainer = Color(0xFFF3EBF4),
        surfaceContainerHigh = Color(0xFFEDE5EE)
    )
}

fun buildDarkColorScheme(palette: SeedPalette): ColorScheme = when (palette) {
    SeedPalette.CYBER_TEAL -> darkColorScheme(
        primary = Color(0xFF7FD5D0),
        onPrimary = Color(0xFF003735),
        primaryContainer = Color(0xFF00504D),
        onPrimaryContainer = Color(0xFF9CF1EC),
        secondary = Color(0xFFB1CCC9),
        onSecondary = Color(0xFF1C3533),
        secondaryContainer = Color(0xFF334B49),
        onSecondaryContainer = Color(0xFFCCE8E5),
        tertiary = Color(0xFFAFC9E6),
        onTertiary = Color(0xFF173249),
        tertiaryContainer = Color(0xFF2F4961),
        onTertiaryContainer = Color(0xFFCEE5FF),
        background = Color(0xFF0E1514),
        onBackground = Color(0xFFDDE4E2),
        surface = Color(0xFF0E1514),
        onSurface = Color(0xFFDDE4E2),
        surfaceVariant = Color(0xFF3F4948),
        onSurfaceVariant = Color(0xFFBEC9C7),
        surfaceContainerLowest = Color(0xFF090F0F),
        surfaceContainerLow = Color(0xFF161D1C),
        surfaceContainer = Color(0xFF1A2120),
        surfaceContainerHigh = Color(0xFF252B2B),
        surfaceContainerHighest = Color(0xFF2F3635),
        outline = Color(0xFF899392),
        outlineVariant = Color(0xFF3F4948)
    )
    SeedPalette.ELECTRIC_INDIGO -> darkColorScheme(
        primary = Color(0xFFB9C3FF),
        onPrimary = Color(0xFF0C228A),
        primaryContainer = Color(0xFF2A3CA0),
        onPrimaryContainer = Color(0xFFDDE1FF),
        secondary = Color(0xFFC3C5DD),
        onSecondary = Color(0xFF2C2F42),
        secondaryContainer = Color(0xFF424659),
        onSecondaryContainer = Color(0xFFDFE1F9),
        tertiary = Color(0xFFE5B9D8),
        onTertiary = Color(0xFF44263E),
        tertiaryContainer = Color(0xFF5D3C55),
        onTertiaryContainer = Color(0xFFFFD7F2),
        background = Color(0xFF121319),
        onBackground = Color(0xFFE4E1E9),
        surface = Color(0xFF121319),
        onSurface = Color(0xFFE4E1E9),
        surfaceContainerLow = Color(0xFF1B1B21),
        surfaceContainer = Color(0xFF1F1F25),
        surfaceContainerHigh = Color(0xFF292A30)
    )
    SeedPalette.AMBER_FORGE -> darkColorScheme(
        primary = Color(0xFFFFB870),
        onPrimary = Color(0xFF4A2800),
        primaryContainer = Color(0xFF6A3B00),
        onPrimaryContainer = Color(0xFFFFDCBE),
        secondary = Color(0xFFE2C1A4),
        onSecondary = Color(0xFF402C18),
        secondaryContainer = Color(0xFF59422C),
        onSecondaryContainer = Color(0xFFFFDCBE),
        tertiary = Color(0xFFC1CC99),
        onTertiary = Color(0xFF2B340F),
        tertiaryContainer = Color(0xFF414B24),
        onTertiaryContainer = Color(0xFFDDE9B3),
        background = Color(0xFF19120C),
        onBackground = Color(0xFFEFE0D5),
        surface = Color(0xFF19120C),
        onSurface = Color(0xFFEFE0D5),
        surfaceContainerLow = Color(0xFF221A13),
        surfaceContainer = Color(0xFF261E17),
        surfaceContainerHigh = Color(0xFF312821)
    )
    SeedPalette.CRIMSON_PULSE -> darkColorScheme(
        primary = Color(0xFFFFB4AB),
        onPrimary = Color(0xFF690005),
        primaryContainer = Color(0xFF93000A),
        onPrimaryContainer = Color(0xFFFFDAD6),
        secondary = Color(0xFFE7BDB8),
        onSecondary = Color(0xFF442926),
        secondaryContainer = Color(0xFF5D3F3B),
        onSecondaryContainer = Color(0xFFFFDAD6),
        tertiary = Color(0xFFE0C38C),
        onTertiary = Color(0xFF3F2E04),
        tertiaryContainer = Color(0xFF574419),
        onTertiaryContainer = Color(0xFFFDDFA6),
        background = Color(0xFF1A1110),
        onBackground = Color(0xFFF1DFDC),
        surface = Color(0xFF1A1110),
        onSurface = Color(0xFFF1DFDC),
        surfaceContainerLow = Color(0xFF231918),
        surfaceContainer = Color(0xFF271D1C),
        surfaceContainerHigh = Color(0xFF322826)
    )
    SeedPalette.EMERALD_MATRIX -> darkColorScheme(
        primary = Color(0xFF7CD99B),
        onPrimary = Color(0xFF00391C),
        primaryContainer = Color(0xFF00522B),
        onPrimaryContainer = Color(0xFF97F6B5),
        secondary = Color(0xFFB6CCB9),
        onSecondary = Color(0xFF223527),
        secondaryContainer = Color(0xFF384B3C),
        onSecondaryContainer = Color(0xFFD1E8D4),
        tertiary = Color(0xFFA2CDDB),
        onTertiary = Color(0xFF033640),
        tertiaryContainer = Color(0xFF214C57),
        onTertiaryContainer = Color(0xFFBEEAF7),
        background = Color(0xFF0F1510),
        onBackground = Color(0xFFDFE4DD),
        surface = Color(0xFF0F1510),
        onSurface = Color(0xFFDFE4DD),
        surfaceContainerLow = Color(0xFF171D18),
        surfaceContainer = Color(0xFF1B211C),
        surfaceContainerHigh = Color(0xFF252B26)
    )
    SeedPalette.VIOLET_STUDIO -> darkColorScheme(
        primary = Color(0xFFDDB8FF),
        onPrimary = Color(0xFF490081),
        primaryContainer = Color(0xFF62269A),
        onPrimaryContainer = Color(0xFFF0DBFF),
        secondary = Color(0xFFD1C1D9),
        onSecondary = Color(0xFF372C3F),
        secondaryContainer = Color(0xFF4E4256),
        onSecondaryContainer = Color(0xFFEDDDF6),
        tertiary = Color(0xFFF3B7C0),
        onTertiary = Color(0xFF4B252C),
        tertiaryContainer = Color(0xFF653A42),
        onTertiaryContainer = Color(0xFFFFD9DF),
        background = Color(0xFF151218),
        onBackground = Color(0xFFE8E0E8),
        surface = Color(0xFF151218),
        onSurface = Color(0xFFE8E0E8),
        surfaceContainerLow = Color(0xFF1E1A20),
        surfaceContainer = Color(0xFF221E24),
        surfaceContainerHigh = Color(0xFF2D282F)
    )
}
