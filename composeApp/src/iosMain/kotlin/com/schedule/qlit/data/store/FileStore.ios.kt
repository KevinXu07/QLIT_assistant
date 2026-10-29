package com.schedule.qlit.data.store

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

@OptIn(ExperimentalForeignApi::class)
internal actual object FileStore {

    private fun documentsPath(): String =
        NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory, NSUserDomainMask, true,
        ).first() as String

    private fun pathFor(name: String): String = "${documentsPath()}/$name"

    actual fun readText(name: String): String? =
        runCatching {
            NSString.stringWithContentsOfFile(pathFor(name), NSUTF8StringEncoding, null)
        }.getOrNull()

    actual fun writeTextAtomic(name: String, content: String) {
        // atomically：系统内部临时文件 + rename，避免半截内容
        (content as NSString).writeToFile(
            pathFor(name), atomically = true,
            encoding = NSUTF8StringEncoding, error = null,
        )
    }

    actual fun delete(name: String) {
        NSFileManager.defaultManager.removeItemAtPath(pathFor(name), null)
    }
}
