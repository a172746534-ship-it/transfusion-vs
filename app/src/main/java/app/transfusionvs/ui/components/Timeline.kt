package app.transfusionvs.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
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
 * chip. Scrolls sideways when a transfusion runs long; at large font sizes it wraps, so nothing is cut.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CardTimeline(points: List<TimelinePoint>, modifier: Modifier = Modifier) {
    if (largeFont()) {
        FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8), verticalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
            points.forEach { p -> TimelineColumn(p, line = false) }
        }
    } else {
        Row(modifier.horizontalScroll(rememberScrollState())) {
            points.forEachIndexed { i, p -> TimelineColumn(p, line = true, first = i == 0, last = i == points.lastIndex) }
        }
    }
}

@Composable
private fun TimelineColumn(p: TimelinePoint, line: Boolean, first: Boolean = false, last: Boolean = false) {
    val c = Ward.c
    Column(Modifier.widthIn(min = TvsSize.TimelineColumn), horizontalAlignment = Alignment.CenterHorizontally) {
        TimelineLabel(p)
        Box(
            Modifier.fillMaxWidth().height(TvsSize.TimelineDot + TvsSpace.s8).drawBehind {
                if (line) {
                    val y = size.height / 2
                    val startX = if (first) size.width / 2 else 0f
                    val endX = if (last) size.width / 2 else size.width
                    val stroke = TvsSize.EdgeStrong.toPx()
                    if (p.dot == DotStyle.OVERDUE) {
                        val cut = TvsSize.TimelineDot.toPx() / 2 + stroke
                        val middle = size.width / 2
                        if (middle - cut > startX) drawLine(c.ink, Offset(startX, y), Offset(middle - cut, y), strokeWidth = stroke)
                        if (endX > middle + cut) drawLine(c.ink, Offset(middle + cut, y), Offset(endX, y), strokeWidth = stroke)
                    } else if (endX > startX) {
                        drawLine(c.ink, Offset(startX, y), Offset(endX, y), strokeWidth = stroke)
                    }
                }
            },
            contentAlignment = Alignment.Center,
        ) { Dot(p.dot) }
        TimeChip(Labels.clock(p.time), p.filledChip)
    }
}

@Composable
private fun TimelineLabel(p: TimelinePoint) {
    val c = Ward.c
    Box(Modifier.heightIn(min = TvsSize.IconMedium), contentAlignment = Alignment.Center) {
        if (p.position != null) {
            // Repositioning: the position icon takes the number's place.
            PositionIcon(p.position, c.ink, TvsSize.IconMedium)
        } else {
            Text(
                if (p.unit != null) "${p.number}\u00A0${p.unit}" else p.number, color = c.ink, style = WardType.BodyBold,
                modifier = Modifier.padding(horizontal = TvsSpace.s4), softWrap = false,
            )
        }
    }
}

/** The same shapes as the rail: done, hollow, a ring for due, a diamond for overdue, a dash for skipped. */
@Composable
private fun Dot(style: DotStyle) {
    val c = Ward.c
    val base = Modifier.size(TvsSize.TimelineDot)
    when (style) {
        DotStyle.FILLED -> Box(base.background(c.ink, CircleShape))
        DotStyle.LATE -> Canvas(base.semantics { contentDescription = "Done late" }) {
            drawCircle(c.soon)
            drawCircle(c.ink, size.minDimension / 2 - TvsSize.EdgeStrong.toPx() / 2, style = Stroke(TvsSize.EdgeStrong.toPx()))
        }
        DotStyle.HOLLOW -> Canvas(base) {
            drawCircle(c.card)
            drawCircle(c.ink, size.minDimension / 2 - TvsSize.EdgeStrong.toPx() / 2, style = Stroke(TvsSize.EdgeStrong.toPx()))
        }
        DotStyle.DUE -> Canvas(base.semantics { contentDescription = "Due now" }) {
            drawCircle(c.card)
            drawCircle(c.now, size.minDimension / 4)
            drawCircle(c.now, size.minDimension / 2 - TvsSize.EdgeStrong.toPx() / 2, style = Stroke(TvsSize.EdgeStrong.toPx()))
        }
        DotStyle.OVERDUE -> Canvas(base.semantics { contentDescription = "Overdue" }) {
            val r = size.minDimension / 2
            val path = Path().apply {
                moveTo(r, 0f)
                lineTo(2 * r, r)
                lineTo(r, 2 * r)
                lineTo(0f, r)
                close()
            }
            drawPath(path, c.overdue)
        }
        DotStyle.SKIPPED -> Canvas(base.semantics { contentDescription = "Skipped" }) {
            drawCircle(c.card)
            drawCircle(c.muted, size.minDimension / 2 - TvsSize.EdgeStrong.toPx() / 2, style = Stroke(TvsSize.EdgeStrong.toPx()))
            val r = size.minDimension / 2
            drawLine(c.muted, Offset(r / 2, r), Offset(r * 3 / 2, r), TvsSize.EdgeStrong.toPx(), StrokeCap.Round)
        }
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
        modifier = modifier.padding(horizontal = TvsSpace.s4, vertical = TvsSpace.s2),
        style = WardType.BodyBold,
        color = if (filled) c.onPrimary else c.ink,
        softWrap = false,
    )
}
