package com.schedule.qlit.data.crypto

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.refTo
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CCCrypt
import platform.CoreCrypto.kCCAlgorithmAES
import platform.CoreCrypto.kCCAlgorithmDES
import platform.CoreCrypto.kCCDecrypt
import platform.CoreCrypto.kCCEncrypt
import platform.CoreCrypto.kCCOptionPKCS7Padding
import platform.CoreCrypto.kCCSuccess
import platform.posix.size_tVar
import platform.zlib.compress
import platform.zlib.compressBound
import platform.zlib.uBytefVar
import platform.zlib.uLongfVar
import platform.zlib.uncompress

@OptIn(ExperimentalForeignApi::class)
private fun ccCrypt(encrypt: Boolean, algorithm: UInt, key: ByteArray, iv: ByteArray, data: ByteArray): ByteArray {
    // CBC + PKCS7 输出至多比输入多一个块
    val out = ByteArray(data.size + 16)
    return memScoped {
        val moved = alloc<size_tVar>()
        val status = key.usePinned { keyP ->
            iv.usePinned { ivP ->
                data.usePinned { dataP ->
                    out.usePinned { outP ->
                        CCCrypt(
                            op = if (encrypt) kCCEncrypt else kCCDecrypt,
                            alg = algorithm,
                            options = kCCOptionPKCS7Padding,
                            key = keyP.addressOf(0),
                            keyLength = key.size.convert(),
                            iv = ivP.addressOf(0),
                            dataIn = dataP.addressOf(0),
                            dataInLength = data.size.convert(),
                            dataOut = outP.addressOf(0),
                            dataOutAvailable = out.size.convert(),
                            dataOutMoved = moved.ptr,
                        )
                    }
                }
            }
        }
        if (status != kCCSuccess) error("CCCrypt failed: $status")
        out.copyOf(moved.ptr[0].toInt())
    }
}

internal actual fun aesCbcEncrypt(key: ByteArray, iv: ByteArray, plain: ByteArray): ByteArray =
    ccCrypt(true, kCCAlgorithmAES, key, iv, plain)

internal actual fun desCbcEncrypt(key: ByteArray, iv: ByteArray, plain: ByteArray): ByteArray =
    ccCrypt(true, kCCAlgorithmDES, key, iv, plain)

internal actual fun desCbcDecrypt(key: ByteArray, iv: ByteArray, cipher: ByteArray): ByteArray =
    ccCrypt(false, kCCAlgorithmDES, key, iv, cipher)

@OptIn(ExperimentalForeignApi::class)
internal actual fun zlibCompress(data: ByteArray): ByteArray = memScoped {
    val bound = compressBound(data.size.convert()).toInt()
    val out = ByteArray(bound)
    val destLen = alloc<uLongfVar>()
    destLen.ptr[0] = bound.convert()
    val rc = data.usePinned { srcP ->
        out.usePinned { dstP ->
            compress(
                dstP.addressOf(0).reinterpret<uBytefVar>(),
                destLen.ptr,
                srcP.addressOf(0).reinterpret<uBytefVar>(),
                data.size.convert(),
            )
        }
    }
    if (rc != 0) error("zlib compress failed: $rc")
    out.copyOf(destLen.ptr[0].toInt())
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun zlibDecompress(data: ByteArray): ByteArray {
    // 解压后长度不可知，从 4 倍起步指数重试，上限 64 MiB
    var capacity = maxOf(data.size * 4, 4096)
    while (capacity <= 64 * 1024 * 1024) {
        val result = memScoped {
            val out = ByteArray(capacity)
            val destLen = alloc<uLongfVar>()
            destLen.ptr[0] = capacity.convert()
            val rc = data.usePinned { srcP ->
                out.usePinned { dstP ->
                    uncompress(
                        dstP.addressOf(0).reinterpret<uBytefVar>(),
                        destLen.ptr,
                        srcP.addressOf(0).reinterpret<uBytefVar>(),
                        data.size.convert(),
                    )
                }
            }
            Triple(rc, out, destLen.ptr[0].toInt())
        }
        when (result.first) {
            0 -> return result.second.copyOf(result.third)
            -5 -> capacity *= 4 // Z_BUF_ERROR：输出空间不足
            else -> error("zlib uncompress failed: ${result.first}")
        }
    }
    error("zlib uncompress: output exceeds 64 MiB")
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun sha1Hex(data: ByteArray): String {
    val digest = UByteArray(20) // CC_SHA1_DIGEST_LENGTH
    data.usePinned { srcP ->
        platform.CoreCrypto.CC_SHA1(srcP.addressOf(0), data.size.convert(), digest.refTo(0))
    }
    return digest.joinToString("") { it.toString(16).padStart(2, '0') }
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun md5Hex(data: ByteArray): String {
    val digest = UByteArray(16) // CC_MD5_DIGEST_LENGTH
    data.usePinned { srcP ->
        platform.CoreCrypto.CC_MD5(srcP.addressOf(0), data.size.convert(), digest.refTo(0))
    }
    return digest.joinToString("") { it.toString(16).padStart(2, '0') }
}
