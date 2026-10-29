package com.schedule.qlit.data.store

import com.schedule.qlit.data.IODispatcher
import com.schedule.qlit.data.model.ScheduleData
import com.schedule.qlit.data.repository.CourseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * 课表的单一状态入口。
 *
 * 文件读写仍由 CourseRepository 负责；这个类负责把持久化结果和应用内
 * StateFlow 放在同一个生命周期里，避免页面和 ViewModel 各自维护一份课表。
 */
class ScheduleStore {
    private val json = Json { ignoreUnknownKeys = true }
    private val operationMutex = Mutex()
    private val _schedule = MutableStateFlow<ScheduleData?>(null)

    val schedule: StateFlow<ScheduleData?> = _schedule.asStateFlow()

    suspend fun load() {
        _schedule.value = CourseRepository.loadSchedule()
    }

    fun replace(schedule: ScheduleData?) {
        _schedule.value = schedule
    }

    suspend fun save(schedule: ScheduleData) {
        operationMutex.withLock {
            saveLocked(schedule)
        }
    }

    suspend fun importSchedule(schedule: ScheduleData) {
        operationMutex.withLock {
            saveLocked(schedule)
            withContext(IODispatcher) {
                CourseRepository.recordImportTime()
            }
        }
    }

    suspend fun downloadAndSave(url: String): ScheduleData? {
        return operationMutex.withLock {
            if (!CourseRepository.downloadAndSave(url)) return@withLock null
            val schedule = CourseRepository.loadSchedule()
            if (schedule != null) _schedule.value = schedule
            schedule
        }
    }

    suspend fun clear() {
        operationMutex.withLock {
            withContext(IODispatcher) {
                CourseRepository.clearSchedule()
            }
            _schedule.value = null
        }
    }

    private suspend fun saveLocked(schedule: ScheduleData) {
        withContext(IODispatcher) {
            CourseRepository.saveJson(json.encodeToString(schedule))
        }
        _schedule.value = schedule
    }
}
