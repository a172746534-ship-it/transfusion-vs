package app.transfusionvs.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import app.transfusionvs.R
import app.transfusionvs.ui.PRIMARY_TAG
import app.transfusionvs.ui.components.CapsLabel
import app.transfusionvs.ui.components.WardButton
import app.transfusionvs.ui.components.WardHeader
import app.transfusionvs.ui.components.largeFont
import app.transfusionvs.ui.components.wardClick
import app.transfusionvs.ui.theme.TvsShape
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.Ward
import app.transfusionvs.ui.theme.WardType

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
 * The back of the card, word for word. DO NOW comes first and each step can be ticked (kept only on
 * screen, no clinical details are stored). "Transfusion stopped" stays in view at the bottom and
 * cancels that bed's remaining alarms, with Undo instead of a dialog.
 */
@Composable
fun ReactionScreen(
    beds: List<BedChoice>,
    onStopped: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val c = Ward.c
    val done = remember { mutableStateListOf(false, false, false, false, false) }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        WardHeader(
            caps = "Blood transfusion",
            title = "Suspected transfusion reaction",
            onBack = onBack,
            icon = R.drawable.ic_warning,
            background = c.dangerFill,
            content = c.onDangerFill,
            capsColor = c.onDangerFill,
        )

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(TvsSpace.s12),
            verticalArrangement = Arrangement.spacedBy(TvsSpace.s12),
        ) {
            // DO NOW — tap each step as it's done
            Column(verticalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
                CapsLabel("Do now")
                DO_NOW.forEachIndexed { i, (title, body) ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = TvsSize.TouchLarge)
                            .background(c.card, TvsShape.Control)
                            .border(TvsSize.Edge, if (done[i]) c.ink else c.card, TvsShape.Control)
                            .wardClick(role = Role.Checkbox) { done[i] = !done[i] }
                            .padding(vertical = TvsSpace.s6, horizontal = TvsSpace.s10),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(TvsSize.Badge).background(if (done[i]) c.ink else c.danger, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(if (done[i]) "✓" else "${i + 1}", color = if (done[i]) c.card else c.onDanger, style = WardType.Lead)
                        }
                        Spacer(Modifier.width(TvsSpace.s12))
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(title) }
                                append("\n")
                                append(body)
                            },
                            color = c.ink, style = WardType.LeadPlain,
                        )
                    }
                }
            }

            // STOP & CALL IF YOU SEE
            Column(
                Modifier.fillMaxWidth().background(c.card, TvsShape.Card).padding(TvsSpace.s12),
                verticalArrangement = Arrangement.spacedBy(TvsSpace.s8),
            ) {
                CapsLabel("Stop & call if you see", c.danger)
                WARNING_SIGNS.forEach { sign ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(TvsSpace.s10).background(c.danger, CircleShape))
                        Spacer(Modifier.width(TvsSpace.s12))
                        Text(sign, color = c.ink, style = WardType.LeadPlain)
                    }
                }
            }
        }

        // Always in view: which bed was stopped. Many beds scroll inside the panel, in two columns.
        if (beds.isNotEmpty()) {
            Column(
                Modifier.fillMaxWidth().testTag(PRIMARY_TAG).windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = TvsSpace.s12).padding(top = TvsSpace.s8, bottom = TvsSpace.s8),
                verticalArrangement = Arrangement.spacedBy(TvsSpace.s8),
            ) {
                if (beds.size > 2) {
                    CapsLabel("Transfusion stopped for:")
                    val columns = if (largeFont()) 1 else 3
                    Column(
                        Modifier.heightIn(max = TvsSize.TouchLarge * 2 + TvsSpace.s8 + TvsSize.IconMedium).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(TvsSpace.s8),
                    ) {
                        beds.chunked(columns).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
                                row.forEach { bed -> StopButton(bed.bedLabel, { onStopped(bed.transfusionId) }, Modifier.weight(1f)) }
                                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                } else {
                    beds.forEach { bed ->
                        StopButton("Transfusion stopped — ${bed.bedLabel}", { onStopped(bed.transfusionId) }, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun StopButton(text: String, onClick: () -> Unit, modifier: Modifier) {
    val c = Ward.c
    WardButton(text, onClick, modifier, fill = c.danger, content = c.onDanger, style = WardType.ButtonSmall)
}
