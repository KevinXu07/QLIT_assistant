package com.schedule.qlit.data.crypto

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * 超星学习通网页版登录的 AES 加密。
 *
 * 对齐网页端 `/fanyalogin` 的算法（参见 docs/05-安卓小助手接入交接.md §4.2）：
 * - 算法：AES-128-CBC
 * - key = iv = `u2oh6Vu^HWe4_AES`（16 字节）
 * - padding：PKCS7
 * - 输出：Base64，NO_WRAP（不带换行）
 *
 * 用户名和密码分别加密后，作为 `uname` / `password` 两个字段提交。
 */
object ChaoxingCrypto {

    private val KEY = "u2oh6Vu^HWe4_AES".encodeToByteArray()

    /** 加密明文，返回无换行的 Base64 字符串。 */
    @OptIn(ExperimentalEncodingApi::class)
    fun encrypt(plain: String): String =
        Base64.encode(aesCbcEncrypt(KEY, KEY, plain.encodeToByteArray()))
}
