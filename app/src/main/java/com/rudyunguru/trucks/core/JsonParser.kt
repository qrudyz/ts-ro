package com.rudyunguru.trucks.core

/** Recursive-descent JSON reader used by [Json]. Lenient about formatting, strict about structure. */
internal class JsonParser(private val s: String) {

    private var i = 0

    fun readDocument(): Any? {
        skipWhitespace()
        val value = readValue()
        skipWhitespace()
        return value
    }

    private fun skipWhitespace() {
        while (i < s.length && s[i].isWhitespace()) i++
    }

    private fun readValue(): Any? {
        if (i >= s.length) return null
        return when (s[i]) {
            '{' -> readObject()
            '[' -> readArray()
            '"' -> readString()
            't' -> { expect("true"); true }
            'f' -> { expect("false"); false }
            'n' -> { expect("null"); null }
            else -> readNumber()
        }
    }

    private fun expect(word: String) {
        require(s.startsWith(word, i)) { "Invalid JSON literal at $i" }
        i += word.length
    }

    private fun readObject(): Map<String, Any?> {
        val map = LinkedHashMap<String, Any?>()
        i++ // '{'
        skipWhitespace()
        if (i < s.length && s[i] == '}') { i++; return map }
        while (i < s.length) {
            skipWhitespace()
            val key = readString()
            skipWhitespace()
            require(i < s.length && s[i] == ':') { "Expected ':' at $i" }
            i++
            skipWhitespace()
            map[key] = readValue()
            skipWhitespace()
            when {
                i < s.length && s[i] == ',' -> { i++; continue }
                i < s.length && s[i] == '}' -> { i++; break }
                else -> break
            }
        }
        return map
    }

    private fun readArray(): List<Any?> {
        val list = ArrayList<Any?>()
        i++ // '['
        skipWhitespace()
        if (i < s.length && s[i] == ']') { i++; return list }
        while (i < s.length) {
            skipWhitespace()
            list.add(readValue())
            skipWhitespace()
            when {
                i < s.length && s[i] == ',' -> { i++; continue }
                i < s.length && s[i] == ']' -> { i++; break }
                else -> break
            }
        }
        return list
    }

    private fun readString(): String {
        require(i < s.length && s[i] == '"') { "Expected string at $i" }
        i++
        val sb = StringBuilder()
        while (i < s.length) {
            val c = s[i++]
            when (c) {
                '"' -> return sb.toString()
                '\\' -> {
                    if (i >= s.length) break
                    when (val esc = s[i++]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            val hex = s.substring(i, (i + 4).coerceAtMost(s.length))
                            i += hex.length
                            sb.append(hex.toIntOrNull(16)?.toChar() ?: '?')
                        }
                        else -> sb.append(esc)
                    }
                }
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun readNumber(): Any {
        val start = i
        if (i < s.length && (s[i] == '-' || s[i] == '+')) i++
        var isFloating = false
        while (i < s.length) {
            val c = s[i]
            when {
                c.isDigit() -> i++
                c == '.' || c == 'e' || c == 'E' || c == '-' || c == '+' -> { isFloating = true; i++ }
                else -> break
            }
        }
        val raw = s.substring(start, i)
        if (raw.isEmpty()) { i++; return 0L }
        return if (isFloating) raw.toDoubleOrNull() ?: 0.0 else raw.toLongOrNull() ?: 0L
    }
}
