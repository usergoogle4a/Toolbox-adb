package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppLanguage
import com.example.data.AppSettings
import com.example.data.SeedPalette
import com.example.data.ShapeStyle
import com.example.data.ThemeMode
import com.example.i18n.LocalAppStrings
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.palettePreview
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    rsaFingerprint: String,
    onLanguageChange: (AppLanguage) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onSeedPaletteChange: (SeedPalette) -> Unit,
    onShapeStyleChange: (ShapeStyle) -> Unit,
    onTerminalFontSizeChange: (Float) -> Unit,
    onRegenerateKeys: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 820.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Language Switcher Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text(strings.languageSection, style = MaterialTheme.typography.titleMedium)
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val items = listOf(
                                AppLanguage.SYSTEM to strings.langSystem,
                                AppLanguage.RUSSIAN to strings.langRussian,
                                AppLanguage.ENGLISH to strings.langEnglish
                            )
                            items.forEach { (lang, label) ->
                                FilterChip(
                                    selected = settings.language == lang,
                                    onClick = { onLanguageChange(lang) },
                                    label = { Text(label) },
                                    leadingIcon = if (settings.language == lang) {
                                        { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    modifier = Modifier.testTag("lang_chip_${lang.name}")
                                )
                            }
                        }
                    }
                }
            }

            // Material You Appearance Studio
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text(strings.appearanceSection, style = MaterialTheme.typography.titleMedium)
                        }

                        // Theme mode (System / Light / Dark / AMOLED)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(strings.themeModeLabel, style = MaterialTheme.typography.labelLarge)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ThemeMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = settings.themeMode == mode,
                                        onClick = { onThemeModeChange(mode) },
                                        label = { Text(strings.themeModeLabel(mode)) },
                                        modifier = Modifier.testTag("theme_mode_${mode.name}")
                                    )
                                }
                            }
                        }

                        // Dynamic Color Toggle (Android 12+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(strings.dynamicColorTitle, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        strings.dynamicColorSubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = settings.dynamicColor,
                                    onCheckedChange = onDynamicColorChange,
                                    modifier = Modifier.testTag("dynamic_color_switch")
                                )
                            }
                        }

                        // Seed Palettes
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(strings.accentPaletteLabel, style = MaterialTheme.typography.labelLarge)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SeedPalette.entries.forEach { palette ->
                                    val preview = palettePreview(palette)
                                    val selected = !settings.dynamicColor && settings.seedPalette == palette
                                    Surface(
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        },
                                        shape = MaterialTheme.shapes.medium,
                                        modifier = Modifier
                                            .clip(MaterialTheme.shapes.medium)
                                            .clickable {
                                                if (settings.dynamicColor) onDynamicColorChange(false)
                                                onSeedPaletteChange(palette)
                                            }
                                            .testTag("palette_${palette.name}")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(preview.primary)
                                                    .border(
                                                        width = if (selected) 2.dp else 1.dp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                                        shape = CircleShape
                                                    )
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = strings.paletteLabel(palette),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Shape Style
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(strings.shapeStyleLabel, style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ShapeStyle.entries.forEach { style ->
                                    FilterChip(
                                        selected = settings.shapeStyle == style,
                                        onClick = { onShapeStyleChange(style) },
                                        label = { Text(strings.shapeLabel(style)) },
                                        modifier = Modifier.testTag("shape_${style.name}")
                                    )
                                }
                            }
                        }

                        // Terminal Font Size
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(strings.terminalFontSizeLabel, style = MaterialTheme.typography.labelLarge)
                                Text(
                                    "${settings.terminalFontSize.roundToInt()} sp",
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMono),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = settings.terminalFontSize,
                                onValueChange = onTerminalFontSizeChange,
                                valueRange = 11f..18f,
                                steps = 6
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "$ adb shell pm list packages -3",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = JetBrainsMono,
                                        fontSize = settings.terminalFontSize.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // RSA Keypair Identity Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text(strings.securitySection, style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = strings.rsaFingerprintLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = rsaFingerprint,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        OutlinedButton(
                            onClick = { onRegenerateKeys(strings.keysRegeneratedMsg) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.regenerateKeysBtn)
                        }
                    }
                }
            }
        }
    }
}
