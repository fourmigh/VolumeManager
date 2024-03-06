package org.caojun.volumemanager

import android.media.AudioManager
import androidx.compose.ui.unit.dp

object Constant {

    enum class AudioStream(val int: Int) {
        VOICE_CALL(AudioManager.STREAM_VOICE_CALL),
        SYSTEM(AudioManager.STREAM_SYSTEM),
        RING(AudioManager.STREAM_RING),
        MUSIC(AudioManager.STREAM_MUSIC),
        ALARM(AudioManager.STREAM_ALARM),
        NOTIFICATION(AudioManager.STREAM_NOTIFICATION),
        DTMF(AudioManager.STREAM_DTMF),
        ACCESSIBILITY(AudioManager.STREAM_ACCESSIBILITY)
    }

    val SPACE_NORMAL = 16.dp
    val SPACE_SMALL = 16.dp

    const val VOLUME_MAX = 15
    const val VOLUME_MIN = 0
}