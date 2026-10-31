package app.transfusionvs.uicheck

import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.text.TextLayoutResult
import app.transfusionvs.alarm.AlarmContent
import app.transfusionvs.alarm.RoundChoice
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.screenshots.Fakes
import app.transfusionvs.screenshots.Fakes.with
import app.transfusionvs.screenshots.TurnFakes
import app.transfusionvs.setup.CheckItem
import app.transfusionvs.setup.OemGuide
import app.transfusionvs.ui.screens.BedChoice
import app.transfusionvs.ui.screens.ChooserContent
import app.transfusionvs.ui.screens.DetailContent
import app.transfusionvs.ui.screens.HomeContent
import app.transfusionvs.ui.screens.ReactionScreen
import app.transfusionvs.ui.screens.SettingsContent
import app.transfusionvs.ui.screens.SettingsValues
import app.transfusionvs.ui.screens.SetupContent
import app.transfusionvs.ui.screens.StartContent
import app.transfusionvs.ui.screens.TurnDetailContent
import app.transfusionvs.ui.screens.TurnStartContent
import app.transfusionvs.ui.theme.TvsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The fixed thresholds of docs/TEST_LOOP.md §4, measured on the real screens on an emulator (run
 * at several widths and font scales by lab/robustness.sh): touch targets ≥ 56 dp (alarm actions
 * ≥ 64 dp), ≥ 8 dp between targets, body text ≥ 16 sp, no truncated text, every action labelled,
 * zero Accessibility Test Framework errors. Every finding is logged with tag UICHECK.
 */
class UiChecks {

    @get:Rule val rule = createComposeRule()

    private fun check(name: String, alarm: Boolean = false, content: @Composable () -> Unit) {
        rule.setContent { TvsTheme { content() } }
        rule.waitForIdle()
        val findings = measure(name, if (alarm) 64f else 56f).toMutableList()
        rule.enableAccessibilityChecks()
        try {
            rule.onRoot().tryPerformAccessibilityChecks()
        } catch (t: Throwable) {
            findings += "ATF $name: ${t.message?.replace('\n', ' ')?.take(1500)}"
        }
        findings.forEach { Log.i("UICHECK", it) }
        Log.i("UICHECK", "SCREEN $name findings=${findings.size}")
        assertTrue("$name: ${findings.size} findings\n" + findings.joinToString("\n"), findings.isEmpty())
    }

    private fun measure(name: String, minTarget: Float): List<String> {
        val root = rule.onRoot(useUnmergedTree = true).fetchSemanticsNode()
        val all = mutableListOf<SemanticsNode>()
        fun rec(n: SemanticsNode) { all += n; n.children.forEach(::rec) }
        rec(root)
        val density = root.layoutInfo.density.density
        val out = mutableListOf<String>()

        val targets = all.filter { it.config.contains(SemanticsActions.OnClick) && !it.config.contains(SemanticsProperties.Disabled) }
        targets.forEach { n ->
            val w = n.size.width / density
            val h = n.size.height / density
            if (minOf(w, h) < minTarget) out += "TOUCH $name '${label(n)}' ${w.toInt()}x${h.toInt()}dp < ${minTarget.toInt()}dp"
            if (label(n).isBlank()) out += "NOLABEL $name action without a label at ${n.boundsInRoot}"
        }
        // Whole rectangles, also below the fold (boundsInRoot is clipped to the scroll viewport).
        fun rect(n: SemanticsNode) = Rect(n.positionInRoot, Size(n.size.width.toFloat(), n.size.height.toFloat()))
        for (i in targets.indices) for (j in i + 1 until targets.size) {
            val a = rect(targets[i])
            val b = rect(targets[j])
            if (a.overlaps(b)) continue
            val dx = maxOf(a.left - b.right, b.left - a.right, 0f)
            val dy = maxOf(a.top - b.bottom, b.top - a.bottom, 0f)
            val gap = maxOf(dx, dy) / density
            if (gap < 8f) out += "SPACING $name '${label(targets[i])}' and '${label(targets[j])}' ${"%.1f".format(gap)}dp < 8dp"
        }
        all.forEach { n ->
            val action = n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action ?: return@forEach
            val results = mutableListOf<TextLayoutResult>()
            if (!action(results)) return@forEach
            val r = results.firstOrNull() ?: return@forEach
            val text = r.layoutInput.text.text
            // Measured from the laid-out lines: hasVisualOverflow also reports text that fits.
            val w = n.size.width + 1f
            val h = n.size.height + 1f
            val cut = r.multiParagraph.height > h || (0 until r.lineCount).any { r.getLineRight(it) - r.getLineLeft(it) > w || r.isLineEllipsized(it) }
            if (cut) out += "TRUNCATED $name '${text.take(60)}'"
            for (line in 0 until r.lineCount - 1) {
                val end = r.getLineEnd(line)
                if (end in 1 until text.length && text[end - 1].isLetterOrDigit() && text[end].isLetterOrDigit()) {
                    out += "MIDWORD $name '${text.take(60)}' breaks inside a word at line ${line + 1}"
                }
            }
            val sp = r.layoutInput.style.fontSize.value
            if (!sp.isNaN() && sp < 16f) out += "SMALLTEXT $name '${text.take(40)}' ${sp}sp < 16sp"
        }
        return out
    }

