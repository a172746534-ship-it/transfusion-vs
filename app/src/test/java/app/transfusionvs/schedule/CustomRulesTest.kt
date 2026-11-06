package app.transfusionvs.schedule

import app.transfusionvs.data.CustomSlot
import app.transfusionvs.data.CustomSlotStatus
import app.transfusionvs.data.CustomTimer
import app.transfusionvs.data.CustomTimerStatus
import app.transfusionvs.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/** The custom timer's rules, one behaviour per test. The property test covers the same at random. */
class CustomRulesTest {

    private val cairo = ZoneId.of("Africa/Cairo")
    private fun at(hm: String): Long = LocalDateTime.parse("2026-01-15T$hm").atZone(cairo).toInstant().toEpochMilli()

    private fun timer(interval: Int = 60, start: String = "08:00", stop: String? = null) = CustomTimer(
        id = 1, bedLabel = "7654812", name = "Check drain", intervalMin = interval,
        startAt = at(start), anchorAt = at(start), fromAt = at(start), stopAt = stop?.let { at(it) },
    )

    private fun slot(id: Long, hm: String, status: CustomSlotStatus = CustomSlotStatus.PENDING, fired: String? = null) =
        CustomSlot(id = id, timerId = 1, slotAt = at(hm), firedAt = fired?.let { at(it) }, status = status)

    // ---- The series ----

    @Test
    fun theFirstAlarmIsOneIntervalAfterTheStart_neverAtTheStart() {
        assertEquals(at("09:00"), CustomRules.next(timer(), at("08:00")))
        assertEquals(at("08:15"), CustomRules.next(timer(15), at("08:00")))
        assertEquals(at("08:00").plus(24 * 60 * MINUTE_MS), CustomRules.next(timer(24 * 60), at("08:00")))
    }

    @Test
    fun aLateDoneDoesNotMoveTheSeries() {
        val slots = listOf(slot(1, "09:00", CustomSlotStatus.ACKED, fired = "09:00").copy(ackAt = at("09:40")), slot(2, "10:00"))
        val plan = CustomRules.arm(timer(), slots, now = at("09:41"), missedTimeoutMin = 10, preAlertMin = 0)
        assertEquals(listOf(at("10:00")), plan.requests.filter { it.kind == AlarmKind.CHECKPOINT }.map { it.at })
    }

    @Test
    fun everyRequestIsACustomOne_andThePreAlertOnlyWhenSet() {
        val slots = listOf(slot(1, "09:00"))
        val without = CustomRules.arm(timer(), slots, now = at("08:30"), missedTimeoutMin = 10, preAlertMin = 0)
        assertTrue(without.requests.all { it.custom && !it.turn })
        assertTrue(without.requests.none { it.kind == AlarmKind.PREALERT })
        val with = CustomRules.arm(timer(), slots, now = at("08:30"), missedTimeoutMin = 10, preAlertMin = 5)
        assertEquals(at("08:55"), with.requests.single { it.kind == AlarmKind.PREALERT }.at)
        assertTrue(with.requests.all { it.custom })
    }

    // ---- Creating alarms ----

    @Test
    fun materializeCreatesUpToTheFirstFutureAlarm_andNothingWhileItIsThere() {
        val first = CustomRules.materialize(timer(), emptyList(), now = at("08:10"))
        assertEquals(listOf(at("09:00")), first.map { it.slotAt })
        assertEquals(CustomSlotStatus.PENDING, first.single().status)
        assertTrue(CustomRules.materialize(timer(), listOf(slot(1, "09:00")), now = at("08:59")).isEmpty())
    }

    @Test
    fun alarmsThatPassedWhileThePhoneWasOff_comeBackMissed_exceptTheOneStillOpen() {
        // Off from 08:30 to 11:20: 09:00 and 10:00 both had their next alarm fire; 11:00 is still open.
        val made = CustomRules.materialize(timer(), emptyList(), now = at("11:20"))
        assertEquals(listOf(at("09:00"), at("10:00"), at("11:00"), at("12:00")), made.map { it.slotAt })
        assertEquals(
            listOf(CustomSlotStatus.MISSED, CustomSlotStatus.MISSED, CustomSlotStatus.PENDING, CustomSlotStatus.PENDING),
            made.map { it.status },
        )
    }

