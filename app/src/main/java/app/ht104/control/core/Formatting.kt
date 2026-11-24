package app.ht104.control.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatTime(ms: Long): String =
    if (ms <= 0L) "—" else SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(ms))

/** Heuristic only: used to highlight likely HT-EP104 candidates, never to assume capabilities. */
object TargetMatcher {
    fun matchesName(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        val n = name.lowercase().filter { it.isLetterOrDigit() }
        return n.contains("ep104") || n.contains("hootoo")
    }
}
