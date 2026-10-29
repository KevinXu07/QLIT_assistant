package com.schedule.qlit.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * TaobaoHomeRepository.parseJsonpPayload 测试。
 *
 * 淘宝 MTop 接口返回 `mtopjsonp_xxx({...})` 形式的 JSONP 包装，
 * 这里钉住括号裁剪逻辑，包括嵌套括号和异常输入。
 */
class TaobaoJsonpParseTest {

    @Test
    fun `extracts json from standard jsonp wrapper`() {
        val raw = """mtopjsonp_home({"status":200,"data":{"count":3}})"""
        val json = TaobaoHomeRepository.parseJsonpPayload(raw)
        assertEquals("""{"status":200,"data":{"count":3}}""", json)
    }

    @Test
    fun `uses first and last paren to handle nested braces`() {
        // 内部 JSON 本身含括号时，靠最外层 () 裁剪
        val raw = """mtopjsonp_native({"fn":"a()b","x":1})"""
        val json = TaobaoHomeRepository.parseJsonpPayload(raw)
        assertEquals("""{"fn":"a()b","x":1}""", json)
    }

    @Test
    fun `returns null when no parens`() {
        assertNull(TaobaoHomeRepository.parseJsonpPayload("plain json without wrapper"))
    }

    @Test
    fun `returns null when closing paren before opening`() {
        // ")x(" 之类异常：end <= start 时返回 null
        assertNull(TaobaoHomeRepository.parseJsonpPayload(")invalid("))
    }

    @Test
    fun `handles empty payload`() {
        val raw = "mtopjsonp_home()"
        assertEquals("", TaobaoHomeRepository.parseJsonpPayload(raw))
    }
}
