package com.schedule.qlit.data.store

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.Foundation.NSUserDefaults
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

private class DefaultsKeyValueStore(private val suite: String) : KeyValueStore {
    private val defaults = NSUserDefaults(suiteName = suite)

    override fun getString(key: String, default: String?): String? =
        defaults.stringForKey(key) ?: default

    override fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }

    override fun getBoolean(key: String, default: Boolean): Boolean =
        if (defaults.objectForKey(key) != null) defaults.boolForKey(key) else default

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = key)
    }

    override fun getInt(key: String, default: Int): Int =
        if (defaults.objectForKey(key) != null) defaults.integerForKey(key).toInt() else default

    override fun putInt(key: String, value: Int) {
        defaults.setInteger(value.toLong(), forKey = key)
    }

    override fun getStringSet(key: String, default: Set<String>): Set<String> =
        (defaults.objectForKey(key) as? List<*>)?.filterIsInstance<String>()?.toSet() ?: default

    override fun putStringSet(key: String, value: Set<String>) {
        defaults.setObject(value.toList(), forKey = key)
    }

    override fun getLong(key: String, default: Long): Long =
        if (defaults.objectForKey(key) != null) defaults.integerForKey(key) else default

    override fun putLong(key: String, value: Long) {
        defaults.setInteger(value, forKey = key)
    }

    override fun contains(key: String): Boolean = defaults.objectForKey(key) != null

    override fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }

    override fun clear() {
        defaults.removePersistentDomainForName(suite)
    }
}

/**
 * Keychain 存储（generic password，service = 存储名，account = key）。
 * 非字符串类型按字符串形式编码落库。查询字典用 CoreFoundation 构造，
 * 避免 ObjC 桥接。
 */
@OptIn(ExperimentalForeignApi::class)
private class KeychainKeyValueStore(private val service: String) : KeyValueStore {

    private fun cfString(s: String) =
        CFStringCreateWithCString(null, s, kCFStringEncodingUTF8)

    private fun queryFor(key: String): CFDictionaryRef = memScoped {
        CFDictionaryCreateMutable(
            null, 0,
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr,
        )!!.also { d ->
            CFDictionarySetValue(d, kSecClass, kSecClassGenericPassword)
            CFDictionarySetValue(d, kSecAttrService, cfString(service))
            CFDictionarySetValue(d, kSecAttrAccount, cfString(key))
        }
    }

    private fun readString(key: String): String? {
        val query = queryFor(key)
        CFDictionarySetValue(query, kSecReturnData, platform.CoreFoundation.kCFBooleanTrue)
        val result = memScoped {
            val r = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, r.ptr)
            if (status != errSecSuccess) null else r.value
        }
        CFRelease(query)
        val data = result?.reinterpret<cnames.structs.__CFData>() ?: return null
        val bytes = ByteArray(CFDataGetLength(data).toInt())
        bytes.usePinned {
            platform.posix.memcpy(it.addressOf(0), CFDataGetBytePtr(data), bytes.size.convert())
        }
        CFRelease(data)
        return bytes.decodeToString()
    }

    private fun writeString(key: String, value: String) {
        val del = queryFor(key)
        SecItemDelete(del)
        CFRelease(del)
        val query = queryFor(key)
        val bytes = value.encodeToByteArray()
        val data = bytes.usePinned {
            CFDataCreate(null, it.addressOf(0).reinterpret(), bytes.size.convert())
        }
        CFDictionarySetValue(query, kSecValueData, data)
        SecItemAdd(query, null)
        CFRelease(data)
        CFRelease(query)
    }

    override fun getString(key: String, default: String?): String? =
        readString(key) ?: default

    override fun putString(key: String, value: String) = writeString(key, value)

    override fun getBoolean(key: String, default: Boolean): Boolean =
        readString(key)?.toBooleanStrictOrNull() ?: default

    override fun putBoolean(key: String, value: Boolean) = writeString(key, value.toString())

    override fun getInt(key: String, default: Int): Int =
        readString(key)?.toIntOrNull() ?: default

    override fun putInt(key: String, value: Int) = writeString(key, value.toString())

    override fun getStringSet(key: String, default: Set<String>): Set<String> =
        readString(key)?.split('\u0001')?.filter { it.isNotEmpty() }?.toSet() ?: default

    override fun putStringSet(key: String, value: Set<String>) =
        writeString(key, value.joinToString("\u0001"))

    override fun getLong(key: String, default: Long): Long =
        readString(key)?.toLongOrNull() ?: default

    override fun putLong(key: String, value: Long) = writeString(key, value.toString())

    override fun contains(key: String): Boolean = readString(key) != null

    override fun remove(key: String) {
        val query = queryFor(key)
        SecItemDelete(query)
        CFRelease(query)
    }

    override fun clear() {
        val d = CFDictionaryCreateMutable(
            null, 0,
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr,
        )!!
        CFDictionarySetValue(d, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(d, kSecAttrService, cfString(service))
        SecItemDelete(d)
        CFRelease(d)
    }
}

internal actual fun createKeyValueStore(name: String, secure: Boolean): KeyValueStore =
    if (secure) KeychainKeyValueStore(name) else DefaultsKeyValueStore(name)
