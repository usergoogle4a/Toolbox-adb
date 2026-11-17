package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.adb.RemoteFileEntry
import com.example.data.AdbCommandCatalog
import com.example.data.AdbCommandCategory
import com.example.data.AdbCommandSpec
import com.example.i18n.LocalAppStrings
import com.example.ui.AdbPackageInfo
import com.example.ui.ConnectionState
import com.example.ui.DeviceMetrics
import com.example.ui.components.ConnectRequiredCard
import com.example.ui.theme.JetBrainsMono

private enum class ParamDialogMode {
    RUN,
    INSERT
}

@Composable
fun CommandsScreen(
    connectionState: ConnectionState,
    packages: List<AdbPackageInfo>,
    deviceMetrics: DeviceMetrics,
    currentRemotePath: String,
    remoteFiles: List<RemoteFileEntry>,
    isLoadingFiles: Boolean,
    onEnsurePackagesLoaded: () -> Unit,
    onEnsureFilesLoaded: () -> Unit,
    onInsertInTerminal: (String) -> Unit,
    onRunShellCommand: (String) -> Unit,
    onSpecialAction: (String) -> Unit,
    onCopyNotice: (String) -> Unit,
    onRefreshMetrics: () -> Unit,
    onApplyResolution: (String) -> Unit,
    onApplyDensity: (String) -> Unit,
    onApplyAnimScale: (String) -> Unit,
    onLoadFiles: (String) -> Unit,
    onNavigateUpDir: () -> Unit,
    onPushFile: (Uri) -> Unit,
    onPullFile: (String, Uri) -> Unit,
    onDeleteFile: (RemoteFileEntry) -> Unit,
    onGoToConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var subTab by rememberSaveable { mutableIntStateOf(0) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 860.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val tabs = listOf(strings.subTabCatalog, strings.subTabDeviceFiles)
                tabs.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = subTab == index,
                        onClick = { subTab = index },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                        modifier = Modifier.testTag("commands_subtab_$index")
                    ) {
                        Text(label, maxLines = 1)
                    }
                }
            }

            if (subTab == 0) {
                CommandCatalogPane(
                    isConnected = connectionState is ConnectionState.Connected,
                    packages = packages,
                    onEnsurePackagesLoaded = onEnsurePackagesLoaded,
                    onInsertInTerminal = onInsertInTerminal,
                    onRunShellCommand = onRunShellCommand,
                    onSpecialAction = onSpecialAction,
                    onCopyNotice = onCopyNotice
                )
            } else {
                if (connectionState !is ConnectionState.Connected) {
                    ConnectRequiredCard(onGoToConnect = onGoToConnect)
                } else {
                    LaunchedEffect(connectionState) {
                        onEnsureFilesLoaded()
                    }
                    DeviceAndFilesPane(
                        metrics = deviceMetrics,
                        currentPath = currentRemotePath,
                        files = remoteFiles,
                        isLoadingFiles = isLoadingFiles,
                        onRefreshMetrics = onRefreshMetrics,
                        onApplyResolution = onApplyResolution,
                        onApplyDensity = onApplyDensity,
                        onApplyAnimScale = onApplyAnimScale,
                        onLoadFiles = onLoadFiles,
                        onNavigateUpDir = onNavigateUpDir,
                        onPushFile = onPushFile,
                        onPullFile = onPullFile,
                        onDeleteFile = onDeleteFile
                    )
                }
            }
        }
    }
}

