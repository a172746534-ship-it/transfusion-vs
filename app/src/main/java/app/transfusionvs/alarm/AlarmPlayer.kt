package app.transfusionvs.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import app.transfusionvs.R

/** Loops the alarm sound and vibration. Only ever used inside AlarmService. */
class AlarmPlayer(private val context: Context) {

    private val alarmAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM) // rings even on silent / vibrate
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private var media: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var focus: AudioFocusRequest? = null

    /** [turn]: a turning round has its own chime and vibration; transfusions keep the alarm sound. */
    fun start(soundOn: Boolean, soundUri: String?, turn: Boolean = false) {
        if (soundOn) startSound(soundUri, turn)
        startVibration(turn)
    }

    fun stop() {
        media?.runCatching { stop() }
        media?.release()
        media = null
        vibrator?.cancel()
        vibrator = null
        focus?.let { context.getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }
        focus = null
    }

    private fun startSound(customUri: String?, turn: Boolean) {
        val audio = context.getSystemService(AudioManager::class.java)
        focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(alarmAttributes)
            .build()
            .also { runCatching { audio.requestAudioFocus(it) } }

        // Try the chosen sound, then the phone's alarm sound, then our bundled tone (the only one
        // readable before the first unlock after a reboot).
        val turnCandidates = listOfNotNull(
            customUri?.let(Uri::parse),
            Uri.parse("android.resource://${context.packageName}/${R.raw.turn_chime}"),
        )
        val candidates = if (turn) turnCandidates else listOfNotNull(
            customUri?.let(Uri::parse),
            runCatching { RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM) }.getOrNull(),
            android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI,
            Uri.parse("android.resource://${context.packageName}/${R.raw.alarm_tone}"),
        )
        for (uri in candidates) {
            val player = MediaPlayer()
            try {
                player.setAudioAttributes(alarmAttributes)
                player.setDataSource(context, uri)
                player.isLooping = true
                player.prepare()
                player.start()
                media = player
                return
            } catch (e: Exception) {
                player.release()
            }
        }
    }

    private fun startVibration(turn: Boolean) {
        val v = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        if (v == null || !v.hasVibrator()) return
        vibrator = v
        // Repeat until stopped. Turns: a double pulse; transfusions: one long buzz.
        val timings = if (turn) longArrayOf(0, 250, 150, 250, 1100) else longArrayOf(0, 800, 600)
        val pattern = VibrationEffect.createWaveform(timings, 0)
        if (Build.VERSION.SDK_INT >= 33) {
            v.vibrate(pattern, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(pattern, alarmAttributes)
        }
    }
}
