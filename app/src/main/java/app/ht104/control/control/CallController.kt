package app.ht104.control.control

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.telecom.TelecomManager

enum class CallState { IDLE, RINGING, IN_CALL }

/** Call control through the public Telecom API. Does not need default-dialer privileges. */
@SuppressLint("MissingPermission")
class CallController(context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val telecom = context.getSystemService(TelecomManager::class.java)

    /** Derived from the audio mode, which needs no extra permission. */
    fun state(): CallState = when (audio?.mode) {
        AudioManager.MODE_RINGTONE -> CallState.RINGING
        AudioManager.MODE_IN_CALL, AudioManager.MODE_IN_COMMUNICATION -> CallState.IN_CALL
        else -> CallState.IDLE
    }

    fun answer(): Boolean = runCatching { telecom?.acceptRingingCall(); true }.getOrDefault(false)

    /** endCall() is available from Android 9; it also rejects a ringing call. */
    fun endOrReject(): Boolean {
        if (Build.VERSION.SDK_INT < 28) return false
        @Suppress("DEPRECATION")
        return runCatching { telecom?.endCall() == true }.getOrDefault(false)
    }

    fun endCallSupported(): Boolean = Build.VERSION.SDK_INT >= 28

    fun isMicMuted(): Boolean = audio?.isMicrophoneMute == true

    fun setMicMuted(muted: Boolean) {
        audio?.isMicrophoneMute = muted
    }
}
