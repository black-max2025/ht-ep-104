package app.ht104.control.core

/** "0A FF 10" style hex dump. */
fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }

/** Returns the bytes as text only if every byte is printable ASCII (a trailing NUL is tolerated). */
fun ByteArray.toPrintableText(): String? {
    if (isEmpty()) return null
    val end = if (last().toInt() == 0) size - 1 else size
    if (end <= 0) return null
    val sb = StringBuilder()
    for (i in 0 until end) {
        val v = this[i].toInt() and 0xFF
        if (v < 0x20 || v > 0x7E) return null
        sb.append(v.toChar())
    }
    return sb.toString()
}
