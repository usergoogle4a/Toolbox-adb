package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.adb.DiscoveredAdbService
import com.example.adb.TransportType
import com.example.adb.UsbAdbCandidate
import com.example.data.SavedDeviceEntity
import com.example.i18n.LocalAppStrings
import com.example.ui.ConnectionState
import com.example.ui.theme.JetBrainsMono

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConnectScreen(
    connectionState: ConnectionState,
    savedDevices: List<SavedDeviceEntity>,
    nsdServices: List<DiscoveredAdbService>,
    isNsdScanning: Boolean,
    usbCandidates: List<UsbAdbCandidate>,
    onConnectEndpoint: (String) -> Unit,
    onConnectTcp: (String, Int) -> Unit,
    onDeleteSavedDevice: (String) -> Unit,
    onToggleWifiScan: () -> Unit,
    onRefreshUsb: () -> Unit,
    onConnectUsb: (UsbAdbCandidate) -> Unit,
    onDisconnect: () -> Unit,
    onEnableTcpIp: () -> Unit,
    onReboot: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var endpointInput by rememberSaveable { mutableStateOf("192.168.1.5:5555") }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 820.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Banner
            item {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_adb_hero_banner),
                            contentDescription = strings.appTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.15f),
                                            Color.Black.copy(alpha = 0.78f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = strings.appTitle,
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.heroSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.88f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Active / Status Card
            item {
                AnimatedVisibility(visible = connectionState !is ConnectionState.Disconnected) {
                    ConnectionStatusDetailCard(
                        state = connectionState,
                        onDisconnect = onDisconnect,
                        onEnableTcpIp = onEnableTcpIp,
                        onReboot = onReboot
                    )
                }
            }

            // TCP/IP Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Wifi,
                                        contentDescription = strings.tcpConnectTitle,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = strings.tcpConnectTitle,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        OutlinedTextField(
                            value = endpointInput,
                            onValueChange = { endpointInput = it },
                            label = { Text(strings.tcpHostLabel) },
                            placeholder = { Text("192.168.1.5:5555") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMono),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tcp_endpoint_input")
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AssistChip(
                                onClick = {
                                    val base = endpointInput.substringBefore(':').trim().ifEmpty { "192.168.1.5" }
                                    endpointInput = "$base:5555"
                                },
                                label = { Text(":5555") }
                            )
                            AssistChip(
                                onClick = { endpointInput = "127.0.0.1:5555" },
                                label = { Text("127.0.0.1:5555") }
                            )
                            Spacer(Modifier.weight(1f))
                            Button(
                                onClick = { onConnectEndpoint(endpointInput) },
                                enabled = connectionState !is ConnectionState.Connecting,
                                modifier = Modifier.testTag("tcp_connect_button")
                            ) {
                                Icon(Icons.Rounded.Cable, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.connectBtn)
                            }
                        }

                        if (savedDevices.isNotEmpty()) {
                            Text(
                                text = strings.savedHostsTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                savedDevices.forEach { saved ->
                                    AssistChip(
                                        onClick = {
                                            endpointInput = saved.endpoint
                                            onConnectTcp(saved.host, saved.port)
                                        },
                                        label = {
                                            Text(
                                                "${saved.label} (${saved.endpoint})",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        trailingIcon = {
                                            IconButton(
                                                onClick = { onDeleteSavedDevice(saved.endpoint) },
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Close,
                                                    contentDescription = "Delete",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // mDNS Wi-Fi Discovery Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
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
                            Icon(
                                imageVector = Icons.Rounded.Radar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = strings.wifiDiscoveryTitle,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            FilledTonalButton(
                                onClick = onToggleWifiScan,
                                modifier = Modifier.testTag("wifi_scan_button")
                            ) {
                                Text(if (isNsdScanning) strings.stopScanBtn else strings.scanWifiBtn)
                            }
                        }

                        if (nsdServices.isEmpty()) {
                            Text(
                                text = strings.noWifiFound,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            nsdServices.forEach { service ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = service.serviceName,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                            Text(
                                                text = "${service.host}:${service.port}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                endpointInput = "${service.host}:${service.port}"
                                                onConnectTcp(service.host, service.port)
                                            }
                                        ) {
                                            Text(strings.connectBtn)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // USB OTG Host Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
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
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Usb,
                                        contentDescription = strings.usbOtgTitle,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = strings.usbOtgTitle,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = onRefreshUsb,
                                modifier = Modifier.testTag("usb_refresh_button")
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.refreshUsbBtn)
                            }
                        }

                        if (usbCandidates.isEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = strings.noUsbFound,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = strings.noUsbHint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            usbCandidates.forEach { candidate ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Usb,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = candidate.displayName,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                            Text(
                                                text = "VID:PID ${candidate.vendorProduct}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Button(
                                            onClick = { onConnectUsb(candidate) },
                                            modifier = Modifier.testTag("usb_connect_button")
                                        ) {
                                            Text(
                                                if (candidate.hasPermission) strings.connectUsbBtn else strings.grantUsbPerm
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
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConnectionStatusDetailCard(
    state: ConnectionState,
    onDisconnect: () -> Unit,
    onEnableTcpIp: () -> Unit,
    onReboot: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val containerColor = when (state) {
        is ConnectionState.Connected -> MaterialTheme.colorScheme.primaryContainer
        is ConnectionState.Error -> MaterialTheme.colorScheme.errorContainer
        is ConnectionState.AwaitingAuthorization -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (state) {
                is ConnectionState.Connecting -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "${strings.statusConnecting} (${state.targetLabel})",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                is ConnectionState.AwaitingAuthorization -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Security, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = strings.statusAwaitingAuth,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = strings.statusAwaitingAuthHint,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                is ConnectionState.Error -> {
                    Text(
                        text = "${strings.statusError}: ${state.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                is ConnectionState.Connected -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.banner.model.ifBlank { state.targetLabel },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${if (state.transportType == TransportType.USB_OTG) strings.transportUsb else strings.transportTcp} • ${state.targetLabel}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        FilledTonalButton(
                            onClick = onDisconnect,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.testTag("disconnect_button")
                        ) {
                            Text(strings.disconnect)
                        }
                    }

                    if (state.transportType == TransportType.USB_OTG) {
                        FilledTonalButton(
                            onClick = onEnableTcpIp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.enableWifiAdbBtn)
                        }
                    }

                    Text(
                        text = strings.quickRebootTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = { onReboot("") },
                            leadingIcon = { Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text(strings.rebootNormal) }
                        )
                        AssistChip(
                            onClick = { onReboot("bootloader") },
                            leadingIcon = { Icon(Icons.Rounded.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text(strings.rebootBootloader) }
                        )
                        AssistChip(
                            onClick = { onReboot("recovery") },
                            label = { Text(strings.rebootRecovery) }
                        )
                        AssistChip(
                            onClick = { onReboot("fastboot") },
                            label = { Text(strings.rebootFastboot) }
                        )
                    }
                }
                ConnectionState.Disconnected -> Unit
            }
        }
    }
}
