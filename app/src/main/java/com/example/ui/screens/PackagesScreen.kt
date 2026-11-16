package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PackagesScreen(
    connectionState: ConnectionState,
    packages: List<AdbPackageInfo>,
    isLoading: Boolean,
    packageDetails: Pair<String, String>?,
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
    var pendingUninstall by remember { mutableStateOf<Pair<AdbPackageInfo, Boolean>?>(null) }

    val apkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onInstallApk(uri)
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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredPackages, key = { it.packageName }) { pkg ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
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
                                    Icon(
                                        imageVector = Icons.Rounded.Android,
                                        contentDescription = null,
                                        tint = if (pkg.isSystem) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
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
                                            text = pkg.apkPath,
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        color = if (pkg.isSystem) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.primaryContainer
                                        },
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = if (pkg.isSystem) strings.badgeSystem else strings.badgeUser,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    if (pkg.isDisabled) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Text(
                                                text = strings.badgeDisabled,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    AssistChip(
                                        onClick = { onLaunch(pkg.packageName) },
                                        leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        label = { Text(strings.actionLaunch) }
                                    )
                                    AssistChip(
                                        onClick = { onForceStop(pkg.packageName) },
                                        leadingIcon = { Icon(Icons.Rounded.StopCircle, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        label = { Text(strings.actionForceStop) }
                                    )
                                    AssistChip(
                                        onClick = { onClearData(pkg.packageName) },
                                        leadingIcon = { Icon(Icons.Rounded.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        label = { Text(strings.actionClearData) }
                                    )
                                    AssistChip(
                                        onClick = { onSetEnabled(pkg.packageName, pkg.isDisabled) },
                                        leadingIcon = { Icon(Icons.Rounded.PowerOff, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        label = { Text(if (pkg.isDisabled) strings.actionEnable else strings.actionDisable) }
                                    )
                                    AssistChip(
                                        onClick = { onInspect(pkg.packageName) },
                                        leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        label = { Text(strings.actionInspect) }
                                    )
                                    AssistChip(
                                        onClick = { pendingUninstall = pkg to pkg.isSystem },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Rounded.Delete,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = if (pkg.isSystem) strings.actionUninstallUser0 else strings.actionUninstall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
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
