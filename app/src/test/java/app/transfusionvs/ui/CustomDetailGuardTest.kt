package app.transfusionvs.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.transfusionvs.data.CustomSlotStatus
import app.transfusionvs.data.CustomTimerWithSlots
import app.transfusionvs.screenshots.CustomFakes
import app.transfusionvs.screenshots.Fakes
import app.transfusionvs.ui.screens.CustomDetailContent
import app.transfusionvs.ui.theme.TvsTheme
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock

/**
 * On a timer's screen, Done is in the pinned place while an alarm is open; once it's done, Stop moves
 * there. A second tap meant for Done must not stop the timer: Stop waits a second after taking the place.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CustomDetailGuardTest {

    @get:Rule val rule = createComposeRule()

    /** One test, so the clock only moves forward: the app's tap gate ignores taps under 400 ms apart. */
    @Test fun stopWorksAtOnce_butNotRightAfterDone() {
        ShadowSystemClock.advanceBy(Duration.ofHours(1))
        var item by mutableStateOf<CustomTimerWithSlots?>(CustomFakes.drain)
        var stops = 0
        rule.setContent {
            TvsTheme {
                CustomDetailContent(
                    item = item, now = Fakes.now, toleranceMin = 15, missedTimeoutMin = 10,
                    onBack = {}, onEdit = {}, onStop = { stops++ }, onSetStop = { _, _ -> }, onDone = {},
                )
            }
        }
        rule.waitForIdle()
        // Nothing due when the screen opens: Stop works at once.
        rule.onNodeWithText("Stop timer").performClick()
        rule.waitForIdle()
        assertEquals(1, stops)

        // An alarm opens, then Done closes it: Stop takes the pinned place again.
        ShadowSystemClock.advanceBy(Duration.ofSeconds(5))
        rule.runOnIdle { item = CustomFakes.sugar }
        rule.waitForIdle()
        rule.runOnIdle {
            item = CustomFakes.sugar.copy(
                slots = CustomFakes.sugar.slots.map { if (it.status == CustomSlotStatus.RINGING) it.copy(status = CustomSlotStatus.ACKED, ackAt = Fakes.now) else it },
            )
        }
        rule.waitForIdle()

        ShadowSystemClock.advanceBy(Duration.ofMillis(500))
        rule.onNodeWithText("Stop timer").performClick()
        rule.waitForIdle()
        assertEquals("a tap half a second after Done is ignored", 1, stops)

        ShadowSystemClock.advanceBy(Duration.ofMillis(700))
        rule.onNodeWithText("Stop timer").performClick()
        rule.waitForIdle()
        assertEquals("a deliberate tap stops it", 2, stops)
    }
}
