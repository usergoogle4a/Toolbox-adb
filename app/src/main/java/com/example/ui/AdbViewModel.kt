package com.example.ui

import android.app.Application
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.usb.UsbDevice
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.adb.AdbClient
import com.example.adb.AdbDeviceBanner
import com.example.adb.AdbKeyManager
import com.example.adb.DiscoveredAdbService
import com.example.adb.NsdAdbDiscovery
import com.example.adb.RemoteFileEntry
import com.example.adb.TcpAdbTransport
import com.example.adb.TransportType
import com.example.adb.UsbAdbCandidate
import com.example.adb.UsbAdbDiscovery
import com.example.adb.UsbAdbTransport
import com.example.data.AdbDatabase
import com.example.data.AppLanguage
import com.example.data.AppSettings
import com.example.data.CommandHistoryEntity
import com.example.data.SavedDeviceEntity
import com.example.data.SeedPalette
import com.example.data.SettingsDataStore
import com.example.data.ShapeStyle
import com.example.data.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data class Connecting(val targetLabel: String, val transportType: TransportType) : ConnectionState
    data class AwaitingAuthorization(val targetLabel: String, val transportType: TransportType) : ConnectionState
    data class Connected(
        val targetLabel: String,
        val transportType: TransportType,
        val banner: AdbDeviceBanner
    ) : ConnectionState
    data class Error(val message: String) : ConnectionState
}

data class TerminalEntry(
    val id: Long,
    val command: String,
    val output: String,
    val isError: Boolean = false,
    val durationMs: Long = 0L
)

enum class PackageFilter {
    ALL,
    USER,
    SYSTEM,
    DISABLED
}

data class AdbPackageInfo(
    val packageName: String,
    val apkPath: String,
    val isSystem: Boolean,
    val isDisabled: Boolean
)

data class LogcatLine(
    val raw: String,
    val level: Char
)

data class DeviceMetrics(
    val model: String = "",
    val brand: String = "",
    val androidVersion: String = "",
    val sdkLevel: String = "",
    val abi: String = "",
    val serial: String = "",
    val batteryLevel: String = "",
    val batteryTemp: String = "",
    val batteryStatus: String = "",
    val wmSize: String = "",
    val wmDensity: String = "",
    val animScale: String = "",
    val ipAddress: String = ""
)

class AdbViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val settingsStore = SettingsDataStore(context)
    private val database = AdbDatabase.getInstance(context)
    private val dao = database.adbDao()
    private val keyManager = AdbKeyManager(context)
    private val nsdDiscovery = NsdAdbDiscovery(context)

    val appSettings: StateFlow<AppSettings> = settingsStore.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        AppSettings()
    )

    val savedDevices: StateFlow<List<SavedDeviceEntity>> = dao.observeSavedDevices().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val commandHistory: StateFlow<List<CommandHistoryEntity>> = dao.observeCommandHistory().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val nsdServices: StateFlow<List<DiscoveredAdbService>> = nsdDiscovery.services
    val isNsdScanning: StateFlow<Boolean> = nsdDiscovery.isScanning

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _usbCandidates = MutableStateFlow<List<UsbAdbCandidate>>(emptyList())
    val usbCandidates: StateFlow<List<UsbAdbCandidate>> = _usbCandidates.asStateFlow()

    private val _terminalEntries = MutableStateFlow<List<TerminalEntry>>(emptyList())
    val terminalEntries: StateFlow<List<TerminalEntry>> = _terminalEntries.asStateFlow()

    private val _terminalDraft = MutableStateFlow("")
    val terminalDraft: StateFlow<String> = _terminalDraft.asStateFlow()

    private val _isRunningCommand = MutableStateFlow(false)
    val isRunningCommand: StateFlow<Boolean> = _isRunningCommand.asStateFlow()

    private val _packages = MutableStateFlow<List<AdbPackageInfo>>(emptyList())
    val packages: StateFlow<List<AdbPackageInfo>> = _packages.asStateFlow()

    private val _isLoadingPackages = MutableStateFlow(false)
    val isLoadingPackages: StateFlow<Boolean> = _isLoadingPackages.asStateFlow()

    private val _packageDetails = MutableStateFlow<Pair<String, String>?>(null)
    val packageDetails: StateFlow<Pair<String, String>?> = _packageDetails.asStateFlow()

    private val _logcatLines = MutableStateFlow<List<LogcatLine>>(emptyList())
    val logcatLines: StateFlow<List<LogcatLine>> = _logcatLines.asStateFlow()

    private val _isLoadingLogcat = MutableStateFlow(false)
    val isLoadingLogcat: StateFlow<Boolean> = _isLoadingLogcat.asStateFlow()

    private val _screenshotBytes = MutableStateFlow<ByteArray?>(null)
    val screenshotBytes: StateFlow<ByteArray?> = _screenshotBytes.asStateFlow()

    private val _screenshotBitmap = MutableStateFlow<Bitmap?>(null)
    val screenshotBitmap: StateFlow<Bitmap?> = _screenshotBitmap.asStateFlow()

    private val _isTakingScreenshot = MutableStateFlow(false)
    val isTakingScreenshot: StateFlow<Boolean> = _isTakingScreenshot.asStateFlow()

    private val _currentRemotePath = MutableStateFlow("/sdcard")
    val currentRemotePath: StateFlow<String> = _currentRemotePath.asStateFlow()

    private val _remoteFiles = MutableStateFlow<List<RemoteFileEntry>>(emptyList())
    val remoteFiles: StateFlow<List<RemoteFileEntry>> = _remoteFiles.asStateFlow()

    private val _isLoadingFiles = MutableStateFlow(false)
    val isLoadingFiles: StateFlow<Boolean> = _isLoadingFiles.asStateFlow()

    private val _deviceMetrics = MutableStateFlow(DeviceMetrics())
    val deviceMetrics: StateFlow<DeviceMetrics> = _deviceMetrics.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _rsaFingerprint = MutableStateFlow(keyManager.getPublicKeyFingerprint())
    val rsaFingerprint: StateFlow<String> = _rsaFingerprint.asStateFlow()

    private var activeClient: AdbClient? = null
    private var entryCounter = 1L

    init {
        refreshUsbDevices()
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun setTerminalDraft(text: String) {
        _terminalDraft.value = text
    }

    fun refreshUsbDevices() {
        _usbCandidates.value = UsbAdbDiscovery.findAdbDevices(context)
    }

    fun requestUsbPermission(device: UsbDevice) {
        UsbAdbDiscovery.requestPermission(context, device)
    }

    fun toggleWifiScan() {
        if (nsdDiscovery.isScanning.value) {
            nsdDiscovery.stopDiscovery()
        } else {
            nsdDiscovery.startDiscovery()
        }
    }

    fun connectEndpoint(rawEndpoint: String) {
        val cleaned = rawEndpoint
            .trim()
            .removePrefix("adb connect")
            .trim()
        if (cleaned.isEmpty()) {
            _connectionState.value = ConnectionState.Error("Enter IP:Port (e.g. 192.168.1.5:5555)")
            return
        }
        val (host, port) = if (cleaned.startsWith("[")) {
            val closeIdx = cleaned.indexOf(']')
            if (closeIdx > 0) {
                val h = cleaned.substring(1, closeIdx)
                val p = cleaned.substring(closeIdx + 1).removePrefix(":").toIntOrNull() ?: 5555
                h to p
            } else {
                cleaned to 5555
            }
        } else {
            val lastColon = cleaned.lastIndexOf(':')
            if (lastColon > 0) {
                val h = cleaned.substring(0, lastColon).trim()
                val pStr = cleaned.substring(lastColon + 1).trim()
                val p = pStr.toIntOrNull()
                if (p == null || p !in 1..65535) {
                    _connectionState.value = ConnectionState.Error("Invalid port: $pStr")
                    return
                }
                h to p
            } else {
                cleaned to 5555
            }
        }
        connectTcp(host, port)
    }

    fun connectTcp(host: String, port: Int) {
        val cleanHost = host.trim()
        if (cleanHost.isEmpty() || port !in 1..65535) {
            _connectionState.value = ConnectionState.Error("Invalid host or port ($cleanHost:$port)")
            return
        }
        viewModelScope.launch {
            disconnectInternal()
            val label = "$cleanHost:$port"
            _connectionState.value = ConnectionState.Connecting(label, TransportType.TCP_IP)
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val transport = TcpAdbTransport(cleanHost, port)
                    val client = AdbClient(transport, keyManager)
                    client.onDisconnected = { err ->
                        viewModelScope.launch {
                            _connectionState.value = ConnectionState.Disconnected
                            err?.message?.let { showMessage("Disconnected: $it") }
                        }
                    }
                    val banner = client.connect(
                        onAwaitingAuth = {
                            _connectionState.value =
                                ConnectionState.AwaitingAuthorization(label, TransportType.TCP_IP)
                        }
                    ).getOrElse { e ->
                        client.disconnect()
                        throw e
                    }
                    activeClient = client
                    banner
                }
            }
            result.onSuccess { banner ->
                _connectionState.value = ConnectionState.Connected(label, TransportType.TCP_IP, banner)
                dao.upsertSavedDevice(
                    SavedDeviceEntity(
                        endpoint = label,
                        host = cleanHost,
                        port = port,
                        label = banner.model.ifBlank { cleanHost },
                        lastConnectedAt = System.currentTimeMillis()
                    )
                )
                refreshAfterConnect()
            }.onFailure { err ->
                _connectionState.value = ConnectionState.Error(err.message ?: "TCP connection failed")
            }
        }
    }

    fun connectUsb(candidate: UsbAdbCandidate) {
        if (!candidate.hasPermission) {
            requestUsbPermission(candidate.device)
            return
        }
        viewModelScope.launch {
            disconnectInternal()
            val label = candidate.displayName
            _connectionState.value = ConnectionState.Connecting(label, TransportType.USB_OTG)
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val transport = UsbAdbTransport(context, candidate.device, candidate.interfaceIndex)
                    val client = AdbClient(transport, keyManager)
                    client.onDisconnected = { err ->
                        viewModelScope.launch {
                            _connectionState.value = ConnectionState.Disconnected
                            err?.message?.let { showMessage("USB Disconnected: $it") }
                        }
                    }
                    val banner = client.connect(
                        onAwaitingAuth = {
                            _connectionState.value =
                                ConnectionState.AwaitingAuthorization(label, TransportType.USB_OTG)
                        }
                    ).getOrElse { e ->
                        client.disconnect()
                        throw e
                    }
                    activeClient = client
                    banner
                }
            }
            result.onSuccess { banner ->
                _connectionState.value = ConnectionState.Connected(label, TransportType.USB_OTG, banner)
                refreshAfterConnect()
            }.onFailure { err ->
                _connectionState.value = ConnectionState.Error(err.message ?: "USB OTG connection failed")
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            disconnectInternal()
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    private suspend fun disconnectInternal() = withContext(Dispatchers.IO) {
        activeClient?.disconnect()
        activeClient = null
    }

    private fun refreshAfterConnect() {
        loadPackages()
        refreshDeviceMetrics()
        loadRemoteFiles("/sdcard")
        refreshLogcat()
    }

    fun deleteSavedDevice(endpoint: String) {
        viewModelScope.launch {
            dao.deleteSavedDevice(endpoint)
        }
    }

    fun runShellCommand(rawCommand: String) {
        val cmd = rawCommand.trim()
        if (cmd.isEmpty()) return
        val client = activeClient ?: run {
            showMessage("No active ADB connection")
            return
        }
        viewModelScope.launch {
            _isRunningCommand.value = true
            dao.upsertCommand(CommandHistoryEntity(command = cmd, lastUsedAt = System.currentTimeMillis()))
            val start = System.currentTimeMillis()
            val res = client.executeShell(cmd)
            val elapsed = System.currentTimeMillis() - start
            val entry = res.fold(
                onSuccess = { out ->
                    TerminalEntry(
                        id = entryCounter++,
                        command = cmd,
                        output = out.ifBlank { "(no output)" },
                        isError = false,
                        durationMs = elapsed
                    )
                },
                onFailure = { err ->
                    TerminalEntry(
                        id = entryCounter++,
                        command = cmd,
                        output = err.message ?: "Command failed",
                        isError = true,
                        durationMs = elapsed
                    )
                }
            )
            _terminalEntries.update { (it + entry).takeLast(120) }
            _isRunningCommand.value = false
        }
    }

    fun toggleCommandFavorite(cmd: String, currentFav: Boolean) {
        viewModelScope.launch {
            dao.setCommandFavorite(cmd, !currentFav)
        }
    }

    fun clearTerminal() {
        _terminalEntries.value = emptyList()
    }

    fun loadPackages() {
        val client = activeClient ?: return
        viewModelScope.launch {
            _isLoadingPackages.value = true
            val allRes = client.executeShell("pm list packages -f")
            val thirdPartyRes = client.executeShell("pm list packages -3")
            val disabledRes = client.executeShell("pm list packages -d")

            val thirdPartySet = thirdPartyRes.getOrDefault("")
                .lineSequence()
                .mapNotNull { it.trim().removePrefix("package:").takeIf { p -> p.isNotBlank() } }
                .toHashSet()

            val disabledSet = disabledRes.getOrDefault("")
                .lineSequence()
                .mapNotNull { it.trim().removePrefix("package:").takeIf { p -> p.isNotBlank() } }
                .toHashSet()

            val parsed = allRes.getOrDefault("")
                .lineSequence()
                .map { it.trim() }
                .filter { it.startsWith("package:") }
                .mapNotNull { line ->
                    val body = line.removePrefix("package:")
                    val eqIdx = body.lastIndexOf('=')
                    if (eqIdx > 0) {
                        val path = body.substring(0, eqIdx)
                        val pkg = body.substring(eqIdx + 1).trim()
                        AdbPackageInfo(
                            packageName = pkg,
                            apkPath = path,
                            isSystem = !thirdPartySet.contains(pkg),
                            isDisabled = disabledSet.contains(pkg)
                        )
                    } else null
                }
                .sortedBy { it.packageName.lowercase() }
                .toList()

            _packages.value = parsed
            _isLoadingPackages.value = false
        }
    }

    fun uninstallPackage(packageName: String, userZeroOnly: Boolean) {
        val client = activeClient ?: return
        viewModelScope.launch {
            val cmd = if (userZeroOnly) "pm uninstall --user 0 $packageName" else "pm uninstall $packageName"
            val res = client.executeShell(cmd)
            res.onSuccess { out ->
                showMessage("$packageName: ${out.trim().ifBlank { "Done" }}")
                loadPackages()
            }.onFailure { err ->
                showMessage("Error: ${err.message}")
            }
        }
    }

    fun forceStopPackage(packageName: String) {
        val client = activeClient ?: return
        viewModelScope.launch {
            client.executeShell("am force-stop $packageName")
            showMessage("Stopped $packageName")
        }
    }

    fun clearPackageData(packageName: String) {
        val client = activeClient ?: return
        viewModelScope.launch {
            val res = client.executeShell("pm clear $packageName")
            showMessage(res.getOrNull()?.trim()?.ifBlank { "Cleared $packageName" } ?: "Failed")
        }
    }

    fun setPackageEnabled(packageName: String, enable: Boolean) {
        val client = activeClient ?: return
        viewModelScope.launch {
            val cmd = if (enable) "pm enable $packageName" else "pm disable-user --user 0 $packageName"
            val res = client.executeShell(cmd)
            showMessage(res.getOrNull()?.trim() ?: "Updated")
            loadPackages()
        }
    }

    fun launchPackage(packageName: String) {
        val client = activeClient ?: return
        viewModelScope.launch {
            val res = client.executeShell("monkey -p $packageName -c android.intent.category.LAUNCHER 1")
            showMessage(res.getOrNull()?.lines()?.lastOrNull()?.trim() ?: "Launched")
        }
    }

    fun inspectPackage(packageName: String) {
        val client = activeClient ?: return
        viewModelScope.launch {
            val res = client.executeShell("dumpsys package $packageName | head -n 90")
            _packageDetails.value = packageName to (res.getOrNull() ?: "Unable to read package info")
        }
    }

    fun dismissPackageDetails() {
        _packageDetails.value = null
    }

    fun installApkFromUri(uri: Uri) {
        val client = activeClient ?: return
        viewModelScope.launch {
            val fileName = resolveFileName(context.contentResolver, uri) ?: "package.apk"
            val remotePath = "/data/local/tmp/$fileName"
            showMessage("Uploading $fileName…")
            val pushRes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    client.syncPush(input, remotePath)
                } ?: Result.failure(IllegalStateException("Cannot open APK"))
            }
            pushRes.onSuccess {
                val installOut = client.executeShell("pm install -r \"$remotePath\"")
                client.executeShell("rm -f \"$remotePath\"")
                showMessage(installOut.getOrNull()?.trim()?.ifBlank { "Installed" } ?: "Install finished")
                loadPackages()
            }.onFailure { err ->
                showMessage("APK upload failed: ${err.message}")
            }
        }
    }

    fun refreshLogcat() {
        val client = activeClient ?: return
        viewModelScope.launch {
            _isLoadingLogcat.value = true
            val res = client.executeShell("logcat -d -t 250")
            val lines = res.getOrDefault("")
                .lineSequence()
                .filter { it.isNotBlank() }
                .map { line ->
                    val lvl = Regex("\\s([VDIWEF])\\s").find(line)?.groupValues?.getOrNull(1)?.firstOrNull() ?: 'I'
                    LogcatLine(raw = line, level = lvl)
                }
                .toList()
                .asReversed()
            _logcatLines.value = lines
            _isLoadingLogcat.value = false
        }
    }

    fun clearLogcat() {
        val client = activeClient ?: return
        viewModelScope.launch {
            client.executeShell("logcat -c")
            _logcatLines.value = emptyList()
            showMessage("Logcat cleared")
        }
    }

    fun sendKeyEvent(keyCode: Int) {
        val client = activeClient ?: return
        viewModelScope.launch {
            client.executeShell("input keyevent $keyCode")
        }
    }

    fun sendRemoteText(text: String) {
        val client = activeClient ?: return
        if (text.isEmpty()) return
        val escaped = text.replace(" ", "%s").replace("\"", "\\\"").replace("'", "\\'")
        viewModelScope.launch {
            client.executeShell("input text \"$escaped\"")
        }
    }

    fun captureScreenshot() {
        val client = activeClient ?: return
        viewModelScope.launch {
            _isTakingScreenshot.value = true
            val res = client.captureScreenshot()
            res.onSuccess { bytes ->
                val bmp = withContext(Dispatchers.Default) {
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                if (bmp != null) {
                    _screenshotBytes.value = bytes
                    _screenshotBitmap.value = bmp
                } else {
                    showMessage("Screenshot captured (${bytes.size} B) but could not decode PNG")
                }
            }.onFailure { err ->
                showMessage("Screenshot error: ${err.message}")
            }
            _isTakingScreenshot.value = false
        }
    }

    fun saveScreenshotToUri(uri: Uri) {
        val bytes = _screenshotBytes.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(bytes)
                    out.flush()
                }
            }.onSuccess {
                showMessage("Screenshot saved")
            }.onFailure { err ->
                showMessage("Save failed: ${err.message}")
            }
        }
    }

    fun enableTcpIpMode(port: Int = 5555) {
        val client = activeClient ?: return
        viewModelScope.launch {
            val res = client.enableTcpIp(port)
            res.onSuccess { msg ->
                showMessage(msg.ifBlank { "TCP/IP mode enabled on port $port" })
            }.onFailure { err ->
                showMessage("Failed: ${err.message}")
            }
        }
    }

    fun rebootDevice(mode: String) {
        val client = activeClient ?: return
        viewModelScope.launch {
            val res = client.reboot(mode)
            res.onSuccess {
                showMessage("Rebooting (${mode.ifBlank { "normal" }})…")
                disconnect()
            }.onFailure { err ->
                showMessage("Reboot failed: ${err.message}")
            }
        }
    }

    fun refreshDeviceMetrics() {
        val client = activeClient ?: return
        viewModelScope.launch {
            val props = client.executeShell(
                "getprop ro.product.model; echo '---'; getprop ro.product.brand; echo '---'; getprop ro.build.version.release; echo '---'; getprop ro.build.version.sdk; echo '---'; getprop ro.product.cpu.abi; echo '---'; getprop ro.serialno"
            ).getOrDefault("").split("---").map { it.trim() }

            val batteryRaw = client.executeShell("dumpsys battery").getOrDefault("")
            val level = Regex("level:\\s*(\\d+)").find(batteryRaw)?.groupValues?.getOrNull(1)?.plus("%") ?: "-"
            val tempRaw = Regex("temperature:\\s*(\\d+)").find(batteryRaw)?.groupValues?.getOrNull(1)?.toFloatOrNull()
            val temp = if (tempRaw != null) "${tempRaw / 10f}°C" else "-"
            val statusCode = Regex("status:\\s*(\\d+)").find(batteryRaw)?.groupValues?.getOrNull(1)
            val statusStr = when (statusCode) {
                "2" -> "Charging"
                "3" -> "Discharging"
                "5" -> "Full"
                else -> "OK"
            }

            val wmSize = client.executeShell("wm size").getOrDefault("").lines().lastOrNull { it.isNotBlank() } ?: "-"
            val wmDensity = client.executeShell("wm density").getOrDefault("").lines().lastOrNull { it.isNotBlank() } ?: "-"
            val anim = client.executeShell("settings get global animator_duration_scale").getOrDefault("").trim().ifBlank { "1.0" }
            val ip = client.executeShell("ip route | awk '/src/ {print \$9; exit}'").getOrDefault("").trim().ifBlank { "-" }

            _deviceMetrics.value = DeviceMetrics(
                model = props.getOrNull(0).orEmpty().ifBlank { "-" },
                brand = props.getOrNull(1).orEmpty().ifBlank { "-" },
                androidVersion = props.getOrNull(2).orEmpty().ifBlank { "-" },
                sdkLevel = props.getOrNull(3).orEmpty().ifBlank { "-" },
                abi = props.getOrNull(4).orEmpty().ifBlank { "-" },
                serial = props.getOrNull(5).orEmpty().ifBlank { "-" },
                batteryLevel = level,
                batteryTemp = temp,
                batteryStatus = statusStr,
                wmSize = wmSize.removePrefix("Physical size: ").removePrefix("Override size: "),
                wmDensity = wmDensity.removePrefix("Physical density: ").removePrefix("Override density: "),
                animScale = anim,
                ipAddress = ip
            )
        }
    }

    fun applyScreenResolution(size: String) {
        val client = activeClient ?: return
        val cmd = if (size.equals("reset", true)) "wm size reset" else "wm size ${size.trim()}"
        viewModelScope.launch {
            val res = client.executeShell(cmd)
            showMessage(res.getOrNull()?.trim()?.ifBlank { "Updated resolution" } ?: "Failed")
            refreshDeviceMetrics()
        }
    }

    fun applyScreenDensity(dpi: String) {
        val client = activeClient ?: return
        val cmd = if (dpi.equals("reset", true)) "wm density reset" else "wm density ${dpi.trim()}"
        viewModelScope.launch {
            val res = client.executeShell(cmd)
            showMessage(res.getOrNull()?.trim()?.ifBlank { "Updated DPI" } ?: "Failed")
            refreshDeviceMetrics()
        }
    }

    fun applyAnimationScale(scale: String) {
        val client = activeClient ?: return
        viewModelScope.launch {
            client.executeShell(
                "settings put global window_animation_scale $scale && settings put global transition_animation_scale $scale && settings put global animator_duration_scale $scale"
            )
            showMessage("Animation scale -> ${scale}x")
            refreshDeviceMetrics()
        }
    }

    fun loadRemoteFiles(path: String = _currentRemotePath.value) {
        val client = activeClient ?: return
        viewModelScope.launch {
            _isLoadingFiles.value = true
            val normalized = path.ifBlank { "/sdcard" }
            val res = client.syncList(normalized)
            res.onSuccess { list ->
                _currentRemotePath.value = normalized
                _remoteFiles.value = list
            }.onFailure { err ->
                showMessage("List error: ${err.message}")
            }
            _isLoadingFiles.value = false
        }
    }

    fun navigateUpDirectory() {
        val cur = _currentRemotePath.value.trimEnd('/')
        if (cur.isEmpty() || cur == "/") return
        val parent = cur.substringBeforeLast('/').ifEmpty { "/" }
        loadRemoteFiles(parent)
    }

    fun pushFileFromUri(uri: Uri) {
        val client = activeClient ?: return
        viewModelScope.launch {
            val name = resolveFileName(context.contentResolver, uri) ?: "upload_${System.currentTimeMillis()}"
            val dest = "${_currentRemotePath.value.trimEnd('/')}/$name"
            val res = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    client.syncPush(input, dest)
                } ?: Result.failure(IllegalStateException("Cannot open file"))
            }
            res.onSuccess { bytes ->
                showMessage("Pushed $name ($bytes B)")
                loadRemoteFiles()
            }.onFailure { err ->
                showMessage("Push failed: ${err.message}")
            }
        }
    }

    fun pullFileToUri(remotePath: String, destUri: Uri) {
        val client = activeClient ?: return
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(destUri)?.use { out ->
                    client.syncPull(remotePath, out)
                } ?: Result.failure(IllegalStateException("Cannot open destination"))
            }
            res.onSuccess { bytes ->
                showMessage("Pulled $bytes B")
            }.onFailure { err ->
                showMessage("Pull failed: ${err.message}")
            }
        }
    }

    fun deleteRemoteFile(entry: RemoteFileEntry) {
        val client = activeClient ?: return
        viewModelScope.launch {
            val cmd = if (entry.isDirectory) "rm -rf \"${entry.fullPath}\"" else "rm -f \"${entry.fullPath}\""
            val res = client.executeShell(cmd)
            res.onSuccess {
                showMessage("Deleted ${entry.name}")
                loadRemoteFiles()
            }.onFailure { err ->
                showMessage("Delete failed: ${err.message}")
            }
        }
    }

    // Settings mutations
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsStore.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { settingsStore.setDynamicColor(enabled) }
    fun setSeedPalette(palette: SeedPalette) = viewModelScope.launch { settingsStore.setSeedPalette(palette) }
    fun setShapeStyle(style: ShapeStyle) = viewModelScope.launch { settingsStore.setShapeStyle(style) }
    fun setLanguage(lang: AppLanguage) = viewModelScope.launch { settingsStore.setLanguage(lang) }
    fun setTerminalFontSize(size: Float) = viewModelScope.launch { settingsStore.setTerminalFontSize(size) }

    fun regenerateRsaKeys(successMsg: String) {
        viewModelScope.launch(Dispatchers.IO) {
            keyManager.regenerateKeyPair()
            _rsaFingerprint.value = keyManager.getPublicKeyFingerprint()
            showMessage(successMsg)
        }
    }

    private fun resolveFileName(resolver: ContentResolver, uri: Uri): String? {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return cursor.getString(idx)
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    override fun onCleared() {
        super.onCleared()
        nsdDiscovery.stopDiscovery()
        activeClient?.disconnect()
    }
}
