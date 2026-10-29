package com.schedule.qlit.data.store

import java.io.File

internal actual object FileStore {

    private fun dir(): File = AppContext.context.filesDir

    actual fun readText(name: String): String? {
        val file = File(dir(), name)
        if (!file.exists()) return null
        return runCatching { file.readText() }.getOrNull()
    }

    actual fun writeTextAtomic(name: String, content: String) {
        val target = File(dir(), name)
        val tmp = File(dir(), "$name.tmp")
        tmp.writeText(content)
        if (!tmp.renameTo(target)) {
            // rename 失败（跨文件系统等）退化为复制式替换，仍避免直接截断正式文件
            java.nio.file.Files.move(
                tmp.toPath(), target.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
            )
        }
    }

    actual fun delete(name: String) {
        File(dir(), name).delete()
    }
}
