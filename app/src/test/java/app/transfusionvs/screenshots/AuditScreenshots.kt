package app.transfusionvs.screenshots

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import app.transfusionvs.alarm.AlarmContent
import app.transfusionvs.alarm.RoundChoice
import app.transfusionvs.data.Checkpoint
import app.transfusionvs.data.CheckpointStatus
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.data.TransfusionStatus
import app.transfusionvs.data.TransfusionWithCheckpoints
import app.transfusionvs.data.TurnPlanStatus
import app.transfusionvs.screenshots.Fakes.with
import app.transfusionvs.setup.CheckItem
import app.transfusionvs.setup.OemGuide
import app.transfusionvs.setup.OemStep
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
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import java.util.Locale
import java.util.TimeZone
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Phase 1 audit: every screen in every state at the three sizes the audit asks for. Written to
 * build/audit (an artifact), always recorded, never compared: this is evidence, not a baseline.
 *   w360_f10  360 × 800 dp, font 1.0: what fits above the fold on a small phone
 *   w360_f20  360 dp tall sheet, font 2.0: truncation and overflow
 *   w412_f10  412 × 915 dp, font 1.0
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xxhdpi")
class AuditScreenshots {

    @Before
    fun fixedClock() {
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Cairo"))
        Locale.setDefault(Locale.ENGLISH)
    }

    private val record = RoborazziOptions(taskType = RoborazziTaskType.Record)

