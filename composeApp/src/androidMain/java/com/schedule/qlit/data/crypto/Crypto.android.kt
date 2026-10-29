package com.schedule.qlit.data.crypto

import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private fun cipherCrypt(transformation: String, algorithm: String, mode: Int, key: ByteArray, iv: ByteArray, data: ByteArray): ByteArray {
    val cipher = Cipher.getInstance(transformation)
    cipher.init(mode, SecretKeySpec(key, algorithm), IvParameterSpec(iv))
    return cipher.doFinal(data)
}

internal actual fun aesCbcEncrypt(key: ByteArray, iv: ByteArray, plain: ByteArray): ByteArray =
    cipherCrypt("AES/CBC/PKCS5Padding", "AES", Cipher.ENCRYPT_MODE, key, iv, plain)

internal actual fun desCbcEncrypt(key: ByteArray, iv: ByteArray, plain: ByteArray): ByteArray =
    cipherCrypt("DES/CBC/PKCS5Padding", "DES", Cipher.ENCRYPT_MODE, key, iv, plain)

internal actual fun desCbcDecrypt(key: ByteArray, iv: ByteArray, cipher: ByteArray): ByteArray =
    cipherCrypt("DES/CBC/PKCS5Padding", "DES", Cipher.DECRYPT_MODE, key, iv, cipher)

internal actual fun zlibCompress(data: ByteArray): ByteArray {
    val deflater = Deflater()
    deflater.reset()
    deflater.setInput(data)
    deflater.finish()

    val out = ByteArrayOutputStream(data.size)
    val buf = ByteArray(1024)
    while (!deflater.finished()) {
        val count = deflater.deflate(buf)
        if (count > 0) {
            out.write(buf, 0, count)
        }
    }
    deflater.end()
    return out.toByteArray()
}

internal actual fun zlibDecompress(data: ByteArray): ByteArray {
    val inf = Inflater()
    inf.setInput(data)
    val out = ByteArrayOutputStream()
    val buf = ByteArray(4096)
    while (!inf.finished()) {
        val count = inf.inflate(buf)
        if (count == 0) break
        out.write(buf, 0, count)
    }
    inf.end()
    return out.toByteArray()
}

internal actual fun sha1Hex(data: ByteArray): String =
    java.security.MessageDigest.getInstance("SHA-1")
        .digest(data)
        .joinToString("") { "%02x".format(it) }

internal actual fun md5Hex(data: ByteArray): String =
    java.security.MessageDigest.getInstance("MD5")
        .digest(data)
        .joinToString("") { "%02x".format(it) }
