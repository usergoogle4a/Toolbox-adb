package com.example

import com.example.adb.AdbProtocol
import com.example.data.AdbCommandCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AdbProtocolTest {
    @Test
    fun encodeMessage_hasValidHeaderChecksumAndMagic() {
        val payload = "shell:getprop\u0000".toByteArray(Charsets.UTF_8)
        val packet = AdbProtocol.encodeMessage(AdbProtocol.A_OPEN, 42, 0, payload)

        assertEquals(AdbProtocol.HEADER_LENGTH + payload.size, packet.size)
        val bb = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN)
        val cmd = bb.int
        val arg0 = bb.int
        val arg1 = bb.int
        val len = bb.int
        val checksum = bb.int
        val magic = bb.int

        assertEquals(AdbProtocol.A_OPEN, cmd)
        assertEquals(42, arg0)
        assertEquals(0, arg1)
        assertEquals(payload.size, len)
        assertEquals(AdbProtocol.computeChecksum(payload), checksum)
        assertEquals(cmd xor -1, magic)
    }

    @Test
    fun readExact_readsFullByteArray() {
        val expected = byteArrayOf(1, 2, 3, 4, 5)
        val read = AdbProtocol.readExact(ByteArrayInputStream(expected), expected.size)
        assertTrue(expected.contentEquals(read))
    }

    @Test
    fun commandCatalog_hasUniqueIdsAndTranslations() {
        val ids = AdbCommandCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(AdbCommandCatalog.all.all {
            it.titleEn.isNotBlank() && it.titleRu.isNotBlank() && it.fullAdbSyntax.isNotBlank()
        })
    }

    @Test
    fun monkeyCommand_rendersUserParameters() {
        val monkey = AdbCommandCatalog.all.first { it.id == "monkey_app" }
        val rendered = monkey.renderShellCommand(mapOf("package" to "com.example.app", "events" to "250"))
        assertEquals("monkey -p com.example.app -v 250", rendered)
    }
}
