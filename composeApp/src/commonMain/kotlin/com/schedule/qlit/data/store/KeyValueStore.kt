package com.schedule.qlit.data.store

/**
 * 跨平台键值存储。Android 对应 SharedPreferences / EncryptedSharedPreferences，
 * iOS 对应 NSUserDefaults / Keychain。
 *
 * 非 string 类型（boolean/long）在各平台以原生类型存取。
 */
interface KeyValueStore {
    fun getString(key: String, default: String? = null): String?
    fun putString(key: String, value: String)
    fun getBoolean(key: String, default: Boolean = false): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getInt(key: String, default: Int = 0): Int
    fun putInt(key: String, value: Int)
    fun getLong(key: String, default: Long = 0L): Long
    fun putLong(key: String, value: Long)
    fun getStringSet(key: String, default: Set<String> = emptySet()): Set<String>
    fun putStringSet(key: String, value: Set<String>)
    fun contains(key: String): Boolean
    fun remove(key: String)
    fun clear()
}

internal expect fun createKeyValueStore(name: String, secure: Boolean): KeyValueStore

/** 取一个键值存储实例。[secure]=true 时走加密/安全存储。 */
internal fun kvStore(name: String, secure: Boolean = false): KeyValueStore =
    createKeyValueStore(name, secure)
