package app.ht104.control.data

import app.ht104.control.diagnostics.EventLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** In-memory event log shared by receivers and the diagnostics screen. */
object ConnectionEventLog {
    private val _entries = MutableStateFlow<List<EventLogEntry>>(emptyList())
    val entries: StateFlow<List<EventLogEntry>> = _entries.asStateFlow()

    fun add(source: String, message: String) {
        _entries.update { (it + EventLogEntry(System.currentTimeMillis(), source, message)).takeLast(300) }
    }
}
