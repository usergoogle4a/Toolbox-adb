package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Cable
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.adb.UsbAdbDiscovery
import com.example.data.AppLanguage
import com.example.i18n.LocalAppStrings
import com.example.i18n.resolveStrings
import com.example.ui.AdbViewModel
import com.example.ui.components.ConnectionStatusPill
import com.example.ui.screens.CommandsScreen
import com.example.ui.screens.ConnectScreen
import com.example.ui.screens.PackagesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TerminalScreen
import com.example.ui.theme.AdbDeckTheme

object AdbRoutes {
    const val CONNECT = "connect"
    const val TERMINAL = "terminal"
    const val PACKAGES = "packages"
    const val COMMANDS = "commands"
    const val SETTINGS = "settings"
}

private data class NavTabItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val labelProvider: @Composable () -> String
)

class MainActivity : ComponentActivity() {
    private val viewModel: AdbViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.appSettings.collectAsStateWithLifecycle()
            val strings = remember(settings.language) { resolveStrings(settings.language) }

            UsbBroadcastObserver(
                onUsbChanged = { viewModel.refreshUsbDevices() },
                onPermissionResult = { device, granted ->
                    viewModel.refreshUsbDevices()
                    if (granted && device != null) {
                        val candidate = viewModel.usbCandidates.value.firstOrNull { it.device.deviceId == device.deviceId }
                        if (candidate != null) {
                            viewModel.connectUsb(candidate)
                        }
                    }
                }
            )

