package com.rudyunguru.trucks.core

/**
 * Minimal JSON writer plus typed accessors (no external dependency; identical behaviour on the JVM
 * and on Android). The DOM is plain Kotlin: `Map<String, Any?>`, `List<Any?>`, `String`,
 * `Long`/`Double`, `Boolean`, `null`. Reading lives in [JsonParser].
 */
object Json {

    // ---------------------------------------------------------------- writing

    fun stringify(value: Any?): String = StringBuilder().also { write(it, value) }.toString()

    private fun write(sb: StringBuilder, value: Any?) {
        when (value) {
            null -> sb.append("null")
            is String -> writeString(sb, value)
            is Boolean -> sb.append(if (value) "true" else "false")
            is Double -> sb.append(formatDouble(value))
            is Float -> sb.append(formatDouble(value.toDouble()))
            is Int, is Long, is Short, is Byte -> sb.append(value.toString())
            is Number -> sb.append(formatDouble(value.toDouble()))
            is Map<*, *> -> {
                sb.append('{')
                var first = true
                for ((k, v) in value) {
                    if (v == null) continue
                    if (!first) sb.append(',')
                    first = false
                    writeString(sb, k.toString())
                    sb.append(':')
                    write(sb, v)
                }
                sb.append('}')
            }
            is Iterable<*> -> {
                sb.append('[')
                var first = true
                for (item in value) {
                    if (!first) sb.append(',')
                    first = false
                    write(sb, item)
                }
                sb.append(']')
            }
            is DoubleArray -> write(sb, value.toList())
            is FloatArray -> write(sb, value.map { it.toDouble() })
            is IntArray -> write(sb, value.toList())
            else -> writeString(sb, value.toString())
        }
    }

    private fun formatDouble(v: Double): String {
        if (v.isNaN() || v.isInfinite()) return "0"
        val asLong = v.toLong()
        return if (asLong.toDouble() == v) asLong.toString() else v.toString()
    }

    private fun writeString(sb: StringBuilder, s: String) {
        sb.append('"')
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c < ' ') sb.append("\\u").append(c.code.toString(16).padStart(4, '0')) else sb.append(c)
            }
        }
        sb.append('"')
    }

    // ---------------------------------------------------------------- reading

    /** Parses JSON text into the DOM described above. Returns null when the payload is invalid. */
    fun parse(text: String): Any? = runCatching { JsonParser(text).readDocument() }.getOrNull()

    /** Parses JSON text that is expected to be an object (used by the save system). */
    fun parseObject(text: String): Map<String, Any?>? = parse(text) as? Map<String, Any?>

    // ---------------------------------------------------------------- typed accessors

    @Suppress("UNCHECKED_CAST")
    fun Map<String, Any?>?.obj(key: String): Map<String, Any?>? = this?.get(key) as? Map<String, Any?>

    fun Map<String, Any?>?.list(key: String): List<Any?> = (this?.get(key) as? List<Any?>) ?: emptyList()

    fun Map<String, Any?>?.objs(key: String): List<Map<String, Any?>> =
        list(key).mapNotNull { it as? Map<String, Any?> }

    fun Map<String, Any?>?.strings(key: String): List<String> = list(key).mapNotNull { it as? String }

    fun Map<String, Any?>?.str(key: String, fallback: String = ""): String =
        (this?.get(key) as? String) ?: fallback

    fun Map<String, Any?>?.num(key: String, fallback: Double = 0.0): Double =
        (this?.get(key) as? Number)?.toDouble() ?: fallback

    fun Map<String, Any?>?.long(key: String, fallback: Long = 0L): Long =
        (this?.get(key) as? Number)?.toLong() ?: fallback

    fun Map<String, Any?>?.int(key: String, fallback: Int = 0): Int =
        (this?.get(key) as? Number)?.toInt() ?: fallback

    fun Map<String, Any?>?.bool(key: String, fallback: Boolean = false): Boolean =
        (this?.get(key) as? Boolean) ?: fallback

    fun Any?.asMap(): Map<String, Any?> = this as? Map<String, Any?> ?: emptyMap()

    fun Any?.asList(): List<Any?> = this as? List<Any?> ?: emptyList()

    fun Any?.asString(fallback: String = ""): String = this as? String ?: fallback

    fun Any?.asDouble(fallback: Double = 0.0): Double = (this as? Number)?.toDouble() ?: fallback

    fun Any?.asLong(fallback: Long = 0L): Long = (this as? Number)?.toLong() ?: fallback

    fun Any?.asInt(fallback: Int = 0): Int = (this as? Number)?.toInt() ?: fallback

    fun Any?.asBool(fallback: Boolean = false): Boolean = this as? Boolean ?: fallback
}
