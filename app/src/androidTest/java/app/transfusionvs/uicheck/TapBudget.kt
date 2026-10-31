package app.transfusionvs.uicheck

import android.util.Log
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import app.transfusionvs.App
import app.transfusionvs.alarm.AlarmActivity
import app.transfusionvs.alarm.AlarmService
import app.transfusionvs.data.Checkpoint
import app.transfusionvs.data.CheckpointStatus
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.data.Position
import app.transfusionvs.data.Transfusion
import app.transfusionvs.data.TurnPlan
import app.transfusionvs.data.TurnSlot
import app.transfusionvs.data.TurnSlotStatus
import app.transfusionvs.data.WardBed
import app.transfusionvs.schedule.TurnGrid
import app.transfusionvs.ui.MainActivity
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Drives the nine core tasks of docs/DESIGN_OVERHAUL.md §3 through the real activities and counts
 * every touch: tap, and scroll needed to reach the target. The app icon is tap 1 where the task
 * starts from the launcher. No task may need typing; none of these flows touches a text field.
 * Each result is logged with tag TAPS.
 *
 * Preconditions, as the spec states them: setup done, ward beds entered. Task 7 runs where a reaction
 * is possible: the maintainer decided the guide belongs to transfusions, not to turning screens.
 */
class TapBudget {

    @get:Rule val rule = createEmptyComposeRule()

    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as App
    private val now get() = System.currentTimeMillis()
    private var taps = 0

    @Before fun clean() = runBlocking {
        app.db.clearAllTables()
        app.settings.setupSeen = true
        app.db.wardDao().addBeds((1..20).map { WardBed("Bed $it") })
        taps = 0
    }

    @After fun stop() {
        AlarmService.stop(app)
    }

    private fun report(task: Int, budget: Int) {
        val ok = taps <= budget
        Log.i("TAPS", "task=$task taps=$taps typing=false budget=$budget ${if (ok) "PASS" else "FAIL"}")
        assertTrue("task $task used $taps touches (budget $budget)", ok)
    }

    private fun clickable(text: String) = hasText(text, substring = true) and hasClickAction()

    private fun waitFor(text: String) {
        rule.waitUntil(15_000) {
            runCatching { rule.onAllNodes(clickable(text)).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
        }
    }

    private fun visible(n: SemanticsNodeInteraction) = runCatching { n.isDisplayed() }.getOrDefault(false)

    /** A person's next tap never lands inside the double-tap window. */
    private fun human() = Thread.sleep(450)

    private fun tapNode(n: SemanticsNodeInteraction) {
        human()
        if (!visible(n)) {
            n.performScrollTo()
            taps++
        }
        n.performClick()
        taps++
        rule.waitForIdle()
    }

    /** Scrolling to reach the target counts as a touch. */
    private fun tap(text: String) {
        waitFor(text)
        tapNode(rule.onAllNodes(clickable(text)).onFirst())
    }

    /** "Back" is also the header's back control; the position button comes last on the screen. */
    private fun tapLast(text: String) {
        waitFor(text)
        tapNode(rule.onAllNodes(clickable(text)).onLast())
    }

    private fun home(waitText: String = "Start transfusion"): ActivityScenario<MainActivity> {
        taps = 1 // the app icon
        return ActivityScenario.launch(MainActivity::class.java).also { waitFor(waitText) }
    }

    private fun alarm(waitText: String) = ActivityScenario.launch(AlarmActivity::class.java).also { waitFor(waitText) }

    private fun running(bed: String) = runBlocking {
        app.db.dao().insertTransfusion(
            Transfusion(bedLabel = bed, colorIndex = 0, startAt = now, plannedDurationMin = 120, baselineConfirmed = true),
        )
    }

    private fun ringing(bed: String) = runBlocking {
        val id = running(bed)
        app.db.dao().insertCheckpoints(
            listOf(Checkpoint(transfusionId = id, type = CheckpointType.M15, offsetMin = 15, scheduledAt = now, firedAt = now, status = CheckpointStatus.RINGING)),
        )
    }

    private fun roundOf(vararg beds: String) = runBlocking {
        val zone = ZoneId.systemDefault()
        val slot = TurnGrid.nextSlotAfter(now - 120 * 60_000L, 120, zone)
        beds.forEachIndexed { i, bed ->
            val plan = app.db.turnDao().insertPlan(
                TurnPlan(
                    bedLabel = bed, colorIndex = i, intervalMin = 120, startAt = slot - 3_600_000, firstSlotAt = slot,
                    rotation = Position.toCsv(Position.DEFAULT_ROTATION), currentPosition = Position.BACK,
                ),
            )
            app.db.turnDao().insertSlots(listOf(TurnSlot(planId = plan, slotAt = slot, firedAt = slot, positionFrom = Position.BACK, status = TurnSlotStatus.RINGING)))
        }
    }

    @Test fun task1_seeAllBeds() {
        val beds = (1..6).map { "Bed $it" }
        beds.forEach { running(it) }
        runBlocking { app.engine.rearm() }
        home()
        // Every bed's state must be readable without touching anything else; one scroll = one touch.
        for (bed in beds) {
            val shown = rule.onAllNodes(hasText(bed)).fetchSemanticsNodes().isNotEmpty() && visible(rule.onAllNodes(hasText(bed)).onFirst())
            if (!shown) {
                rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(bed))
                taps++
                rule.waitForIdle()
            }
        }
        report(1, 1)
    }

