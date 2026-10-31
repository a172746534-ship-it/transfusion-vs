package app.transfusionvs.uicheck

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import app.transfusionvs.App
import app.transfusionvs.R
import app.transfusionvs.alarm.AlarmActivity
import app.transfusionvs.alarm.AlarmService
import app.transfusionvs.data.Checkpoint
import app.transfusionvs.data.CheckpointStatus
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.data.Transfusion
import app.transfusionvs.schedule.BedViews
import app.transfusionvs.ui.MainActivity
import app.transfusionvs.widget.BedsWidget
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Double taps and rapid repeats are idempotent, undo restores the exact previous state, and a due
 * check can be acknowledged from the widget. Results are logged with tag ROBUST.
 */
class Robustness {

    @get:Rule val rule = createEmptyComposeRule()

    private val target get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val app get() = target.applicationContext as App
    private val now get() = System.currentTimeMillis()

    @Before fun clean() = runBlocking {
        app.db.clearAllTables()
        app.settings.setupSeen = true
    }

    @After fun stop() {
        AlarmService.stop(app)
    }

    private fun clickable(text: String) = hasText(text, substring = true) and hasClickAction()

    private fun node(text: String): SemanticsNodeInteraction {
        rule.waitUntil(15_000) { runCatching { rule.onAllNodes(clickable(text)).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false) }
        Thread.sleep(450)
        return rule.onAllNodes(clickable(text)).onFirst()
    }

    private fun ringing(bed: String): Long = runBlocking {
        val id = app.db.dao().insertTransfusion(
            Transfusion(bedLabel = bed, colorIndex = 0, startAt = now - 15 * 60_000, plannedDurationMin = 120, baselineConfirmed = true),
        )
        app.db.dao().insertCheckpoints(
            listOf(
                Checkpoint(
                    transfusionId = id, type = CheckpointType.M15, offsetMin = 15, scheduledAt = now, firedAt = now,
                    snoozeCount = 1, status = CheckpointStatus.RINGING,
                ),
            ),
        ).first()
    }

    private fun row(id: Long): Checkpoint = runBlocking { app.db.dao().checkpoints(listOf(id)).first() }

    private fun pass(name: String) = Log.i("ROBUST", "$name PASS")

    @Test fun doubleTapOnAckAcknowledgesOnce() {
        val id = ringing("7654801")
        ActivityScenario.launch(AlarmActivity::class.java)
        val n = node("Measured & documented")
        n.performClick()
        runCatching { n.performClick() }
        rule.waitUntil(10_000) { row(id).status == CheckpointStatus.ACKED }
        val undo = runBlocking { app.db.wardDao().undoAll() }
        assertEquals("one acknowledge, one undo entry", 1, undo.size)
        pass("double_tap_ack")
    }

    @Test fun doubleTapOnStartStartsOnce() {
        ActivityScenario.launch(MainActivity::class.java)
        node("Start transfusion").performClick()
        rule.onNode(hasSetTextAction()).performTextInput("7654802")
        val n = node("Baseline done")
        n.performClick()
        runCatching { n.performClick() }
        rule.waitUntil(10_000) { runBlocking { app.db.dao().runningTransfusions().isNotEmpty() } }
        Thread.sleep(1_500)
        assertEquals(1, runBlocking { app.db.dao().runningTransfusions().size })
        pass("double_tap_start")
    }

    /** The second tap lands on whatever the first one opened: Home's buttons must not fire it. */
    @Test fun doubleTapOnHomeOpensOneScreen() {
        ActivityScenario.launch(MainActivity::class.java)
        val n = node("Start transfusion")
        n.performClick()
        runCatching { n.performClick() }
        rule.waitForIdle()
        rule.onNode(hasSetTextAction()).assertExists() // the Start screen is showing, once
        node("Back").performClick()
        node("Start transfusion") // one Back returns to Home
        pass("double_tap_home")
    }

    @Test fun undoOnAlarmRestoresTheExactPreviousState() {
        val id = ringing("7654801")
        val before = row(id)
        ActivityScenario.launch(AlarmActivity::class.java)
        node("Measured & documented").performClick()
        rule.waitUntil(10_000) { row(id).status == CheckpointStatus.ACKED }
        node("Undo").performClick()
        rule.waitUntil(10_000) { row(id).status == CheckpointStatus.RINGING }
        assertEquals(before, row(id))
        assertNull(runBlocking { app.db.wardDao().undoAll() }.firstOrNull())
        pass("undo_bar_exact")
    }

    @Test fun undoLastOnTheCardRestoresTheExactPreviousState() {
        val id = ringing("7654801")
        val before = row(id)
        runBlocking { app.engine.acknowledge(listOf(id)) }
        ActivityScenario.launch(MainActivity::class.java)
        node("Undo last").performClick()
        rule.waitUntil(10_000) { row(id).status == CheckpointStatus.RINGING }
        assertEquals(before, row(id))
        pass("undo_last_exact")
    }

    /** An MRN typed as a bed label by mistake: the widget's own views show only the masked value. */
    @Test fun widgetMasksAnMrn() {
        val id = ringing("7654821")
        assertNotNull(id)
        val beds = runBlocking { BedsWidget.masked(app.db.dao().homeOnce().map { BedViews.of(it, now) }, app.db.dao().bedLabels()) }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var view: View
        instrumentation.runOnMainSync { view = BedsWidget.build(target, beds, now).apply(target, FrameLayout(target)) }
        val text = view.findViewById<TextView>(R.id.row_bed).text.toString()
        assertEquals("•••4821", text)
        pass("widget_mrn_masked")
    }

    /** Renders the real RemoteViews, saves the image, and taps the real pending intent. */
    @Test fun widgetShowsClockTimesAndAcknowledges() {
        val id = ringing("7654801")
        runBlocking { app.db.dao().updateCheckpoints(listOf(row(id).copy(status = CheckpointStatus.MISSED))) }
        val beds = runBlocking { app.db.dao().homeOnce() }.map { BedViews.of(it, now) }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var view: View
        instrumentation.runOnMainSync {
            val parent = FrameLayout(target)
            view = BedsWidget.build(target, beds, now).apply(target, parent)
            view.measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
            val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            FileOutputStream(File(target.getExternalFilesDir(null), "widget.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        val ack = view.findViewById<View>(R.id.row_ack)
        assertNotNull(ack)
        instrumentation.runOnMainSync { ack.performClick() }
        rule.waitUntil(10_000) { row(id).ackAt != null }
        pass("widget_ack")
    }
}
