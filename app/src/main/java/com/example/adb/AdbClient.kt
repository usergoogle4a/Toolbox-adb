package com.example.adb

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class AdbDeviceBanner(
    val rawBanner: String,
    val productName: String,
    val model: String,
    val device: String,
    val state: String
)

data class RemoteFileEntry(
    val name: String,
    val fullPath: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val mode: Int,
    val modifiedSeconds: Long
)

class AdbStream internal constructor(
    val localId: Int,
    private val client: AdbClient
) {
    internal val remoteIdDeferred = CompletableDeferred<Int>()
    internal val incoming = Channel<ByteArray>(Channel.UNLIMITED)
    internal val readySignal = Channel<Unit>(Channel.CONFLATED)

    @Volatile
    var isClosed = false
        internal set

    fun write(data: ByteArray) {
        if (isClosed) throw IOException("Stream closed")
        val remoteId = remoteIdDeferred.getCompletedOrNull()
            ?: throw IOException("Stream not yet opened")
        var offset = 0
        while (offset < data.size) {
            val chunkSize = minOf(client.maxPayload, data.size - offset)
            val slice = data.copyOfRange(offset, offset + chunkSize)
            client.sendWrite(localId, remoteId, slice)
            offset += chunkSize
        }
    }

    suspend fun readAll(): ByteArray {
        val out = ByteArrayOutputStream()
        for (chunk in incoming) {
            out.write(chunk)
        }
        return out.toByteArray()
    }

    suspend fun readChunk(): ByteArray? {
        return incoming.receiveCatching().getOrNull()
    }

    fun close() {
        if (!isClosed) {
            isClosed = true
            val remoteId = remoteIdDeferred.getCompletedOrNull() ?: 0
            runCatching { client.sendClose(localId, remoteId) }
            incoming.close()
        }
    }

    private fun <T> CompletableDeferred<T>.getCompletedOrNull(): T? =
        if (isCompleted && !isCancelled) runCatching { getCompleted() }.getOrNull() else null
}

