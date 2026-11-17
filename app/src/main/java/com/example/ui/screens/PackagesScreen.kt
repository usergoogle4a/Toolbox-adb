package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.i18n.LocalAppStrings
import com.example.ui.AdbPackageInfo
import com.example.ui.ConnectionState
import com.example.ui.PackageFilter
import com.example.ui.components.ConnectRequiredCard
import com.example.ui.theme.JetBrainsMono

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackagesScreen(
    connectionState: ConnectionState,
    packages: List<AdbPackageInfo>,
    isLoading: Boolean,
    packageDetails: Pair<String, String>?,
    onEnsureLoaded: () -> Unit,
    onRefresh: () -> Unit,
    onInstallApk: (Uri) -> Unit,
    onUninstall: (String, Boolean) -> Unit,
    onForceStop: (String) -> Unit,
    onClearData: (String) -> Unit,
    onSetEnabled: (String, Boolean) -> Unit,
    onLaunch: (String) -> Unit,
    onInspect: (String) -> Unit,
    onDismissDetails: () -> Unit,
    onGoToConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var filter by rememberSaveable { mutableStateOf(PackageFilter.ALL) }
    var search by rememberSaveable { mutableStateOf("") }
    var selectedPkgForSheet by remember { mutableStateOf<AdbPackageInfo?>(null) }
    var pendingUninstall by remember { mutableStateOf<Pair<AdbPackageInfo, Boolean>?>(null) }

    val apkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onInstallApk(uri)
    }

    LaunchedEffect(connectionState) {
        if (connectionState is ConnectionState.Connected) {
            onEnsureLoaded()
        }
    }

    if (connectionState !is ConnectionState.Connected) {
        ConnectRequiredCard(onGoToConnect = onGoToConnect, modifier = modifier)
        return
    }

    val filteredPackages = remember(packages, filter, search) {
        packages.filter { pkg ->
            val matchesFilter = when (filter) {
                PackageFilter.ALL -> true
                PackageFilter.USER -> !pkg.isSystem
                PackageFilter.SYSTEM -> pkg.isSystem
                PackageFilter.DISABLED -> pkg.isDisabled
            }
            val matchesSearch = search.isBlank() ||
                pkg.packageName.contains(search, ignoreCase = true) ||
                pkg.apkPath.contains(search, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 860.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${strings.packagesTitle} (${filteredPackages.size})",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    onClick = { apkLauncher.launch(arrayOf("application/vnd.android.package-archive", "*/*")) },
                    modifier = Modifier.testTag("install_apk_button")
                ) {
                    Icon(Icons.Rounded.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.installApkBtn)
                }
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.testTag("refresh_packages_button")
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = strings.refreshPackagesBtn)
                }
            }

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text(strings.searchPackagesHint) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("package_search_input")
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                val filterItems = listOf(
                    PackageFilter.ALL to strings.filterAll,
                    PackageFilter.USER to strings.filterUser,
                    PackageFilter.SYSTEM to strings.filterSystem,
                    PackageFilter.DISABLED to strings.filterDisabled
                )
                filterItems.forEach { (item, label) ->
                    FilterChip(
                        selected = filter == item,
                        onClick = { filter = item },
                        label = { Text(label) }
                    )
                }
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (filteredPackages.isEmpty() && !isLoading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text(
                        text = strings.noPackagesFound,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(
                        items = filteredPackages,
                        key = { it.packageName },
                        contentType = { "pkg_row" }
                    ) { pkg ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPkgForSheet = pkg }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Android,
                                    contentDescription = null,
                                    tint = if (pkg.isSystem) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pkg.packageName,
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = buildString {
                                            append(if (pkg.isSystem) strings.badgeSystem else strings.badgeUser)
                                            if (pkg.isDisabled) append(" • ${strings.badgeDisabled}")
                                            append(" • ${pkg.apkPath}")
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { pendingUninstall = pkg to pkg.isSystem }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = strings.actionUninstall,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                                IconButton(onClick = { selectedPkgForSheet = pkg }) {
                                    Icon(
                                        imageVector = Icons.Rounded.MoreVert,
                                        contentDescription = strings.packageActionsTitle
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedPkgForSheet?.let { pkg ->
        ModalBottomSheet(onDismissRequest = { selectedPkgForSheet = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text(strings.packageActionsTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(
                        pkg.packageName,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text(strings.actionLaunch) },
                    leadingContent = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                    modifier = Modifier.clickable {
                        onLaunch(pkg.packageName)
                        selectedPkgForSheet = null
                    }
                )
                ListItem(
                    headlineContent = { Text(strings.actionForceStop) },
                    leadingContent = { Icon(Icons.Rounded.StopCircle, contentDescription = null) },
                    modifier = Modifier.clickable {
                        onForceStop(pkg.packageName)
                        selectedPkgForSheet = null
                    }
                )
                ListItem(
                    headlineContent = { Text(strings.actionClearData) },
                    leadingContent = { Icon(Icons.Rounded.CleaningServices, contentDescription = null) },
                    modifier = Modifier.clickable {
                        onClearData(pkg.packageName)
                        selectedPkgForSheet = null
                    }
                )
                ListItem(
                    headlineContent = { Text(if (pkg.isDisabled) strings.actionEnable else strings.actionDisable) },
                    leadingContent = { Icon(Icons.Rounded.PowerOff, contentDescription = null) },
                    modifier = Modifier.clickable {
                        onSetEnabled(pkg.packageName, pkg.isDisabled)
                        selectedPkgForSheet = null
                    }
                )
                ListItem(
                    headlineContent = { Text(strings.actionInspect) },
                    leadingContent = { Icon(Icons.Rounded.Info, contentDescription = null) },
                    modifier = Modifier.clickable {
                        onInspect(pkg.packageName)
                        selectedPkgForSheet = null
                    }
                )
                ListItem(
                    headlineContent = {
                        Text(
                            if (pkg.isSystem) strings.actionUninstallUser0 else strings.actionUninstall,
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    leadingContent = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable {
                        pendingUninstall = pkg to pkg.isSystem
                        selectedPkgForSheet = null
                    }
                )
            }
        }
    }

    pendingUninstall?.let { (pkg, userZero) ->
        AlertDialog(
            onDismissRequest = { pendingUninstall = null },
            title = { Text(strings.confirmUninstallTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.confirmUninstallBody)
                    Text(
                        text = pkg.packageName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        onUninstall(pkg.packageName, userZero)
                        pendingUninstall = null
                    }
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingUninstall = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    packageDetails?.let { (pkgName, dump) ->
        AlertDialog(
            onDismissRequest = onDismissDetails,
            title = { Text("${strings.packageDetailsTitle}: $pkgName") },
            text = {
                Box(
                    modifier = Modifier
                        .height(320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    SelectionContainer {
                        Text(
                            text = dump,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissDetails) {
                    Text(strings.close)
                }
            }
        )
    }
}
