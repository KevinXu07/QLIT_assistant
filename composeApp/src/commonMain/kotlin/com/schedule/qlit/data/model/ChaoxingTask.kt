package com.schedule.qlit.data.model

import androidx.compose.runtime.Immutable

/**
 * 超星学习通待办统一模型，覆盖作业和考试两种类型。
 *
 * 字段来源是两条 HTML 列表页（作业 transfer/stu-work、考试 transfer/examlist），
 * 详见 docs/05-安卓小助手接入交接.md §3。
 *
 * @param type        `work` 作业 / `exam` 考试
 * @param title       作业/考试标题
 * @param courseName  课程名（考试列表页通常缺失，需结合课程列表补全）
 * @param courseId    课程 ID
 * @param classId     班级 ID（注意：作业 URL 用 `clazzId`，考试 URL 用 `classId`）
 * @param taskrefId   任务 ID
 * @param statusText  状态原文（未提交 / 未交 / 待批阅 / 已完成 等）
 * @param pending     是否待完成
 * @param statusCode  0=待完成，1=待批阅，2=已完成，-1=未知
 * @param remainText  剩余时间原文（如“剩余129小时43分钟”）
 * @param detailUrl   详情跳转 URL（解析自列表项的 `data` 属性）
 */
@Immutable
data class ChaoxingTask(
    val type: String = "",
    val title: String = "",
    val courseName: String = "",
    val courseId: String = "",
    val classId: String = "",
    val taskrefId: String = "",
    val statusText: String = "",
    val pending: Boolean = false,
    val statusCode: Int = -1,
    val remainText: String = "",
    val detailUrl: String = ""
) {
    val isWork: Boolean get() = type == TYPE_WORK
    val isExam: Boolean get() = type == TYPE_EXAM

    companion object {
        const val TYPE_WORK = "work"
        const val TYPE_EXAM = "exam"
    }
}

/** 一次拉取作业 + 考试的结构化结果。 */
@Immutable
data class ChaoxingTaskResult(
    val works: List<ChaoxingTask> = emptyList(),
    val exams: List<ChaoxingTask> = emptyList()
) {
    val pendingWorkCount: Int get() = works.count { it.pending }
    val pendingExamCount: Int get() = exams.count { it.pending }
}
