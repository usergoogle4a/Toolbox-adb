package com.example.adb

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class UsbAdbCandidate(
    val device: UsbDevice,
    val interfaceIndex: Int,
    val hasPermission: Boolean,
    val displayName: String,
    val vendorProduct: String
)

object UsbAdbDiscovery {
    const val ACTION_USB_PERMISSION = "com.aistudio.adbdeck.USB_PERMISSION"
    private const val ADB_CLASS = 0xFF
    private const val ADB_SUBCLASS = 0x42
    private const val ADB_PROTOCOL = 0x01

    fun findAdbDevices(context: Context): List<UsbAdbCandidate> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return emptyList()
        val list = mutableListOf<UsbAdbCandidate>()
        for (device in usbManager.deviceList.values) {
            val ifaceIndex = findAdbInterfaceIndex(device)
            if (ifaceIndex >= 0) {
                val productName = device.productName?.takeIf { it.isNotBlank() }
                    ?: device.manufacturerName?.takeIf { it.isNotBlank() }
                    ?: "Android USB Device"
                val vidPid = "%04X:%04X".format(device.vendorId, device.productId)
                list += UsbAdbCandidate(
                    device = device,
                    interfaceIndex = ifaceIndex,
                    hasPermission = usbManager.hasPermission(device),
                    displayName = productName,
                    vendorProduct = vidPid
                )
            }
        }
        return list
    }

    fun findAdbInterfaceIndex(device: UsbDevice): Int {
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == ADB_CLASS &&
                iface.interfaceSubclass == ADB_SUBCLASS &&
                iface.interfaceProtocol == ADB_PROTOCOL
            ) {
                return i
            }
        }
        return -1
    }

    fun requestPermission(context: Context, device: UsbDevice) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val intent = Intent(ACTION_USB_PERMISSION).apply {
            setPackage(context.packageName)
        }
        val pi = PendingIntent.getBroadcast(context, device.deviceId, intent, flags)
        usbManager.requestPermission(device, pi)
    }
}

class UsbAdbTransport(
    context: Context,
    private val usbDevice: UsbDevice,
    interfaceIndex: Int
) : AdbTransport {
    override val label: String =
        "${usbDevice.productName ?: "USB Device"} (%04X:%04X)".format(usbDevice.vendorId, usbDevice.productId)
    override val transportType: TransportType = TransportType.USB_OTG

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val usbInterface: UsbInterface = usbDevice.getInterface(interfaceIndex)
    private val connection: UsbDeviceConnection
    private val endpointIn: UsbEndpoint
    private val endpointOut: UsbEndpoint
    private val writeLock = Any()

    @Volatile
    private var closed = false

    init {
        if (!usbManager.hasPermission(usbDevice)) {
            throw SecurityException("USB permission not granted for ${usbDevice.deviceName}")
        }
        val conn = usbManager.openDevice(usbDevice)
            ?: throw IOException("Unable to open USB device ${usbDevice.deviceName}")
        if (!conn.claimInterface(usbInterface, true)) {
            conn.close()
            throw IOException("Unable to claim ADB USB interface")
        }
        connection = conn

        var inEp: UsbEndpoint? = null
        var outEp: UsbEndpoint? = null
        for (i in 0 until usbInterface.endpointCount) {
            val ep = usbInterface.getEndpoint(i)
            if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (ep.direction == UsbConstants.USB_DIR_IN) inEp = ep
                if (ep.direction == UsbConstants.USB_DIR_OUT) outEp = ep
            }
        }
        endpointIn = inEp ?: throw IOException("ADB bulk IN endpoint not found")
        endpointOut = outEp ?: throw IOException("ADB bulk OUT endpoint not found")
    }

    override fun readMessage(): AdbMessage {
        val headerBytes = bulkReadExact(AdbProtocol.HEADER_LENGTH)
        val header = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)
        val command = header.int
        val arg0 = header.int
        val arg1 = header.int
        val length = header.int
        val checksum = header.int
        val magic = header.int

        if (length < 0 || length > AdbProtocol.MAX_PAYLOAD) {
            throw IOException("Invalid USB ADB payload length: $length")
        }
        val payload = if (length > 0) bulkReadExact(length) else ByteArray(0)
        return AdbMessage(command, arg0, arg1, length, checksum, magic, payload)
    }

    private fun bulkReadExact(total: Int): ByteArray {
        val out = ByteArray(total)
        var offset = 0
        while (offset < total) {
            if (closed) throw IOException("USB transport closed")
            val chunkSize = minOf(16384, total - offset)
            val buf = ByteArray(chunkSize)
            val transferred = connection.bulkTransfer(endpointIn, buf, chunkSize, 0)
            if (transferred < 0) {
                throw IOException("USB bulkTransfer read failed ($transferred)")
            }
            if (transferred > 0) {
                System.arraycopy(buf, 0, out, offset, transferred)
                offset += transferred
            }
        }
        return out
    }

    override fun writeMessage(command: Int, arg0: Int, arg1: Int, payload: ByteArray) {
        val header = AdbProtocol.encodeHeader(command, arg0, arg1, payload)
        synchronized(writeLock) {
            if (closed) throw IOException("USB transport closed")
            bulkWriteAll(header)
            if (payload.isNotEmpty()) {
                bulkWriteAll(payload)
            }
        }
    }

    private fun bulkWriteAll(data: ByteArray) {
        var offset = 0
        while (offset < data.size) {
            val len = minOf(16384, data.size - offset)
            val slice = if (offset == 0 && len == data.size) data else data.copyOfRange(offset, offset + len)
            val sent = connection.bulkTransfer(endpointOut, slice, len, 5000)
            if (sent <= 0) {
                throw IOException("USB bulkTransfer write failed ($sent)")
            }
            offset += sent
        }
    }

    override fun close() {
        closed = true
        runCatching { connection.releaseInterface(usbInterface) }
        runCatching { connection.close() }
    }
}