    @Test
    fun nothingAtOrAfterTheStopTime() {
        val made = CustomRules.materialize(timer(stop = "10:00"), emptyList(), now = at("09:30"))
        assertEquals(emptyList<Long>(), made.map { it.slotAt }.filter { it >= at("10:00") })
        assertEquals(listOf(at("09:00")), made.map { it.slotAt })
    }

    @Test
    fun aStoppedTimerMakesNothing() {
        assertTrue(CustomRules.materialize(timer().copy(status = CustomTimerStatus.STOPPED), emptyList(), at("12:00")).isEmpty())
    }

    // ---- An alarm's life ----

    @Test
    fun dueRings_timeoutSilences_nextAlarmCloses() {
        val t = timer()
        val ringing = CustomRules.step(t, listOf(slot(1, "09:00")), now = at("09:00"), missedTimeoutMin = 10).single()
        assertEquals(CustomSlotStatus.RINGING, ringing.status)
        assertEquals(at("09:00"), ringing.firedAt)

        val silenced = CustomRules.step(t, listOf(ringing), now = at("09:10"), missedTimeoutMin = 10).single()
        assertEquals(CustomSlotStatus.RINGING, silenced.status)
        assertTrue(silenced.silenced)
        assertFalse(CustomRules.isAudible(silenced, at("09:10")))
        assertTrue(CustomRules.isOverdue(silenced, at("09:10"), 10))

        val missed = CustomRules.step(t, listOf(silenced), now = at("10:00"), missedTimeoutMin = 10).single()
        assertEquals(CustomSlotStatus.MISSED, missed.status)
    }

    @Test
    fun anIntervalShorterThanTheMissedTimeout_closesAtTheNextAlarm() {
        val t = timer(15)
        val ringing = slot(1, "08:15", CustomSlotStatus.RINGING, fired = "08:15")
        // Missed timeout 20 min > interval: the next alarm at 08:30 closes it first, still sounding.
        assertEquals(CustomSlotStatus.MISSED, CustomRules.step(t, listOf(ringing), at("08:30"), 20).single().status)
        assertTrue(CustomRules.step(t, listOf(ringing), at("08:29"), 20).isEmpty())
    }

    @Test
    fun snoozeIsOfferedOnlyIfItEndsBeforeTheNextAlarm_andWithinTheLimit() {
        val t = timer(15)
        val s = slot(1, "08:15", CustomSlotStatus.RINGING, fired = "08:15")
        assertTrue(CustomRules.canSnooze(t, s, now = at("08:20"), snoozeMin = 5))
        assertFalse(CustomRules.canSnooze(t, s, now = at("08:25"), snoozeMin = 5))
        assertFalse(CustomRules.canSnooze(t, s.copy(snoozeCount = Settings.MAX_SNOOZES), now = at("08:16"), snoozeMin = 2))
    }

    @Test
    fun aSnoozedAlarmIsQuietUntilItsTime() {
        val s = slot(1, "09:00", CustomSlotStatus.RINGING, fired = "09:00").copy(snoozeUntil = at("09:02"))
        assertFalse(CustomRules.isAudible(s, at("09:01")))
        assertTrue(CustomRules.isAudible(s, at("09:02")))
    }

    @Test
    fun autoStopOnlyAfterTheStopTime_withNothingOpen() {
        val t = timer(stop = "10:00")
        assertFalse(CustomRules.shouldAutoStop(t, emptyList(), at("09:59")))
        assertFalse(CustomRules.shouldAutoStop(t, listOf(slot(1, "09:00", CustomSlotStatus.RINGING, fired = "09:00")), at("10:01")))
        assertTrue(CustomRules.shouldAutoStop(t, listOf(slot(1, "09:00", CustomSlotStatus.ACKED)), at("10:01")))
        assertFalse(CustomRules.shouldAutoStop(timer(), emptyList(), at("23:00")))
    }

    // ---- Editing ----

    @Test
    fun theSameInterval_isTheSameTimer() {
        val t = timer()
        assertSame(t, CustomRules.edited(t, emptyList(), 60, at("09:30")))
    }

