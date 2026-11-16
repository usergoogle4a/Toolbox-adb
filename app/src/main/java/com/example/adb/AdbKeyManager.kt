package com.example.adb

import android.content.Context
import android.util.Base64
import java.io.File
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

class AdbKeyManager(private val context: Context) {
    private val privateKeyFile = File(context.filesDir, "adbkey.der")
    private val publicKeyFile = File(context.filesDir, "adbkey.pub.der")

    @Volatile
    private var cachedKeyPair: KeyPair? = null

    companion object {
        private const val KEY_BITS = 2048
        private const val RSA_NUM_BYTES = KEY_BITS / 8 // 256
        private const val RSA_NUM_WORDS = RSA_NUM_BYTES / 4 // 64
        private val SHA1_DIGEST_INFO_PREFIX = byteArrayOf(
            0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e,
            0x03, 0x02, 0x1a, 0x05, 0x00, 0x04, 0x14
        )
    }

    @Synchronized
    fun getOrCreateKeyPair(): KeyPair {
        cachedKeyPair?.let { return it }
        if (privateKeyFile.exists() && publicKeyFile.exists()) {
            try {
                val kf = KeyFactory.getInstance("RSA")
                val priv = kf.generatePrivate(PKCS8EncodedKeySpec(privateKeyFile.readBytes()))
                val pub = kf.generatePublic(X509EncodedKeySpec(publicKeyFile.readBytes()))
                return KeyPair(pub, priv).also { cachedKeyPair = it }
            } catch (_: Exception) {
                // Regenerate corrupted keys
            }
        }
        return generateAndSave()
    }

    @Synchronized
    fun regenerateKeyPair(): KeyPair {
        if (privateKeyFile.exists()) privateKeyFile.delete()
        if (publicKeyFile.exists()) publicKeyFile.delete()
        cachedKeyPair = null
        return generateAndSave()
    }

    private fun generateAndSave(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(KEY_BITS)
        val kp = kpg.generateKeyPair()
        privateKeyFile.writeBytes(kp.private.encoded)
        publicKeyFile.writeBytes(kp.public.encoded)
        cachedKeyPair = kp
        return kp
    }

    fun getPublicKeyFingerprint(): String {
        val pub = getOrCreateKeyPair().public as RSAPublicKey
        val adbPub = encodeAndroidRsaPublicKey(pub)
        val digest = MessageDigest.getInstance("SHA-256").digest(adbPub)
        return digest.take(16).joinToString(":") { "%02X".format(it) }
    }

    fun signToken(token: ByteArray): ByteArray {
        val priv = getOrCreateKeyPair().private as RSAPrivateKey
        val padded = ByteArray(RSA_NUM_BYTES)
        val digestInfoLen = SHA1_DIGEST_INFO_PREFIX.size + token.size
        val psLen = RSA_NUM_BYTES - 3 - digestInfoLen
        require(psLen >= 8) { "Invalid token size for RSA signature" }

        padded[0] = 0x00
        padded[1] = 0x01
        for (i in 2 until 2 + psLen) {
            padded[i] = 0xFF.toByte()
        }
        padded[2 + psLen] = 0x00
        System.arraycopy(SHA1_DIGEST_INFO_PREFIX, 0, padded, 3 + psLen, SHA1_DIGEST_INFO_PREFIX.size)
        System.arraycopy(token, 0, padded, 3 + psLen + SHA1_DIGEST_INFO_PREFIX.size, token.size)

        val messageInt = BigInteger(1, padded)
        val signedInt = messageInt.modPow(priv.privateExponent, priv.modulus)
        return toFixedUnsignedBytes(signedInt, RSA_NUM_BYTES)
    }

    fun getAdbPublicKeyPayload(): ByteArray {
        val pub = getOrCreateKeyPair().public as RSAPublicKey
        val encodedStruct = encodeAndroidRsaPublicKey(pub)
        val b64 = Base64.encodeToString(encodedStruct, Base64.NO_WRAP)
        return "$b64 adbdeck@android\u0000".toByteArray(Charsets.UTF_8)
    }

    private fun encodeAndroidRsaPublicKey(publicKey: RSAPublicKey): ByteArray {
        val n = publicKey.modulus
        val r32 = BigInteger.ONE.shiftLeft(32)
        val r = BigInteger.ONE.shiftLeft(KEY_BITS)
        val rr = r.multiply(r).mod(n)
        val n0inv = n.mod(r32).modInverse(r32).negate().mod(r32).toLong().toInt()

        val nBytesLe = toFixedUnsignedBytes(n, RSA_NUM_BYTES).reversedArray()
        val rrBytesLe = toFixedUnsignedBytes(rr, RSA_NUM_BYTES).reversedArray()

        val buf = ByteBuffer.allocate(4 + 4 + RSA_NUM_BYTES + RSA_NUM_BYTES + 4)
            .order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(RSA_NUM_WORDS)
        buf.putInt(n0inv)
        buf.put(nBytesLe)
        buf.put(rrBytesLe)
        buf.putInt(publicKey.publicExponent.toInt())
        return buf.array()
    }

    private fun toFixedUnsignedBytes(value: BigInteger, length: Int): ByteArray {
        val raw = value.toByteArray()
        val out = ByteArray(length)
        if (raw.size == length) {
            System.arraycopy(raw, 0, out, 0, length)
        } else if (raw.size > length) {
            System.arraycopy(raw, raw.size - length, out, 0, length)
        } else {
            System.arraycopy(raw, 0, out, length - raw.size, raw.size)
        }
        return out
    }
}
