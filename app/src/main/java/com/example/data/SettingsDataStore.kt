package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "adb_deck_settings")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED
}

enum class SeedPalette {
    CYBER_TEAL,
    ELECTRIC_INDIGO,
    AMBER_FORGE,
    CRIMSON_PULSE,
    EMERALD_MATRIX,
    VIOLET_STUDIO
}

enum class ShapeStyle {
    ROUNDED,
    EXPRESSIVE,
    TECHNICAL
}

enum class AppLanguage {
    SYSTEM,
    ENGLISH,
    RUSSIAN
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val seedPalette: SeedPalette = SeedPalette.CYBER_TEAL,
    val shapeStyle: ShapeStyle = ShapeStyle.ROUNDED,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val terminalFontSize: Float = 13f
)

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val SEED_PALETTE = stringPreferencesKey("seed_palette")
        val SHAPE_STYLE = stringPreferencesKey("shape_style")
        val LANGUAGE = stringPreferencesKey("language")
        val TERMINAL_FONT_SIZE = floatPreferencesKey("terminal_font_size")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
            seedPalette = prefs[Keys.SEED_PALETTE]?.let { runCatching { SeedPalette.valueOf(it) }.getOrNull() }
                ?: SeedPalette.CYBER_TEAL,
            shapeStyle = prefs[Keys.SHAPE_STYLE]?.let { runCatching { ShapeStyle.valueOf(it) }.getOrNull() }
                ?: ShapeStyle.ROUNDED,
            language = prefs[Keys.LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
                ?: AppLanguage.SYSTEM,
            terminalFontSize = (prefs[Keys.TERMINAL_FONT_SIZE] ?: 13f).coerceIn(11f, 18f)
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setSeedPalette(palette: SeedPalette) {
        context.dataStore.edit { it[Keys.SEED_PALETTE] = palette.name }
    }

    suspend fun setShapeStyle(style: ShapeStyle) {
        context.dataStore.edit { it[Keys.SHAPE_STYLE] = style.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setTerminalFontSize(size: Float) {
        context.dataStore.edit { it[Keys.TERMINAL_FONT_SIZE] = size.coerceIn(11f, 18f) }
    }
}
