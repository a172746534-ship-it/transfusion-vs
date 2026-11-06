package app.transfusionvs.screenshots

import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.viewinterop.AndroidView
import app.transfusionvs.alarm.AlarmContent
import app.transfusionvs.alarm.CustomRing
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.data.CustomTimerWithSlots
import app.transfusionvs.schedule.BedViews
import app.transfusionvs.ui.screens.CustomDetailContent
import app.transfusionvs.ui.screens.CustomStartContent
import app.transfusionvs.ui.screens.HomeContent
import app.transfusionvs.ui.theme.TvsTheme
import app.transfusionvs.ui.theme.Ward
import app.transfusionvs.widget.BedsWidget
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
 * The custom timer's screens in every state, at the audit's sizes (as AuditScreenshots): written to
 * build/audit, always recorded, never compared. The name rules are checked in CustomNameFitTest.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xxhdpi")
class CustomScreenshots {

    @Before
    fun fixedClock() {
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Cairo"))
        Locale.setDefault(Locale.ENGLISH)
    }

    private val record = RoborazziOptions(taskType = RoborazziTaskType.Record)

    private fun variant(tag: String, name: String, qualifiers: String, font: Float, night: Boolean = false, content: @Composable () -> Unit) {
        RuntimeEnvironment.setQualifiers(qualifiers)
        // The real font scale too: a dialog is a window of its own and doesn't see the density below.
        RuntimeEnvironment.setFontScale(font)
        captureRoboImage("build/audit/${name}__$tag.png", roborazziOptions = record) {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, font)) { TvsTheme(night = night) { content() } }
        }
    }

    private fun shots(name: String, content: @Composable () -> Unit) {
        variant("w360_f10", name, "w360dp-h800dp-xxhdpi", 1f, content = content)
        variant("w360_f20", name, "w360dp-h2600dp-xxhdpi", 2f, content = content)
        variant("w412_f10", name, "w412dp-h915dp-xxhdpi", 1f, content = content)
        variant("w412_f10_night", name, "w412dp-h915dp-xxhdpi", 1f, night = true, content = content)
    }

    private val noop: () -> Unit = {}

    @Composable
    private fun Home(custom: List<CustomTimerWithSlots>, withOthers: Boolean = true) = HomeContent(
        home = if (withOthers) Fakes.home else emptyList(), turns = if (withOthers) TurnFakes.home else emptyList(),
        ringing = false, setupProblems = 0, now = Fakes.now,
        onStartTransfusion = noop, onStartTurning = noop, onOpen = {}, onOpenTurnPlan = {}, onReaction = noop, onSetup = noop,
        onSettings = noop, onOpenAlarm = noop, onAcknowledge = {}, custom = custom,
    )

    @Composable
    private fun Alarm(customs: List<CustomRing>, rows: List<app.transfusionvs.data.CheckpointWithBed> = emptyList(), round: app.transfusionvs.alarm.RoundUi? = null) =
        AlarmContent(rows = rows, now = Fakes.now, snoozeMin = 2, onAcknowledge = {}, onSnooze = noop, onStopForReaction = {}, round = round, customs = customs)

    @Composable
    private fun Detail(item: CustomTimerWithSlots) = CustomDetailContent(
        item = item, now = Fakes.now, toleranceMin = 15, missedTimeoutMin = 10,
        onBack = noop, onEdit = noop, onStop = {}, onSetStop = { _, _ -> }, onDone = {},
    )

    // ---- Home ----
    @Test fun home_with_custom() = shots("home_with_custom") { Home(CustomFakes.home) }
    @Test fun home_only_custom() = shots("home_only_custom") { Home(listOf(CustomFakes.dressing, CustomFakes.drain), withOthers = false) }

    // ---- Start ----
    @Test fun custom_start_empty() = shots("custom_start_empty") { CustomStartContent(editing = null, now = Fakes.now, onSubmit = {}, onBack = noop) }
    @Test fun custom_start_valid() = shots("custom_start_valid") {
        CustomStartContent(editing = null, now = Fakes.now, onSubmit = {}, onBack = noop, initialName = "Check chest drain", initialMrn = "7654821")
    }
    @Test fun custom_start_other() = shots("custom_start_other") {
        CustomStartContent(editing = null, now = Fakes.now, onSubmit = {}, onBack = noop, initialName = "Check chest drain", initialMrn = "7654821", initialOther = true)
    }
    @Test fun custom_start_long_name() = shots("custom_start_long_name") {
        CustomStartContent(editing = null, now = Fakes.now, onSubmit = {}, onBack = noop, initialName = CustomFakes.LONG_NAME, initialMrn = "7654824")
    }
    @Test fun custom_edit() = shots("custom_edit") { CustomStartContent(editing = CustomFakes.drain.timer, now = Fakes.now, onSubmit = {}, onBack = noop) }

    // ---- Detail ----
    @Test fun custom_detail_upcoming() = shots("custom_detail_upcoming") { Detail(CustomFakes.drain) }
    @Test fun custom_detail_ringing() = shots("custom_detail_ringing") { Detail(CustomFakes.sugar) }
    @Test fun custom_detail_overdue() = shots("custom_detail_overdue") { Detail(CustomFakes.ivSite) }
    @Test fun custom_detail_stop_set() = shots("custom_detail_stop_set") { Detail(CustomFakes.withStop) }
    @Test fun custom_detail_stopped() = shots("custom_detail_stopped") { Detail(CustomFakes.stopped) }

    // ---- Alarm ----
    @Test fun custom_alarm_one() = shots("custom_alarm_1") { Alarm(CustomFakes.rings(CustomFakes.sugar)) }
    @Test fun custom_alarm_long_name() = shots("custom_alarm_long_name") { Alarm(CustomFakes.rings(CustomFakes.dressing)) }
    @Test fun custom_alarm_wide_name() = shots("custom_alarm_wide_name") { Alarm(CustomFakes.rings(CustomFakes.ringing(CustomFakes.WIDE_NAME))) }
    @Test fun custom_alarm_several() = shots("custom_alarm_3") { Alarm(CustomFakes.rings(CustomFakes.sugar, CustomFakes.dressing, CustomFakes.ivSite)) }
    @Test fun custom_alarm_with_transfusion() = shots("custom_alarm_with_transfusion") {
        Alarm(CustomFakes.rings(CustomFakes.sugar), rows = listOf(Fakes.ringing(Fakes.bed14, CheckpointType.M15, 15)))
    }
    @Test fun custom_alarm_with_transfusion_and_round() = shots("custom_alarm_with_transfusion_and_round") {
        Alarm(
            CustomFakes.rings(CustomFakes.sugar, CustomFakes.dressing),
            rows = listOf(Fakes.ringing(Fakes.bed14, CheckpointType.M15, 15)), round = TurnFakes.round(TurnFakes.roundBeds.take(2)),
        )
    }
    @Test fun custom_alarm_with_round() = shots("custom_alarm_with_round") { Alarm(CustomFakes.rings(CustomFakes.sugar), round = TurnFakes.round(TurnFakes.roundBeds.take(2))) }

    // ---- Widget ----
    @Test fun custom_widget() = shots("custom_widget") {
        val beds = Fakes.home.map { BedViews.of(it, Fakes.now) } + TurnFakes.home.map { BedViews.of(it, Fakes.now, 10) } +
            CustomFakes.home.map { BedViews.of(it, Fakes.now, 10) }
        val night = Ward.c.isNight
        AndroidView(factory = { ctx -> BedsWidget.build(ctx, BedsWidget.masked(BedViews.sorted(beds), beds.map { it.bed }), Fakes.now, night).apply(ctx, FrameLayout(ctx)) })
    }
}
