package com.schedule.qlit.data.store

/**
 * 应用私有目录下的文本文件存取（Android filesDir / iOS Documents）。
 * `writeTextAtomic` 先写临时文件再原子替换，避免读到半截内容。
 */
internal expect object FileStore {
    /** 文件内容；不存在或读取失败返回 null。 */
    fun readText(name: String): String?

    /** 原子写：临时文件 + rename（iOS 走 NSString atomically 写）。 */
    fun writeTextAtomic(name: String, content: String)

    fun delete(name: String)
}