    private fun label(n: SemanticsNode): String {
        val own = (n.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text } ?: "") +
            (n.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" ") ?: "")
        return (own + " " + n.children.joinToString(" ") { label(it) }).trim()
    }

    private val noop: () -> Unit = {}

    @Composable
    private fun Home(ringing: Boolean = false, problems: Int = 0, n: Int = 2) = HomeContent(
        home = if (n == 0) emptyList() else Fakes.home.take(n), turns = if (n == 0) emptyList() else TurnFakes.home.take(n),
        ringing = ringing, setupProblems = problems, now = Fakes.now,
        onStart = noop, onOpen = {}, onOpenTurnPlan = {}, onReaction = noop, onSetup = noop, onSettings = noop, onOpenAlarm = noop, onAcknowledge = {},
    )

    @Test fun homeEmpty() = check("home_empty") { Home(n = 0) }
    @Test fun homeBeds() = check("home_beds") { Home(ringing = true, problems = 2) }
    @Test fun chooser() = check("chooser") { ChooserContent(onTransfusion = noop, onRepositioning = noop, onBack = noop) }
    @Test fun start() = check("start") {
        StartContent(editing = null, limitMin = 240, warnBeforeMin = 15, openedAt = Fakes.now, onSubmit = { _, _, _ -> }, onBack = noop)
    }
    @Test fun detail() = check("detail") {
        DetailContent(
            item = Fakes.bed12.with(CheckpointType.HOURLY, 60, ackAfterMin = 9), now = Fakes.at("2026-01-15T11:40"), toleranceMin = 5,
            onBack = noop, onEdit = noop, onReaction = noop, onEnd = {}, onAcknowledge = {},
        )
    }
    @Test fun reaction() = check("reaction") {
        ReactionScreen(beds = listOf(BedChoice(1, "Bed 12", 0), BedChoice(2, "Isolation Bed 23", 1)), onStopped = {}, onBack = noop)
    }
    @Test fun alarm() = check("alarm", alarm = true) {
        AlarmContent(
            rows = listOf(Fakes.ringing(Fakes.bed14, CheckpointType.M15, 15)), now = Fakes.now, snoozeMin = 2,
            onAcknowledge = {}, onSnooze = noop, onStopForReaction = {},
        )
    }
    @Test fun alarmTwoBeds() = check("alarm_two_beds", alarm = true) {
        AlarmContent(
            rows = listOf(Fakes.ringing(Fakes.bed12, CheckpointType.HOURLY, 60), Fakes.ringing(Fakes.bed14, CheckpointType.M15, 15)),
            now = Fakes.now, snoozeMin = 2, onAcknowledge = {}, onSnooze = noop, onStopForReaction = {},
        )
    }
    @Test fun round() = check("round", alarm = true) {
        AlarmContent(rows = emptyList(), now = Fakes.now, snoozeMin = 2, onAcknowledge = {}, onSnooze = noop, onStopForReaction = {}, round = TurnFakes.round())
    }
    @Test fun roundChecklist() = check("round_checklist", alarm = true) {
        AlarmContent(
            rows = emptyList(), now = Fakes.now, snoozeMin = 2, onAcknowledge = {}, onSnooze = noop, onStopForReaction = {},
            round = TurnFakes.round(started = true), initiallyOpen = mapOf(61L to RoundChoice.CANT_TURN, 62L to RoundChoice.OTHER_POSITION),
        )
    }
    @Test fun turnStart() = check("turn_start") {
        TurnStartContent(editing = null, defaultIntervalMin = 120, openedAt = Fakes.at("2026-01-15T09:45"), onSubmit = {}, onBack = noop)
    }
    @Test fun turnDetail() = check("turn_detail") {
        TurnDetailContent(item = TurnFakes.bed3, now = Fakes.now, windowMin = 15, onBack = noop, onEdit = noop, onStop = {}, onRestart = {}, onOpenRound = noop)
    }
    @Test fun settings() = check("settings") {
        SettingsContent(
            values = SettingsValues(
                soundOn = true, soundUri = null, snoozeMin = 2, missedTimeoutMin = 10, toleranceMin = 5,
                preAlertMin = 0, limitMin = 240, warnBeforeMin = 15,
            ),
            soundTitle = "Phone's default alarm sound", onChange = {}, onPickSound = noop, onBack = noop, onSetup = noop,
        )
    }
    @Test fun setup() = check("setup") {
        SetupContent(
            checks = listOf(CheckItem("Notifications allowed", "Needed for the full-screen alarm and the Overdue reminder.", false, listOf(Intent()))),
            guide = OemGuide("Android", emptyList()), tests = emptyList(), ringing = false, message = null,
            onBack = noop, onFix = {}, onOpenStep = {}, onTest = { _, _ -> }, onCancelTest = {}, onOpenAlarm = noop, versionName = "test",
        )
    }
}
