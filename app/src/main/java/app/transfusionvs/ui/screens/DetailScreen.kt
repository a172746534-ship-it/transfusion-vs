package app.transfusionvs.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.transfusionvs.R
import app.transfusionvs.data.Checkpoint
import app.transfusionvs.data.CheckpointStatus
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.data.Transfusion
import app.transfusionvs.data.TransfusionStatus
import app.transfusionvs.data.TransfusionWithCheckpoints
import app.transfusionvs.schedule.BedViews
import app.transfusionvs.schedule.Labels
import app.transfusionvs.schedule.MINUTE_MS
import app.transfusionvs.schedule.TimelineBuilder
import app.transfusionvs.schedule.Timeliness
import app.transfusionvs.ui.MainViewModel
import app.transfusionvs.ui.PRIMARY_TAG
import app.transfusionvs.ui.components.PatientButton
import app.transfusionvs.ui.components.CapsLabel
import app.transfusionvs.ui.components.CardTimeline
import app.transfusionvs.ui.components.MeasureDocumentBlocks
import app.transfusionvs.ui.components.Panel
import app.transfusionvs.ui.components.PrimaryButton
import app.transfusionvs.ui.components.ReactionBar
import app.transfusionvs.ui.components.SecondaryButton
import app.transfusionvs.ui.components.StateText
import app.transfusionvs.ui.components.StatusPill
import app.transfusionvs.ui.components.UndoSlot
import app.transfusionvs.ui.components.WardButton
import app.transfusionvs.ui.components.WardHeader
import app.transfusionvs.ui.components.largeFont
import app.transfusionvs.ui.components.moreBelow
import app.transfusionvs.ui.components.rememberNow
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.Ward
import app.transfusionvs.ui.theme.WardType

/** The next check first, then the timeline in the card layout and every checkpoint with its status. */
@Composable
fun DetailScreen(
    vm: MainViewModel,
    id: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onReaction: () -> Unit,
) {
    val flow = remember(id) { vm.transfusion(id) }
    val item by flow.collectAsStateWithLifecycle(initialValue = null)
    val now by rememberNow(15_000)
    DetailContent(
        item = item,
        now = now,
        toleranceMin = vm.toleranceMin,
        onBack = onBack,
        onEdit = onEdit,
        onReaction = onReaction,
        onEnd = vm::end,
        onAcknowledge = vm::acknowledge,
        onForget = {
            vm.forgetStopped(it)
            onBack()
        },
    )
}

/** Stateless Detail (rendered by the screenshot tests with fake data). */
@Composable
fun DetailContent(
    item: TransfusionWithCheckpoints?,
    now: Long,
    toleranceMin: Int,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onReaction: () -> Unit,
    onEnd: (Long) -> Unit,
    onAcknowledge: (List<Long>) -> Unit,
    onForget: (Long) -> Unit = {},
) {
    val c = Ward.c
    val t = item?.transfusion
    val rows = item?.checkpoints.orEmpty()
    val stopped = t?.status == TransfusionStatus.STOPPED_REACTION

    Column(Modifier.fillMaxSize().background(c.bg)) {
        WardHeader(
            caps = when (t?.status) {
                TransfusionStatus.RUNNING -> "Blood transfusion running"
                TransfusionStatus.ENDED -> "Ended"
                TransfusionStatus.STOPPED_REACTION -> "Stopped — reaction"
                null -> ""
            },
            title = t?.bedLabel ?: "",
            titleIsKey = true,
            onBack = onBack,
            icon = R.drawable.ic_drop,
            background = if (stopped) c.dangerFill else c.header,
            content = if (stopped) c.onDangerFill else c.onHeader,
            capsColor = if (stopped) c.onDangerFill else c.headerCaps,
        )
        if (t == null || item == null) return@Column

        val endVs = rows.firstOrNull { it.type == CheckpointType.END && it.status == CheckpointStatus.PENDING }
        // The one action this bed needs now: pinned where the thumb is, not somewhere in the list.
        val dueRow = rows.filter { it.status == CheckpointStatus.RINGING || (it.status == CheckpointStatus.MISSED && it.ackAt == null) }.minByOrNull { it.scheduledAt }

        val scroll = rememberScrollState()
        Column(
            Modifier.weight(1f).moreBelow(scroll.canScrollForward).verticalScroll(scroll).padding(TvsSpace.s12),
            verticalArrangement = Arrangement.spacedBy(TvsSpace.s12),
        ) {
            if (stopped) {
                Panel {
                    Text(
                        "Stopped at ${t.endAt?.let { Labels.clock(it) } ?: ""}. Remaining alarms for this patient are cancelled.",
                        color = c.ink, style = WardType.Lead,
                    )
                    if (dueRow != null) Text("A check that was already overdue can still be documented below.", color = c.muted, style = WardType.Body)
                }
            }

            if (endVs != null) {
                Panel {
                    CapsLabel("End VS — ${t.bedLabel}")
                    Text("Transfusion ended at ${Labels.clock(endVs.scheduledAt)}.", color = c.ink, style = WardType.Body)
                    MeasureDocumentBlocks(c.ink, c.muted)
                    if (dueRow != null) Text("A check that was already overdue can still be documented below.", color = c.muted, style = WardType.Body)
                }
            }

            if (!stopped && endVs == null) {
                val view = BedViews.of(item, now)
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            CapsLabel("Next")
                            Text(view.what, color = c.ink, style = WardType.Lead)
                            StateText(view)
                        }
                        view.time?.let { Text(Labels.clock(it), color = c.ink, style = WardType.Hero, softWrap = false, textAlign = TextAlign.End) }
                    }
                }
            }

            Panel {
                CapsLabel("Time from start")
                Text(subtitle(t), color = c.ink, style = WardType.Body)
                Text("Baseline first · hourly = same minute as start", color = c.muted, style = WardType.Body)
                CardTimeline(TimelineBuilder.live(t, rows, now, toleranceMin))
            }

            Panel {
                CapsLabel("Checkpoints")
                visibleRows(t, rows, now).forEach { row ->
                    CheckpointRow(row, now, toleranceMin)
                    // Stopped or waiting on End VS: the late documentation stays possible, but quietly.
                    if ((stopped || endVs != null) && row.status == CheckpointStatus.MISSED && row.ackAt == null) {
                        PatientButton(Labels.title(row.type, row.offsetMin), Labels.ACK, { onAcknowledge(listOf(row.id)) })
                    }
                }
            }

            if (t.status == TransfusionStatus.RUNNING) {
                SecondaryButton("Edit start time or duration", onEdit)
            }
            // With a check due, that is the action in the thumb's place; ending waits down here.
            if (t.status == TransfusionStatus.RUNNING && dueRow != null) {
                Spacer(Modifier.height(TvsSpace.s4))
                SecondaryButton("End transfusion", { onEnd(t.id) })
            }
            if (stopped) {
                Spacer(Modifier.height(TvsSpace.s4))
                SecondaryButton("Remove from Home", { onForget(t.id) })
            }
        }

        Column(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = TvsSpace.s12).padding(top = TvsSpace.s8, bottom = TvsSpace.s8),
            // Apart on purpose: End is the costlier mistake for a thumb aiming at the reaction row.
            verticalArrangement = Arrangement.spacedBy(TvsSpace.s16),
        ) {
            UndoSlot()
            if (stopped) {
                WardButton(
                    "Open the reaction steps", onReaction, Modifier.fillMaxWidth().testTag(PRIMARY_TAG),
                    fill = c.danger, content = c.onDanger, icon = R.drawable.ic_warning,
                )
            } else {
                ReactionBar(onClick = onReaction, minHeight = TvsSize.TouchLarge, quiet = true)
                when {
                    endVs != null -> PatientButton("End VS", Labels.ACK, { onAcknowledge(listOf(endVs.id)) }, Modifier.fillMaxWidth().testTag(PRIMARY_TAG))
                    dueRow != null -> PatientButton(Labels.title(dueRow.type, dueRow.offsetMin), Labels.ACK, { onAcknowledge(listOf(dueRow.id)) }, Modifier.fillMaxWidth().testTag(PRIMARY_TAG))
                    // Quiet on purpose: it cancels the alarms, and nothing else on this screen is asking to be pressed.
                    t.status == TransfusionStatus.RUNNING -> SecondaryButton("End transfusion", { onEnd(t.id) }, Modifier.fillMaxWidth().testTag(PRIMARY_TAG), TvsSize.TouchLarge)
                }
            }
        }
    }
}

