package app.transfusionvs.directions

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.transfusionvs.R
import app.transfusionvs.data.Position
import app.transfusionvs.data.Position.BACK
import app.transfusionvs.data.Position.LEFT_30
import app.transfusionvs.data.Position.RIGHT_30
import app.transfusionvs.ui.components.PositionIcon
import java.io.File
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import org.json.JSONObject

// Phase 2 prototypes: the three creative directions, drawn with sample data. Test-only: nothing
// here ships. The colours come from lab/palettes.json, the same file lab/color_check.py verifies.

data class P(
    val bg: Color, val card: Color, val ink: Color, val muted: Color,
    val calm: Color, val onCalm: Color, val soon: Color, val onSoon: Color,
    val now: Color, val onNow: Color, val overdue: Color, val onOverdue: Color,
    val danger: Color, val onDanger: Color, val typeTx: Color, val typeRp: Color,
    val alarmBg: Color, val alarmInk: Color, val alarmAction: Color, val alarmOnAction: Color,
    val dangerFill: Color, val onDangerFill: Color, val dangerEdge: Color,
    val primary: Color, val onPrimary: Color, val primaryEdge: Color,
    val night: Boolean,
) {
    val track get() = muted.copy(alpha = 0.3f)
    val alarmMuted get() = alarmInk.copy(alpha = 0.8f)
}

fun palette(direction: String, theme: String): P {
    val j = JSONObject(File("../lab/palettes.json").readText()).getJSONObject(direction).getJSONObject(theme)
    fun c(k: String) = Color(android.graphics.Color.parseColor(j.getString(k)))
    return P(
        c("bg"), c("card"), c("ink"), c("muted"), c("calm"), c("on_calm"), c("soon"), c("on_soon"), c("now"), c("on_now"),
        c("overdue"), c("on_overdue"), c("danger"), c("on_danger"), c("type_tx"), c("type_rp"),
        c("alarm_bg"), c("alarm_ink"), c("alarm_action"), c("alarm_on_action"),
        c("danger_fill"), c("on_danger_fill"), c("danger_edge"), c("primary"), c("on_primary"), c("primary_edge"), theme == "night",
    )
}

enum class U { CALM, SOON, NOW, OVERDUE }
enum class Kind(@DrawableRes val icon: Int, val tag: String) { TX(R.drawable.ic_drop, "VS"), RP(R.drawable.ic_turn, "TURN") }

data class Bed(
    val bed: String, val kind: Kind, val time: String, val what: String, val state: U, val stateText: String,
    val done: Int, val total: Int, val progress: Float,
) {
    val number get() = bed.removePrefix("Bed ")
}

val SampleBeds = listOf(
    Bed("Bed 12", Kind.TX, "10:50", "30-min check", U.OVERDUE, "OVERDUE +15 min", 2, 7, 1f),
    Bed("Bed 14", Kind.TX, "11:05", "15-min check", U.NOW, "DUE NOW", 1, 7, 1f),
    Bed("Bed 7", Kind.TX, "11:15", "1-h check", U.SOON, "SOON · 10 min", 3, 7, 0.78f),
    Bed("Bed 21", Kind.TX, "11:52", "1-h check", U.CALM, "in 47 min", 3, 7, 0.22f),
    Bed("Bed 3", Kind.RP, "12:00", "Back → Left 30°", U.CALM, "in 55 min", 2, 6, 0.54f),
    Bed("Bed 9", Kind.RP, "12:00", "Left 30° → Back", U.CALM, "in 55 min", 2, 6, 0.54f),
)

data class Turn(val bed: String, val from: Position, val to: Position)

val SampleRound = listOf(
    Turn("Bed 3", LEFT_30, BACK), Turn("Bed 7", BACK, RIGHT_30), Turn("Bed 9", RIGHT_30, LEFT_30), Turn("Bed 23", BACK, RIGHT_30),
)

private fun P.of(s: U) = when (s) { U.CALM -> calm; U.SOON -> soon; U.NOW -> now; U.OVERDUE -> overdue }
private fun P.on(s: U) = when (s) { U.CALM -> onCalm; U.SOON -> onSoon; U.NOW -> onNow; U.OVERDUE -> onOverdue }
private fun P.type(k: Kind) = if (k == Kind.TX) typeTx else typeRp

