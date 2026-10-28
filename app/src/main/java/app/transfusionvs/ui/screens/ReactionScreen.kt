package app.transfusionvs.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.transfusionvs.R
import app.transfusionvs.ui.components.CapsLabel
import app.transfusionvs.ui.theme.Tvs
import app.transfusionvs.ui.theme.TvsShape
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.TvsFont

/** A running bed the nurse can mark as stopped. */
data class BedChoice(val transfusionId: Long, val bedLabel: String, val colorIndex: Int)

private val WARNING_SIGNS = listOf(
    "Fever or rigors",
    "Rash, itch, swelling",
    "Breathless, wheeze",
    "↓SpO₂, ↓BP or ↑HR",
    "Back or loin pain",
    "Dark or red urine",
    "New distress or irritability",
)

private val DO_NOW = listOf(
    "STOP the transfusion." to "Keep the IV line open.",
    "Call the doctor now." to "Stay with the child.",
    "Check vital signs + SpO₂." to "Check patient ID against the bag.",
    "Inform the blood bank." to "Keep the bag and giving set.",
    "Document in Cerner" to "once the child is safe.",
)

/**
 * The back of the card, exactly. DO NOW is a tappable checklist (kept only on screen —
 * no clinical details are stored). "Transfusion stopped" cancels that bed's remaining alarms.
 */
@Composable
fun ReactionScreen(
    beds: List<BedChoice>,
    onStopped: (Long) -> Unit,
    onBack: () -> Unit,
    backLabel: String = "‹  Back",
) {
    val done = remember { mutableStateListOf(false, false, false, false, false) }
    var confirm by rememberSaveable { mutableStateOf<Long?>(null) }

    Column(Modifier.fillMaxSize().background(Tvs.White)) {
        // Red header with the warning triangle
        Column(
            Modifier.fillMaxWidth().background(Tvs.Red)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = TvsSpace.s8, end = TvsSpace.s16, top = TvsSpace.s6, bottom = TvsSpace.s16),
        ) {
            Box(
                Modifier.heightIn(min = TvsSize.Touch).clickable(onClick = onBack).padding(horizontal = TvsSpace.s8),
                contentAlignment = Alignment.CenterStart,
            ) { Text(backLabel, color = Tvs.White, fontSize = TvsFont.BodyLarge, fontWeight = FontWeight.Bold) }
            Row(Modifier.padding(start = TvsSpace.s8), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_warning), contentDescription = null, tint = Tvs.White, modifier = Modifier.size(TvsSize.IconLarge))
                Spacer(Modifier.width(TvsSpace.s12))
                Text("Suspected transfusion reaction", color = Tvs.White, fontSize = TvsFont.Title, lineHeight = TvsFont.TitleLine, fontWeight = FontWeight.Black)
            }
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(TvsSpace.s16),
            verticalArrangement = Arrangement.spacedBy(TvsSpace.s18),
        ) {
            // STOP & CALL IF YOU SEE
            Column(
                Modifier.fillMaxWidth().background(Tvs.Pink, TvsShape.Card).padding(TvsSpace.s18),
                verticalArrangement = Arrangement.spacedBy(TvsSpace.s14),
            ) {
                CapsLabel("Stop & call if you see", Tvs.Red)
                WARNING_SIGNS.forEach { sign ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(TvsSize.Bullet).background(Tvs.Red, CircleShape))
                        Spacer(Modifier.width(TvsSpace.s14))
                        Text(sign, color = Tvs.Navy, fontSize = TvsFont.Lead)
                    }
                }
            }

            // DO NOW — tap each step as it's done
            Column(verticalArrangement = Arrangement.spacedBy(TvsSpace.s6)) {
                CapsLabel("Do now", Tvs.Navy)
                DO_NOW.forEachIndexed { i, (title, body) ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = TvsSize.TouchLarge)
                            .background(if (done[i]) Tvs.Panel else Tvs.White, TvsShape.Control)
                            .clickable { done[i] = !done[i] }
                            .padding(vertical = TvsSpace.s8, horizontal = TvsSpace.s4),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val circle = if (i == DO_NOW.lastIndex) Tvs.Navy else Tvs.Red
                        Box(
                            Modifier.size(TvsSize.BadgeLarge).background(if (done[i]) Tvs.Green else circle, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(if (done[i]) "✓" else "${i + 1}", color = Tvs.White, fontSize = TvsFont.BodyLarge, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(TvsSpace.s14))
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Black)) { append(title) }
                                append("\n")
                                append(body)
                            },
                            color = Tvs.Navy, fontSize = TvsFont.Lead, lineHeight = TvsFont.StepLine,
                        )
                    }
                }
            }

            // Transfusion stopped → cancels that bed's remaining alarms
            if (beds.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(TvsSpace.s10)) {
                    if (beds.size > 1) Text("Which bed was stopped?", color = Tvs.Navy, fontSize = TvsFont.Body, fontWeight = FontWeight.Bold)
                    beds.forEach { bed ->
                        Button(
                            onClick = { confirm = bed.transfusionId },
                            modifier = Modifier.fillMaxWidth().heightIn(min = TvsSize.TouchLarge),
                            shape = TvsShape.Card,
                            colors = ButtonDefaults.buttonColors(containerColor = Tvs.Red, contentColor = Tvs.White),
                        ) {
                            BedDot(bed.colorIndex, TvsSize.DotCompact)
                            Spacer(Modifier.width(TvsSpace.s10))
                            Text("Transfusion stopped — ${bed.bedLabel}", fontSize = TvsFont.Lead, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
            Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
    }

    val confirmBed = beds.firstOrNull { it.transfusionId == confirm }
    if (confirmBed != null) {
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Stop ${confirmBed.bedLabel}?") },
            text = { Text("Its remaining alarms are cancelled and it is marked \"Stopped — reaction\". No clinical details are stored.") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    onStopped(confirmBed.transfusionId)
                }) { Text("Transfusion stopped", color = Tvs.Red, fontWeight = FontWeight.Black) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
        )
    }
}
