package app.transfusionvs.screenshots

import app.transfusionvs.alarm.CustomRing
import app.transfusionvs.alarm.CustomRingBuilder
import app.transfusionvs.data.CustomSlot
import app.transfusionvs.data.CustomSlotStatus
import app.transfusionvs.data.CustomTimer
import app.transfusionvs.data.CustomTimerStatus
import app.transfusionvs.data.CustomTimerWithSlots

/** Custom timers for screenshots, on the same fixed day as [Fakes] (now = 11:05). Fake MRNs and tasks only. */
object CustomFakes {

    /** A typical 30-character task name. */
    const val LONG_NAME = "Check wound dressing and drain"

    /** The worst case: 30 of the widest letter. */
    val WIDE_NAME = "W".repeat(30)

    private fun timer(id: Long, bed: String, name: String, interval: Int, start: String, stop: String? = null) = CustomTimer(
        id = id, bedLabel = bed, name = name, intervalMin = interval,
        startAt = Fakes.at("2026-01-15T$start"), anchorAt = Fakes.at("2026-01-15T$start"), fromAt = Fakes.at("2026-01-15T$start"),
        stopAt = stop?.let { Fakes.at("2026-01-15T$it") },
    )

    private fun slot(id: Long, timer: Long, at: String, status: CustomSlotStatus, fired: String? = null, ack: String? = null, silenced: Boolean = false) =
        CustomSlot(
            id = id, timerId = timer, slotAt = Fakes.at("2026-01-15T$at"), status = status, silenced = silenced,
            firedAt = fired?.let { Fakes.at("2026-01-15T$it") }, ackAt = ack?.let { Fakes.at("2026-01-15T$it") },
        )

    /** Upcoming: done at 10:33, next at 11:30. */
    val drain = CustomTimerWithSlots(
        timer(301, "7654821", "Check chest drain", 60, "09:30"),
        listOf(
            slot(3011, 301, "10:30", CustomSlotStatus.ACKED, fired = "10:30", ack = "10:33"),
            slot(3012, 301, "11:30", CustomSlotStatus.PENDING),
        ),
    )

    /** Ringing since 11:00. */
    val sugar = CustomTimerWithSlots(
        timer(302, "7654822", "Blood sugar", 240, "07:00"),
        listOf(slot(3021, 302, "11:00", CustomSlotStatus.RINGING, fired = "11:00"), slot(3022, 302, "15:00", CustomSlotStatus.PENDING)),
    )

    /** Overdue: rang at 10:50, silenced at the missed timeout, still open. */
    val ivSite = CustomTimerWithSlots(
        timer(303, "7654823", "IV site check", 30, "10:20"),
        listOf(slot(3031, 303, "10:50", CustomSlotStatus.RINGING, fired = "10:50", silenced = true), slot(3032, 303, "11:20", CustomSlotStatus.PENDING)),
    )

    /** A 30-character name, ringing since 11:05. */
    val dressing = CustomTimerWithSlots(
        timer(304, "7654824", LONG_NAME, 30, "10:05"),
        listOf(
            slot(3041, 304, "10:35", CustomSlotStatus.ACKED, fired = "10:35", ack = "10:52"),
            slot(3042, 304, "11:05", CustomSlotStatus.RINGING, fired = "11:05"),
            slot(3043, 304, "11:35", CustomSlotStatus.PENDING),
        ),
    )

    /** Arabic task name, upcoming. */
    val arabic = CustomTimerWithSlots(
        timer(305, "7654825", "قياس السكر قبل الوجبة", 120, "10:00"),
        listOf(slot(3051, 305, "12:00", CustomSlotStatus.PENDING)),
    )

    /** "End of my shift" set. */
    val withStop = drain.copy(timer = drain.timer.copy(stopAt = Fakes.at("2026-01-15T20:00")))

    val stopped = drain.copy(
        timer = drain.timer.copy(status = CustomTimerStatus.STOPPED, stoppedAt = Fakes.at("2026-01-15T11:00")),
        slots = drain.slots.map { if (it.status == CustomSlotStatus.PENDING) it.copy(status = CustomSlotStatus.CANCELLED) else it },
    )

    val home = listOf(drain, sugar, ivSite, arabic)

    fun ringing(name: String) = CustomTimerWithSlots(
        timer(310, "7654826", name, 60, "10:05"),
        listOf(slot(3101, 310, "11:05", CustomSlotStatus.RINGING, fired = "11:05")),
    )

    fun rings(vararg items: CustomTimerWithSlots): List<CustomRing> = CustomRingBuilder.build(items.toList(), Fakes.now, snoozeMin = 2)
}
