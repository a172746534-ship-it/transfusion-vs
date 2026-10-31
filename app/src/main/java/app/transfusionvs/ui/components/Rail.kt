package app.transfusionvs.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import app.transfusionvs.schedule.RailState
import app.transfusionvs.schedule.Urgency
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.Ward

/**
 * The signature: the paper card's dots on a line, alive. Filled dots are done, the line fills toward
 * the next dot, and that dot changes shape with urgency, so it never rests on colour alone.
 */
@Composable
fun Rail(
    state: RailState,
    urgency: Urgency,
    modifier: Modifier = Modifier,
    height: Dp = TvsSize.Rail,
    ink: Color = Ward.c.ink,
    background: Color = Ward.c.card,
    /** What is still to come: the line, and the hollow dots on it. */
    track: Color = Ward.c.railLine,
    hollow: Color = Ward.c.muted,
    beadColor: Color = Ward.c.of(urgency),
    onBead: Color = Ward.c.on(urgency),
) {
    val c = Ward.c
    Canvas(modifier.fillMaxWidth().height(height)) {
        if (state.total < 2) return@Canvas
        val y = size.height / 2
        val r = size.height * 0.27f
        val gap = (size.width - r * 4) / (state.total - 1)
        fun x(i: Int) = r * 2 + gap * i
        drawLine(track, Offset(x(0), y), Offset(x(state.total - 1), y), r * 0.4f)
        val filledTo = if (state.done == 0) x(0) else x(state.done - 1) + gap * state.progress
        drawLine(ink, Offset(x(0), y), Offset(filledTo, y), r * 0.55f)
        for (i in 0 until state.total) {
            when {
                i < state.done -> drawCircle(ink, r * 0.8f, Offset(x(i), y))
                i == state.done -> Unit
                else -> {
                    drawCircle(background, r * 0.7f, Offset(x(i), y))
                    drawCircle(hollow, r * 0.7f, Offset(x(i), y), style = Stroke(r * 0.28f))
                }
            }
        }
        bead(urgency, beadColor, Offset(x(state.done), y), r * 1.25f, background, onBead)
    }
}

/** Hollow, half, solid with a ring, and a red diamond with "!". */
private fun DrawScope.bead(u: Urgency, color: Color, center: Offset, r: Float, background: Color, onColor: Color) {
    when (u) {
        Urgency.CALM -> {
            drawCircle(background, r, center)
            drawCircle(color, r, center, style = Stroke(r * 0.42f))
        }
        Urgency.SOON -> {
            drawCircle(background, r, center)
            drawArc(color, 90f, 180f, true, Offset(center.x - r, center.y - r), Size(r * 2, r * 2))
            drawCircle(color, r, center, style = Stroke(r * 0.42f))
        }
        Urgency.NOW -> {
            drawCircle(color, r * 0.72f, center)
            drawCircle(color, r * 1.25f, center, style = Stroke(r * 0.3f))
        }
        Urgency.OVERDUE -> {
            val d = r * 1.35f
            val path = Path().apply {
                moveTo(center.x, center.y - d)
                lineTo(center.x + d, center.y)
                lineTo(center.x, center.y + d)
                lineTo(center.x - d, center.y)
                close()
            }
            drawPath(path, color)
            drawLine(onColor, Offset(center.x, center.y - d * 0.5f), Offset(center.x, center.y + d * 0.12f), r * 0.3f, StrokeCap.Round)
            drawCircle(onColor, r * 0.17f, Offset(center.x, center.y + d * 0.46f))
        }
    }
}
