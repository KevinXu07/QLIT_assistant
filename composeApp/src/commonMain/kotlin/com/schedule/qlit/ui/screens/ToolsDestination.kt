package com.schedule.qlit.ui.screens

/** 工具页对外可见的目的地名称，深链和导航共用这一份定义。 */
enum class ToolsDestination {
    LIST,
    TAOBAO_PICKUP,
    CHAOXING,
    ;

    companion object {
        val deepLinkNames: Set<String> = entries
            .filter { it != LIST }
            .mapTo(linkedSetOf()) { it.name }

        fun fromName(name: String): ToolsDestination? =
            entries.firstOrNull { it.name == name }
    }
}
