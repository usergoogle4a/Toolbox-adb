package com.example.adb

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DiscoveredAdbService(
    val serviceName: String,
    val host: String,
    val port: Int,
    val serviceType: String
)

class NsdAdbDiscovery(context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val _services = MutableStateFlow<List<DiscoveredAdbService>>(emptyList())
    val services: StateFlow<List<DiscoveredAdbService>> = _services.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val listeners = mutableListOf<NsdManager.DiscoveryListener>()

    fun startDiscovery() {
        val manager = nsdManager ?: return
        stopDiscovery()
        _services.value = emptyList()
        _isScanning.value = true

        val types = listOf("_adb._tcp.", "_adb-tls-connect._tcp.")
        for (type in types) {
            val listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(regType: String) {}

                override fun onServiceFound(service: NsdServiceInfo) {
                    @Suppress("DEPRECATION")
                    manager.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            @Suppress("DEPRECATION")
                            val hostAddr = serviceInfo.host?.hostAddress ?: return
                            val port = serviceInfo.port
                            if (port <= 0) return
                            val entry = DiscoveredAdbService(
                                serviceName = serviceInfo.serviceName,
                                host = hostAddr,
                                port = port,
                                serviceType = type
                            )
                            _services.update { current ->
                                (current.filterNot { it.host == entry.host && it.port == entry.port } + entry)
                            }
                        }
                    })
                }

                override fun onServiceLost(service: NsdServiceInfo) {
                    _services.update { current ->
                        current.filterNot { it.serviceName == service.serviceName }
                    }
                }

                override fun onDiscoveryStopped(serviceType: String) {}
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    runCatching { manager.stopServiceDiscovery(this) }
                }
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                    runCatching { manager.stopServiceDiscovery(this) }
                }
            }
            listeners += listener
            runCatching {
                manager.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)
            }
        }
    }

    fun stopDiscovery() {
        val manager = nsdManager ?: return
        for (listener in listeners) {
            runCatching { manager.stopServiceDiscovery(listener) }
        }
        listeners.clear()
        _isScanning.value = false
    }
}
