package app.transfusionvs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.transfusionvs.schedule.DotStyle
import app.transfusionvs.schedule.Labels
import app.transfusionvs.schedule.TimelinePoint
import app.transfusionvs.ui.theme.TvsShape
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.Ward
import app.transfusionvs.ui.theme.WardType

/**
 * The card's "TIME FROM START" row: label (0 / 15 min / … / End) over a dot on the line, over a time
 * chip. Scrolls sideways when a transfusion runs long.
 */
@Composable
fun CardTimeline(points: List<TimelinePoint>, modifier: Modifier = Modifier) {
    val c = Ward.c
    Row(modifier.horizontalScroll(rememberScrollState())) {
        points.forEachIndexed { i, p ->
            Column(Modifier.widthIn(min = TvsSize.TimelineColumn), horizontalAlignment = Alignment.CenterHorizontally) {
                TimelineLabel(p)
                Box(
                    Modifier.fillMaxWidth().height(TvsSize.TimelineDot + TvsSpace.s8).drawBehind {
                        val y = size.height / 2
                        val startX = if (i == 0) size.width / 2 else 0f
                        val endX = if (i == points.lastIndex) size.width / 2 else size.width
                        if (endX > startX) drawLine(c.ink, Offset(startX, y), Offset(endX, y), strokeWidth = TvsSize.EdgeStrong.toPx())
                    },
                    contentAlignment = Alignment.Center,
                ) { Dot(p.dot) }
                TimeChip(Labels.clock(p.time), p.filledChip)
            }
        }
    }
}

@Composable
private fun TimelineLabel(p: TimelinePoint) {
    val c = Ward.c
    if (p.position != null) {
        // Repositioning: the position icon takes the number's place (same height).
        PositionIcon(p.position, c.ink, TvsSize.IconMedium)
        return
    }
    Text(if (p.unit != null) "${p.number} ${p.unit}" else p.number, color = c.ink, style = WardType.BodyBold)
}

@Composable
private fun Dot(style: DotStyle) {
    val c = Ward.c
    val base = Modifier.size(TvsSize.TimelineDot)
    when (style) {
        DotStyle.FILLED -> Box(base.background(c.ink, CircleShape))
        DotStyle.OVERDUE -> Box(base.semantics { contentDescription = "Overdue" }.background(c.overdue, TvsShape.Pill))
        DotStyle.HOLLOW -> Box(
            base.background(c.card, CircleShape).drawBehind {
                drawCircle(c.ink, size.minDimension / 2 - TvsSize.EdgeStrong.toPx() / 2, style = Stroke(TvsSize.EdgeStrong.toPx()))
            },
        )
    }
}

/** Filled for start and hourly; outlined for 15 min, 30 min and End, as on the card. */
@Composable
fun TimeChip(text: String, filled: Boolean) {
    val c = Ward.c
    val shape = TvsShape.Pill
    val modifier = if (filled) Modifier.background(c.primary, shape) else Modifier.background(c.card, shape).border(TvsSize.Edge, c.muted, shape)
    Text(
        text,
        modifier = modifier.padding(horizontal = TvsSpace.s6, vertical = TvsSpace.s2),
        style = WardType.BodyBold,
        color = if (filled) c.onPrimary else c.ink,
    )
}
