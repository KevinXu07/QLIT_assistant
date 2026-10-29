package com.schedule.qlit.data.store

/**
 * 出行登记相关凭据与表单默认值的安全存储。
 *
 * Android 用 EncryptedSharedPreferences（Tink 底层 AES-GCM）、iOS 用 Keychain，
 * 避免把 JSESSIONID 明文落在普通偏好里。表单默认值（地点/事由/紧急电话）
 * 是用户勾选「记住」后才存的。
 */
object CredentialStore {

    private const val FILE_NAME = "qlit_travel_secrets"
    private const val LEGACY_FALLBACK_FILE_NAME = "${FILE_NAME}_fallback"

    private const val KEY_JSESSIONID = "student_jsessionid"
    private const val KEY_REMEMBER = "remember_form"
    private const val KEY_MDD = "form_mdd"
    private const val KEY_SY = "form_sy"
    private const val KEY_JJDH = "form_jjdh"
    private const val KEY_LEGACY_MIGRATED = "legacy_fallback_migrated"
    @kotlin.concurrent.Volatile private var legacyMigrationChecked = false

    /** 表单默认值（地点/事由/紧急电话），由「记住」勾选触发保存。 */
    data class FormDefaults(
        val mdd: String,
        val sy: String,
        val jjdh: String,
    )

    private val legacyKeys = listOf(KEY_JSESSIONID, KEY_MDD, KEY_SY, KEY_JJDH)

    /** 安全存储初始化失败时返回 null——绝不降级为明文持久化。 */
    private fun secureStore(): KeyValueStore? {
        val store = try {
            kvStore(FILE_NAME, secure = true)
        } catch (t: Throwable) {
            println("CredentialStore: secure store 初始化失败，本次不持久化出行凭据: $t")
            return null
        }
        runCatching { migrateLegacyFallback(store) }.onFailure {
            println("CredentialStore: 旧版明文出行凭据迁移失败，将在下次访问时重试: $it")
        }
        return store
    }

    private fun migrateLegacyFallback(secure: KeyValueStore) {
        if (legacyMigrationChecked) return
        val fallback = kvStore(LEGACY_FALLBACK_FILE_NAME)
        val hasLegacy = legacyKeys.any { fallback.contains(it) } || fallback.contains(KEY_REMEMBER)

        if (!hasLegacy) {
            legacyKeys.forEach { fallback.remove(it) }
            fallback.remove(KEY_REMEMBER)
            legacyMigrationChecked = true
            return
        }

        // A previous pass may have committed the secure copy but failed to clear the
        // plaintext keys. In that case only retry cleanup; never overwrite newer secure data.
        if (secure.getBoolean(KEY_LEGACY_MIGRATED, false)) {
            clearLegacy(fallback)
            legacyMigrationChecked = true
            return
        }

        if (fallback.contains(KEY_JSESSIONID)) {
            fallback.getString(KEY_JSESSIONID)?.let { secure.putString(KEY_JSESSIONID, it) }
        }
        if (fallback.contains(KEY_REMEMBER)) {
            secure.putBoolean(KEY_REMEMBER, fallback.getBoolean(KEY_REMEMBER, false))
        }
        if (fallback.contains(KEY_MDD)) {
            fallback.getString(KEY_MDD)?.let { secure.putString(KEY_MDD, it) }
        }
        if (fallback.contains(KEY_SY)) {
            fallback.getString(KEY_SY)?.let { secure.putString(KEY_SY, it) }
        }
        if (fallback.contains(KEY_JJDH)) {
            fallback.getString(KEY_JJDH)?.let { secure.putString(KEY_JJDH, it) }
        }
        secure.putBoolean(KEY_LEGACY_MIGRATED, true)
        clearLegacy(fallback)
        legacyMigrationChecked = true
    }

    private fun clearLegacy(fallback: KeyValueStore) {
        legacyKeys.forEach { fallback.remove(it) }
        fallback.remove(KEY_REMEMBER)
    }

    // ── JSESSIONID ────────────────────────────────────────────

    fun saveJsessionid(value: String) {
        secureStore()?.putString(KEY_JSESSIONID, value.trim())
    }

    fun getJsessionid(): String? =
        secureStore()?.getString(KEY_JSESSIONID)?.takeIf { it.isNotBlank() }

    fun clearJsessionid() {
        secureStore()?.remove(KEY_JSESSIONID)
    }

    // ── 表单默认值（地点/事由/紧急电话）────────────────────────

    fun isRememberEnabled(): Boolean =
        secureStore()?.getBoolean(KEY_REMEMBER, false) ?: false

    fun setRememberEnabled(enabled: Boolean) {
        secureStore()?.putBoolean(KEY_REMEMBER, enabled)
        if (!enabled) clearFormDefaults()
    }

    fun saveFormDefaults(mdd: String, sy: String, jjdh: String) {
        val store = secureStore() ?: return
        store.putString(KEY_MDD, mdd)
        store.putString(KEY_SY, sy)
        store.putString(KEY_JJDH, jjdh)
    }

    fun getFormDefaults(): FormDefaults? {
        if (!isRememberEnabled()) return null
        val p = secureStore() ?: return null
        val mdd = p.getString(KEY_MDD)
        val sy = p.getString(KEY_SY)
        val jjdh = p.getString(KEY_JJDH)
        // 三个字段都有才算有效默认值
        return if (!mdd.isNullOrBlank() || !sy.isNullOrBlank() || !jjdh.isNullOrBlank()) {
            FormDefaults(mdd.orEmpty(), sy.orEmpty(), jjdh.orEmpty())
        } else null
    }

    private fun clearFormDefaults() {
        val store = secureStore() ?: return
        store.remove(KEY_MDD)
        store.remove(KEY_SY)
        store.remove(KEY_JJDH)
    }
}
