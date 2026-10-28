package app.transfusionvs.alarm

import app.transfusionvs.data.CheckpointType
import app.transfusionvs.data.CheckpointWithBed
import app.transfusionvs.schedule.Labels

/** One bed on the alarm: it gets its own ✓ button that acknowledges all of its rows. */
data class RingBed(
    val transfusionId: Long,
    val bedLabel: String,
    val colorIndex: Int,
    val isTest: Boolean,
    val checkpointIds: List<Long>,
    /** e.g. "15-min check", or "2-h check + Time-limit warning". */
    val what: String,
    val subtitle: String,
) {
    val title: String get() = if (isTest) what else "$what — $bedLabel"
    val shortLabel: String get() = if (isTest) "Test" else bedLabel
}

/** Everything that is sounding right now, as shown on the alarm screen and notification. */
data class RingSpec(
    val beds: List<RingBed>,
    val canSnooze: Boolean,
    val snoozeMin: Int,
    val soundOn: Boolean,
    val soundUri: String?,
    val missedTimeoutMin: Int,
) {
    val title: String get() = if (beds.size == 1) beds[0].title else "${beds.size} beds due now"

    companion object {
        fun build(
            audible: List<CheckpointWithBed>,
            canSnooze: Boolean,
            snoozeMin: Int,
            soundOn: Boolean,
            soundUri: String?,
            missedTimeoutMin: Int,
        ) = RingSpec(
            beds = groupByBed(audible),
            canSnooze = canSnooze,
            snoozeMin = snoozeMin,
            soundOn = soundOn,
            soundUri = soundUri,
            missedTimeoutMin = missedTimeoutMin,
        )

        fun groupByBed(rows: List<CheckpointWithBed>): List<RingBed> =
            rows.groupBy { it.transfusionId }.values.map { bedRows ->
                val first = bedRows.first()
                RingBed(
                    transfusionId = first.transfusionId,
                    bedLabel = first.bedLabel,
                    colorIndex = first.colorIndex,
                    isTest = first.isTest,
                    checkpointIds = bedRows.map { it.id },
                    what = bedRows.joinToString(" + ") { Labels.title(it.type, it.offsetMin) },
                    subtitle = Labels.subtitle(
                        bedRows.firstOrNull { it.type != CheckpointType.HOURLY }?.type ?: first.type,
                    ),
                )
            }
    }
}
