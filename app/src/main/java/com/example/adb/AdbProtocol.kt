package com.example.adb

import java.io.EOFException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AdbProtocol {
    const val A_SYNC = 0x434e5953
    const val A_CNXN = 0x4e584e43
    const val A_OPEN = 0x4e45504f
    const val A_OKAY = 0x59414b4f
    const val A_CLSE = 0x45534c43
    const val A_WRTE = 0x45545257
    const val A_AUTH = 0x48545541

    const val AUTH_TOKEN = 1
    const val AUTH_SIGNATURE = 2
    const val AUTH_RSAPUBLICKEY = 3

    const val VERSION = 0x01000001
    const val MAX_PAYLOAD = 256 * 1024
    const val HEADER_LENGTH = 24

    const val CONNECT_SYSTEM_IDENTITY = "host::features=shell_v2,cmd,stat_v2,ls_v2,fixed_push_mkdir,apex,abb,fixed_push_symlink_timestamp,abb_exec,remount_shell,track_app,sendrecv_v2,sendrecv_v2_brotli,sendrecv_v2_lz4,sendrecv_v2_zstd,sendrecv_v2_dry_run_send"

    fun computeChecksum(data: ByteArray): Int {
        var sum = 0
        for (b in data) {
            sum += (b.toInt() and 0xFF)
        }
        return sum
    }

    fun encodeMessage(
        command: Int,
        arg0: Int,
        arg1: Int,
        payload: ByteArray = ByteArray(0)
    ): ByteArray {
        val buffer = ByteBuffer.allocate(HEADER_LENGTH + payload.size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(command)
        buffer.putInt(arg0)
        buffer.putInt(arg1)
        buffer.putInt(payload.size)
        buffer.putInt(computeChecksum(payload))
        buffer.putInt(command xor -1)
        if (payload.isNotEmpty()) {
            buffer.put(payload)
        }
        return buffer.array()
    }

    fun encodeHeader(
        command: Int,
        arg0: Int,
        arg1: Int,
        payload: ByteArray = ByteArray(0)
    ): ByteArray {
        val buffer = ByteBuffer.allocate(HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(command)
        buffer.putInt(arg0)
        buffer.putInt(arg1)
        buffer.putInt(payload.size)
        buffer.putInt(computeChecksum(payload))
        buffer.putInt(command xor -1)
        return buffer.array()
    }

    fun readExact(input: InputStream, size: Int): ByteArray {
        val buf = ByteArray(size)
        var offset = 0
        while (offset < size) {
            val read = input.read(buf, offset, size - offset)
            if (read < 0) throw EOFException("ADB stream closed unexpectedly")
            offset += read
        }
        return buf
    }
}

data class AdbMessage(
    val command: Int,
    val arg0: Int,
    val arg1: Int,
    val payloadLength: Int,
    val checksum: Int,
    val magic: Int,
    val payload: ByteArray
) {
    fun isValid(): Boolean = (command xor -1) == magic

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AdbMessage) return false
        return command == other.command &&
            arg0 == other.arg0 &&
            arg1 == other.arg1 &&
            payloadLength == other.payloadLength &&
            checksum == other.checksum &&
            magic == other.magic &&
            payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = command
        result = 31 * result + arg0
        result = 31 * result + arg1
        result = 31 * result + payloadLength
        result = 31 * result + checksum
        result = 31 * result + magic
        result = 31 * result + payload.contentHashCode()
        return result
    }
}
