package app.transfusionvs.screenshots

import android.content.Intent
import androidx.compose.runtime.Composable
import app.transfusionvs.alarm.AlarmContent
import app.transfusionvs.data.CheckpointStatus
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.screenshots.Fakes.with
import app.transfusionvs.setup.CheckItem
import app.transfusionvs.setup.OemGuide
import app.transfusionvs.setup.OemStep
import app.transfusionvs.ui.screens.BedChoice
import app.transfusionvs.ui.screens.DetailContent
import app.transfusionvs.ui.screens.HomeContent
import app.transfusionvs.ui.screens.ReactionScreen
import app.transfusionvs.ui.screens.SettingsContent
import app.transfusionvs.ui.screens.SettingsValues
import app.transfusionvs.ui.screens.SetupContent
import app.transfusionvs.ui.screens.StartContent
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
 * Reference screenshots of the transfusion screens. They prove that design-system refactors
 * change nothing visible (CI fails on any pixel difference) and they sit next to the
 * repositioning screens in review.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = PHONE)
class TransfusionScreenshots {

    @Before
    fun fixedClock() {
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Cairo"))
        Locale.setDefault(Locale.ENGLISH)
    }

    private fun shot(name: String, content: @Composable () -> Unit) {
        captureRoboImage("src/test/screenshots/$name.png") { TvsTheme { content() } }
    }

    @Test
    fun home() = shot("transfusion_home") {
        HomeContent(
            home = Fakes.home, turns = emptyList(), ringing = false, setupProblems = 0, now = Fakes.now,
            onStartTransfusion = {}, onStartTurning = {}, onOpen = {}, onOpenTurnPlan = {}, onReaction = {}, onSetup = {}, onSettings = {}, onOpenAlarm = {}, onAcknowledge = {},
        )
    }

    @Test
    fun homeEmpty() = shot("transfusion_home_empty") {
        HomeContent(
            home = emptyList(), turns = emptyList(), ringing = false, setupProblems = 2, now = Fakes.now,
            onStartTransfusion = {}, onStartTurning = {}, onOpen = {}, onOpenTurnPlan = {}, onReaction = {}, onSetup = {}, onSettings = {}, onOpenAlarm = {}, onAcknowledge = {},
        )
    }

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun start() = shot("transfusion_start") {
        StartContent(editing = null, beds = Fakes.wardBeds, busy = emptyMap(), defaultDurationMin = 120, limitMin = 240, warnBeforeMin = 15, openedAt = Fakes.now, onSubmit = { _, _, _ -> }, onBack = {})
    }

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun edit() = shot("transfusion_edit") {
        StartContent(
            editing = Fakes.bed12.transfusion, beds = Fakes.wardBeds, busy = emptyMap(), defaultDurationMin = 120, limitMin = 240, warnBeforeMin = 15, openedAt = Fakes.now,
            onSubmit = { _, _, _ -> }, onBack = {},
        )
    }

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun detail() = shot("transfusion_detail") {
        DetailContent(
            item = Fakes.bed12.with(CheckpointType.HOURLY, 60, ackAfterMin = 9),
            now = Fakes.at("2026-01-15T11:40"), toleranceMin = 5,
            onBack = {}, onEdit = {}, onReaction = {}, onEnd = {}, onAcknowledge = {},
        )
    }

    @Test
    fun alarmOneBed() = shot("transfusion_alarm_one_bed") {
        AlarmContent(
            rows = listOf(Fakes.ringing(Fakes.bed14, CheckpointType.M15, 15)),
            now = Fakes.now, snoozeMin = 2, onAcknowledge = {}, onSnooze = {}, onStopForReaction = {},
        )
    }

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun alarmTwoBeds() = shot("transfusion_alarm_two_beds") {
        AlarmContent(
            rows = listOf(
                Fakes.ringing(Fakes.bed12, CheckpointType.HOURLY, 60),
                Fakes.ringing(Fakes.bed14, CheckpointType.M15, 15),
            ),
            now = Fakes.now, snoozeMin = 2, onAcknowledge = {}, onSnooze = {}, onStopForReaction = {},
        )
    }

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun reaction() = shot("transfusion_reaction") {
        ReactionScreen(
            beds = listOf(BedChoice(1, "7654812", 0), BedChoice(2, "7654814", 1)),
            onStopped = {}, onBack = {},
        )
    }

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun setup() = shot("setup_check") {
        SetupContent(
            checks = listOf(
                CheckItem("Notifications allowed", "Needed for the full-screen alarm and the Overdue reminder.", true, emptyList()),
                CheckItem("Exact alarms allowed", "Lets every checkpoint ring at the exact minute, even in deep sleep.", true, emptyList()),
                CheckItem("Full-screen alarm allowed", "Shows the alarm over the lock screen.", false, listOf(Intent())),
                CheckItem("Battery optimization off", "Stops Android from putting the app to sleep.", false, listOf(Intent())),
                CheckItem("Alarm volume above 0", "Alarms ring on silent and vibrate, but not if the alarm volume is at 0.", true, emptyList()),
                CheckItem("Do Not Disturb lets alarms through", "\"Total silence\" mutes alarms. Use Alarms-only or allow alarms.", true, emptyList()),
            ),
            guide = OemGuide(
                "Xiaomi / Redmi / POCO",
                listOf(
                    OemStep("Autostart → turn ON for A S.", listOf(Intent())),
                    OemStep("Battery saver → choose \"No restrictions\".", listOf(Intent())),
                    OemStep("Lock the app in Recents: open Recents, then long-press (or pull down) its card and tap the lock icon."),
                ),
            ),
            tests = listOf(
                Fakes.test(1, "2026-01-15T10:58", CheckpointStatus.ACKED, fired = "2026-01-15T10:58", ack = "2026-01-15T10:59"),
                Fakes.test(2, "2026-01-15T17:05", CheckpointStatus.PENDING),
            ),
            ringing = false,
            message = "Test alarm set for 11:06. Lock the phone now and wait — it must ring even on silent.",
            onBack = {}, onFix = {}, onOpenStep = {}, onTest = { _, _ -> }, onCancelTest = {}, onOpenAlarm = {},
            versionName = "test",
        )
    }

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun settings() = shot("settings") {
        SettingsContent(
            values = SettingsValues(
                soundOn = true, soundUri = null, snoozeMin = 2, missedTimeoutMin = 10, toleranceMin = 5,
                preAlertMin = 0, limitMin = 240, warnBeforeMin = 15,
            ),
            soundTitle = "Phone's default alarm sound",
            onChange = {}, onPickSound = {}, onBack = {}, onSetup = {},
        )
    }
}

/** A typical phone (Pixel 7 class). */
const val PHONE = "w393dp-h852dp-xxhdpi"

/** Same width, taller, so long screens are captured whole. */
const val TALL_PHONE = "w393dp-h1600dp-xxhdpi"

/** For the longest screen (repositioning Start with its preview). */
const val XTALL_PHONE = "w393dp-h2600dp-xxhdpi"
