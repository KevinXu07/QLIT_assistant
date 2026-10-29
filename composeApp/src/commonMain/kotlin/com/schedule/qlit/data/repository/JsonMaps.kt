package com.schedule.qlit.data.repository

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull

/**
 * Gson `Map<String, Any?>` 语义的 kotlinx 桥：数字→Double、嵌套 Map/List、
 * `null`→JsonNull。各仓库共用以保持与旧 Gson 读写路径的行为一致。
 */
internal val lenientJson = Json { ignoreUnknownKeys = true }

internal fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is JsonElement -> this
    is Boolean -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is String -> JsonPrimitive(this)
    is Map<*, *> -> JsonObject(
        entries.associate { (k, v) -> k.toString() to v.toJsonElement() }
    )
    is Iterable<*> -> JsonArray(map { it.toJsonElement() })
    else -> JsonPrimitive(toString())
}

internal fun JsonElement?.toAnyValue(): Any? = when (this) {
    null, JsonNull -> null
    is JsonObject -> entries.associate { (k, v) -> k to v.toAnyValue() }
    is JsonArray -> map { it.toAnyValue() }
    is JsonPrimitive -> when {
        isString -> content
        else -> booleanOrNull ?: doubleOrNull ?: content
    }
}

internal fun parseJsonMap(raw: String): Map<String, Any?> =
    (lenientJson.parseToJsonElement(raw) as? JsonObject)
        ?.entries?.associate { (k, v) -> k to v.toAnyValue() }
        ?: emptyMap()

/** `URLEncoder.encode(s, "UTF-8")` 的 common 等价：unreserved 原样，空格→`+`。 */
internal fun urlEncodeForm(value: String): String {
    val out = StringBuilder(value.length)
    for (byte in value.encodeToByteArray()) {
        val c = byte.toInt().toChar()
        when {
            c.isLetterOrDigit() || c == '.' || c == '-' || c == '*' || c == '_' ->
                out.append(c)
            c == ' ' -> out.append('+')
            else -> out.append('%').append(
                (byte.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0')
            )
        }
    }
    return out.toString()
}
