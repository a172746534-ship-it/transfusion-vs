package app.transfusionvs.directions

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Phase 2: Home, Alarm and Round alarm of each direction, day and night. Written to build/directions. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xxhdpi")
class DirectionShots {

    private val record = RoborazziOptions(taskType = RoborazziTaskType.Record)

    private fun shot(direction: String, screen: String, content: @Composable (P) -> Unit) {
        for (theme in listOf("day", "night")) {
            val p = palette(direction, theme)
            captureRoboImage("build/directions/${direction}__${screen}__$theme.png", roborazziOptions = record) { MaterialTheme { content(p) } }
        }
    }

    @Test fun wardCardHome() = shot("ward_card", "home") { WardCard.Home(it) }
    @Test fun wardCardAlarm() = shot("ward_card", "alarm") { WardCard.Alarm(it) }
    @Test fun wardCardRound() = shot("ward_card", "round") { WardCard.Round(it) }
    @Test fun haloHome() = shot("halo", "home") { Halo.Home(it) }
    @Test fun haloAlarm() = shot("halo", "alarm") { Halo.Alarm(it) }
    @Test fun haloRound() = shot("halo", "round") { Halo.Round(it) }
    @Test fun boardHome() = shot("ward_board", "home") { Board.Home(it) }
    @Test fun boardAlarm() = shot("ward_board", "alarm") { Board.Alarm(it) }
    @Test fun boardRound() = shot("ward_board", "round") { Board.Round(it) }
}
