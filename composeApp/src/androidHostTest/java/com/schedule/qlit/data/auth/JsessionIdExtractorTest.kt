package com.schedule.qlit.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JsessionIdExtractorTest {

    @Test
    fun selectsCookieThatAppliesToStudentPathWithoutRequiringNodeSuffix() {
        val result = select(
            candidate("mj-session", path = "/mj_view"),
            candidate("student-session", path = "/student"),
        )

        assertEquals("student-session", result?.value)
    }

    @Test
    fun prefersMoreSpecificStudentPathBeforeStickyRouteHint() {
        val result = select(
            candidate("root-session-n1.tomcat1", path = "/"),
            candidate("student-session", path = "/student"),
        )

        assertEquals("student-session", result?.value)
    }

    @Test
    fun skipsExpiredAndPreviouslyRejectedValues() {
        val result = JsessionIdExtractor.selectStudentJsession(
            candidates = listOf(
                candidate("expired", path = "/student", expires = 999.0),
                candidate("rejected", path = "/student"),
                candidate("fresh", path = "/student"),
            ),
            ignoredValues = setOf("rejected"),
            nowEpochSeconds = 1_000.0,
        )

        assertEquals("fresh", result?.value)
    }

    @Test
    fun rejectsForeignDomainAndUnrelatedPath() {
        val result = select(
            candidate("foreign", domain = ".example.com", path = "/student"),
            candidate("unrelated", path = "/other"),
        )

        assertNull(result)
    }

    private fun select(
        vararg candidates: JsessionIdExtractor.CookieCandidate,
    ): JsessionIdExtractor.Result? = JsessionIdExtractor.selectStudentJsession(
        candidates = candidates.toList(),
        nowEpochSeconds = 1_000.0,
    )

    private fun candidate(
        value: String,
        domain: String = "pass.qlit.edu.cn",
        path: String,
        expires: Double = -1.0,
    ) = JsessionIdExtractor.CookieCandidate(value, domain, path, expires)
}
