package app.transfusionvs.alarm

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Haptics with meaning: "confirmed" is two quick taps, unlike the alarm's long buzz. */
object Haptics {
    /** Off, gap, on, gap, on (ms). */
    private val CONFIRMED = longArrayOf(0, 40, 70, 40)

    fun confirmed(context: Context) {
        val v = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        if (v != null && v.hasVibrator()) v.vibrate(VibrationEffect.createWaveform(CONFIRMED, -1))
    }
}