@Composable
private fun T(
    text: String, color: Color, size: TextUnit, family: FontFamily, weight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier, features: String? = null, tracking: TextUnit = TextUnit.Unspecified, align: TextAlign? = null,
    line: TextUnit = TextUnit.Unspecified,
) = Text(
    text, modifier, color = color, textAlign = align, softWrap = true,
    style = TextStyle(fontFamily = family, fontSize = size, fontWeight = weight, fontFeatureSettings = features, letterSpacing = tracking, lineHeight = line),
)

@Composable
private fun Btn(
    text: String, bg: Color, fg: Color, family: FontFamily, height: Dp = 64.dp, size: TextUnit = 20.sp, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    border: Color? = null, modifier: Modifier = Modifier.fillMaxWidth(), weight: FontWeight = FontWeight.Black,
) = Box(
    modifier.heightIn(min = height).background(bg, shape).let { if (border != null) it.border(2.dp, border, shape) else it },
    contentAlignment = Alignment.Center,
) { T(text, fg, size, family, weight) }

@Composable
private fun ReactionBar(p: P, family: FontFamily, bold: FontWeight = FontWeight.Black, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp)) = Row(
    Modifier.fillMaxWidth().heightIn(min = 64.dp).background(p.dangerFill, shape).border(2.dp, p.dangerEdge, shape).padding(horizontal = 14.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Icon(painterResource(R.drawable.ic_warning), null, tint = p.onDangerFill, modifier = Modifier.size(28.dp))
    Spacer(Modifier.width(12.dp))
    Column {
        T("SUSPECTED REACTION?", p.onDangerFill, 16.sp, family, bold)
        T("STOP the transfusion & call", p.onDangerFill, 16.sp, family, FontWeight.Normal)
    }
}

/** The urgency shapes: hollow, half, solid + ring, diamond. They never rely on colour alone. */
private fun DrawScope.bead(s: U, c: Color, center: Offset, r: Float, card: Color, onC: Color) {
    when (s) {
        U.CALM -> {
            drawCircle(card, r, center)
            drawCircle(c, r, center, style = Stroke(r * 0.42f))
        }
        U.SOON -> {
            drawCircle(card, r, center)
            drawArc(c, 90f, 180f, true, Offset(center.x - r, center.y - r), Size(r * 2, r * 2))
            drawCircle(c, r, center, style = Stroke(r * 0.42f))
        }
        U.NOW -> {
            drawCircle(c, r * 0.72f, center)
            drawCircle(c, r * 1.25f, center, style = Stroke(r * 0.3f))
        }
        U.OVERDUE -> {
            val d = r * 1.35f
            val path = Path().apply {
                moveTo(center.x, center.y - d); lineTo(center.x + d, center.y); lineTo(center.x, center.y + d); lineTo(center.x - d, center.y); close()
            }
            drawPath(path, c)
            drawLine(onC, Offset(center.x, center.y - d * 0.5f), Offset(center.x, center.y + d * 0.12f), r * 0.3f, StrokeCap.Round)
            drawCircle(onC, r * 0.17f, Offset(center.x, center.y + d * 0.46f))
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Direction 1: WARD CARD. The paper card, alive. Its dots on a line become a live rail per bed.
// ------------------------------------------------------------------------------------------------
object WardCard {
    val display = FontFamily(Font(R.font.rh_display_bold, FontWeight.Bold), Font(R.font.rh_display_black, FontWeight.Black))
    val text = FontFamily(Font(R.font.rh_text_regular, FontWeight.Normal), Font(R.font.rh_text_bold, FontWeight.Bold))
    const val NUM = "tnum, zero"

    @Composable
    private fun Rail(b: Bed, p: P, modifier: Modifier = Modifier, height: Dp = 20.dp, ink: Color = p.ink, card: Color = p.card) {
        val s = b.state
        Canvas(modifier.fillMaxWidth().height(height)) {
            val y = size.height / 2
            val r = size.height * 0.27f
            val gap = (size.width - r * 4) / (b.total - 1)
            fun x(i: Int) = r * 2 + gap * i
            drawLine(p.track, Offset(x(0), y), Offset(x(b.total - 1), y), r * 0.4f)
            val nowX = x((b.done - 1).coerceAtLeast(0)) + gap * b.progress
            drawLine(ink, Offset(x(0), y), Offset(nowX, y), r * 0.55f)
            for (i in 0 until b.total) {
                when {
                    i < b.done -> drawCircle(ink, r * 0.8f, Offset(x(i), y))
                    i == b.done -> Unit
                    else -> {
                        drawCircle(card, r * 0.7f, Offset(x(i), y))
                        drawCircle(p.muted, r * 0.7f, Offset(x(i), y), style = Stroke(r * 0.28f))
                    }
                }
            }
            bead(s, p.of(s), Offset(x(b.done), y), r * 1.25f, card, p.on(s))
        }
    }

    @Composable
    private fun Pill(s: U, text: String, p: P) = Box(Modifier.background(p.of(s), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
        T(text, p.on(s), 16.sp, WardCard.text, FontWeight.Bold)
    }

    @Composable
    private fun Card(b: Bed, p: P) {
        val shape = RoundedCornerShape(14.dp)
        Row(
            Modifier.fillMaxWidth().background(p.card, shape).let { if (b.state == U.OVERDUE) it.border(3.dp, p.overdue, shape) else it },
        ) {
            Box(Modifier.width(7.dp).height(88.dp).background(p.type(b.kind), RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)))
            Column(Modifier.padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(b.kind.icon), null, tint = p.type(b.kind), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    T(b.bed, p.ink, 24.sp, display, FontWeight.Black, features = NUM)
                    Spacer(Modifier.width(8.dp))
                    T(b.kind.tag, p.type(b.kind), 16.sp, display, FontWeight.Black, Modifier.weight(1f))
                    T(b.time, p.ink, 26.sp, display, FontWeight.Black, features = NUM)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    T(b.what, p.muted, 16.sp, text, modifier = Modifier.weight(1f))
                    if (b.state == U.CALM) T(b.stateText, p.muted, 16.sp, text, FontWeight.Bold, features = NUM) else Pill(b.state, b.stateText, p)
                }
                Spacer(Modifier.height(2.dp))
                Rail(b, p)
            }
        }
    }

    @Composable
    fun Home(p: P) {
        Column(Modifier.fillMaxSize().background(p.bg)) {
            Column(Modifier.fillMaxWidth().background(if (p.night) p.card else p.ink).padding(horizontal = 16.dp, vertical = 10.dp)) {
                T("6 BEDS RUNNING · 11:05", if (p.night) p.soon else Color(0xFFF5A623), 16.sp, display, FontWeight.Black, tracking = 1.6.sp, features = NUM)
                T("1 overdue · 1 due now", if (p.night) p.ink else Color.White, 22.sp, display, FontWeight.Black)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SampleBeds.forEach { Card(it, p) }
            }
            Column(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReactionBar(p, display)
                Btn("Start", p.primary, p.onPrimary, display, size = 22.sp, border = p.primaryEdge)
            }
        }
    }

    @Composable
    private fun Blocks(p: P) = Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.fillMaxWidth().background(if (p.night) Color(0xFF16233D) else Color(0xFF1D4E9E), RoundedCornerShape(14.dp)).padding(12.dp)) {
            T("1 MEASURE", if (p.night) p.ink else Color.White, 18.sp, display, FontWeight.Black)
            T("T · HR · RR · BP · SpO₂", if (p.night) p.ink else Color.White, 16.sp, text)
        }
        Column(Modifier.fillMaxWidth().background(if (p.night) Color(0xFF2B2110) else Color(0xFFF5A623), RoundedCornerShape(14.dp)).padding(12.dp)) {
            T("2 DOCUMENT", if (p.night) p.soon else p.ink, 18.sp, display, FontWeight.Black)
            T("in Cerner, right away", if (p.night) p.ink else p.ink, 16.sp, text)
        }
    }

    @Composable
    fun Alarm(p: P) {
        val rail = Bed("Bed 14", Kind.TX, "11:05", "15-min check", U.NOW, "DUE NOW", 1, 7, 1f)
        Column(Modifier.fillMaxSize().background(p.alarmBg).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            T("BLOOD TRANSFUSION RUNNING", if (p.night) p.alarmAction else Color(0xFFF5A623), 16.sp, display, FontWeight.Black, tracking = 1.6.sp)
            T("15-min check", p.alarmInk, 34.sp, display, FontWeight.Black)
            Row(verticalAlignment = Alignment.Bottom) {
                T("Bed 14", p.alarmInk, 56.sp, display, FontWeight.Black, Modifier.weight(1f), NUM)
                T("11:05", p.alarmInk, 56.sp, display, FontWeight.Black, features = NUM)
            }
            Rail(rail, p.copy(now = p.alarmAction, onNow = p.alarmOnAction), height = 30.dp, ink = p.alarmInk, card = p.alarmBg)
            Blocks(p)
            Spacer(Modifier.weight(1f))
            Btn("Measured & documented ✓", p.alarmAction, p.alarmOnAction, display, height = 96.dp, size = 24.sp)
            Btn("Snooze 2 min", Color.Transparent, p.alarmInk, display, border = p.alarmMuted, size = 20.sp, weight = FontWeight.Bold)
            ReactionBar(p, display)
        }
    }

    @Composable
    fun Round(p: P) {
        Column(Modifier.fillMaxSize().background(p.alarmBg).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            T("REPOSITIONING ROUND", if (p.night) p.alarmAction else Color(0xFFF5A623), 16.sp, display, FontWeight.Black, tracking = 1.6.sp)
            T("Turning round", p.alarmInk, 34.sp, display, FontWeight.Black)
            Row(verticalAlignment = Alignment.Bottom) {
                T("10:00", p.alarmInk, 56.sp, display, FontWeight.Black, Modifier.weight(1f), NUM)
                T("4 beds", p.alarmInk, 34.sp, display, FontWeight.Black, features = NUM)
            }
            T("Check pressure points: occiput · ears · sacrum · heels", p.alarmMuted, 16.sp, text)
            SampleRound.forEach { TurnRow(it, p, p.alarmInk, p.alarmInk.copy(alpha = 0.09f), display, text, RoundedCornerShape(12.dp), NUM) }
            Spacer(Modifier.weight(1f))
            Btn("Start round", p.alarmAction, p.alarmOnAction, display, height = 96.dp, size = 24.sp)
            Btn("Snooze 2 min", Color.Transparent, p.alarmInk, display, border = p.alarmMuted, size = 20.sp, weight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TurnRow(
    t: Turn, p: P, ink: Color, bg: Color, head: FontFamily, body: FontFamily, shape: androidx.compose.ui.graphics.Shape, features: String?,
) = Row(
    Modifier.fillMaxWidth().heightIn(min = 56.dp).background(bg, shape).padding(horizontal = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    T(t.bed, ink, 24.sp, head, FontWeight.Black, Modifier.weight(1f), features)
    PositionIcon(t.from, ink, 26.dp)
    T("  →  ", ink, 18.sp, body, FontWeight.Bold)
    PositionIcon(t.to, ink, 30.dp)
    Spacer(Modifier.width(6.dp))
    T(t.to.label, ink, 18.sp, body, FontWeight.Bold, Modifier.width(84.dp))
}

// ------------------------------------------------------------------------------------------------
// Direction 2: HALO. Each bed is a ring that closes as its next check approaches. Quiet until the
// ring closes; an overdue bed breaks out of the circle into a stop-sign octagon.
// ------------------------------------------------------------------------------------------------
object Halo {
    val f = FontFamily(Font(R.font.figtree_regular, FontWeight.Normal), Font(R.font.figtree_bold, FontWeight.Bold), Font(R.font.figtree_black, FontWeight.Black))
    const val NUM = "tnum"

    @Composable
    private fun Ring(b: Bed, p: P, modifier: Modifier, ink: Color = p.ink, weight: Float? = null, content: @Composable BoxScope.() -> Unit) = Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val unit = min(size.width, size.height) / 96f
            val c = Offset(size.width / 2, size.height / 2)
            val stroke = (weight ?: when (b.state) { U.CALM -> 7f; U.SOON -> 11f; U.NOW -> 15f; U.OVERDUE -> 10f }) * unit
            val r = min(size.width, size.height) / 2 - stroke / 2 - unit * 2
            val color = p.of(b.state)
            if (b.state == U.OVERDUE) {
                val path = Path()
                for (i in 0 until 8) {
                    val a = Math.toRadians(22.5 + 45.0 * i)
                    val pt = Offset(c.x + r * 1.04f * cos(a).toFloat(), c.y + r * 1.04f * sin(a).toFloat())
                    if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                }
                path.close()
                drawPath(path, color)
            } else {
                drawCircle(p.track, r, c, style = Stroke(unit * 4))
                val tl = Offset(c.x - r, c.y - r)
                drawArc(color, -90f, 360f * b.progress, false, tl, Size(r * 2, r * 2), style = Stroke(stroke, cap = StrokeCap.Round))
                if (b.state == U.SOON) {
                    // Checkpoints ahead appear as ticks, closing the distance.
                    for (i in 0 until 8) {
                        val a = Math.toRadians(-90.0 + 45.0 * i)
                        drawCircle(ink, unit * 1.6f, Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat()))
                    }
                }
            }
        }
        content()
    }

    @Composable
    private fun Tile(b: Bed, p: P, modifier: Modifier) {
        val shape = RoundedCornerShape(24.dp)
        Column(
            modifier.background(p.card, shape).padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(b.kind.icon), null, tint = p.type(b.kind), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                T(b.kind.tag, p.type(b.kind), 16.sp, f, FontWeight.Black)
            }
            Ring(b, p, Modifier.size(88.dp)) {
                T(b.number, if (b.state == U.OVERDUE) p.onOverdue else p.ink, 30.sp, f, FontWeight.Black, features = NUM)
            }
            T(b.time, p.ink, 28.sp, f, FontWeight.Black, features = NUM)
            T(b.stateText, if (b.state == U.CALM) p.muted else if (b.state == U.SOON) p.ink else p.of(b.state), 16.sp, f, FontWeight.Bold, features = NUM, align = TextAlign.Center)
        }
    }

    @Composable
    fun Home(p: P) {
        Column(Modifier.fillMaxSize().background(p.bg)) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    T("1 overdue, 1 due now", p.ink, 24.sp, f, FontWeight.Black)
                    T("6 beds · 11:05", p.muted, 16.sp, f, FontWeight.Bold, features = NUM)
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SampleBeds.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { pair.forEach { Tile(it, p, Modifier.weight(1f)) } }
                }
            }
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReactionBar(p, f, shape = RoundedCornerShape(24.dp))
                Btn("Start", p.primary, p.onPrimary, f, size = 22.sp, shape = RoundedCornerShape(24.dp), border = p.primaryEdge)
            }
        }
    }

    @Composable
    fun Alarm(p: P) {
        val b = Bed("Bed 14", Kind.TX, "11:05", "15-min check", U.NOW, "DUE NOW", 1, 7, 1f)
        Column(Modifier.fillMaxSize().background(p.alarmBg).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_drop), null, tint = p.alarmInk, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
                T("Blood transfusion running", p.alarmMuted, 16.sp, f, FontWeight.Bold)
            }
            Ring(b, p.copy(now = p.alarmAction), Modifier.size(248.dp), ink = p.alarmInk, weight = 7f) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    T("Bed", p.alarmMuted, 20.sp, f, FontWeight.Bold)
                    T("14", p.alarmInk, 64.sp, f, FontWeight.Black, features = NUM, line = 68.sp)
                    T("11:05", p.alarmInk, 40.sp, f, FontWeight.Black, features = NUM)
                }
            }
            T("15-min check", p.alarmInk, 34.sp, f, FontWeight.Black)
            Column(Modifier.fillMaxWidth().background(p.alarmInk.copy(alpha = 0.09f), RoundedCornerShape(24.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                T("1  Measure  T · HR · RR · BP · SpO₂", p.alarmInk, 17.sp, f, FontWeight.Bold)
                T("2  Document  in Cerner, right away", p.alarmInk, 17.sp, f, FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            Btn("Measured & documented ✓", p.alarmAction, p.alarmOnAction, f, height = 88.dp, size = 24.sp, shape = RoundedCornerShape(28.dp))
            Btn("Snooze 2 min", Color.Transparent, p.alarmInk, f, border = p.alarmMuted, shape = RoundedCornerShape(28.dp), weight = FontWeight.Bold)
            ReactionBar(p, f, shape = RoundedCornerShape(24.dp))
        }
    }

    @Composable
    fun Round(p: P) {
        val b = Bed("", Kind.RP, "10:00", "", U.NOW, "", 1, 6, 1f)
        Column(Modifier.fillMaxSize().background(p.alarmBg).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_turn), null, tint = p.alarmInk, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
                T("Repositioning round", p.alarmMuted, 16.sp, f, FontWeight.Bold)
            }
            Ring(b, p.copy(now = p.alarmAction), Modifier.size(184.dp), ink = p.alarmInk, weight = 7f) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    T("10:00", p.alarmInk, 40.sp, f, FontWeight.Black, features = NUM)
                    T("4 beds", p.alarmMuted, 20.sp, f, FontWeight.Bold)
                }
            }
            T("Turning round", p.alarmInk, 34.sp, f, FontWeight.Black)
            T("Check pressure points: occiput · ears · sacrum · heels", p.alarmMuted, 16.sp, f, align = TextAlign.Center)
            SampleRound.forEach { TurnRow(it, p, p.alarmInk, p.alarmInk.copy(alpha = 0.09f), f, f, RoundedCornerShape(20.dp), NUM) }
            Spacer(Modifier.weight(1f))
            Btn("Start round", p.alarmAction, p.alarmOnAction, f, height = 88.dp, size = 24.sp, shape = RoundedCornerShape(28.dp))
            Btn("Snooze 2 min", Color.Transparent, p.alarmInk, f, border = p.alarmMuted, shape = RoundedCornerShape(28.dp), weight = FontWeight.Bold)
        }
    }

}

