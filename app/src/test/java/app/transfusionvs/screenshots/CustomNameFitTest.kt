package app.transfusionvs.screenshots

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import app.transfusionvs.alarm.AlarmContent
import app.transfusionvs.data.CustomTimerWithSlots
import app.transfusionvs.ui.screens.HomeContent
import app.transfusionvs.ui.theme.TvsTheme
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The task name at font 2.0 on a 360 dp phone: a typical 30-character name takes at most two lines
 * on the card and on the alarm, never below 16 sp nominal; the widest 30 letters are never clipped
 * (and on the alarm take at most three lines). Measured from the real text layout.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xxhdpi")
class CustomNameFitTest {

    @get:Rule val rule = createComposeRule()

    @Before
    fun fixedClock() {
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Cairo"))
        Locale.setDefault(Locale.ENGLISH)
    }

    private fun atFont2(content: @Composable () -> Unit) {
        rule.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, 2f)) { TvsTheme { content() } }
        }
        rule.waitForIdle()
    }

    private fun layoutOf(text: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        val action = rule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode().config.getOrNull(SemanticsActions.GetTextLayoutResult)
        assertTrue("no text layout for $text", action?.action?.invoke(results) == true)
        return results.single().also { println("NAMEFIT \"$text\": ${it.lineCount} lines at ${it.layoutInput.style.fontSize}, overflow=${it.hasVisualOverflow}") }
    }

    @Composable
    private fun Alarm(item: CustomTimerWithSlots) =
        AlarmContent(rows = emptyList(), now = Fakes.now, snoozeMin = 2, onAcknowledge = {}, onSnooze = {}, onStopForReaction = {}, customs = CustomFakes.rings(item))

    @Composable
    private fun Home(item: CustomTimerWithSlots) = HomeContent(
        home = emptyList(), turns = emptyList(), ringing = false, setupProblems = 0, now = Fakes.now,
        onStartTransfusion = {}, onStartTurning = {}, onOpen = {}, onOpenTurnPlan = {}, onReaction = {}, onSetup = {},
        onSettings = {}, onOpenAlarm = {}, onAcknowledge = {}, custom = listOf(item),
    )

    @Test fun aTypical30CharacterName_onTheAlarm_takesAtMostTwoLines() {
        atFont2 { Alarm(CustomFakes.dressing) }
        val l = layoutOf(CustomFakes.LONG_NAME)
        assertTrue("lines ${l.lineCount}", l.lineCount <= 2)
        assertFalse(l.hasVisualOverflow)
        assertTrue("never below 16 sp nominal: ${l.layoutInput.style.fontSize}", l.layoutInput.style.fontSize.value >= 16f)
    }

    @Test fun theWidestName_onTheAlarm_takesAtMostThreeLines_neverClipped() {
        atFont2 { Alarm(CustomFakes.ringing(CustomFakes.WIDE_NAME)) }
        val l = layoutOf(CustomFakes.WIDE_NAME)
        assertTrue("lines ${l.lineCount}", l.lineCount <= 3)
        assertFalse(l.hasVisualOverflow)
        assertTrue(l.layoutInput.style.fontSize.value >= 16f)
    }

    @Test fun aTypical30CharacterName_onTheCard_takesAtMostTwoLines() {
        atFont2 { Home(CustomFakes.dressing) }
        val l = layoutOf(CustomFakes.LONG_NAME)
        assertTrue("lines ${l.lineCount}", l.lineCount <= 2)
        assertFalse(l.hasVisualOverflow)
        assertTrue(l.layoutInput.style.fontSize.value >= 16f)
    }

    @Test fun theWidestName_onTheCard_isNeverClipped() {
        atFont2 { Home(CustomFakes.ringing(CustomFakes.WIDE_NAME)) }
        val l = layoutOf(CustomFakes.WIDE_NAME)
        assertFalse(l.hasVisualOverflow)
        assertTrue(l.layoutInput.style.fontSize.value >= 16f)
    }
}
