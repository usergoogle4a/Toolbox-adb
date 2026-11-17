package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
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
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CommandHistoryEntity
import com.example.i18n.LocalAppStrings
import com.example.ui.AdbViewModel
import com.example.ui.ConnectionState
import com.example.ui.LogcatLine
import com.example.ui.TerminalEntry
import com.example.ui.components.ConnectRequiredCard
import com.example.ui.theme.JetBrainsMono
import kotlinx.coroutines.launch

@Composable
fun TerminalScreen(
    connectionState: ConnectionState,
    terminalDraft: String,
    terminalEntries: List<TerminalEntry>,
    commandHistory: List<CommandHistoryEntity>,
    isRunningCommand: Boolean,
    terminalFontSize: Float,
    logcatLines: List<LogcatLine>,
    isLogcatStreaming: Boolean,
    screenshotBitmap: Bitmap?,
    isTakingScreenshot: Boolean,
    onDraftChange: (String) -> Unit,
    onRunCommand: (String) -> Unit,
    onClearTerminal: () -> Unit,
    onStartLogcatStream: () -> Unit,
    onStopLogcatStream: () -> Unit,
    onToggleLogcatStream: () -> Unit,
    onClearLogcat: () -> Unit,
    onSendKeyEvent: (Int) -> Unit,
    onSendRemoteText: (String) -> Unit,
    onCaptureScreenshot: () -> Unit,
    onSaveScreenshot: (Uri) -> Unit,
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
                    isStreaming = isLogcatStreaming,
                    fontSizeSp = terminalFontSize,
                    onStartStream = onStartLogcatStream,
                    onStopStream = onStopLogcatStream,
                    onToggleStream = onToggleLogcatStream,
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
        listOf(
            "getprop ro.build.version.release",
            "pm list packages -3",
            "dumpsys battery",
            "ip addr show",
            "df -h",
            "ps -A",
            "uname -a"
        )
    }

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            listState.scrollToItem(entries.lastIndex)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = MaterialTheme.shapes.large,
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
                    items(entries, key = { it.id }, contentType = { "shell_entry" }) { entry ->
                        val previewOutput = remember(entry.output) {
                            if (entry.output.length > 8000) {
                                entry.output.take(8000) + "\n… [truncated for smooth UI; tap Copy for full output]"
                            } else {
                                entry.output
                            }
                        }
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
                                        text = previewOutput,
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
    isStreaming: Boolean,
    fontSizeSp: Float,
    onStartStream: () -> Unit,
    onStopStream: () -> Unit,
    onToggleStream: () -> Unit,
    onClear: () -> Unit
) {
    val strings = LocalAppStrings.current
    var query by rememberSaveable { mutableStateOf("") }
    var minLevel by rememberSaveable { mutableStateOf('V') }
    var autoFollow by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-start live stream when entering Logcat tab, pause when leaving
    DisposableEffect(Unit) {
        onStartStream()
        onDispose { onStopStream() }
    }

    val filtered by remember(lines, query, minLevel) {
        derivedStateOf {
            val minRank = levelRank(minLevel)
            if (minRank == 0 && query.isBlank()) {
                lines
            } else {
                lines.filter { line ->
                    levelRank(line.level) >= minRank &&
                        (query.isBlank() || line.raw.contains(query, ignoreCase = true))
                }
            }
        }
    }

    // Detect if user manually scrolled up away from the bottom
    val isAtBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = listState.layoutInfo.totalItemsCount
            total == 0 || lastVisible >= total - 2
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && !isAtBottom) {
            autoFollow = false
        }
    }

    // Follow latest line smoothly at bottom as ring buffer updates
    val lastLineId = filtered.lastOrNull()?.id ?: -1L
    LaunchedEffect(lastLineId, filtered.size, autoFollow) {
        if (autoFollow && filtered.isNotEmpty()) {
            listState.scrollToItem(filtered.lastIndex)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
            FilledTonalButton(
                onClick = onToggleStream,
                modifier = Modifier.testTag("logcat_stream_toggle")
            ) {
                Icon(
                    imageVector = if (isStreaming) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(if (isStreaming) strings.pauseStreamLogs else strings.startStreamLogs)
            }
            IconButton(onClick = onClear) {
                Icon(Icons.Rounded.ClearAll, contentDescription = strings.clearLogs)
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            listOf('V', 'D', 'I', 'W', 'E').forEach { lvl ->
                FilterChip(
                    selected = minLevel == lvl,
                    onClick = { minLevel = lvl },
                    label = { Text("$lvl+") }
                )
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "${lines.size}/${AdbViewModel.MAX_LOGCAT_LINES}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val logFontSp = (fontSizeSp - 1.5f).coerceAtLeast(10f).sp
                val errorColor = MaterialTheme.colorScheme.error
                val warnColor = MaterialTheme.colorScheme.tertiary
                val debugColor = MaterialTheme.colorScheme.secondary
                val normalColor = MaterialTheme.colorScheme.onSurface

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = filtered,
                        key = { it.id },
                        contentType = { "log_line" }
                    ) { line ->
                        val color = when (line.level) {
                            'E', 'F' -> errorColor
                            'W' -> warnColor
                            'D' -> debugColor
                            else -> normalColor
                        }
                        Text(
                            text = line.raw,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = JetBrainsMono,
                                fontSize = logFontSp,
                                lineHeight = (logFontSp.value + 4f).sp
                            ),
                            color = color,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        )
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        )
                    }
                }

                if (!autoFollow && filtered.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            autoFollow = true
                            scope.launch {
                                listState.scrollToItem(filtered.lastIndex)
                            }
                        },
                        icon = { Icon(Icons.Rounded.ArrowDownward, contentDescription = null) },
                        text = { Text(strings.followLatestBtn) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                    )
                }
            }
        }
    }
}

private fun levelRank(level: Char): Int = when (level) {
    'V' -> 0
    'D' -> 1
    'I' -> 2
    'W' -> 3
    'E' -> 4
    'F' -> 5
    else -> 2
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RemoteAndScreenshotPane(
    screenshotBitmap: Bitmap?,
    isTakingScreenshot: Boolean,
    onSendKeyEvent: (Int) -> Unit,
    onSendRemoteText: (String) -> Unit,
    onCaptureScreenshot: () -> Unit,
    onSaveScreenshot: (Uri) -> Unit
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
