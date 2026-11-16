package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CommandHistoryEntity
import com.example.i18n.LocalAppStrings
import com.example.ui.ConnectionState
import com.example.ui.LogcatLine
import com.example.ui.TerminalEntry
import com.example.ui.components.ConnectRequiredCard
import com.example.ui.theme.JetBrainsMono

@Composable
fun TerminalScreen(
    connectionState: ConnectionState,
    terminalDraft: String,
    terminalEntries: List<TerminalEntry>,
    commandHistory: List<CommandHistoryEntity>,
    isRunningCommand: Boolean,
    terminalFontSize: Float,
    logcatLines: List<LogcatLine>,
    isLoadingLogcat: Boolean,
    screenshotBitmap: Bitmap?,
    isTakingScreenshot: Boolean,
    onDraftChange: (String) -> Unit,
    onRunCommand: (String) -> Unit,
    onClearTerminal: () -> Unit,
    onRefreshLogcat: () -> Unit,
    onClearLogcat: () -> Unit,
    onSendKeyEvent: (Int) -> Unit,
    onSendRemoteText: (String) -> Unit,
    onCaptureScreenshot: () -> Unit,
    onSaveScreenshot: (android.net.Uri) -> Unit,
    onCopyNotice: (String) -> Unit,
    onGoToConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var subTab by rememberSaveable { mutableIntStateOf(0) }

    if (connectionState !is ConnectionState.Connected) {
        ConnectRequiredCard(onGoToConnect = onGoToConnect, modifier = modifier)
        return
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 860.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val tabs = listOf(strings.subTabShell, strings.subTabLogcat, strings.subTabRemote)
                tabs.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = subTab == index,
                        onClick = { subTab = index },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                        modifier = Modifier.testTag("terminal_subtab_$index")
                    ) {
                        Text(label, maxLines = 1)
                    }
                }
            }

            when (subTab) {
                0 -> ShellPane(
                    draft = terminalDraft,
                    entries = terminalEntries,
                    history = commandHistory,
                    isRunning = isRunningCommand,
                    fontSizeSp = terminalFontSize,
                    onDraftChange = onDraftChange,
                    onRunCommand = onRunCommand,
                    onClear = onClearTerminal,
                    onCopyNotice = onCopyNotice
                )
                1 -> LogcatPane(
                    lines = logcatLines,
                    isLoading = isLoadingLogcat,
                    fontSizeSp = terminalFontSize,
                    onRefresh = onRefreshLogcat,
                    onClear = onClearLogcat
                )
                else -> RemoteAndScreenshotPane(
                    screenshotBitmap = screenshotBitmap,
                    isTakingScreenshot = isTakingScreenshot,
                    onSendKeyEvent = onSendKeyEvent,
                    onSendRemoteText = onSendRemoteText,
                    onCaptureScreenshot = onCaptureScreenshot,
                    onSaveScreenshot = onSaveScreenshot
                )
            }
        }
    }
}

