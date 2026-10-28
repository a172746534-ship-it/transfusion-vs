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
                .padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
        ) {
            Box(
                Modifier.heightIn(min = 56.dp).clickable(onClick = onBack).padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) { Text(backLabel, color = Tvs.White, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
            Row(Modifier.padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_warning), contentDescription = null, tint = Tvs.White, modifier = Modifier.size(34.dp))
                Spacer(Modifier.width(12.dp))
                Text("Suspected transfusion reaction", color = Tvs.White, fontSize = 24.sp, lineHeight = 29.sp, fontWeight = FontWeight.Black)
            }
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // STOP & CALL IF YOU SEE
            Column(
                Modifier.fillMaxWidth().background(Tvs.Pink, RoundedCornerShape(16.dp)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CapsLabel("Stop & call if you see", Tvs.Red)
                WARNING_SIGNS.forEach { sign ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(9.dp).background(Tvs.Red, CircleShape))
                        Spacer(Modifier.width(14.dp))
                        Text(sign, color = Tvs.Navy, fontSize = 18.sp)
                    }
                }
            }

            // DO NOW — tap each step as it's done
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CapsLabel("Do now", Tvs.Navy)
                DO_NOW.forEachIndexed { i, (title, body) ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 64.dp)
                            .background(if (done[i]) Tvs.Panel else Tvs.White, RoundedCornerShape(12.dp))
                            .clickable { done[i] = !done[i] }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val circle = if (i == DO_NOW.lastIndex) Tvs.Navy else Tvs.Red
                        Box(
                            Modifier.size(36.dp).background(if (done[i]) Tvs.Green else circle, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(if (done[i]) "✓" else "${i + 1}", color = Tvs.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Black)) { append(title) }
                                append("\n")
                                append(body)
                            },
                            color = Tvs.Navy, fontSize = 18.sp, lineHeight = 24.sp,
                        )
                    }
                }
            }

            // Transfusion stopped → cancels that bed's remaining alarms
            if (beds.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (beds.size > 1) Text("Which bed was stopped?", color = Tvs.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    beds.forEach { bed ->
                        Button(
                            onClick = { confirm = bed.transfusionId },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Tvs.Red, contentColor = Tvs.White),
                        ) {
                            BedDot(bed.colorIndex, 14.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Transfusion stopped — ${bed.bedLabel}", fontSize = 18.sp, fontWeight = FontWeight.Black)
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
