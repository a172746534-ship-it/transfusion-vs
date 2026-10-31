package app.transfusionvs.alarm

import android.view.MotionEvent

/**
 * One tap on the alarm must never be an accident: touches are ignored for the first second after
 * the screen appears, and while the proximity sensor is covered (phone in a pocket). A gesture that
 * started while guarded stays ignored until the finger lifts. Pure, so it is unit tested.
 */
class PocketGuard(private val armMs: Long = ARM_MS) {
    private var shownAt = Long.MIN_VALUE
    private var swallowing = false

    /** Set from the proximity sensor. */
    var covered = false

    fun onShown(uptimeMs: Long) {
        shownAt = uptimeMs
        swallowing = false
    }

    /** False = drop this touch event. */
    fun allows(action: Int, uptimeMs: Long): Boolean {
        if (action == MotionEvent.ACTION_DOWN) {
            swallowing = covered || (shownAt != Long.MIN_VALUE && uptimeMs - shownAt < armMs)
        }
        val allowed = !swallowing
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) swallowing = false
        return allowed
    }

    companion object {
        const val ARM_MS = 1_000L
    }
}