    @Test fun task2_ackFromAlarm() {
        ringing("Bed 12")
        alarm("Measured & documented")
        tap("Measured & documented")
        report(2, 1)
    }

    @Test fun task3_wholeRoundAsSuggested() {
        roundOf("Bed 3", "Bed 7", "Bed 9")
        alarm("Start round")
        tap("Start round")
        tap("All turned as suggested")
        report(3, 2)
        rule.waitUntil(10_000) { runBlocking { app.db.turnDao().ringingSlots().isEmpty() } }
    }

    @Test fun task4_startTransfusion() {
        home()
        tap("Start transfusion")
        tap("Bed 5")
        tap("Baseline done")
        report(4, 4)
        rule.waitUntil(10_000) { runBlocking { app.db.dao().runningTransfusions().any { it.bedLabel == "Bed 5" } } }
    }

    @Test fun task5_startPlan() {
        home()
        tap("Start turning")
        tap("Bed 6")
        tapLast("Back")
        report(5, 4)
        rule.waitUntil(10_000) { runBlocking { app.db.turnDao().allPlans().any { it.bedLabel == "Bed 6" && it.currentPosition == Position.BACK } } }
    }

    @Test fun task6_endTransfusion() {
        running("Bed 12")
        runBlocking { app.engine.rearm() }
        home("Bed 12")
        tap("Bed 12")
        tap("End transfusion")
        report(6, 3)
        rule.waitUntil(10_000) { runBlocking { app.db.dao().runningTransfusions().isEmpty() } }
    }

    @Test fun task7_reactionFromHome() {
        running("Bed 12")
        runBlocking { app.engine.rearm() }
        home("SUSPECTED REACTION")
        tap("SUSPECTED REACTION")
        rule.onAllNodes(hasText("Do now", ignoreCase = true)).onFirst().assertExists()
        report(7, 2)
    }

    @Test fun task7_reactionFromAlarm() {
        ringing("Bed 12")
        alarm("SUSPECTED REACTION")
        tap("SUSPECTED REACTION")
        rule.onAllNodes(hasText("Do now", ignoreCase = true)).onFirst().assertExists()
        report(7, 2)
    }

    @Test fun task7_reactionFromDetail() {
        running("Bed 12")
        runBlocking { app.engine.rearm() }
        home("Bed 12")
        tap("Bed 12")
        taps = 0 // "from anywhere": counted from this screen
        tap("SUSPECTED REACTION")
        rule.onAllNodes(hasText("Do now", ignoreCase = true)).onFirst().assertExists()
        report(7, 2)
    }

    @Test fun task8_markStopped() {
        running("Bed 12")
        runBlocking { app.engine.rearm() }
        home("SUSPECTED REACTION")
        tap("SUSPECTED REACTION")
        tap("Transfusion stopped")
        report(8, 3)
        rule.waitUntil(10_000) { runBlocking { app.db.dao().runningTransfusions().isEmpty() } }
    }

    @Test fun task9_cantTurnWithReason() {
        roundOf("Bed 3")
        alarm("Start round")
        tap("Start round")
        tap("Can't turn")
        tap("Procedure")
        report(9, 3)
        rule.waitUntil(10_000) { runBlocking { app.db.turnDao().ringingSlots().isEmpty() } }
        assertEquals(TurnSlotStatus.SKIPPED, runBlocking { app.db.turnDao().allPlans().flatMap { app.db.turnDao().slotsFor(it.id) } }.first().status)
    }
}