            CompositionLocalProvider(LocalAppStrings provides strings) {
                AdbDeckTheme(settings = settings) {
                    AdbDeckAppShell(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            viewModel.refreshUsbDevices()
        }
    }
}

@Composable
private fun UsbBroadcastObserver(
    onUsbChanged: () -> Unit,
    onPermissionResult: (UsbDevice?, Boolean) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    UsbAdbDiscovery.ACTION_USB_PERMISSION -> {
                        @Suppress("DEPRECATION")
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                        } else {
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        }
                        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        onPermissionResult(device, granted)
                    }
                    UsbManager.ACTION_USB_DEVICE_ATTACHED,
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> onUsbChanged()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(UsbAdbDiscovery.ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdbDeckAppShell(viewModel: AdbViewModel) {
    val strings = LocalAppStrings.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AdbRoutes.CONNECT
    val snackbarHostState = remember { SnackbarHostState() }

    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val savedDevices by viewModel.savedDevices.collectAsStateWithLifecycle()
    val nsdServices by viewModel.nsdServices.collectAsStateWithLifecycle()
    val isNsdScanning by viewModel.isNsdScanning.collectAsStateWithLifecycle()
    val usbCandidates by viewModel.usbCandidates.collectAsStateWithLifecycle()
    val terminalDraft by viewModel.terminalDraft.collectAsStateWithLifecycle()
    val terminalEntries by viewModel.terminalEntries.collectAsStateWithLifecycle()
    val commandHistory by viewModel.commandHistory.collectAsStateWithLifecycle()
    val isRunningCommand by viewModel.isRunningCommand.collectAsStateWithLifecycle()
    val packages by viewModel.packages.collectAsStateWithLifecycle()
    val isLoadingPackages by viewModel.isLoadingPackages.collectAsStateWithLifecycle()
    val packageDetails by viewModel.packageDetails.collectAsStateWithLifecycle()
    val logcatLines by viewModel.logcatLines.collectAsStateWithLifecycle()
    val isLogcatStreaming by viewModel.isLogcatStreaming.collectAsStateWithLifecycle()
    val screenshotBitmap by viewModel.screenshotBitmap.collectAsStateWithLifecycle()
    val isTakingScreenshot by viewModel.isTakingScreenshot.collectAsStateWithLifecycle()
    val currentRemotePath by viewModel.currentRemotePath.collectAsStateWithLifecycle()
    val remoteFiles by viewModel.remoteFiles.collectAsStateWithLifecycle()
    val isLoadingFiles by viewModel.isLoadingFiles.collectAsStateWithLifecycle()
    val deviceMetrics by viewModel.deviceMetrics.collectAsStateWithLifecycle()
    val rsaFingerprint by viewModel.rsaFingerprint.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val navItems = remember {
        listOf(
            NavTabItem(AdbRoutes.CONNECT, Icons.Rounded.Cable, Icons.Outlined.Cable) { LocalAppStrings.current.navConnect },
            NavTabItem(AdbRoutes.TERMINAL, Icons.Rounded.Terminal, Icons.Outlined.Terminal) { LocalAppStrings.current.navTerminal },
            NavTabItem(AdbRoutes.PACKAGES, Icons.Rounded.Apps, Icons.Outlined.Apps) { LocalAppStrings.current.navPackages },
            NavTabItem(AdbRoutes.COMMANDS, Icons.Rounded.Code, Icons.Outlined.Code) { LocalAppStrings.current.navCommands },
            NavTabItem(AdbRoutes.SETTINGS, Icons.Rounded.Palette, Icons.Outlined.Palette) { LocalAppStrings.current.navSettings }
        )
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 640.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(strings.appTitle) },
                    actions = {
                        ConnectionStatusPill(
                            state = connectionState,
                            onClick = { navigateTo(AdbRoutes.CONNECT) }
                        )
                        IconButton(
                            onClick = {
                                val nextLang = if (strings.isRussian) AppLanguage.ENGLISH else AppLanguage.RUSSIAN
                                viewModel.setLanguage(nextLang)
                            },
                            modifier = Modifier.testTag("quick_language_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Translate,
                                contentDescription = strings.languageSection
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        navItems.forEach { item ->
                            val selected = currentRoute == item.route
                            val label = item.labelProvider()
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_${item.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        navItems.forEach { item ->
                            val selected = currentRoute == item.route
                            val label = item.labelProvider()
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.testTag("rail_${item.route}")
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = AdbRoutes.CONNECT,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(AdbRoutes.CONNECT) {
                        ConnectScreen(
                            connectionState = connectionState,
                            savedDevices = savedDevices,
                            nsdServices = nsdServices,
                            isNsdScanning = isNsdScanning,
                            usbCandidates = usbCandidates,
                            onConnectEndpoint = viewModel::connectEndpoint,
                            onConnectTcp = viewModel::connectTcp,
                            onDeleteSavedDevice = viewModel::deleteSavedDevice,
                            onToggleWifiScan = viewModel::toggleWifiScan,
                            onRefreshUsb = viewModel::refreshUsbDevices,
                            onConnectUsb = viewModel::connectUsb,
                            onDisconnect = viewModel::disconnect,
                            onEnableTcpIp = { viewModel.enableTcpIpMode(5555) },
                            onReboot = viewModel::rebootDevice
                        )
                    }
                    composable(AdbRoutes.TERMINAL) {
                        TerminalScreen(
                            connectionState = connectionState,
                            terminalDraft = terminalDraft,
                            terminalEntries = terminalEntries,
                            commandHistory = commandHistory,
                            isRunningCommand = isRunningCommand,
                            terminalFontSize = settings.terminalFontSize,
                            logcatLines = logcatLines,
                            isLogcatStreaming = isLogcatStreaming,
                            screenshotBitmap = screenshotBitmap,
                            isTakingScreenshot = isTakingScreenshot,
                            onDraftChange = viewModel::setTerminalDraft,
                            onRunCommand = { cmd ->
                                viewModel.runShellCommand(cmd)
                                viewModel.setTerminalDraft("")
                            },
                            onClearTerminal = viewModel::clearTerminal,
                            onStartLogcatStream = viewModel::startLogcatStream,
                            onStopLogcatStream = viewModel::stopLogcatStream,
                            onToggleLogcatStream = viewModel::toggleLogcatStream,
                            onClearLogcat = viewModel::clearLogcat,
                            onSendKeyEvent = viewModel::sendKeyEvent,
                            onSendRemoteText = viewModel::sendRemoteText,
                            onCaptureScreenshot = viewModel::captureScreenshot,
                            onSaveScreenshot = viewModel::saveScreenshotToUri,
                            onCopyNotice = viewModel::showMessage,
                            onGoToConnect = { navigateTo(AdbRoutes.CONNECT) }
                        )
                    }
                    composable(AdbRoutes.PACKAGES) {
                        PackagesScreen(
                            connectionState = connectionState,
                            packages = packages,
                            isLoading = isLoadingPackages,
                            packageDetails = packageDetails,
                            onEnsureLoaded = viewModel::ensurePackagesLoaded,
                            onRefresh = viewModel::loadPackages,
                            onInstallApk = viewModel::installApkFromUri,
                            onUninstall = viewModel::uninstallPackage,
                            onForceStop = viewModel::forceStopPackage,
                            onClearData = viewModel::clearPackageData,
                            onSetEnabled = viewModel::setPackageEnabled,
                            onLaunch = viewModel::launchPackage,
                            onInspect = viewModel::inspectPackage,
                            onDismissDetails = viewModel::dismissPackageDetails,
                            onGoToConnect = { navigateTo(AdbRoutes.CONNECT) }
                        )
                    }
                    composable(AdbRoutes.COMMANDS) {
                        CommandsScreen(
                            connectionState = connectionState,
                            packages = packages,
                            deviceMetrics = deviceMetrics,
                            currentRemotePath = currentRemotePath,
                            remoteFiles = remoteFiles,
                            isLoadingFiles = isLoadingFiles,
                            onEnsurePackagesLoaded = viewModel::ensurePackagesLoaded,
                            onEnsureFilesLoaded = viewModel::ensureFilesLoaded,
                            onInsertInTerminal = { cmd ->
                                viewModel.setTerminalDraft(cmd)
                                navigateTo(AdbRoutes.TERMINAL)
                            },
                            onRunShellCommand = { cmd ->
                                viewModel.runShellCommand(cmd)
                                navigateTo(AdbRoutes.TERMINAL)
                            },
                            onSpecialAction = { action ->
                                when (action) {
                                    "tcpip" -> viewModel.enableTcpIpMode(5555)
                                    "reboot" -> viewModel.rebootDevice("")
                                    "reboot_bootloader" -> viewModel.rebootDevice("bootloader")
                                    "reboot_recovery" -> viewModel.rebootDevice("recovery")
                                    "reboot_sideload" -> viewModel.rebootDevice("sideload")
                                    "screencap" -> {
                                        viewModel.captureScreenshot()
                                        navigateTo(AdbRoutes.TERMINAL)
                                    }
                                }
                            },
                            onCopyNotice = viewModel::showMessage,
                            onRefreshMetrics = viewModel::refreshDeviceMetrics,
                            onApplyResolution = viewModel::applyScreenResolution,
                            onApplyDensity = viewModel::applyScreenDensity,
                            onApplyAnimScale = viewModel::applyAnimationScale,
                            onLoadFiles = viewModel::loadRemoteFiles,
                            onNavigateUpDir = viewModel::navigateUpDirectory,
                            onPushFile = viewModel::pushFileFromUri,
                            onPullFile = viewModel::pullFileToUri,
                            onDeleteFile = viewModel::deleteRemoteFile,
                            onGoToConnect = { navigateTo(AdbRoutes.CONNECT) }
                        )
                    }
                    composable(AdbRoutes.SETTINGS) {
                        SettingsScreen(
                            settings = settings,
                            rsaFingerprint = rsaFingerprint,
                            onLanguageChange = viewModel::setLanguage,
                            onThemeModeChange = viewModel::setThemeMode,
                            onDynamicColorChange = viewModel::setDynamicColor,
                            onSeedPaletteChange = viewModel::setSeedPalette,
                            onShapeStyleChange = viewModel::setShapeStyle,
                            onTerminalFontSizeChange = viewModel::setTerminalFontSize,
                            onRegenerateKeys = viewModel::regenerateRsaKeys
                        )
                    }
                }
            }
        }
    }
}
