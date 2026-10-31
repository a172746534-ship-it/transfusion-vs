package app.transfusionvs.screenshots

import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import app.transfusionvs.alarm.AlarmContent
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.schedule.Labels
import app.transfusionvs.ui.screens.HomeContent
import app.transfusionvs.ui.theme.TvsTheme
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A phone set to Arabic must not mirror the app: the UI language is English, and a mirrored alarm
 * screen scrambled its clinical instruction ("min check —-15"). The images are evidence, written
 * outside the reference folder.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ar-rEG-w393dp-h852dp-xxhdpi")
class ArabicPhoneTest {

    @get:Rule val rule = createComposeRule()

    @Before
    fun fixedClock() {
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Cairo"))
        Locale.setDefault(Locale.forLanguageTag("ar-EG"))
    }

    private val record = RoborazziOptions(taskType = RoborazziTaskType.Record)

    @Composable
    private fun Alarm() = AlarmContent(
        rows = listOf(Fakes.ringing(Fakes.bed14, CheckpointType.M15, 15)),
        now = Fakes.now, snoozeMin = 2, onAcknowledge = {}, onSnooze = {}, onStopForReaction = {},
    )

    @Test
    fun theManifestDoesNotAskForMirroring() {
        val flags = RuntimeEnvironment.getApplication().applicationInfo.flags
        assertEquals(0, flags and ApplicationInfo.FLAG_SUPPORTS_RTL)
    }

    @Test
    fun theAlarmStaysLeftToRightWithItsWordsInOrder() {
        var direction: LayoutDirection? = null
        rule.setContent {
            TvsTheme {
                direction = LocalLayoutDirection.current
                Alarm()
            }
        }
        rule.waitForIdle()
        assertEquals(LayoutDirection.Ltr, direction)
        val title = rule.onNodeWithText("15-min check").fetchSemanticsNode().boundsInRoot
        val width = rule.onRoot().fetchSemanticsNode().boundsInRoot.width
        assertTrue("the title starts at the left edge, at ${title.left} of $width", title.left < width / 4)
        rule.onNodeWithText(Labels.ACK).assertExists()
        rule.onNodeWithText("Bed 14").assertExists()
    }

    @Test
    fun alarmAsSeenOnAnArabicPhone() {
        captureRoboImage("build/outputs/roborazzi/arabic_alarm.png", roborazziOptions = record) { TvsTheme { Alarm() } }
    }

    @Test
    fun homeAsSeenOnAnArabicPhone() {
        captureRoboImage("build/outputs/roborazzi/arabic_home.png", roborazziOptions = record) {
            TvsTheme {
                HomeContent(
                    home = Fakes.home, turns = TurnFakes.home, ringing = false, setupProblems = 0, now = Fakes.now,
                    onStartTransfusion = {}, onStartTurning = {}, onOpen = {}, onOpenTurnPlan = {}, onReaction = {}, onSetup = {}, onSettings = {}, onOpenAlarm = {}, onAcknowledge = {},
                )
            }
        }
    }
}
