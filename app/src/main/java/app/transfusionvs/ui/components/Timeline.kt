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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.transfusionvs.schedule.DotStyle
import app.transfusionvs.schedule.Labels
import app.transfusionvs.schedule.TimelinePoint
import app.transfusionvs.ui.theme.TimeFigures
import app.transfusionvs.ui.theme.Tvs
import app.transfusionvs.ui.theme.TvsShape
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.TvsFont

/**
 * The card's "TIME FROM START" row: label (0 / 15 min / … / End) over a dot on the line,
 * over a time chip. Scrolls sideways when a transfusion runs long.
 * [compact] is the mini version used on Home cards.
 */
@Composable
fun CardTimeline(points: List<TimelinePoint>, modifier: Modifier = Modifier, compact: Boolean = false) {
    val columnWidth = if (compact) TvsSize.TimelineColumnCompact else TvsSize.TimelineColumn
    val dotSize = if (compact) TvsSize.DotCompact else TvsSize.TimelineDot
    Row(modifier.horizontalScroll(rememberScrollState())) {
        points.forEachIndexed { i, p ->
            Column(Modifier.width(columnWidth), horizontalAlignment = Alignment.CenterHorizontally) {
                TimelineLabel(p, compact)
                Box(
                    Modifier.fillMaxWidth().height(dotSize + TvsSpace.s8).drawBehind {
                        val y = size.height / 2
                        val startX = if (i == 0) size.width / 2 else 0f
                        val endX = if (i == points.lastIndex) size.width / 2 else size.width
                        if (endX > startX) {
                            drawLine(Tvs.Navy, Offset(startX, y), Offset(endX, y), strokeWidth = TvsSize.Stroke.toPx())
                        }
                    },
                    contentAlignment = Alignment.Center,
                ) { Dot(p.dot, dotSize) }
                TimeChip(Labels.clock(p.time), p.filledChip, compact)
            }
        }
    }
}

@Composable
private fun TimelineLabel(p: TimelinePoint, compact: Boolean) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            p.number,
            style = TimeFigures,
            color = Tvs.Navy,
            fontSize = if (compact) TvsFont.Caption else TvsFont.Number,
            fontWeight = FontWeight.Black,
        )
        if (p.unit != null) {
            Text(
                " ${p.unit}",
                color = Tvs.Navy,
                fontSize = if (compact) TvsFont.Micro else TvsFont.Tiny,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = if (compact) TvsSpace.s2 else TvsSpace.s3),
            )
        }
    }
}

@Composable
private fun Dot(style: DotStyle, size: androidx.compose.ui.unit.Dp) {
    val base = Modifier.size(size)
    when (style) {
        DotStyle.FILLED -> Box(base.background(Tvs.Navy, CircleShape))
        DotStyle.OVERDUE -> Box(base.background(Tvs.Red, CircleShape))
        DotStyle.HOLLOW -> Box(base.background(Tvs.White, CircleShape).border(TvsSize.Stroke, Tvs.Navy, CircleShape))
    }
}

/** Navy-filled for start and hourly; outlined for 15 min, 30 min and End. */
@Composable
fun TimeChip(text: String, filled: Boolean, compact: Boolean = false) {
    val shape = if (compact) TvsShape.ChipCompact else TvsShape.Chip
    val modifier = if (filled) {
        Modifier.background(Tvs.Navy, shape)
    } else {
        Modifier.background(Tvs.White, shape).border(TvsSize.Border, Tvs.Outline, shape)
    }
    Text(
        text,
        modifier = modifier.padding(horizontal = if (compact) TvsSpace.s4 else TvsSpace.s5, vertical = if (compact) TvsSpace.s3 else TvsSpace.s5),
        style = TimeFigures,
        color = if (filled) Tvs.White else Tvs.Navy,
        fontSize = if (compact) TvsFont.Tiny else TvsFont.Label,
        fontWeight = FontWeight.Black,
    )
}
