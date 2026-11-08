package spike

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.loadXmlImageVector
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import java.io.File
import org.jetbrains.skia.EncodedImageFormat
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.xml.sax.InputSource

class ProbeTest {
    @get:Rule
    val rule = createComposeRule()

    private fun res(path: String) = javaClass.classLoader.getResourceAsStream(path)!!.readBytes()

    @Test
    fun everyReferenceVectorParses() {
        val names = File(javaClass.classLoader.getResource("drawable")!!.toURI()).list()!!.sorted()
        for (n in names) {
            val v = loadXmlImageVector(InputSource(res("drawable/$n").inputStream()), Density(1f))
            println("SPIKE vector $n ${v.defaultWidth}x${v.defaultHeight} viewport ${v.viewportWidth}x${v.viewportHeight}")
        }
        assertTrue(names.size >= 20)
    }

    @Test
    fun fontsFromBytesRenderInAScene() {
        val family = FontFamily(
            Font("rh_text_regular", res("font/rh_text_regular.ttf"), FontWeight.Normal, FontStyle.Normal),
            Font("rh_display_black", res("font/rh_display_black.ttf"), FontWeight.Black, FontStyle.Normal),
        )
        val scene = ImageComposeScene(1080, 600, Density(3f, 1f)) {
            Text("10:35 · 7654812 · Measured & documented ✓", style = TextStyle(fontFamily = family, fontSize = 24.sp, fontWeight = FontWeight.Black))
        }
        val png = scene.render().encodeToData(EncodedImageFormat.PNG)!!.bytes
        File("build/spike-font.png").writeBytes(png)
        println("SPIKE font scene png ${png.size} bytes")
        scene.close()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun desktopTestSceneSizeAndDensity() = runDesktopComposeUiTest(width = 1080, height = 2400) {
        var seen = ""
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(3f, 2f)) {
                val d = LocalDensity.current
                @OptIn(ExperimentalComposeUiApi::class)
                val size = LocalWindowInfo.current.containerSize
                seen = "density=${d.density} fontScale=${d.fontScale} container=${size.width}x${size.height}px"
                Box(Modifier.fillMaxSize()) { Text("probe") }
            }
        }
        onNodeWithText("probe").assertExists()
        println("SPIKE runDesktopComposeUiTest(1080x2400): $seen")
    }

    @Test
    fun theDefaultComposeRule() {
        var seen = ""
        rule.setContent {
            val d = LocalDensity.current
            @OptIn(ExperimentalComposeUiApi::class)
            val size = LocalWindowInfo.current.containerSize
            seen = "density=${d.density} fontScale=${d.fontScale} container=${size.width}x${size.height}px"
            Text("rule")
        }
        rule.onNodeWithText("rule").assertExists()
        println("SPIKE createComposeRule default: $seen")
    }
}
