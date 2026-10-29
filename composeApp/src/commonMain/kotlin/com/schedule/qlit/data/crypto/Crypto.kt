package com.schedule.qlit.data.crypto

/**
 * 平台加解密原语。Android 走 javax.crypto / java.util.zip，
 * iOS 走 CommonCrypto / Compression framework。
 */
internal expect fun aesCbcEncrypt(key: ByteArray, iv: ByteArray, plain: ByteArray): ByteArray

internal expect fun desCbcEncrypt(key: ByteArray, iv: ByteArray, plain: ByteArray): ByteArray

internal expect fun desCbcDecrypt(key: ByteArray, iv: ByteArray, cipher: ByteArray): ByteArray

internal expect fun zlibCompress(data: ByteArray): ByteArray

internal expect fun zlibDecompress(data: ByteArray): ByteArray

/** SHA-1 十六进制摘要（Hydream 验签用）。 */
internal expect fun sha1Hex(data: ByteArray): String

/** MD5 十六进制摘要（MTop 签名用）。 */
internal expect fun md5Hex(data: ByteArray): String
