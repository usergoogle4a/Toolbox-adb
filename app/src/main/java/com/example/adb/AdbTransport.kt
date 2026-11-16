package com.example.adb

import java.io.Closeable

enum class TransportType {
    TCP_IP,
    USB_OTG
}

interface AdbTransport : Closeable {
    val label: String
    val transportType: TransportType
    fun readMessage(): AdbMessage
    fun writeMessage(command: Int, arg0: Int, arg1: Int, payload: ByteArray = ByteArray(0))
}
