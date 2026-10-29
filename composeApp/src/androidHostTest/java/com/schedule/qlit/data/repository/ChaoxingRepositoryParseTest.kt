package com.schedule.qlit.data.repository

import com.schedule.qlit.data.model.ChaoxingTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 用 docs/05-安卓小助手接入交接.md §3.1 / §3.2 给出的 HTML 结构片段，
 * 验证 ChaoxingRepository 的作业 / 考试列表解析。
 *
 * 由于无真实账号无法跑端到端，这里保证：协议字段、状态映射、clazzId/classId
 * 差异、HTML 实体转义这几条易错路径正确。
 */
class ChaoxingRepositoryParseTest {

    // ── 作业 ──────────────────────────────────────────────────

    @Test
    fun `parses work item with full fields`() {
        val html = """
            <ul>
            <li onclick="goTask(this);" data="https://mooc1-api.chaoxing.com/mooc-ans/android/mtaskmsgspecial?taskrefId=abc123&courseId=100&clazzId=200&type=work&enc_task=xyz">
              <p>高等数学第三次作业</p>
              <span class="status">未提交</span>
              <span aria-hidden="true">《高等数学D2》</span>
              <span class="fr">剩余129小时43分钟</span>
            </li>
            </ul>
        """.trimIndent()

        val items = ChaoxingRepository.parseWorkItems(html)
        assertEquals(1, items.size)
        val item = items[0]
        assertEquals(ChaoxingTask.TYPE_WORK, item.type)
        assertEquals("高等数学第三次作业", item.title)
        assertEquals("高等数学D2", item.courseName)
        assertEquals("100", item.courseId)
        assertEquals("200", item.classId)   // clazzId（双 z）
        assertEquals("abc123", item.taskrefId)
        assertEquals("未提交", item.statusText)
        assertTrue(item.pending)
        assertEquals(0, item.statusCode)
        assertEquals("剩余129小时43分钟", item.remainText)
        assertTrue(item.detailUrl.contains("#INNER"))
        assertTrue(item.detailUrl.startsWith("https://"))
    }

    @Test
    fun `work pending status mapping`() {
        // 未提交 → pending
        assertWorkStatus("未提交", expectedPending = true, expectedCode = 0)
        // 待批阅 → 非待完成
        assertWorkStatus("待批阅", expectedPending = false, expectedCode = 1)
        // 已完成 → 非待完成
        assertWorkStatus("已完成", expectedPending = false, expectedCode = 2)
    }

    @Test
    fun `work HTML entity and whitespace are unescaped`() {
        val html = """
            <li onclick="goTask(this);" data="https://x/y?taskrefId=t1&courseId=1&clazzId=2&type=work">
              <p>线代 &amp; 概率论&nbsp;作业</p>
              <span class="status">未提交</span>
            </li>
        """.trimIndent()

        val item = ChaoxingRepository.parseWorkItems(html).single()
        assertEquals("线代 & 概率论 作业", item.title)
    }

    @Test
    fun `work without course span still parses`() {
        val html = """
            <li onclick="goTask(this);" data="/mooc-ans/android/mtaskmsgspecial?taskrefId=t2&courseId=9&clazzId=8&type=work">
              <p>无课程名作业</p>
              <span class="status">已完成</span>
            </li>
        """.trimIndent()

        val item = ChaoxingRepository.parseWorkItems(html).single()
        assertEquals("无课程名作业", item.title)
        assertEquals("", item.courseName)
        assertFalse(item.pending)
        // 相对 URL 补成绝对
        assertTrue(item.detailUrl.startsWith("https://mooc1-api.chaoxing.com/"))
    }

    @Test
    fun `work completed status parses without status class`() {
        val html = """
            <li onclick="goTask(this);" data="/mooc-ans/android/mtaskmsgspecial?taskrefId=t3&courseId=9&clazzId=8&type=work">
              <p>已完成状态作业</p>
              <span>已完成</span>
              <span aria-hidden="true">《大学英语》</span>
              <span class="fr">已截止</span>
            </li>
        """.trimIndent()

        val item = ChaoxingRepository.parseWorkItems(html).single()
        assertEquals("已完成", item.statusText)
        assertFalse(item.pending)
        assertEquals(2, item.statusCode)
        assertEquals("大学英语", item.courseName)
    }

    @Test
    fun `work completed status parses when hidden span is status`() {
        val html = """
            <li onclick="goTask(this);" data="/mooc-ans/android/mtaskmsgspecial?taskrefId=t4&courseId=9&clazzId=8&type=work">
              <p>多选题</p>
              <span aria-hidden="true">已完成</span>
              <span class="fr">剩余396小时19分钟</span>
            </li>
        """.trimIndent()

        val item = ChaoxingRepository.parseWorkItems(html).single()
        assertEquals("多选题", item.title)
        assertEquals("已完成", item.statusText)
        assertFalse(item.pending)
        assertEquals(2, item.statusCode)
        assertEquals("", item.courseName)
        assertEquals("剩余396小时19分钟", item.remainText)
    }

    @Test
    fun `parses multiple work items`() {
        val html = buildString {
            repeat(3) { i ->
                append("""
                    <li onclick="goTask(this);" data="https://h/u?taskrefId=$i&courseId=$i&clazzId=$i&type=work">
                      <p>作业$i</p><span class="status">未提交</span>
                    </li>
                """.trimIndent())
            }
        }
        val items = ChaoxingRepository.parseWorkItems(html)
        assertEquals(3, items.size)
        assertEquals(listOf("作业0", "作业1", "作业2"), items.map { it.title })
        assertTrue(items.all { it.pending })
    }