@Composable
private fun CommandCatalogPane(
    isConnected: Boolean,
    packages: List<AdbPackageInfo>,
    onEnsurePackagesLoaded: () -> Unit,
    onInsertInTerminal: (String) -> Unit,
    onRunShellCommand: (String) -> Unit,
    onSpecialAction: (String) -> Unit,
    onCopyNotice: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val clipboard = LocalClipboardManager.current
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<AdbCommandCategory?>(null) }
    var paramDialogState by remember { mutableStateOf<Pair<AdbCommandSpec, ParamDialogMode>?>(null) }

    val commands = remember(query, selectedCategory, strings.isRussian) {
        AdbCommandCatalog.all.filter { cmd ->
            val catOk = selectedCategory == null || cmd.category == selectedCategory
            val qOk = query.isBlank() ||
                cmd.fullAdbSyntax.contains(query, ignoreCase = true) ||
                cmd.titleEn.contains(query, ignoreCase = true) ||
                cmd.titleRu.contains(query, ignoreCase = true) ||
                cmd.descriptionEn.contains(query, ignoreCase = true) ||
                cmd.descriptionRu.contains(query, ignoreCase = true)
            catOk && qOk
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(strings.searchCommandsHint) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("command_search_input")
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text(strings.allCategories) }
            )
            AdbCommandCategory.entries.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(strings.categoryLabel(cat)) }
                )
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(commands, key = { it.id }, contentType = { "adb_cmd" }) { cmd ->
                CommandCatalogCard(
                    cmd = cmd,
                    isConnected = isConnected,
                    onCopy = {
                        clipboard.setText(AnnotatedString(cmd.fullAdbSyntax))
                        onCopyNotice(strings.copiedNotice)
                    },
                    onInsert = {
                        if (cmd.parameters.isNotEmpty()) {
                            if (isConnected && cmd.parameters.any { it.isPackagePicker }) {
                                onEnsurePackagesLoaded()
                            }
                            paramDialogState = cmd to ParamDialogMode.INSERT
                        } else {
                            onInsertInTerminal(cmd.shellCommand ?: cmd.fullAdbSyntax)
                        }
                    },
                    onRun = {
                        if (cmd.specialAction != null) {
                            onSpecialAction(cmd.specialAction)
                        } else if (cmd.parameters.isNotEmpty()) {
                            if (cmd.parameters.any { it.isPackagePicker }) {
                                onEnsurePackagesLoaded()
                            }
                            paramDialogState = cmd to ParamDialogMode.RUN
                        } else if (cmd.shellCommand != null) {
                            onRunShellCommand(cmd.shellCommand)
                        }
                    }
                )
            }
        }
    }

    paramDialogState?.let { (cmd, mode) ->
        CommandParametersDialog(
            cmd = cmd,
            mode = mode,
            packages = packages,
            onDismiss = { paramDialogState = null },
            onConfirm = { renderedCmd ->
                paramDialogState = null
                if (mode == ParamDialogMode.RUN) {
                    onRunShellCommand(renderedCmd)
                } else {
                    onInsertInTerminal(renderedCmd)
                }
            }
        )
    }
}

