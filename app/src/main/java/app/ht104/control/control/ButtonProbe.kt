package app.ht104.control.control

import android.content.Context
import android.content.Intent
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.view.KeyEvent
import androidx.core.content.IntentCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Observes the media-button events the phone receives from the headset's physical buttons.
 * While active it consumes those events (they will not control other players). It cannot observe
 * the call answer/reject button during a call: that is handled by the system.
 */
class ButtonProbe(private val context: Context) {
    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log.asStateFlow()
    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    private var session: MediaSession? = null
    private var lastDownTime = 0L

    fun start() {
        if (session != null) return
        val s = MediaSession(context, "HT104ButtonProbe")
        s.setCallback(object : MediaSession.Callback() {
            override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                val ev = IntentCompat.getParcelableExtra(mediaButtonIntent, Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                if (ev != null) record(ev)
                return true
            }
        })
        s.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS,
                )
                .setState(PlaybackState.STATE_PLAYING, 0L, 1f)
                .build(),
        )
        s.isActive = true
        session = s
        _active.value = true
        add("Probe started. Press the headset buttons now.")
    }

    fun stop() {
        session?.let {
            it.isActive = false
            it.release()
        }
        session = null
        _active.value = false
        add("Probe stopped.")
    }

    fun clear() {
        _log.value = emptyList()
    }

    private fun record(ev: KeyEvent) {
        val action = if (ev.action == KeyEvent.ACTION_DOWN) "DOWN" else "UP"
        val longPress = (ev.flags and KeyEvent.FLAG_LONG_PRESS) != 0
        var delta = ""
        if (ev.action == KeyEvent.ACTION_DOWN) {
            if (lastDownTime != 0L) delta = " +${ev.eventTime - lastDownTime}ms"
            lastDownTime = ev.eventTime
        }
        add("${KeyEvent.keyCodeToString(ev.keyCode)} $action repeat=${ev.repeatCount} longPress=$longPress$delta")
    }

    private fun add(line: String) {
        _log.update { (it + "${System.currentTimeMillis()} $line").takeLast(200) }
    }
}
