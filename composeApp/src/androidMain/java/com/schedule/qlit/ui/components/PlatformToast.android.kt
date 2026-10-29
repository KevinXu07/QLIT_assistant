package com.schedule.qlit.ui.components

import android.widget.Toast
import com.schedule.qlit.data.store.AppContext

internal actual fun showPlatformToast(message: String) {
    Toast.makeText(AppContext.context, message, Toast.LENGTH_SHORT).show()
}
