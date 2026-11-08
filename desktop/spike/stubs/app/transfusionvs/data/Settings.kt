package app.transfusionvs.data

// Spike stub only (deleted with desktop/spike/): the reference's Settings surface without an Android
// Context, so the copied reference files compile against it and the compile errors left are Android ones.
class Settings(@Suppress("unused") private val file: java.nio.file.Path? = null) {
    var toleranceMin: Int = 5
    var snoozeMin: Int = 2
    var missedTimeoutMin: Int = 10
    var preAlertMin: Int = 0
    var limitMin: Int = 240
    var warnBeforeMin: Int = 15
    var soundOn: Boolean = true
    var alarmSoundUri: String? = null
    var turnIntervalMin: Int = 120
    var turnWindowMin: Int = 15
    var turnSoundUri: String? = null
    var setupSeen: Boolean = false
    var lastDurationMin: Int = 120
    var themeMode: Int = 0
    var nightStartHour: Int = 20
    var nightEndHour: Int = 8

    companion object {
        val HOUR_RANGE = 0..23
        const val MAX_SNOOZES = 3
        val TOLERANCE_RANGE = 1..15
        val SNOOZE_RANGE = 1..2
        val MISSED_RANGE = 5..30
        val LIMIT_RANGE = 60..480
        val WARN_RANGE = 5..60
        val PRE_ALERT_CHOICES = listOf(0, 1, 2, 5)
        val TURN_INTERVALS = listOf(60, 120, 240)
        val TURN_WINDOW_RANGE = 5..30
    }
}