// ------------------------------------------------------------------------------------------------
// Direction 3: WARD BOARD. A departures board for the ward: the clock time is the headline, one
// line per bed, a ruler with a marker sliding to the next tick. Readable from across the room.
// ------------------------------------------------------------------------------------------------
object Board {
    val mono = FontFamily(Font(R.font.jetbrains_bold, FontWeight.Bold), Font(R.font.jetbrains_black, FontWeight.Black))
    val cond = FontFamily(Font(R.font.barlow_regular, FontWeight.Normal), Font(R.font.barlow_medium, FontWeight.Medium), Font(R.font.barlow_bold, FontWeight.Bold))

    @Composable
    private fun Ruler(b: Bed, p: P, ink: Color, modifier: Modifier = Modifier) = Canvas(modifier.fillMaxWidth().height(10.dp)) {
        val y = size.height
        drawLine(ink.copy(alpha = 0.45f), Offset(0f, y - 1), Offset(size.width, y - 1), 2f)
        val n = b.total * 4
        for (i in 0..n) {
            val x = size.width * i / n
            val major = i % 4 == 0
            drawLine(ink.copy(alpha = if (major) 0.9f else 0.35f), Offset(x, y), Offset(x, y - if (major) size.height else size.height * 0.45f), if (major) 4f else 2f)
        }
        val at = (b.done - 1 + b.progress).coerceAtLeast(0f) / (b.total - 1) * size.width
        val m = Path().apply { moveTo(at, y); lineTo(at - 12f, y - size.height); lineTo(at + 12f, y - size.height); close() }
        drawPath(m, ink)
    }

