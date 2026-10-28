package app.transfusionvs.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// No patient identifiers and no vital values are ever stored: documentation stays in Cerner.
// All times are epoch millis; they are shown with java.time in the device timezone.

enum class TransfusionStatus { RUNNING, ENDED, STOPPED_REACTION }

enum class CheckpointType {
    START, M15, M30, HOURLY, PLANNED_END, LIMIT_WARNING,
    /** Internal: the End VS prompt shown when End is tapped. */
    END,
    /** Internal: "Test alarm" from Setup check; never counted in stats. */
    TEST,
}

enum class CheckpointStatus { PENDING, RINGING, ACKED, MISSED, CANCELLED }

@Entity(tableName = "transfusion")
data class Transfusion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bedLabel: String,
    val colorIndex: Int,
    /** T0, truncated to the whole minute. */
    val startAt: Long,
    val plannedDurationMin: Int,
    val endAt: Long? = null,
    val status: TransfusionStatus = TransfusionStatus.RUNNING,
    val baselineConfirmed: Boolean,
    /** True for Setup-check test alarms. */
    val isTest: Boolean = false,
)

@Entity(
    tableName = "checkpoint",
    foreignKeys = [
        ForeignKey(
            entity = Transfusion::class,
            parentColumns = ["id"],
            childColumns = ["transfusionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("transfusionId"), Index("status")],
)
data class Checkpoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transfusionId: Long,
    val type: CheckpointType,
    /** Minutes after T0. Stable identity of the checkpoint when T0 or the duration is edited. */
    val offsetMin: Int,
    val scheduledAt: Long,
    /** First time it started ringing; the missed timer counts from here. */
    val firedAt: Long? = null,
    /** "Measured & documented" time. Also set on a late acknowledge after MISSED. */
    val ackAt: Long? = null,
    val snoozeCount: Int = 0,
    /** While RINGING: silent until this time (snoozed). Null = audible. */
    val snoozeUntil: Long? = null,
    val status: CheckpointStatus = CheckpointStatus.PENDING,
)

/** A checkpoint joined with its bed, for alarm screens and notifications. */
data class CheckpointWithBed(
    val id: Long,
    val transfusionId: Long,
    val type: CheckpointType,
    val offsetMin: Int,
    val scheduledAt: Long,
    val firedAt: Long?,
    val ackAt: Long?,
    val snoozeCount: Int,
    val snoozeUntil: Long?,
    val status: CheckpointStatus,
    val bedLabel: String,
    val colorIndex: Int,
    val startAt: Long,
    val plannedDurationMin: Int,
    val isTest: Boolean,
)
