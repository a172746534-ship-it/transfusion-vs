package app.transfusionvs.screenshots

import androidx.compose.runtime.Composable
import app.transfusionvs.alarm.AlarmContent
import app.transfusionvs.alarm.RoundChoice
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.ui.screens.HomeContent
import app.transfusionvs.ui.screens.TurnDetailContent
import app.transfusionvs.ui.screens.TurnStartContent
import app.transfusionvs.ui.theme.TvsTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale
import java.util.TimeZone

/**
 * Repositioning screens, each next to its transfusion counterpart in review
 * (repositioning_home ↔ transfusion_home, repositioning_start ↔ transfusion_start, …).
 * Design stage: sample data only; nothing here touches the alarm engine.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = PHONE)
class RepositioningScreenshots {

    @Before
    fun fixedClock() {
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Cairo"))
        Locale.setDefault(Locale.ENGLISH)
    }

    private fun shot(name: String, content: @Composable () -> Unit) {
        captureRoboImage("src/test/screenshots/$name.png") { TvsTheme { content() } }
    }

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun home() = shot("repositioning_home") {
        HomeContent(
            home = listOf(Fakes.bed14), turns = TurnFakes.home, ringing = false, setupProblems = 0, now = Fakes.now,
            onStartTransfusion = {}, onStartTurning = {}, onOpen = {}, onOpenTurnPlan = {}, onReaction = {}, onSetup = {}, onSettings = {}, onOpenAlarm = {}, onAcknowledge = {},
        )
    }

    @Test
    fun homeOnlyRepositioning() = shot("repositioning_home_only") {
        HomeContent(
            home = emptyList(), turns = listOf(TurnFakes.bed3), ringing = false, setupProblems = 0, now = Fakes.now,
            onStartTransfusion = {}, onStartTurning = {}, onOpen = {}, onOpenTurnPlan = {}, onReaction = {}, onSetup = {}, onSettings = {}, onOpenAlarm = {}, onAcknowledge = {},
        )
    }

    /** Started at 09:45: 10:00 is under 30 min away, so the first turn defaults to 12:00. */
    @Test
    @Config(qualifiers = XTALL_PHONE)
    fun start() = shot("repositioning_start") {
        TurnStartContent(editing = null, defaultIntervalMin = 120, openedAt = Fakes.at("2026-01-15T09:45"), onSubmit = {}, onBack = {})
    }

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun edit() = shot("repositioning_edit") {
        TurnStartContent(editing = TurnFakes.isolation.plan, defaultIntervalMin = 120, openedAt = Fakes.now, onSubmit = {}, onBack = {})
    }

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun detail() = shot("repositioning_detail") {
        TurnDetailContent(
            item = TurnFakes.bed3, now = Fakes.now, windowMin = 15,
            onBack = {}, onEdit = {}, onStop = {}, onRestart = {}, onOpenRound = {},
        )
    }

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun roundAlarm() = shot("round_alarm") {
        AlarmContent(
            rows = emptyList(), now = Fakes.now, snoozeMin = 2, onAcknowledge = {}, onSnooze = {}, onStopForReaction = {},
            round = TurnFakes.round(),
        )
    }

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun roundChecklist() = shot("round_checklist") {
        AlarmContent(
            rows = emptyList(), now = Fakes.now, snoozeMin = 2, onAcknowledge = {}, onSnooze = {}, onStopForReaction = {},
            round = TurnFakes.round(started = true),
            initiallyOpen = mapOf(61L to RoundChoice.CANT_TURN, 62L to RoundChoice.OTHER_POSITION),
        )
    }

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun roundSevenBeds() = shot("round_seven_beds") {
        AlarmContent(
            rows = emptyList(), now = Fakes.now, snoozeMin = 2, onAcknowledge = {}, onSnooze = {}, onStopForReaction = {},
            round = TurnFakes.round(TurnFakes.sevenBeds),
        )
    }

    /** A transfusion check and a round in the same minute: one screen, transfusion first. */
    @Test
    @Config(qualifiers = TALL_PHONE)
    fun roundWithTransfusion() = shot("round_with_transfusion") {
        AlarmContent(
            rows = listOf(Fakes.ringing(Fakes.bed14, CheckpointType.M15, 15)),
            now = Fakes.now, snoozeMin = 2, onAcknowledge = {}, onSnooze = {}, onStopForReaction = {},
            round = TurnFakes.round(TurnFakes.roundBeds.take(2)),
        )
    }
}
