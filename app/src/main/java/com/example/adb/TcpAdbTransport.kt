package com.example.adb

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TcpAdbTransport(
    val host: String,
    val port: Int,
    connectTimeoutMs: Int = 8000
) : AdbTransport {
    override val label: String = "$host:$port"
    override val transportType: TransportType = TransportType.TCP_IP

    private val socket = Socket().also { s ->
        s.tcpNoDelay = true
        s.connect(InetSocketAddress(host, port), connectTimeoutMs)
    }

    private val input = BufferedInputStream(socket.getInputStream())
    private val output = BufferedOutputStream(socket.getOutputStream())
    private val writeLock = Any()

    override fun readMessage(): AdbMessage {
        val headerBytes = AdbProtocol.readExact(input, AdbProtocol.HEADER_LENGTH)
        val header = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)
        val command = header.int
        val arg0 = header.int
        val arg1 = header.int
        val length = header.int
        val checksum = header.int
        val magic = header.int

        if (length < 0 || length > AdbProtocol.MAX_PAYLOAD) {
            throw IllegalStateException("Invalid ADB payload length: $length")
        }
        val payload = if (length > 0) AdbProtocol.readExact(input, length) else ByteArray(0)
        return AdbMessage(command, arg0, arg1, length, checksum, magic, payload)
    }

    override fun writeMessage(command: Int, arg0: Int, arg1: Int, payload: ByteArray) {
        val packet = AdbProtocol.encodeMessage(command, arg0, arg1, payload)
        synchronized(writeLock) {
            output.write(packet)
            output.flush()
        }
    }

    override fun close() {
        runCatching { socket.close() }
    }
}
