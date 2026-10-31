package app.transfusionvs.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import app.transfusionvs.ui.theme.WardColors
import app.transfusionvs.ui.theme.WardDay
import app.transfusionvs.ui.theme.WardNight
import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * What ships is what lab/color_check.py verified, and every pair the screens actually use meets the
 * fixed thresholds of docs/TEST_LOOP.md §4: text ≥ 4.5:1, large text and UI ≥ 3:1, key numbers ≥ 7:1.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WardColorsTest {

    private fun hex(c: Color) = "#%06X".format(0xFFFFFF and (((c.red * 255 + 0.5f).toInt() shl 16) or ((c.green * 255 + 0.5f).toInt() shl 8) or (c.blue * 255 + 0.5f).toInt()))

    private fun tokens(c: WardColors) = mapOf(
        "bg" to c.bg, "card" to c.card, "ink" to c.ink, "muted" to c.muted, "calm" to c.calm, "on_calm" to c.onCalm,
        "soon" to c.soon, "on_soon" to c.onSoon, "now" to c.now, "on_now" to c.onNow, "overdue" to c.overdue, "on_overdue" to c.onOverdue,
        "danger" to c.danger, "on_danger" to c.onDanger, "type_tx" to c.typeTx, "type_rp" to c.typeRp,
        "alarm_bg" to c.alarmBg, "alarm_ink" to c.alarmInk, "alarm_action" to c.alarmAction, "alarm_on_action" to c.alarmOnAction,
        "danger_fill" to c.dangerFill, "on_danger_fill" to c.onDangerFill, "danger_edge" to c.dangerEdge,
        "primary" to c.primary, "on_primary" to c.onPrimary, "primary_edge" to c.primaryEdge,
    )

    @Test
    fun tokensMatchTheVerifiedPalette() {
        val json = JSONObject(File("../lab/palettes.json").readText()).getJSONObject("ward_card")
        for ((theme, colors) in listOf("day" to WardDay, "night" to WardNight)) {
            val t = json.getJSONObject(theme)
            tokens(colors).forEach { (key, color) -> assertEquals("$theme.$key", t.getString(key).uppercase(), hex(color)) }
        }
    }

    private fun contrast(fg: Color, bg: Color): Double {
        val a = fg.compositeOver(bg).luminance() + 0.05
        val b = bg.luminance() + 0.05
        return maxOf(a, b) / minOf(a, b)
    }

    private fun check(what: String, fg: Color, bg: Color, min: Double) {
        val r = contrast(fg, bg)
        assertTrue("$what: ${"%.2f".format(r)}:1 < $min:1", r >= min)
    }

    private fun checkAll(name: String, c: WardColors) {
        val text = 4.5
        val ui = 3.0
        val key = 7.0
        // Key numbers: times and beds on the cards, the header and the alarm.
        check("$name key on card", c.ink, c.card, key)
        check("$name key on alarm", c.alarmInk, c.alarmBg, key)
        check("$name key on danger fill", c.onDangerFill, c.dangerFill, key)
        // Text.
        check("$name muted on card", c.muted, c.card, text)
        check("$name muted on page", c.muted, c.bg, text)
        check("$name ink on page", c.ink, c.bg, text)
        check("$name header title", c.onHeader, c.header, text)
        check("$name header caps", c.headerCaps, c.header, text)
        check("$name alarm muted", c.alarmMuted, c.alarmBg, text)
        check("$name alarm caps", c.alarmCaps, c.alarmBg, text)
        check("$name alarm step", c.alarmStep, c.alarmBg, text)
        check("$name alarm chip label", c.alarmBg, c.alarmChip, text)
        check("$name setup warning", c.onAlert, c.alertFill, text)
        check("$name rail line", c.railLine.compositeOver(c.card), c.card, ui)
        check("$name quiet reaction words", c.danger, c.card, text)
        check("$name disabled label", c.muted, c.bg, text)
        check("$name alarm ink on tint", c.alarmInk, c.alarmTint.compositeOver(c.alarmBg), text)
        check("$name action label", c.alarmOnAction, c.alarmAction, text)
        check("$name primary label", c.onPrimary, c.primary, text)
        check("$name danger button", c.onDanger, c.danger, text)
        check("$name overdue pill", c.onOverdue, c.overdue, text)
        check("$name now pill", c.onNow, c.now, text)
        check("$name soon pill", c.onSoon, c.soon, text)
        check("$name type tag transfusion", c.typeTx, c.card, text)
        check("$name type tag turning", c.typeRp, c.card, text)
        check("$name danger label on card", c.danger, c.card, text)
        // Large text and UI components.
        check("$name chip edge", c.muted, c.card, ui)
        check("$name primary edge", c.primaryEdge, c.bg, ui)
        check("$name overdue edge", c.overdue, c.card, ui)
        check("$name alarm secondary edge", c.alarmMuted, c.alarmBg, ui)
        check("$name alarm action on background", c.alarmAction, c.alarmBg, ui)
        check("$name reaction edge", c.dangerEdge, c.bg, ui)
        check("$name calm bead", c.calm, c.card, ui)
        check("$name soon bead", c.soon, c.card, ui)
        check("$name now bead", c.now, c.card, ui)
    }

    @Test fun dayMeetsTheThresholds() = checkAll("day", WardDay)

    @Test fun nightMeetsTheThresholds() = checkAll("night", WardNight)
}