@Composable
private fun ShellPane(
    draft: String,
    entries: List<TerminalEntry>,
    history: List<CommandHistoryEntity>,
    isRunning: Boolean,
    fontSizeSp: Float,
    onDraftChange: (String) -> Unit,
    onRunCommand: (String) -> Unit,
    onClear: () -> Unit,
    onCopyNotice: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()

    val quickChips = remember {
        listOf("getprop ro.build.version.release", "pm list packages -3", "dumpsys battery", "ip addr show", "df -h", "ps -A", "uname -a")
    }

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.lastIndex)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Quick command chips + history
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickChips.forEach { cmd ->
                AssistChip(
                    onClick = {
                        onDraftChange(cmd)
                        onRunCommand(cmd)
                    },
                    label = {
                        Text(
                            text = cmd.substringBefore(" "),
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono)
                        )
                    }
                )
            }
            history.take(6).forEach { h ->
                AssistChip(
                    onClick = { onDraftChange(h.command) },
                    label = {
                        Text(
                            text = h.command,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono)
                        )
                    }
                )
            }
        }

        // Command Input Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = { Text(strings.commandHint) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMono),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onRunCommand(draft) }),
                leadingIcon = { Icon(Icons.Rounded.Terminal, contentDescription = null) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("shell_command_input")
            )
            Button(
                onClick = { onRunCommand(draft) },
                enabled = draft.isNotBlank() && !isRunning,
                modifier = Modifier
                    .height(54.dp)
                    .testTag("shell_run_button")
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = strings.runCmd)
            }
            IconButton(
                onClick = onClear,
                modifier = Modifier.testTag("shell_clear_button")
            ) {
                Icon(Icons.Rounded.ClearAll, contentDescription = strings.clearConsole)
            }
        }

        if (isRunning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // Output console
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = MaterialTheme.shapes.large,
            tonalElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (entries.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text(
                        text = strings.emptyTerminalHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(entries, key = { it.id }) { entry ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (entry.isError) {
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                }
                            ),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "$ ${entry.command}",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontFamily = JetBrainsMono,
                                            fontSize = fontSizeSp.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${entry.durationMs}ms",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    IconButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(entry.output))
                                            onCopyNotice(strings.copiedNotice)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ContentCopy,
                                            contentDescription = strings.copyOutput,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                SelectionContainer {
                                    Text(
                                        text = entry.output,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = JetBrainsMono,
                                            fontSize = fontSizeSp.sp,
                                            lineHeight = (fontSizeSp + 5f).sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogcatPane(
    lines: List<LogcatLine>,
    isLoading: Boolean,
    fontSizeSp: Float,
    onRefresh: () -> Unit,
    onClear: () -> Unit
) {
    val strings = LocalAppStrings.current
    var query by rememberSaveable { mutableStateOf("") }
    var minLevel by rememberSaveable { mutableStateOf('V') }

    val filtered = remember(lines, query, minLevel) {
        val order = mapOf('V' to 0, 'D' to 1, 'I' to 2, 'W' to 3, 'E' to 4, 'F' to 5)
        val minRank = order[minLevel] ?: 0
        lines.filter { line ->
            (order[line.level] ?: 2) >= minRank &&
                (query.isBlank() || line.raw.contains(query, ignoreCase = true))
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(strings.filterLogPlaceholder) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            FilledTonalButton(onClick = onRefresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.refreshLogs)
            }
            IconButton(onClick = onClear) {
                Icon(Icons.Rounded.ClearAll, contentDescription = strings.clearLogs)
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            listOf('V', 'D', 'I', 'W', 'E').forEach { lvl ->
                FilterChip(
                    selected = minLevel == lvl,
                    onClick = { minLevel = lvl },
                    label = { Text("$lvl+") }
                )
            }
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filtered) { line ->
                    val color = when (line.level) {
                        'E', 'F' -> MaterialTheme.colorScheme.error
                        'W' -> MaterialTheme.colorScheme.tertiary
                        'D' -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Text(
                        text = line.raw,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = JetBrainsMono,
                            fontSize = (fontSizeSp - 1f).coerceAtLeast(10f).sp
                        ),
                        color = color
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RemoteAndScreenshotPane(
    screenshotBitmap: Bitmap?,
    isTakingScreenshot: Boolean,
    onSendKeyEvent: (Int) -> Unit,
    onSendRemoteText: (String) -> Unit,
    onCaptureScreenshot: () -> Unit,
    onSaveScreenshot: (android.net.Uri) -> Unit
) {
    val strings = LocalAppStrings.current
    var remoteText by rememberSaveable { mutableStateOf("") }
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri != null) onSaveScreenshot(uri)
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
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
                    Text(strings.remoteKeysTitle, style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(onClick = { onSendKeyEvent(4) }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Back")
                        }
                        FilledTonalButton(onClick = { onSendKeyEvent(3) }) {
                            Icon(Icons.Rounded.Home, contentDescription = "Home", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Home")
                        }
                        FilledTonalButton(onClick = { onSendKeyEvent(187) }) {
                            Icon(Icons.Rounded.Menu, contentDescription = "Recents", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Recents")
                        }
                        FilledTonalButton(onClick = { onSendKeyEvent(26) }) {
                            Icon(Icons.Rounded.PowerSettingsNew, contentDescription = "Power", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Power")
                        }
                        FilledTonalButton(onClick = { onSendKeyEvent(24) }) {
                            Icon(Icons.Rounded.VolumeUp, contentDescription = "Vol+", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Vol +")
                        }
                        FilledTonalButton(onClick = { onSendKeyEvent(25) }) {
                            Icon(Icons.Rounded.VolumeDown, contentDescription = "Vol-", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Vol -")
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = remoteText,
                            onValueChange = { remoteText = it },
                            label = { Text(strings.sendTextLabel) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                onSendRemoteText(remoteText)
                                remoteText = ""
                            },
                            enabled = remoteText.isNotEmpty()
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = strings.sendTextBtn)
                        }
                    }
                }
            }
        }

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = strings.screenshotTitle,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = onCaptureScreenshot,
                            enabled = !isTakingScreenshot
                        ) {
                            if (isTakingScreenshot) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(strings.takeScreenshotBtn)
                        }
                    }

                    if (screenshotBitmap != null) {
                        Image(
                            bitmap = screenshotBitmap.asImageBitmap(),
                            contentDescription = strings.screenshotTitle,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(340.dp)
                        )
                        OutlinedButton(
                            onClick = { saveLauncher.launch("adb_screenshot_${System.currentTimeMillis()}.png") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.saveScreenshotBtn)
                        }
                    }
                }
            }
        }
    }
}
