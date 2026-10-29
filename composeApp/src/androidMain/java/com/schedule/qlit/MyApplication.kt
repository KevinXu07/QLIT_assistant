package com.schedule.qlit

import android.app.Application
import com.schedule.qlit.data.repository.UsageStatsReporter
import com.schedule.qlit.data.store.AppContext

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppContext.init(this)
        // 小米小部件要求 Widget 刷新跑在独立 ":widgetProvider" 进程；
        // 后台统计只在主进程初始化，避免在小部件进程里被反复拉起。
        if (android.app.Application.getProcessName() != packageName) return
        // 匿名使用统计：每天首启一条 ping，后台静默，失败无感
        UsageStatsReporter.reportOnceDailyAsync()
    }
}
