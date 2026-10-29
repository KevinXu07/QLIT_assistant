package com.schedule.qlit.data

import kotlinx.coroutines.CoroutineDispatcher

/**
 * 供仓储层跑阻塞/IO 工作的调度器。
 * common 里 `Dispatchers.IO` 是 internal，故各平台 actual：
 * Android → Dispatchers.IO；iOS（K/N）→ Dispatchers.Default。
 */
internal expect val IODispatcher: CoroutineDispatcher
