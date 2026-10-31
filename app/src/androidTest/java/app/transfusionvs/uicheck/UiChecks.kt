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
import app.transfusionvs.data.CheckpointStatus
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.data.TransfusionWithCheckpoints
import app.transfusionvs.screenshots.Fakes
import app.transfusionvs.screenshots.Fakes.with
import app.transfusionvs.screenshots.TurnFakes
import app.transfusionvs.setup.CheckItem
import app.transfusionvs.setup.OemGuide
import app.transfusionvs.ui.PRIMARY_TAG
import app.transfusionvs.ui.screens.BedChoice
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
 * exactly one primary action in the bottom third, zero Accessibility Test Framework errors.
 * Every finding is logged with tag UICHECK.
 */
class UiChecks {

    @get:Rule val rule = createComposeRule()

    private fun check(name: String, alarm: Boolean = false, primary: Boolean = false, night: Boolean = false, content: @Composable () -> Unit) {
        rule.setContent { TvsTheme(night = night) { content() } }
        rule.waitForIdle()
        val findings = measure(name, if (alarm) 64f else 56f, primary).toMutableList()
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

    private fun measure(name: String, minTarget: Float, primary: Boolean): List<String> {
        val root = rule.onRoot(useUnmergedTree = true).fetchSemanticsNode()
        val all = mutableListOf<SemanticsNode>()
        fun rec(n: SemanticsNode) { all += n; n.children.forEach(::rec) }
        rec(root)
        val density = root.layoutInfo.density.density
        val fontScale = root.layoutInfo.density.fontScale
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
        if (primary && fontScale < 1.3f) {
            val primaries = all.filter { it.config.getOrNull(SemanticsProperties.TestTag) == PRIMARY_TAG }
            if (primaries.size != 1) {
                out += "PRIMARY $name has ${primaries.size} primary actions, wants exactly 1"
            } else {
                val centre = primaries[0].positionInRoot.y + primaries[0].size.height / 2f
                if (centre < root.size.height * 2f / 3f) out += "PRIMARY $name centre at ${"%.0f".format(100 * centre / root.size.height)}% of the height, above the bottom third"
            }
        }
        return out
    }

    private fun label(n: SemanticsNode): String {
        val own = (n.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text } ?: "") +
            (n.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" ") ?: "")
        return (own + " " + n.children.joinToString(" ") { label(it) }).trim()
    }

    private val noop: () -> Unit = {}
    private val longBed = "Isolation Bed 23"
    private val wardBeds = (1..19).map { "Bed $it" } + longBed

    private val manyBeds = (1..20).map { i ->
        Fakes.transfusion(100L + i, if (i == 20) longBed else "Bed $i", i % 8, "2026-01-15T10:${(i * 2).toString().padStart(2, '0')}", 120)
    }

    private fun ringingBed(): TransfusionWithCheckpoints = Fakes.bed14.copy(
        checkpoints = Fakes.bed14.checkpoints.map { if (it.type == CheckpointType.M15) it.copy(status = CheckpointStatus.RINGING, firedAt = Fakes.now) else it },
    )

    @Composable
    private fun Home(ringing: Boolean = false, problems: Int = 0, n: Int = 2, tx: List<TransfusionWithCheckpoints>? = null) = HomeContent(
        home = tx ?: if (n == 0) emptyList() else Fakes.home.take(n), turns = if (n == 0) emptyList() else TurnFakes.home.take(n),
        ringing = ringing, setupProblems = problems, now = Fakes.now,
        onStartTransfusion = noop, onStartTurning = noop, onOpen = {}, onOpenTurnPlan = {}, onReaction = noop, onSetup = noop, onSettings = noop, onOpenAlarm = noop, onAcknowledge = {},
    )

    @Test fun homeEmpty() = check("home_empty", primary = true) { Home(n = 0) }
    @Test fun homeBeds() = check("home_beds", primary = true) { Home(ringing = true, problems = 2) }
    @Test fun homeTwentyBeds() = check("home_20beds", primary = true) { Home(tx = manyBeds) }
    @Test fun homeBedsNight() = check("home_beds_night", primary = true, night = true) { Home() }
    @Test fun start() = check("start", primary = true) {
        StartContent(
            editing = null, beds = wardBeds, busy = emptyMap(), defaultDurationMin = 120, limitMin = 240, warnBeforeMin = 15,
            openedAt = Fakes.now, onSubmit = { _, _, _ -> }, onBack = noop,
        )
    }
    @Test fun detail() = check("detail", primary = true) {
        DetailContent(
            item = Fakes.bed12.with(CheckpointType.HOURLY, 60, ackAfterMin = 9), now = Fakes.at("2026-01-15T11:40"), toleranceMin = 5,
            onBack = noop, onEdit = noop, onReaction = noop, onEnd = {}, onAcknowledge = {},
        )
    }
    @Test fun reaction() = check("reaction", primary = true) {
        ReactionScreen(beds = listOf(BedChoice(1, "Bed 12", 0), BedChoice(2, longBed, 1)), onStopped = {}, onBack = noop)
    }
    @Test fun reactionManyBeds() = check("reaction_20beds", primary = true) {
        ReactionScreen(beds = (1..20).map { BedChoice(it.toLong(), if (it == 20) longBed else "Bed $it", it % 8) }, onStopped = {}, onBack = noop)
    }
    @Test fun alarm() = check("alarm", alarm = true, primary = true) {
        AlarmContent(
            rows = listOf(Fakes.ringing(Fakes.bed14, CheckpointType.M15, 15)), now = Fakes.now, snoozeMin = 2,
            onAcknowledge = {}, onSnooze = noop, onStopForReaction = {},
        )
    }
    @Test fun alarmNight() = check("alarm_night", alarm = true, primary = true, night = true) {
        AlarmContent(
            rows = listOf(Fakes.ringing(Fakes.bed14, CheckpointType.M15, 15)), now = Fakes.now, snoozeMin = 2,
            onAcknowledge = {}, onSnooze = noop, onStopForReaction = {},
        )
    }
    // Several beds on the alarm: each has its own ✓, so there is no single primary action by design.
    @Test fun alarmTwoBeds() = check("alarm_two_beds", alarm = true) {
        AlarmContent(
            rows = listOf(Fakes.ringing(Fakes.bed12, CheckpointType.HOURLY, 60), Fakes.ringing(Fakes.bed14, CheckpointType.M15, 15)),
            now = Fakes.now, snoozeMin = 2, onAcknowledge = {}, onSnooze = noop, onStopForReaction = {},
        )
    }
    @Test fun round() = check("round", alarm = true, primary = true) {
        AlarmContent(rows = emptyList(), now = Fakes.now, snoozeMin = 2, onAcknowledge = {}, onSnooze = noop, onStopForReaction = {}, round = TurnFakes.round())
    }
    @Test fun roundSeven() = check("round_seven", alarm = true, primary = true) {
        AlarmContent(rows = emptyList(), now = Fakes.now, snoozeMin = 2, onAcknowledge = {}, onSnooze = noop, onStopForReaction = {}, round = TurnFakes.round(TurnFakes.sevenBeds))
    }
    @Test fun roundChecklist() = check("round_checklist", alarm = true, primary = true) {
        AlarmContent(
            rows = emptyList(), now = Fakes.now, snoozeMin = 2, onAcknowledge = {}, onSnooze = noop, onStopForReaction = {},
            round = TurnFakes.round(started = true), initiallyOpen = mapOf(61L to RoundChoice.CANT_TURN, 62L to RoundChoice.OTHER_POSITION),
        )
    }
    @Test fun turnStart() = check("turn_start", primary = true) {
        TurnStartContent(editing = null, beds = wardBeds, busy = emptyMap(), defaultIntervalMin = 120, openedAt = Fakes.at("2026-01-15T09:45"), onSubmit = {}, onBack = noop, initiallyExpanded = true)
    }
    @Test fun turnDetail() = check("turn_detail", primary = true) {
        TurnDetailContent(item = TurnFakes.bed3, now = Fakes.now, windowMin = 15, onBack = noop, onEdit = noop, onStop = {}, onRestart = {}, onOpenRound = noop)
    }
    @Test fun settings() = check("settings", primary = true) {
        SettingsContent(
            values = SettingsValues(
                soundOn = true, soundUri = null, snoozeMin = 2, missedTimeoutMin = 10, toleranceMin = 5,
                preAlertMin = 0, limitMin = 240, warnBeforeMin = 15,
            ),
            beds = wardBeds, soundTitle = "Phone's default alarm sound", onChange = {}, onPickSound = noop, onBack = noop, onSetup = noop,
        )
    }
    @Test fun setup() = check("setup", primary = true) {
        SetupContent(
            checks = listOf(CheckItem("Notifications allowed", "Needed for the full-screen alarm and the Overdue reminder.", false, listOf(Intent()))),
            guide = OemGuide("Android", emptyList()), tests = emptyList(), ringing = false, message = null,
            onBack = noop, onFix = {}, onOpenStep = {}, onTest = { _, _ -> }, onCancelTest = {}, onOpenAlarm = noop, versionName = "test",
        )
    }
    @Test fun homeRinging() = check("home_ringing", primary = true) { Home(tx = listOf(ringingBed())) }
}