@Composable
private fun CommandParametersDialog(
    cmd: AdbCommandSpec,
    mode: ParamDialogMode,
    packages: List<AdbPackageInfo>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val paramValues = remember(cmd.id) {
        mutableStateMapOf<String, String>().apply {
            cmd.parameters.forEach { put(it.key, "") }
        }
    }
    var showError by remember { mutableStateOf(false) }

    val userPackages = remember(packages) {
        packages.filter { !it.isSystem }.take(12)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (strings.isRussian) cmd.titleRu else cmd.titleEn)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = cmd.fullAdbSyntax,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                cmd.parameters.forEach { param ->
                    val currentVal = paramValues[param.key].orEmpty()
                    OutlinedTextField(
                        value = currentVal,
                        onValueChange = { newVal ->
                            val filtered = if (param.isNumeric) newVal.filter { it.isDigit() } else newVal
                            paramValues[param.key] = filtered
                            showError = false
                        },
                        label = { Text(if (strings.isRussian) param.labelRu else param.labelEn) },
                        placeholder = { Text(param.exampleHint) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (param.isNumeric) KeyboardType.Number else KeyboardType.Text
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMono),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (param.isPackagePicker && userPackages.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            userPackages.forEach { pkg ->
                                AssistChip(
                                    onClick = {
                                        paramValues[param.key] = pkg.packageName
                                        showError = false
                                    },
                                    label = {
                                        Text(
                                            pkg.packageName.substringAfterLast('.'),
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                if (showError) {
                    Text(
                        text = strings.paramRequiredError,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val allFilled = cmd.parameters.all { !paramValues[it.key].isNullOrBlank() }
                    if (!allFilled) {
                        showError = true
                    } else {
                        val rendered = cmd.renderShellCommand(paramValues) ?: return@Button
                        onConfirm(rendered)
                    }
                }
            ) {
                Text(if (mode == ParamDialogMode.RUN) strings.runNow else strings.insertToShell)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
private fun CommandCatalogCard(
    cmd: AdbCommandSpec,
    isConnected: Boolean,
    onCopy: () -> Unit,
    onInsert: () -> Unit,
    onRun: () -> Unit
) {
    val strings = LocalAppStrings.current
    val title = if (strings.isRussian) cmd.titleRu else cmd.titleEn
    val desc = if (strings.isRussian) cmd.descriptionRu else cmd.descriptionEn

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = strings.categoryLabel(cmd.category),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = cmd.fullAdbSyntax,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(onClick = onCopy) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.copyOutput)
                }
                if (cmd.shellCommand != null) {
                    FilledTonalButton(onClick = onInsert) {
                        Icon(Icons.Rounded.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.insertToShell)
                    }
                }
                if (isConnected && (cmd.shellCommand != null || cmd.specialAction != null)) {
                    Spacer(Modifier.weight(1f))
                    Button(onClick = onRun) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.runNow)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeviceAndFilesPane(
    metrics: DeviceMetrics,
    currentPath: String,
    files: List<RemoteFileEntry>,
    isLoadingFiles: Boolean,
    onRefreshMetrics: () -> Unit,
    onApplyResolution: (String) -> Unit,
    onApplyDensity: (String) -> Unit,
    onApplyAnimScale: (String) -> Unit,
    onLoadFiles: (String) -> Unit,
    onNavigateUpDir: () -> Unit,
    onPushFile: (Uri) -> Unit,
    onPullFile: (String, Uri) -> Unit,
    onDeleteFile: (RemoteFileEntry) -> Unit
) {
    val strings = LocalAppStrings.current
    var resInput by rememberSaveable(metrics.wmSize) { mutableStateOf(metrics.wmSize) }
    var dpiInput by rememberSaveable(metrics.wmDensity) { mutableStateOf(metrics.wmDensity) }
    var pendingPullPath by remember { mutableStateOf<String?>(null) }

    val pushLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onPushFile(uri)
    }

    val pullLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        val path = pendingPullPath
        pendingPullPath = null
        if (uri != null && path != null) onPullFile(path, uri)
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "overview") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = strings.deviceOverviewTitle,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onRefreshMetrics) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                        }
                    }
                    MetricRow("Device", "${metrics.brand} ${metrics.model} (${metrics.abi})")
                    MetricRow("Android", "${metrics.androidVersion} (API ${metrics.sdkLevel})")
                    MetricRow("Battery", "${metrics.batteryLevel} • ${metrics.batteryTemp} • ${metrics.batteryStatus}")
                    MetricRow("IP", metrics.ipAddress)
                    MetricRow("Serial", metrics.serial)
                }
            }
        }

        item(key = "tweaks") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(strings.displayTweaksTitle, style = MaterialTheme.typography.titleMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = resInput,
                            onValueChange = { resInput = it },
                            label = { Text(strings.resolutionLabel) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalButton(onClick = { onApplyResolution(resInput) }) {
                            Text(strings.applyBtn)
                        }
                        OutlinedButton(onClick = { onApplyResolution("reset") }) {
                            Text(strings.resetBtn)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = dpiInput,
                            onValueChange = { dpiInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text(strings.densityLabel) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalButton(onClick = { onApplyDensity(dpiInput) }) {
                            Text(strings.applyBtn)
                        }
                        OutlinedButton(onClick = { onApplyDensity("reset") }) {
                            Text(strings.resetBtn)
                        }
                    }

                    Text(
                        text = "${strings.animationScaleLabel} (${metrics.animScale}x)",
                        style = MaterialTheme.typography.labelMedium
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("0", "0.5", "1", "1.5").forEach { scale ->
                            AssistChip(
                                onClick = { onApplyAnimScale(scale) },
                                label = { Text("${scale}x") }
                            )
                        }
                    }
                }
            }
        }

        item(key = "files_header") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.fileBrowserTitle, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = currentPath,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        FilledTonalButton(onClick = { pushLauncher.launch(arrayOf("*/*")) }) {
                            Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.pushFileBtn)
                        }
                        IconButton(onClick = { onLoadFiles(currentPath) }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                        }
                    }

                    if (isLoadingFiles) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    if (currentPath != "/") {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onNavigateUpDir)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(Icons.Rounded.ArrowUpward, contentDescription = null)
                                Spacer(Modifier.width(10.dp))
                                Text(strings.parentFolder, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    if (files.isEmpty() && !isLoadingFiles) {
                        Text(
                            text = strings.emptyFolder,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(
            items = files,
            key = { it.fullPath },
            contentType = { "remote_file" }
        ) { entry ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = entry.isDirectory) {
                        onLoadFiles(entry.fullPath)
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = if (entry.isDirectory) Icons.Rounded.Folder else Icons.Rounded.Description,
                        contentDescription = null,
                        tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!entry.isDirectory) {
                            Text(
                                text = formatByteSize(entry.sizeBytes),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (!entry.isDirectory) {
                        IconButton(onClick = {
                            pendingPullPath = entry.fullPath
                            pullLauncher.launch(entry.name)
                        }) {
                            Icon(Icons.Rounded.Download, contentDescription = strings.pullFileBtn)
                        }
                    }
                    IconButton(onClick = { onDeleteFile(entry) }) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = strings.deleteFileBtn,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

private fun formatByteSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${"%.1f".format(bytes / (1024f * 1024f))} MB"
}