    @Test
    fun aNewIntervalStartsFromTheLastDone_andNeverRingsInThePast() {
        val slots = listOf(slot(1, "09:00", CustomSlotStatus.ACKED).copy(ackAt = at("09:05")))
        val e = CustomRules.edited(timer(), slots, 30, now = at("09:50"))
        assertEquals(at("09:05"), e.anchorAt)
        assertEquals(at("09:50"), e.fromAt)
        assertEquals(at("10:05"), CustomRules.next(e, at("09:50")))
        // Nothing done yet: from the start.
        val fresh = CustomRules.edited(timer(), emptyList(), 120, now = at("08:20"))
        assertEquals(at("08:00"), fresh.anchorAt)
        assertEquals(at("10:00"), CustomRules.next(fresh, at("08:20")))
    }

    // ---- Status words ----

    @Test
    fun statusWordsMatchTheOtherTypes() {
        val now = at("09:30")
        assertEquals("upcoming", CustomStatus.of(slot(1, "10:00"), now, 15, 10).text)
        assertEquals("ringing", CustomStatus.of(slot(1, "09:25", CustomSlotStatus.RINGING, fired = "09:25"), now, 15, 10).text)
        assertEquals("overdue", CustomStatus.of(slot(1, "09:00", CustomSlotStatus.RINGING, fired = "09:00"), now, 15, 10).text)
        assertEquals("on time", CustomStatus.of(slot(1, "09:00", CustomSlotStatus.ACKED).copy(ackAt = at("09:10")), now, 15, 10).text)
        assertEquals("late +20 min", CustomStatus.of(slot(1, "09:00", CustomSlotStatus.ACKED).copy(ackAt = at("09:20")), now, 15, 10).text)
        assertEquals("missed", CustomStatus.of(slot(1, "08:00", CustomSlotStatus.MISSED), now, 15, 10).text)
    }

    // ---- Names ----

    @Test
    fun aNameIsOneLine_upTo30Characters_countedFromTheFirstVisibleOne() {
        // Line breaks and tabs are control characters: they go.
        assertEquals("Checkdrain", CustomName.clean("Check\ndrain"))
        assertEquals("Check drain", CustomName.clean("Check\t drain\r"))
        assertEquals("a".repeat(30), CustomName.clean("a".repeat(40)))
        assertEquals("   " + "b".repeat(30), CustomName.clean("   " + "b".repeat(35)))
        // Arabic, and characters outside the basic plane, count as one each.
        val arabic = "قياس السكر قبل الأكل وبعده كل ساعتين"
        assertEquals(30, CustomName.clean(arabic).let { it.codePointCount(0, it.length) })
        val emoji = "🩸".repeat(31)
        assertEquals(30, CustomName.clean(emoji).let { it.codePointCount(0, it.length) })
    }

    @Test
    fun emptyAndInvisibleNamesAreRejected() {
        assertFalse(CustomName.valid(""))
        assertFalse(CustomName.valid("     "))
        assertFalse(CustomName.valid("​‌‍"))
        assertFalse(CustomName.valid("   "))
        assertFalse(CustomName.valid("﻿ ؜"))
        assertFalse(CustomName.valid("a".repeat(31)))
        assertTrue(CustomName.valid("a"))
        assertTrue(CustomName.valid("  Drain  "))
        assertTrue(CustomName.valid("قياس السكر"))
        assertTrue(CustomName.valid("Drain فحص 2"))
        assertTrue(CustomName.valid("x".repeat(30)))
    }

    @Test
    fun theSameName_ignoresCaseSpacesAndComposition() {
        assertEquals(CustomName.key("Check Drain"), CustomName.key("  check   DRAIN "))
        assertEquals(CustomName.key("Café"), CustomName.key("Café"))
        assertTrue(CustomName.key("Drain") != CustomName.key("Drains"))
    }

    @Test
    fun noAlarmBeforeTheSeriesStart() {
        assertNull(CustomRules.materialize(timer(), emptyList(), at("08:00")).firstOrNull { it.slotAt <= at("08:00") })
    }
}
