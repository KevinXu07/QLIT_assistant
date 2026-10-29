package com.schedule.qlit.data.store

import com.schedule.qlit.data.repository.UpdateManager

actual fun platformIsUpdateCheckEnabled(): Boolean =
    UpdateManager.isUpdateEnabled(AppContext.context)

actual fun platformSetUpdateCheckEnabled(enabled: Boolean) =
    UpdateManager.setUpdateEnabled(AppContext.context, enabled)
