package app.transfusionvs.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import app.transfusionvs.alarm.AlarmContent
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.schedule.Labels
import app.transfusionvs.ui.screens.ReactionScreen
import app.transfusionvs.ui.theme.TvsTheme
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Clinical content is frozen: the wording of /design/front.png and /design/back.png, word for word.
 * A redesign may change how it looks, never what it says, and this is what proves it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w393dp-h2000dp-xxhdpi")
class ClinicalContentTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun reactionGuideMatchesTheBackOfTheCard() {
        rule.setContent { TvsTheme { ReactionScreen(beds = emptyList(), onStopped = {}, onBack = {}) } }
        listOf(
            "Suspected transfusion reaction",
            "Fever or rigors", "Rash, itch, swelling", "Breathless, wheeze", "↓SpO₂, ↓BP or ↑HR",
            "Back or loin pain", "Dark or red urine", "New distress or irritability",
        ).forEach { rule.onNodeWithText(it).assertExists() }
        listOf(
            "STOP the transfusion.", "Keep the IV line open.",
            "Call the doctor now.", "Stay with the child.",
            "Check vital signs + SpO₂.", "Check patient ID against the bag.",
            "Inform the blood bank.", "Keep the bag and giving set.",
            "Document in Cerner", "once the child is safe.",
        ).forEach { rule.onNodeWithText(it, substring = true).assertExists() }
        // "STOP & CALL IF YOU SEE" and "DO NOW" are caps labels on the card.
        assertTrue(rule.onAllNodesWithText("stop & call if you see", ignoreCase = true).fetchSemanticsNodes().isNotEmpty())
        assertTrue(rule.onAllNodesWithText("do now", ignoreCase = true).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun alarmCarriesTheFrontOfTheCard() {
        rule.setContent {
            TvsTheme {
                AlarmContent(
                    rows = listOf(Fakes.ringing(Fakes.bed14, CheckpointType.M15, 15)), now = Fakes.now, snoozeMin = 2,
                    onAcknowledge = {}, onSnooze = {}, onStopForReaction = {},
                )
            }
        }
        rule.onNodeWithText("T · HR · RR · BP · SpO₂", substring = true).assertExists()
        rule.onNodeWithText("in Cerner, right away", substring = true).assertExists()
        assertTrue(rule.onAllNodesWithText("measure", ignoreCase = true, substring = true).fetchSemanticsNodes().isNotEmpty())
        assertTrue(rule.onAllNodesWithText("document", ignoreCase = true, substring = true).fetchSemanticsNodes().isNotEmpty())
    }

    /** One digit system everywhere, whatever the phone's language: Latin, like the card. */
    @Test
    fun timesKeepLatinDigitsOnAnArabicPhone() {
        val before = Locale.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Cairo"))
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            assertEquals("11:05", Labels.clock(Fakes.now))
            assertEquals("2 h", Labels.duration(120))
            assertEquals("15 min", Labels.duration(15))
        } finally {
            Locale.setDefault(before)
        }
    }
}