    private fun variant(tag: String, name: String, qualifiers: String, font: Float, content: @Composable () -> Unit) {
        RuntimeEnvironment.setQualifiers(qualifiers)
        captureRoboImage("build/audit/${name}__$tag.png", roborazziOptions = record) {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, font)) { TvsTheme { content() } }
        }
    }

    private fun shots(name: String, content: @Composable () -> Unit) {
        variant("w360_f10", name, "w360dp-h800dp-xxhdpi", 1f, content)
        variant("w360_f20", name, "w360dp-h2600dp-xxhdpi", 2f, content)
        variant("w412_f10", name, "w412dp-h915dp-xxhdpi", 1f, content)
    }

    private val noop: () -> Unit = {}
    private val longBed = "Isolation Bed 23"
    private val many = (1..20).map { i ->
        Fakes.transfusion(100L + i, if (i == 20) longBed else "Bed $i", i % 8, "2026-01-15T10:${(i * 2).toString().padStart(2, '0')}", 120)
    }

    @Composable
    private fun Home(
        tx: List<TransfusionWithCheckpoints> = emptyList(),
        turns: List<app.transfusionvs.data.TurnPlanWithSlots> = emptyList(),
        ringing: Boolean = false,
        problems: Int = 0,
    ) = HomeContent(
        home = tx, turns = turns, ringing = ringing, setupProblems = problems, now = Fakes.now,
        onStart = noop, onOpen = {}, onOpenTurnPlan = {}, onReaction = noop, onSetup = noop, onSettings = noop, onOpenAlarm = noop, onAcknowledge = {},
    )

    private fun ringingBed(): TransfusionWithCheckpoints = Fakes.bed14.copy(
        checkpoints = Fakes.bed14.checkpoints.map { if (it.type == CheckpointType.M15) it.copy(status = CheckpointStatus.RINGING, firedAt = Fakes.now) else it },
    )

    private fun ended(status: TransfusionStatus): TransfusionWithCheckpoints {
        val t = Fakes.bed12
        val end = Fakes.at("2026-01-15T10:58")
        val cps = t.checkpoints.map { if (it.status == CheckpointStatus.PENDING) it.copy(status = CheckpointStatus.CANCELLED) else it }
        return t.copy(
            transfusion = t.transfusion.copy(status = status, endAt = end),
            checkpoints = if (status == TransfusionStatus.ENDED) {
                cps + Checkpoint(id = 999, transfusionId = t.transfusion.id, type = CheckpointType.END, offsetMin = 38, scheduledAt = end)
            } else {
                cps
            },
        )
    }

    @Test fun home_empty() = shots("home_empty") { Home() }
    @Test fun home_setup_warning() = shots("home_setup_warning") { Home(problems = 2) }
    @Test fun home_one_bed_upcoming() = shots("home_1bed_upcoming") { Home(tx = listOf(Fakes.bed14)) }
    @Test fun home_due_ringing() = shots("home_ringing") { Home(tx = listOf(ringingBed()), ringing = true) }
    @Test fun home_overdue_missed() = shots("home_overdue_missed") { Home(tx = listOf(Fakes.bed12)) }
    @Test fun home_both_features() = shots("home_both_features") { Home(tx = Fakes.home, turns = TurnFakes.home) }
    @Test fun home_end_vs_pending() = shots("home_end_vs_pending") { Home(tx = listOf(ended(TransfusionStatus.ENDED))) }
    @Test fun home_stopped_reaction() = shots("home_stopped_reaction") { Home(tx = listOf(ended(TransfusionStatus.STOPPED_REACTION))) }
    @Test fun home_20_beds() = shots("home_20beds") { Home(tx = many) }

    @Test fun chooser() = shots("chooser") { ChooserContent(onTransfusion = noop, onRepositioning = noop, onBack = noop) }

    @Test fun start_empty() = shots("start_empty") {
        StartContent(editing = null, limitMin = 240, warnBeforeMin = 15, openedAt = Fakes.now, onSubmit = { _, _, _ -> }, onBack = noop)
    }
    @Test fun start_edit() = shots("start_edit") {
        StartContent(editing = Fakes.bed12.transfusion, limitMin = 240, warnBeforeMin = 15, openedAt = Fakes.now, onSubmit = { _, _, _ -> }, onBack = noop)
    }

    @Composable
    private fun Detail(item: TransfusionWithCheckpoints, now: Long = Fakes.now) = DetailContent(
        item = item, now = now, toleranceMin = 5, onBack = noop, onEdit = noop, onReaction = noop, onEnd = {}, onAcknowledge = {},
    )
    @Test fun detail_upcoming() = shots("detail_upcoming") { Detail(Fakes.bed14) }
    @Test fun detail_ringing() = shots("detail_ringing") { Detail(ringingBed()) }
    @Test fun detail_overdue_missed() = shots("detail_overdue_missed") { Detail(Fakes.bed12) }
    @Test fun detail_on_time_and_late() = shots("detail_on_time_late") {
        Detail(Fakes.bed12.with(CheckpointType.HOURLY, 60, ackAfterMin = 9), Fakes.at("2026-01-15T11:40"))
    }
    @Test fun detail_ended_prompt() = shots("detail_ended_end_vs") { Detail(ended(TransfusionStatus.ENDED)) }
    @Test fun detail_stopped() = shots("detail_stopped_reaction") { Detail(ended(TransfusionStatus.STOPPED_REACTION)) }

    @Composable
    private fun Alarm(
        rows: List<app.transfusionvs.data.CheckpointWithBed> = emptyList(),
        round: app.transfusionvs.alarm.RoundUi? = null,
        open: Map<Long, RoundChoice> = emptyMap(),
    ) = AlarmContent(
        rows = rows, now = Fakes.now, snoozeMin = 2, onAcknowledge = {}, onSnooze = noop, onStopForReaction = {}, round = round, initiallyOpen = open,
    )
    @Test fun alarm_one_bed() = shots("alarm_1bed") { Alarm(listOf(Fakes.ringing(Fakes.bed14, CheckpointType.M15, 15))) }
    @Test fun alarm_two_beds() = shots("alarm_2beds") {
        Alarm(listOf(Fakes.ringing(Fakes.bed12, CheckpointType.HOURLY, 60), Fakes.ringing(Fakes.bed14, CheckpointType.M15, 15)))
    }
    @Test fun alarm_snoozed() = shots("alarm_snoozed") {
        Alarm(listOf(Fakes.ringing(Fakes.bed14, CheckpointType.M15, 15).copy(snoozeCount = 1, snoozeUntil = Fakes.now + 120_000)))
    }
    @Test fun alarm_no_more_snooze() = shots("alarm_snooze_used_up") {
        Alarm(listOf(Fakes.ringing(Fakes.bed14, CheckpointType.M15, 15).copy(snoozeCount = 3)))
    }
    @Test fun round_alarm() = shots("round_alarm") { Alarm(round = TurnFakes.round()) }
    @Test fun round_seven() = shots("round_7beds") { Alarm(round = TurnFakes.round(TurnFakes.sevenBeds)) }
    @Test fun round_checklist() = shots("round_checklist") {
        Alarm(round = TurnFakes.round(started = true), open = mapOf(61L to RoundChoice.CANT_TURN, 62L to RoundChoice.OTHER_POSITION))
    }
    @Test fun alarm_combined() = shots("alarm_transfusion_and_round") {
        Alarm(listOf(Fakes.ringing(Fakes.bed14, CheckpointType.M15, 15)), TurnFakes.round(TurnFakes.roundBeds.take(2)))
    }

    private val bedsChoice = (1..20).map { BedChoice(it.toLong(), if (it == 20) longBed else "Bed $it", it % 8) }
    @Test fun reaction_no_bed() = shots("reaction_no_bed") { ReactionScreen(beds = emptyList(), onStopped = {}, onBack = noop) }
    @Test fun reaction_one_bed() = shots("reaction_1bed") { ReactionScreen(beds = bedsChoice.take(1), onStopped = {}, onBack = noop) }
    @Test fun reaction_20_beds() = shots("reaction_20beds") { ReactionScreen(beds = bedsChoice, onStopped = {}, onBack = noop) }

    @Test fun turn_start() = shots("turn_start") {
        TurnStartContent(editing = null, defaultIntervalMin = 120, openedAt = Fakes.at("2026-01-15T09:45"), onSubmit = {}, onBack = noop)
    }
    @Test fun turn_edit() = shots("turn_edit") {
        TurnStartContent(editing = TurnFakes.isolation.plan, defaultIntervalMin = 120, openedAt = Fakes.now, onSubmit = {}, onBack = noop)
    }

    @Composable
    private fun TurnDetail(item: app.transfusionvs.data.TurnPlanWithSlots) = TurnDetailContent(
        item = item, now = Fakes.now, windowMin = 15, onBack = noop, onEdit = noop, onStop = {}, onRestart = {}, onOpenRound = noop,
    )
    @Test fun turn_detail_on_track() = shots("turn_detail_on_track") { TurnDetail(TurnFakes.bed3) }
    @Test fun turn_detail_overdue() = shots("turn_detail_overdue_missed") { TurnDetail(TurnFakes.bed7) }
    @Test fun turn_detail_skipped() = shots("turn_detail_skipped") { TurnDetail(TurnFakes.isolation) }
    @Test fun turn_detail_stopped() = shots("turn_detail_stopped") {
        TurnDetail(TurnFakes.bed3.copy(plan = TurnFakes.bed3.plan.copy(status = TurnPlanStatus.STOPPED)))
    }

    @Test fun settings() = shots("settings") {
        SettingsContent(
            values = SettingsValues(
                soundOn = true, soundUri = null, snoozeMin = 2, missedTimeoutMin = 10, toleranceMin = 5,
                preAlertMin = 0, limitMin = 240, warnBeforeMin = 15,
            ),
            soundTitle = "Phone's default alarm sound", onChange = {}, onPickSound = noop, onBack = noop, onSetup = noop,
        )
    }

    @Test fun setup() = shots("setup_check") {
        SetupContent(
            checks = listOf(
                CheckItem("Notifications allowed", "Needed for the full-screen alarm and the Overdue reminder.", true, emptyList()),
                CheckItem("Full-screen alarm allowed", "Shows the alarm over the lock screen.", false, listOf(Intent())),
                CheckItem("Alarm volume above 0", "Alarms ring on silent and vibrate, but not if the alarm volume is at 0.", false, listOf(Intent())),
            ),
            guide = OemGuide("Xiaomi / Redmi / POCO", listOf(OemStep("Autostart → turn ON for A S.", listOf(Intent())))),
            tests = emptyList(), ringing = false, message = null,
            onBack = noop, onFix = {}, onOpenStep = {}, onTest = { _, _ -> }, onCancelTest = {}, onOpenAlarm = noop, versionName = "test",
        )
    }

    /** The app has no dark theme: the night qualifier must look identical. Kept as proof. */
    @Test fun dark_system_setting() {
        variant("w360_f10_night", "home_both_features", "w360dp-h800dp-night-xxhdpi", 1f) { Home(tx = Fakes.home, turns = TurnFakes.home) }
        variant("w360_f10_night", "alarm_1bed", "w360dp-h800dp-night-xxhdpi", 1f) { Alarm(listOf(Fakes.ringing(Fakes.bed14, CheckpointType.M15, 15))) }
    }
}