    private fun assertWorkStatus(stateText: String, expectedPending: Boolean, expectedCode: Int) {
        val html = """
            <li onclick="goTask(this);" data="https://x/y?taskrefId=t&courseId=1&clazzId=2&type=work">
              <p>x</p><span class="status">$stateText</span>
            </li>
        """.trimIndent()
        val item = ChaoxingRepository.parseWorkItems(html).single()
        assertEquals("status code for '$stateText'", expectedCode, item.statusCode)
        assertEquals("pending for '$stateText'", expectedPending, item.pending)
    }

    // ── 考试 ──────────────────────────────────────────────────

    @Test
    fun `parses exam item with full fields`() {
        val html = """
            <ul>
            <li onclick="goTask(this);" data="/exam-ans/exam/phone/task-exam?taskrefId=e1&courseId=300&classId=400&from=examlist" title="期末考试">
              <dt>期末考试</dt>
              <dd>剩余585小时43分钟</dd>
              <span class="ks_state">未交</span>
            </li>
            </ul>
        """.trimIndent()

        val items = ChaoxingRepository.parseExamItems(html)
        assertEquals(1, items.size)
        val item = items[0]
        assertEquals(ChaoxingTask.TYPE_EXAM, item.type)
        assertEquals("期末考试", item.title)        // 取自 title 属性
        assertEquals("", item.courseName)            // 考试页无课程名
        assertEquals("300", item.courseId)
        assertEquals("400", item.classId)            // classId（双 s）
        assertEquals("e1", item.taskrefId)
        assertEquals("未交", item.statusText)
        assertTrue(item.pending)
        assertEquals(0, item.statusCode)
        assertEquals("剩余585小时43分钟", item.remainText)
        // 考试 URL 补 ut=s 与 #INNER
        assertTrue(item.detailUrl.contains("ut=s"))
        assertTrue(item.detailUrl.contains("#INNER"))
    }

    @Test
    fun `exam pending status mapping`() {
        assertExamStatus("未交", expectedPending = true, expectedCode = 0)
        assertExamStatus("待批阅", expectedPending = false, expectedCode = 1)
        assertExamStatus("已完成", expectedPending = false, expectedCode = 2)
    }

    @Test
    fun `exam without title attr falls back to dt`() {
        val html = """
            <li onclick="goTask(this);" data="/exam-ans/exam/phone/task-exam?taskrefId=e2&courseId=3&classId=4" title="">
              <p>用 p 标签兜底的标题</p>
              <dd>剩余10小时</dd>
              <span class="ks_state">未交</span>
            </li>
        """.trimIndent()
        val item = ChaoxingRepository.parseExamItems(html).single()
        assertEquals("用 p 标签兜底的标题", item.title)
    }

    @Test
    fun `enriches truncated work course name from course list`() {
        val work = ChaoxingTask(
            type = ChaoxingTask.TYPE_WORK,
            title = "作业2",
            courseName = "中国近现代史...",
            courseId = "228946679",
            classId = "141328299"
        )
        val courseMap = ChaoxingRepository.parseCourseNameMap(
            """
                {
                  "channelList": [
                    {
                      "course": {
                        "id": 228946679,
                        "name": "中国近现代史纲要",
                        "classId": 141328299
                      }
                    }
                  ]
                }
            """.trimIndent()
        )

        val enriched = ChaoxingRepository.enrichCourseNames(listOf(work), emptyList(), courseMap).first.single()
        assertEquals("中国近现代史纲要", enriched.courseName)
    }

    @Test
    fun `enriches exam course name by course and class id`() {
        val work = ChaoxingTask(
            type = ChaoxingTask.TYPE_WORK,
            title = "期末作业",
            courseName = "齐鲁自然文化",
            courseId = "263139612",
            classId = "146485067"
        )
        val exam = ChaoxingTask(
            type = ChaoxingTask.TYPE_EXAM,
            title = "碧波沧海小测",
            courseName = "",
            courseId = "263139612",
            classId = "146485067"
        )

        val enriched = ChaoxingRepository.enrichCourseNames(listOf(work), listOf(exam)).second.single()
        assertEquals("齐鲁自然文化", enriched.courseName)
    }

    private fun assertExamStatus(stateText: String, expectedPending: Boolean, expectedCode: Int) {
        val html = """
            <li onclick="goTask(this);" data="/e?taskrefId=e&courseId=1&classId=2" title="x">
              <dt>x</dt><dd>t</dd><span class="ks_state">$stateText</span>
            </li>
        """.trimIndent()
        val item = ChaoxingRepository.parseExamItems(html).single()
        assertEquals("status code for '$stateText'", expectedCode, item.statusCode)
        assertEquals("pending for '$stateText'", expectedPending, item.pending)
    }

    // ── 边界 ─────────────────────────────────────────────────

    @Test
    fun `empty html returns empty list`() {
        assertTrue(ChaoxingRepository.parseWorkItems("").isEmpty())
        assertTrue(ChaoxingRepository.parseExamItems("").isEmpty())
    }

    @Test
    fun `login redirected html does not crash and yields nothing`() {
        val loginPageHtml = "<html><body>请输入手机号 请输入密码 <div>passport2.chaoxing.com/login</div></body></html>"
        assertTrue(ChaoxingRepository.parseWorkItems(loginPageHtml).isEmpty())
        assertTrue(ChaoxingRepository.parseExamItems(loginPageHtml).isEmpty())
    }
}
