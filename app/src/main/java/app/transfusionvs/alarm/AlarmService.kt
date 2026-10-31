package app.transfusionvs.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.transfusionvs.app
import kotlinx.coroutines.launch

/**
 * Foreground service that rings: loops the alarm sound (USAGE_ALARM, so it is heard on silent
 * and vibrate) plus vibration, and holds the full-screen alarm notification.
 *
 * Type "systemExempted": allowed for apps holding USE_EXACT_ALARM / SCHEDULE_EXACT_ALARM, has no
 * time limit, and satisfies Android 17's background-audio rule together with USAGE_ALARM.
 * The app never plays sound anywhere else.
 */
class AlarmService : Service() {

    private var player: AlarmPlayer? = null
    private var playingTurn = false
    private var wakeLock: PowerManager.WakeLock? = null

    /** Opening the sound file reads the disk, so playback runs on its own thread, in order. */
    private val audioThread = HandlerThread("alarm-audio")
    private lateinit var audio: Handler

    override fun onCreate() {
        super.onCreate()
        instance = this
        audioThread.start()
        audio = Handler(audioThread.looper)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val spec = latest
        // startForeground must be called right away, even if there turns out to be nothing to ring.
        ServiceCompat.startForeground(this, Notifications.ID_ALARM, Notifications.alarm(this, spec), serviceType())
        isRunning = true
        if (spec == null) {
            stopRinging()
            return START_NOT_STICKY
        }
        show(spec)
        // Re-check the database: something may have been acknowledged in the meantime.
        app.scope.launch { app.engine.syncRinging(canStartService = false) }
        return START_NOT_STICKY
    }

    private fun show(spec: RingSpec) {
        Notifications.notifyAlarm(this, spec)
        // A transfusion joining a ringing round (or the reverse) switches to that type's sound.
        if (player != null && playingTurn != spec.turnOnly) {
            player?.let { old -> audio.post { old.stop() } }
            player = null
        }
        if (player == null) {
            if (wakeLock == null) acquireWakeLock(spec.missedTimeoutMin)
            playingTurn = spec.turnOnly
            val uri = if (spec.turnOnly) spec.turnSoundUri else spec.soundUri
            val next = AlarmPlayer(this)
            player = next
            audio.post { next.start(spec.soundOn, uri, spec.turnOnly) }
        }
    }

    private fun acquireWakeLock(missedTimeoutMin: Int) {
        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TransfusionVS:alarm").apply {
            setReferenceCounted(false)
            acquire((missedTimeoutMin + 5) * 60_000L) // safety cap; released when ringing stops
        }
    }

    private fun stopRinging() {
        releasePlayback()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        isRunning = false
        stopSelf()
    }

    private fun releasePlayback() {
        player?.let { old -> audio.post { old.stop() } }
        player = null
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    override fun onDestroy() {
        releasePlayback()
        audioThread.quitSafely()
        isRunning = false
        instance = null
        super.onDestroy()
    }

    private fun serviceType(): Int =
        if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED else 0

    companion object {
        @Volatile
        var isRunning = false
            private set

        @Volatile
        private var latest: RingSpec? = null

        private var instance: AlarmService? = null
        private val main = Handler(Looper.getMainLooper())

        /** Only call where Android allows starting a foreground service (exact alarm, visible screen). */
        fun start(context: Context, spec: RingSpec) {
            latest = spec
            try {
                ContextCompat.startForegroundService(context, Intent(context, AlarmService::class.java))
            } catch (e: Exception) {
                // Fallback: show the full-screen notification anyway. The alarm screen it opens is
                // visible, so it can start this service itself.
                Notifications.notifyAlarm(context, spec)
            }
        }

        fun update(spec: RingSpec) {
            latest = spec
            main.post { instance?.show(spec) }
        }

        fun stop(context: Context) {
            latest = null
            if (!isRunning) {
                Notifications.cancelAlarm(context)
                return
            }
            main.post { instance?.stopRinging() ?: Notifications.cancelAlarm(context) }
        }
    }
}