private fun subtitle(t: Transfusion): String = when (t.status) {
    TransfusionStatus.RUNNING ->
        "Started ${Labels.clock(t.startAt)} · planned ${Labels.duration(t.plannedDurationMin)} " +
            "(ends ${Labels.clock(t.startAt + t.plannedDurationMin * MINUTE_MS)})"
    TransfusionStatus.ENDED -> "Started ${Labels.clock(t.startAt)} · ended ${t.endAt?.let { Labels.clock(it) } ?: ""}"
    TransfusionStatus.STOPPED_REACTION -> "Started ${Labels.clock(t.startAt)} · stopped ${t.endAt?.let { Labels.clock(it) } ?: ""}"
}

/** Hide cancelled rows and far-future hourly checks past the planned end (they appear as they come up). */
private fun visibleRows(t: Transfusion, rows: List<Checkpoint>, now: Long): List<Checkpoint> =
    rows.filter { it.status != CheckpointStatus.CANCELLED }
        .filter { it.offsetMin <= t.plannedDurationMin || it.status != CheckpointStatus.PENDING || it.scheduledAt <= now + 60 * MINUTE_MS || it.type == CheckpointType.LIMIT_WARNING }
        .filter { !(it.type == CheckpointType.END && it.status == CheckpointStatus.PENDING) } // shown above, as the prompt
        .sortedBy { it.scheduledAt }

@Composable
private fun CheckpointRow(row: Checkpoint, now: Long, toleranceMin: Int) {
    val c = Ward.c
    val status = Timeliness.status(row, now, toleranceMin)
    val pill: @Composable () -> Unit = {
        if (row.type != CheckpointType.START) StatusPill(status) else Text("✓ logged", color = c.ink, style = WardType.BodyBold)
    }
    val words: @Composable () -> Unit = {
        Text(Labels.title(row.type, row.offsetMin), color = c.ink, style = WardType.BodyBold)
        if (row.type == CheckpointType.PLANNED_END || row.type == CheckpointType.LIMIT_WARNING) {
            Text(Labels.subtitle(row.type), color = c.muted, style = WardType.Body)
        }
        if (row.ackAt != null && row.type != CheckpointType.START) {
            Text("Documented ${Labels.clock(row.ackAt)}", color = c.muted, style = WardType.Body)
        }
    }
    if (largeFont()) {
        Column(verticalArrangement = Arrangement.spacedBy(TvsSpace.s2)) {
            Text(Labels.clock(row.scheduledAt), style = WardType.Time, color = c.ink, softWrap = false)
            words()
            pill()
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Labels.clock(row.scheduledAt), style = WardType.Time, color = c.ink, softWrap = false, modifier = Modifier.width(TvsSize.TimeColumn))
            Column(Modifier.weight(1f)) { words() }
            pill()
        }
    }
}