    /** Block tag: outline, hatched, solid, inverted triangle + word. */
    @Composable
    private fun Tag(s: U, text: String, p: P, modifier: Modifier = Modifier) {
        val base = modifier.heightIn(min = 26.dp)
        val shape = RectangleShape
        Row(
            when (s) {
                U.CALM -> base.border(2.dp, p.calm, shape)
                else -> base.background(p.of(s), shape)
            }.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (s == U.SOON) Canvas(Modifier.size(12.dp)) {
                val w = size.width
                for (i in 0..3) drawLine(p.onSoon, Offset(w * i / 3f, w), Offset(w, w * i / 3f), 3f)
                for (i in 1..3) drawLine(p.onSoon, Offset(0f, w * i / 3f), Offset(w * i / 3f, 0f), 3f)
            }
            if (s == U.NOW) Box(Modifier.size(12.dp).background(p.onNow))
            if (s == U.OVERDUE) Canvas(Modifier.size(14.dp)) {
                val path = Path().apply { moveTo(size.width / 2, 0f); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
                drawPath(path, p.onOverdue)
            }
            if (s != U.CALM) Spacer(Modifier.width(5.dp))
            T(text, if (s == U.CALM) p.ink else p.on(s), 16.sp, cond, FontWeight.Bold)
        }
    }

    @Composable
    private fun Line(b: Bed, p: P) {
        val alarm = b.state == U.OVERDUE
        val bg = if (alarm) p.dangerFill else p.card
        val ink = if (alarm) p.onDangerFill else p.ink
        val sub = if (alarm) p.onDangerFill else p.muted
        Column(Modifier.fillMaxWidth().background(bg).padding(horizontal = 12.dp, vertical = 7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                T(b.time, ink, 36.sp, mono, FontWeight.Black, Modifier.width(118.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        T(b.bed.uppercase(), ink, 26.sp, cond, FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        Icon(painterResource(b.kind.icon), null, tint = if (alarm) ink else p.type(b.kind), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(3.dp))
                        T(b.kind.tag, if (alarm) ink else p.type(b.kind), 16.sp, cond, FontWeight.Bold)
                    }
                    T(b.what, sub, 16.sp, cond, FontWeight.Medium)
                }
                if (alarm) {
                    Tag(U.OVERDUE, "+15 MIN", p.copy(overdue = ink, onOverdue = bg))
                } else {
                    Tag(b.state, b.stateText.removePrefix("SOON · ").uppercase(), p)
                }
            }
            Spacer(Modifier.height(4.dp))
            Ruler(b, p, ink)
        }
    }

    @Composable
    fun Home(p: P) {
        Column(Modifier.fillMaxSize().background(p.bg)) {
            Row(Modifier.fillMaxWidth().background(p.primary).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                T("11:05", p.onPrimary, 28.sp, mono, FontWeight.Black)
                Spacer(Modifier.width(12.dp))
                T("6 BEDS · 1 OVERDUE · 1 DUE NOW", p.onPrimary, 17.sp, cond, FontWeight.Bold, tracking = 0.5.sp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SampleBeds.forEach { Line(it, p) }
            }
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReactionBar(p, cond, FontWeight.Bold, shape = RectangleShape)
                Btn("START", p.primary, p.onPrimary, cond, size = 26.sp, shape = RectangleShape, weight = FontWeight.Bold, border = p.primaryEdge)
            }
        }
    }

    @Composable
    fun Alarm(p: P) {
        val ink = p.alarmInk
        Column(Modifier.fillMaxSize().background(p.alarmBg).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            T("BLOOD TRANSFUSION RUNNING", ink, 18.sp, cond, FontWeight.Bold, tracking = 1.sp)
            T("11:05", ink, 104.sp, mono, FontWeight.Black, line = 110.sp)
            T("BED 14", ink, 64.sp, cond, FontWeight.Bold, line = 66.sp)
            T("15-MIN CHECK", ink, 34.sp, cond, FontWeight.Bold)
            Ruler(Bed("", Kind.TX, "", "", U.NOW, "", 1, 7, 1f), p, ink)
            Column(Modifier.fillMaxWidth().border(2.dp, ink).padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                T("1 MEASURE  T · HR · RR · BP · SpO₂", ink, 20.sp, cond, FontWeight.Bold)
                T("2 DOCUMENT  in Cerner, right away", ink, 20.sp, cond, FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            Btn("MEASURED & DOCUMENTED ✓", p.alarmAction, p.alarmOnAction, cond, height = 96.dp, size = 26.sp, shape = RectangleShape, weight = FontWeight.Bold)
            Btn("SNOOZE 2 MIN", Color.Transparent, ink, cond, border = ink, size = 22.sp, shape = RectangleShape, weight = FontWeight.Bold)
            ReactionBar(p, cond, FontWeight.Bold, shape = RectangleShape)
        }
    }

    @Composable
    fun Round(p: P) {
        val ink = p.alarmInk
        Column(Modifier.fillMaxSize().background(p.alarmBg).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            T("REPOSITIONING ROUND", ink, 18.sp, cond, FontWeight.Bold, tracking = 1.sp)
            Row(verticalAlignment = Alignment.Bottom) {
                T("10:00", ink, 84.sp, mono, FontWeight.Black, Modifier.weight(1f), line = 90.sp)
                T("4 BEDS", ink, 34.sp, cond, FontWeight.Bold)
            }
            T("TURNING ROUND", ink, 34.sp, cond, FontWeight.Bold)
            T("Check pressure points: occiput · ears · sacrum · heels", ink, 17.sp, cond, FontWeight.Medium)
            SampleRound.forEach { TurnRow(it, p, ink, Color.Transparent, cond, cond, RectangleShape, null); Box(Modifier.fillMaxWidth().height(2.dp).background(ink)) }
            Spacer(Modifier.weight(1f))
            Btn("START ROUND", p.alarmAction, p.alarmOnAction, cond, height = 96.dp, size = 26.sp, shape = RectangleShape, weight = FontWeight.Bold)
            Btn("SNOOZE 2 MIN", Color.Transparent, ink, cond, border = ink, size = 22.sp, shape = RectangleShape, weight = FontWeight.Bold)
        }
    }
}