class AdbClient(
    private val transport: AdbTransport,
    private val keyManager: AdbKeyManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val nextLocalId = AtomicInteger(1)
    private val streams = ConcurrentHashMap<Int, AdbStream>()
    private var readerJob: Job? = null

    @Volatile
    var maxPayload: Int = 4096
        private set

    @Volatile
    var isConnected: Boolean = false
        private set

    var onDisconnected: ((Throwable?) -> Unit)? = null

    suspend fun connect(
        onAwaitingAuth: () -> Unit = {}
    ): Result<AdbDeviceBanner> = withContext(Dispatchers.IO) {
        runCatching {
            val bannerPayload = "${AdbProtocol.CONNECT_SYSTEM_IDENTITY}\u0000".toByteArray(Charsets.UTF_8)
            transport.writeMessage(
                AdbProtocol.A_CNXN,
                AdbProtocol.VERSION,
                AdbProtocol.MAX_PAYLOAD,
                bannerPayload
            )

            var sentPubKey = false
            var response: AdbMessage
            while (true) {
                response = transport.readMessage()
                if (!response.isValid()) {
                    throw IOException("Corrupted ADB packet magic")
                }
                when (response.command) {
                    AdbProtocol.A_AUTH -> {
                        if (response.arg0 == AdbProtocol.AUTH_TOKEN) {
                            if (!sentPubKey) {
                                val sig = keyManager.signToken(response.payload)
                                transport.writeMessage(
                                    AdbProtocol.A_AUTH,
                                    AdbProtocol.AUTH_SIGNATURE,
                                    0,
                                    sig
                                )
                                sentPubKey = true
                            } else {
                                onAwaitingAuth()
                                val pubPayload = keyManager.getAdbPublicKeyPayload()
                                transport.writeMessage(
                                    AdbProtocol.A_AUTH,
                                    AdbProtocol.AUTH_RSAPUBLICKEY,
                                    0,
                                    pubPayload
                                )
                            }
                        }
                    }
                    AdbProtocol.A_CNXN -> {
                        maxPayload = minOf(response.arg1.takeIf { it > 0 } ?: 4096, AdbProtocol.MAX_PAYLOAD)
                        isConnected = true
                        startReaderLoop()
                        return@runCatching parseBanner(String(response.payload, Charsets.UTF_8))
                    }
                    else -> {
                        throw IOException("Unexpected ADB handshake command: 0x${response.command.toString(16)}")
                    }
                }
            }
            @Suppress("UNREACHABLE_CODE")
            error("Unreachable")
        }
    }

    private fun startReaderLoop() {
        readerJob = scope.launch {
            try {
                while (isConnected) {
                    val msg = transport.readMessage()
                    if (!msg.isValid()) continue
                    when (msg.command) {
                        AdbProtocol.A_OKAY -> {
                            val stream = streams[msg.arg1]
                            if (stream != null) {
                                if (!stream.remoteIdDeferred.isCompleted) {
                                    stream.remoteIdDeferred.complete(msg.arg0)
                                }
                                stream.readySignal.trySend(Unit)
                            }
                        }
                        AdbProtocol.A_WRTE -> {
                            val stream = streams[msg.arg1]
                            if (stream != null) {
                                stream.incoming.trySend(msg.payload)
                                transport.writeMessage(AdbProtocol.A_OKAY, msg.arg1, msg.arg0)
                            } else {
                                transport.writeMessage(AdbProtocol.A_CLSE, msg.arg1, msg.arg0)
                            }
                        }
                        AdbProtocol.A_CLSE -> {
                            val stream = streams.remove(msg.arg1)
                            if (stream != null) {
                                stream.isClosed = true
                                if (!stream.remoteIdDeferred.isCompleted) {
                                    stream.remoteIdDeferred.completeExceptionally(IOException("Stream refused by target"))
                                }
                                stream.incoming.close()
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                if (isConnected) {
                    isConnected = false
                    streams.values.forEach {
                        it.isClosed = true
                        it.incoming.close(t)
                    }
                    streams.clear()
                    onDisconnected?.invoke(t)
                }
            }
        }
    }

    suspend fun openStream(destination: String): AdbStream = withContext(Dispatchers.IO) {
        if (!isConnected) throw IOException("Not connected to ADB target")
        val localId = nextLocalId.getAndIncrement()
        val stream = AdbStream(localId, this@AdbClient)
        streams[localId] = stream
        val payload = "$destination\u0000".toByteArray(Charsets.UTF_8)
        transport.writeMessage(AdbProtocol.A_OPEN, localId, 0, payload)
        withTimeout(10_000) {
            stream.remoteIdDeferred.await()
        }
        stream
    }

    internal fun sendWrite(localId: Int, remoteId: Int, data: ByteArray) {
        transport.writeMessage(AdbProtocol.A_WRTE, localId, remoteId, data)
    }

    internal fun sendClose(localId: Int, remoteId: Int) {
        streams.remove(localId)
        transport.writeMessage(AdbProtocol.A_CLSE, localId, remoteId)
    }

    suspend fun executeShell(command: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val stream = openStream("shell:$command")
            val bytes = stream.readAll()
            stream.close()
            String(bytes, Charsets.UTF_8).replace("\r\n", "\n")
        }
    }

    suspend fun executeRaw(service: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val stream = openStream(service)
            val bytes = stream.readAll()
            stream.close()
            bytes
        }
    }

    suspend fun captureScreenshot(): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            // Try exec:screencap -p first (binary clean, no PTY CRLF conversion), fallback to shell
            val raw = executeRaw("exec:screencap -p").getOrElse {
                executeRaw("shell:screencap -p").getOrThrow()
            }
            if (raw.isEmpty()) throw IOException("Empty screenshot response")
            raw
        }
    }

    suspend fun reboot(mode: String = ""): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val stream = openStream("reboot:$mode")
            stream.close()
        }
    }

    suspend fun enableTcpIp(port: Int = 5555): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val stream = openStream("tcpip:$port")
            val out = String(stream.readAll(), Charsets.UTF_8)
            stream.close()
            out.trim()
        }
    }

    suspend fun syncList(remotePath: String): Result<List<RemoteFileEntry>> = withContext(Dispatchers.IO) {
        runCatching {
            val stream = openStream("sync:")
            try {
                val pathBytes = remotePath.toByteArray(Charsets.UTF_8)
                val req = ByteBuffer.allocate(8 + pathBytes.size).order(ByteOrder.LITTLE_ENDIAN)
                req.put("LIST".toByteArray(Charsets.US_ASCII))
                req.putInt(pathBytes.size)
                req.put(pathBytes)
                stream.write(req.array())

                val accumulator = ByteArrayOutputStream()
                val entries = mutableListOf<RemoteFileEntry>()
                var done = false
                while (!done) {
                    val chunk = stream.readChunk() ?: break
                    accumulator.write(chunk)
                    val buf = accumulator.toByteArray()
                    var offset = 0
                    while (offset + 4 <= buf.size) {
                        val tag = String(buf, offset, 4, Charsets.US_ASCII)
                        if (tag == "DONE") {
                            if (offset + 20 <= buf.size) {
                                done = true
                                offset += 20
                                break
                            } else {
                                break
                            }
                        } else if (tag == "DENT") {
                            if (offset + 20 > buf.size) break
                            val bb = ByteBuffer.wrap(buf, offset + 4, 16).order(ByteOrder.LITTLE_ENDIAN)
                            val mode = bb.int
                            val size = bb.int.toLong() and 0xFFFFFFFFL
                            val mtime = bb.int.toLong() and 0xFFFFFFFFL
                            val nameLen = bb.int
                            if (offset + 20 + nameLen > buf.size) break
                            val name = String(buf, offset + 20, nameLen, Charsets.UTF_8)
                            offset += 20 + nameLen
                            if (name != "." && name != "..") {
                                val cleanBase = remotePath.trimEnd('/')
                                val full = if (cleanBase.isEmpty()) "/$name" else "$cleanBase/$name"
                                val isDir = (mode and 0x4000) != 0
                                entries += RemoteFileEntry(
                                    name = name,
                                    fullPath = full,
                                    isDirectory = isDir,
                                    sizeBytes = size,
                                    mode = mode,
                                    modifiedSeconds = mtime
                                )
                            }
                        } else {
                            done = true
                            break
                        }
                    }
                    val remaining = buf.copyOfRange(offset, buf.size)
                    accumulator.reset()
                    accumulator.write(remaining)
                }

                // Send QUIT
                val quit = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
                quit.put("QUIT".toByteArray(Charsets.US_ASCII))
                quit.putInt(0)
                runCatching { stream.write(quit.array()) }

                entries.sortedWith(compareByDescending<RemoteFileEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
            } finally {
                stream.close()
            }
        }
    }

    suspend fun syncPush(
        inputStream: InputStream,
        remotePath: String,
        permissions: Int = 0b110100100
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val stream = openStream("sync:")
            try {
                val spec = "$remotePath,${permissions or 0x8000}".toByteArray(Charsets.UTF_8)
                val sendHdr = ByteBuffer.allocate(8 + spec.size).order(ByteOrder.LITTLE_ENDIAN)
                sendHdr.put("SEND".toByteArray(Charsets.US_ASCII))
                sendHdr.putInt(spec.size)
                sendHdr.put(spec)
                stream.write(sendHdr.array())

                var totalSent = 0L
                val buffer = ByteArray(32 * 1024)
                while (true) {
                    val read = inputStream.read(buffer)
                    if (read <= 0) break
                    val dataPkt = ByteBuffer.allocate(8 + read).order(ByteOrder.LITTLE_ENDIAN)
                    dataPkt.put("DATA".toByteArray(Charsets.US_ASCII))
                    dataPkt.putInt(read)
                    dataPkt.put(buffer, 0, read)
                    stream.write(dataPkt.array())
                    totalSent += read
                }

                val donePkt = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
                donePkt.put("DONE".toByteArray(Charsets.US_ASCII))
                donePkt.putInt((System.currentTimeMillis() / 1000L).toInt())
                stream.write(donePkt.array())

                val reply = stream.readChunk() ?: throw IOException("No sync confirmation")
                if (reply.size >= 4) {
                    val tag = String(reply, 0, 4, Charsets.US_ASCII)
                    if (tag == "FAIL") {
                        val msg = if (reply.size > 8) String(reply, 8, reply.size - 8, Charsets.UTF_8) else "Push failed"
                        throw IOException(msg)
                    }
                }
                totalSent
            } finally {
                stream.close()
            }
        }
    }

    suspend fun syncPull(remotePath: String, outputStream: OutputStream): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val stream = openStream("sync:")
            try {
                val pathBytes = remotePath.toByteArray(Charsets.UTF_8)
                val req = ByteBuffer.allocate(8 + pathBytes.size).order(ByteOrder.LITTLE_ENDIAN)
                req.put("RECV".toByteArray(Charsets.US_ASCII))
                req.putInt(pathBytes.size)
                req.put(pathBytes)
                stream.write(req.array())

                var totalBytes = 0L
                val acc = ByteArrayOutputStream()
                var done = false
                while (!done) {
                    val chunk = stream.readChunk() ?: break
                    acc.write(chunk)
                    val buf = acc.toByteArray()
                    var offset = 0
                    while (offset + 8 <= buf.size) {
                        val tag = String(buf, offset, 4, Charsets.US_ASCII)
                        val len = ByteBuffer.wrap(buf, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
                        when (tag) {
                            "DATA" -> {
                                if (offset + 8 + len > buf.size) break
                                outputStream.write(buf, offset + 8, len)
                                totalBytes += len
                                offset += 8 + len
                            }
                            "DONE" -> {
                                done = true
                                offset += 8
                                break
                            }
                            "FAIL" -> {
                                val msg = if (offset + 8 + len <= buf.size) {
                                    String(buf, offset + 8, len, Charsets.UTF_8)
                                } else {
                                    "Pull failed"
                                }
                                throw IOException(msg)
                            }
                            else -> {
                                done = true
                                break
                            }
                        }
                    }
                    val rem = buf.copyOfRange(offset, buf.size)
                    acc.reset()
                    acc.write(rem)
                }
                outputStream.flush()
                totalBytes
            } finally {
                stream.close()
            }
        }
    }

    fun disconnect() {
        isConnected = false
        readerJob?.cancel()
        streams.values.forEach {
            it.isClosed = true
            it.incoming.close()
        }
        streams.clear()
        runCatching { transport.close() }
        scope.cancel()
    }

    private fun parseBanner(raw: String): AdbDeviceBanner {
        val clean = raw.trimEnd('\u0000', '\n', '\r')
        val parts = clean.split("::")
        val state = parts.firstOrNull().orEmpty().ifBlank { "device" }
        val propsPart = parts.getOrNull(1).orEmpty()
        val map = propsPart.split(";").mapNotNull { kv ->
            val idx = kv.indexOf('=')
            if (idx > 0) kv.substring(0, idx) to kv.substring(idx + 1) else null
        }.toMap()
        return AdbDeviceBanner(
            rawBanner = clean,
            productName = map["ro.product.name"] ?: "Android",
            model = map["ro.product.model"] ?: transport.label,
            device = map["ro.product.device"] ?: "",
            state = state
        )
    }
}
