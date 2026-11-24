package app.ht104.control.core

/** Minimal dependency-free JSON encoder (Map / Iterable / String / Number / Boolean / null). */
object Json {
    fun encode(value: Any?, pretty: Boolean = true): String {
        val sb = StringBuilder()
        write(sb, value, 0, pretty)
        return sb.toString()
    }

    private fun newline(sb: StringBuilder, level: Int, pretty: Boolean) {
        if (!pretty) return
        sb.append('\n')
        repeat(level) { sb.append("  ") }
    }

    private fun write(sb: StringBuilder, v: Any?, level: Int, pretty: Boolean) {
        when (v) {
            null -> sb.append("null")
            is String -> quote(sb, v)
            is Boolean -> sb.append(v.toString())
            is Number -> sb.append(v.toString())
            is Map<*, *> -> {
                if (v.isEmpty()) {
                    sb.append("{}")
                    return
                }
                sb.append('{')
                var first = true
                for ((k, x) in v) {
                    if (!first) sb.append(',')
                    first = false
                    newline(sb, level + 1, pretty)
                    quote(sb, k.toString())
                    sb.append(if (pretty) ": " else ":")
                    write(sb, x, level + 1, pretty)
                }
                newline(sb, level, pretty)
                sb.append('}')
            }
            is Iterable<*> -> {
                val items = v.toList()
                if (items.isEmpty()) {
                    sb.append("[]")
                    return
                }
                sb.append('[')
                var first = true
                for (x in items) {
                    if (!first) sb.append(',')
                    first = false
                    newline(sb, level + 1, pretty)
                    write(sb, x, level + 1, pretty)
                }
                newline(sb, level, pretty)
                sb.append(']')
            }
            else -> quote(sb, v.toString())
        }
    }

    private fun quote(sb: StringBuilder, s: String) {
        sb.append('"')
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c.code < 0x20) sb.append("\\u%04x".format(c.code)) else sb.append(c)
            }
        }
        sb.append('"')
    }
}
