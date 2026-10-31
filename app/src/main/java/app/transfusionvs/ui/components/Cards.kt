package app.transfusionvs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.transfusionvs.R
import app.transfusionvs.schedule.BedType
import app.transfusionvs.schedule.BedView
import app.transfusionvs.schedule.Labels
import app.transfusionvs.schedule.Urgency
import app.transfusionvs.ui.theme.TvsShape
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.Ward
import app.transfusionvs.ui.theme.WardType

// The Home building blocks, shared by both features so they are one product.

fun BedType.icon(): Int = if (this == BedType.TRANSFUSION) R.drawable.ic_drop else R.drawable.ic_turn
fun BedType.tag(): String = if (this == BedType.TRANSFUSION) "VS" else "TURN"
fun BedType.spoken(): String = if (this == BedType.TRANSFUSION) "Transfusion" else "Turning"

@Composable
fun BedType.color(): Color = if (this == BedType.TRANSFUSION) Ward.c.typeTx else Ward.c.typeRp

/** Full-width tappable banner ("Alarm ringing — tap to open", "Setup check: …"). */
@Composable
fun Alert(text: String, color: Color, textColor: Color, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = TvsSize.Touch).background(color, TvsShape.Card)
            .wardClick(onClick = onClick).padding(horizontal = TvsSpace.s12, vertical = TvsSpace.s8),
        contentAlignment = Alignment.CenterStart,
    ) { Text(text, color = textColor, style = WardType.Lead) }
}

@Composable
private fun TypeTag(type: BedType, tint: Color) {
    Icon(painterResource(type.icon()), contentDescription = null, tint = tint, modifier = Modifier.size(TvsSize.IconSmall))
    Spacer(Modifier.width(TvsSpace.s6))
    Text(type.tag(), color = tint, style = WardType.Tag, modifier = Modifier.semantics { contentDescription = type.spoken() })
}

/** The state, in words: quiet text when calm, a filled pill from "due soon" up. */
@Composable
private fun StateText(view: BedView) {
    val c = Ward.c
    when {
        view.stoppedForReaction -> Unit
        view.urgency == Urgency.CALM -> PlainStatus(view.stateText, c.muted)
        else -> FilledStatus(view.stateText, c.of(view.urgency), c.on(view.urgency))
    }
}

/**
 * One bed: type strip, icon and tag, bed, next clock time, what is due, how urgent, and the rail.
 * [actions] follow below (acknowledge, open round, undo last).
 */
@Composable
fun BedCard(view: BedView, onOpen: () -> Unit, actions: @Composable () -> Unit = {}) {
    val c = Ward.c
    val shape = TvsShape.Card
    val stopped = view.stoppedForReaction
    val ink = if (stopped) c.onDangerFill else c.ink
    val sub = if (stopped) c.onDangerFill else c.muted
    val background = if (stopped) c.dangerFill else c.card
    val tint = if (stopped) c.onDangerFill else view.type.color()
    val edge = when {
        stopped -> c.dangerEdge
        view.urgency == Urgency.OVERDUE -> c.overdue
        else -> null
    }
    val large = largeFont()
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min)
            .background(background, shape)
            .let { if (edge != null) it.border(TvsSize.EdgeStrong, edge, shape) else it }
            .wardClick(onClick = onOpen),
    ) {
        if (!stopped) Box(Modifier.width(TvsSize.TypeStrip).fillMaxHeight().background(view.type.color(), TvsShape.CardStart))
        Column(Modifier.weight(1f).padding(start = TvsSpace.s10, end = TvsSpace.s12, top = TvsSpace.s6, bottom = TvsSpace.s6)) {
            val time = view.time?.let { Labels.clock(it) }
            if (large) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TypeTag(view.type, tint)
                    Spacer(Modifier.weight(1f))
                    if (time != null) Text(time, color = ink, style = WardType.CardTime)
                }
                Text(view.bed, color = ink, style = WardType.CardBed)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(view.type.icon()), contentDescription = null, tint = tint, modifier = Modifier.size(TvsSize.IconSmall))
                    Spacer(Modifier.width(TvsSpace.s6))
                    Text(view.bed, color = ink, style = WardType.CardBed)
                    Spacer(Modifier.width(TvsSpace.s8))
                    Text(view.type.tag(), color = tint, style = WardType.Tag, modifier = Modifier.weight(1f).semantics { contentDescription = view.type.spoken() })
                    if (time != null) Text(time, color = ink, style = WardType.CardTime)
                }
            }
            if (stopped) {
                Text(view.stateText, color = ink, style = WardType.Button)
                Text(view.what, color = sub, style = WardType.Body)
            } else if (large) {
                Text(view.what, color = sub, style = WardType.Body)
                StateText(view)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(view.what, color = sub, style = WardType.Body, modifier = Modifier.weight(1f))
                    StateText(view)
                }
            }
            if (!stopped && !view.idle && view.rail.total > 1) {
                Spacer(Modifier.height(TvsSpace.s2))
                Rail(view.rail, view.urgency)
            }
            actions()
        }
    }
}

/** Clean empty state. */
@Composable
fun EmptyCard(title: String, body: String) {
    val c = Ward.c
    Column(
        Modifier.fillMaxWidth().background(c.card, TvsShape.Card).padding(TvsSpace.s16),
        verticalArrangement = Arrangement.spacedBy(TvsSpace.s6),
    ) {
        Text(title, color = c.ink, style = WardType.Heading)
        Text(body, color = c.muted, style = WardType.Body)
    }
}
