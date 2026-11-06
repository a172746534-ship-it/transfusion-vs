package app.transfusionvs.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

// Custom timers: anything that isn't a transfusion or a turning round. The patient by MRN only
// (`bedLabel`), a short task name, and an interval. Nothing is kept once it stops (a day later).

enum class CustomTimerStatus { RUNNING, STOPPED }

enum class CustomSlotStatus { PENDING, RINGING, ACKED, MISSED, CANCELLED }

@Entity(tableName = "custom_timer", indices = [Index("status")])
data class CustomTimer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The patient's MRN. */
    val bedLabel: String,
    /** What the timer is for, as typed: 1–30 characters, no patient names. */
    val name: String,
    val intervalMin: Int,
    /** When it was started, on the whole minute. */
    val startAt: Long,
    /** The series: alarms fire at anchorAt + n × interval. */
    val anchorAt: Long,
    /** No alarm at or before this (the start, or the moment the interval was last edited). */
    val fromAt: Long,
    /** No alarm at or after this. Null = no stop time. */
    val stopAt: Long? = null,
    val status: CustomTimerStatus = CustomTimerStatus.RUNNING,
    val stoppedAt: Long? = null,
)

/** One alarm of a custom timer. */
@Entity(
    tableName = "custom_slot",
    foreignKeys = [
        ForeignKey(entity = CustomTimer::class, parentColumns = ["id"], childColumns = ["timerId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("timerId"), Index("status")],
)
data class CustomSlot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timerId: Long,
    val slotAt: Long,
    val firedAt: Long? = null,
    val ackAt: Long? = null,
    val status: CustomSlotStatus = CustomSlotStatus.PENDING,
    val snoozeCount: Int = 0,
    val snoozeUntil: Long? = null,
    /** Past the missed timeout: no more sound; it stays open (Overdue) until done or the next alarm. */
    val silenced: Boolean = false,
)

data class CustomTimerWithSlots(
    @Embedded val timer: CustomTimer,
    @Relation(parentColumn = "id", entityColumn = "timerId")
    val slots: List<CustomSlot>,
)

/** What the start or edit screen hands over to the engine. */
data class CustomDraft(val bedLabel: String, val name: String, val intervalMin: Int)
