package app.transfusionvs.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.transfusionvs.R
import app.transfusionvs.data.Position
import app.transfusionvs.data.TurnPlanStatus
import app.transfusionvs.data.TurnPlanWithSlots
import app.transfusionvs.data.TurnSlot
import app.transfusionvs.data.TurnSlotStatus
import app.transfusionvs.schedule.Labels
import app.transfusionvs.schedule.MINUTE_MS
import app.transfusionvs.schedule.TurnGrid
import app.transfusionvs.schedule.TurnStatus
import app.transfusionvs.schedule.TurnTimeline
import app.transfusionvs.ui.PRIMARY_TAG
import app.transfusionvs.ui.components.AckButton
import app.transfusionvs.ui.components.CapsLabel
import app.transfusionvs.ui.components.CardTimeline
import app.transfusionvs.ui.components.Panel
import app.transfusionvs.ui.components.PositionChange
import app.transfusionvs.ui.components.PositionIcon
import app.transfusionvs.ui.components.PrimaryButton
import app.transfusionvs.ui.components.SecondaryButton
import app.transfusionvs.ui.components.StatusPill
import app.transfusionvs.ui.components.UndoSlot
import app.transfusionvs.ui.components.WardHeader
import app.transfusionvs.ui.components.largeFont
import app.transfusionvs.ui.theme.TvsShape
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.Ward
import app.transfusionvs.ui.theme.WardType

/** The same skeleton as the transfusion Detail: timeline, the turns with their status, and Stop / Edit / Restart. */
@Composable
fun TurnDetailContent(
    item: TurnPlanWithSlots?,
    now: Long,
    windowMin: Int,
    missedTimeoutMin: Int = 10,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onStop: (Long) -> Unit,
    onRestart: (Long) -> Unit,
    onOpenRound: () -> Unit,
) {
    val c = Ward.c
    val p = item?.plan
    val slots = item?.slots.orEmpty()
    val rotation = p?.let { Position.parseList(it.rotation) }.orEmpty()

    Column(Modifier.fillMaxSize().background(c.bg)) {
        WardHeader(
            caps = when (p?.status) {
                TurnPlanStatus.RUNNING -> "Repositioning running"
                TurnPlanStatus.STOPPED -> "Stopped"
                null -> ""
            },
            title = p?.bedLabel ?: "",
            subtitle = p?.let {
                "Every ${Labels.duration(it.intervalMin)} · first turn ${Labels.clock(it.firstSlotAt)}" +
                    (it.stopAt?.let { s -> " · until ${Labels.clock(s)}" } ?: "")
            },
            onBack = onBack,
            icon = R.drawable.ic_turn,
        )
        if (p == null) return@Column

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(TvsSpace.s12),
            verticalArrangement = Arrangement.spacedBy(TvsSpace.s12),
        ) {
            // Handover summary: position now, next turn, and the longest gap without a turn.
            Panel {
                CapsLabel("Now")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PositionIcon(p.currentPosition, c.ink, TvsSize.IconLarge)
                    Spacer(Modifier.width(TvsSpace.s12))
                    Column(Modifier.weight(1f)) {
                        val since = slots.filter { it.status == TurnSlotStatus.ACKED }.mapNotNull { it.ackAt }.maxOrNull()
                        Text(
                            "Lying: ${p.currentPosition.label}" + (since?.let { " · since ${Labels.clock(it)}" } ?: ""),
                            color = c.ink, style = WardType.Lead,
                        )
                        val next = slots.filter { it.status == TurnSlotStatus.PENDING && it.slotAt > now }.minByOrNull { it.slotAt }
                        if (next != null && p.status == TurnPlanStatus.RUNNING) {
                            Text(
                                "Next: ${TurnGrid.nextPosition(p.currentPosition, rotation).label} at ${Labels.clock(next.slotAt)}",
                                color = c.muted, style = WardType.Body,
                            )
                        }
                    }
                }
                val turnTimes = slots.filter { it.status == TurnSlotStatus.ACKED }.mapNotNull { it.ackAt }
                val gap = TurnGrid.longestGapMs(turnTimes, maxOf(p.startAt, now - 24 * 60 * MINUTE_MS), now)
                Row(
                    Modifier.fillMaxWidth().background(c.bg, TvsShape.Control).padding(TvsSpace.s12),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Longest gap without a turn (24\u00A0h)", color = c.muted, style = WardType.BodyBold, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(TvsSpace.s8))
                    Text(TurnStatus.gap(gap), style = WardType.Time, color = c.ink)
                }
            }

            Panel {
                CapsLabel("Turn times")
                Text("Every ${Labels.duration(p.intervalMin)} · a late turn doesn't move them", color = c.muted, style = WardType.Body)
                CardTimeline(TurnTimeline.live(p, slots, now))
            }

            Panel {
                CapsLabel("Turns")
                slots.filter { it.status != TurnSlotStatus.CANCELLED && (it.status != TurnSlotStatus.PENDING || it.slotAt <= now + 4 * 60 * MINUTE_MS) }
                    .sortedBy { it.slotAt }
                    .forEach { s -> TurnRow(s, now, windowMin, missedTimeoutMin, onOpenRound) }
            }

            if (p.status == TurnPlanStatus.RUNNING) {
                SecondaryButton("Edit positions, interval or stop time", onEdit)
            }
        }

        Column(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = TvsSpace.s12).padding(bottom = TvsSpace.s8),
            verticalArrangement = Arrangement.spacedBy(TvsSpace.s8),
        ) {
            UndoSlot()
            if (p.status == TurnPlanStatus.RUNNING) {
                PrimaryButton("Stop plan", { onStop(p.id) }, Modifier.fillMaxWidth().testTag(PRIMARY_TAG))
            } else {
                PrimaryButton("Restart plan", { onRestart(p.id) }, Modifier.fillMaxWidth().testTag(PRIMARY_TAG))
            }
        }
    }
}

@Composable
private fun TurnRow(s: TurnSlot, now: Long, windowMin: Int, missedTimeoutMin: Int, onOpenRound: () -> Unit) {
    val c = Ward.c
    val status = TurnStatus.of(s, now, windowMin, missedTimeoutMin)
    Column(verticalArrangement = Arrangement.spacedBy(TvsSpace.s6)) {
        val words: @Composable () -> Unit = {
            val to = s.positionTo
            if (s.status == TurnSlotStatus.ACKED && to != null) {
                PositionChange(s.positionFrom, to, c.ink, TvsSize.IconSmall)
                s.ackAt?.let { Text("Turned ${Labels.clock(it)}", color = c.muted, style = WardType.Body) }
            } else {
                Text("Turn", color = c.ink, style = WardType.BodyBold)
            }
        }
        if (largeFont()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Labels.clock(s.slotAt), style = WardType.Time, color = c.ink, softWrap = false, modifier = Modifier.weight(1f))
                StatusPill(status)
            }
            words()
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Labels.clock(s.slotAt), style = WardType.Time, color = c.ink, softWrap = false, modifier = Modifier.width(TvsSize.TimeColumn))
                Column(Modifier.weight(1f)) { words() }
                StatusPill(status)
            }
        }
        if (s.status == TurnSlotStatus.RINGING) {
            AckButton("Open round", onOpenRound)
        }
    }
}
