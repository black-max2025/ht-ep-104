package app.ht104.control.control

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent

/** Uses Android's own media-key dispatch and volume APIs; AVRCP itself is handled by the OS. */
class AudioController(context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)

    fun isPlaying(): Boolean = audio?.isMusicActive == true

    fun play() = sendKey(KeyEvent.KEYCODE_MEDIA_PLAY)
    fun pause() = sendKey(KeyEvent.KEYCODE_MEDIA_PAUSE)
    fun next() = sendKey(KeyEvent.KEYCODE_MEDIA_NEXT)
    fun previous() = sendKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)

    fun volumeUp() = audio?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
    fun volumeDown() = audio?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)

    fun volume(): Int = audio?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
    fun maxVolume(): Int = audio?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 1

    fun setVolume(level: Int) {
        audio?.setStreamVolume(AudioManager.STREAM_MUSIC, level.coerceIn(0, maxVolume()), 0)
    }

    private fun sendKey(code: Int) {
        val am = audio ?: return
        val now = SystemClock.uptimeMillis()
        am.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0))
        am.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0))
    }
}
