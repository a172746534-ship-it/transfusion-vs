package app.transfusionvs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.transfusionvs.data.UndoAction
import app.transfusionvs.ui.theme.TvsShape
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.Ward
import app.transfusionvs.ui.theme.WardType
import kotlinx.coroutines.delay

/** How long the "Undo" bar stays up. */
const val UNDO_BAR_MS = 5_000L

/** Undo instead of "Are you sure?": shows the newest action for 5 seconds, then goes away. */
@Composable
fun UndoBar(action: UndoAction?, onUndo: (Long) -> Unit, modifier: Modifier = Modifier, clockNow: Long = System.currentTimeMillis()) {
    if (action == null) return
    var visible by remember(action.id) { mutableStateOf(clockNow - action.at < UNDO_BAR_MS) }
    LaunchedEffect(action.id) {
        delay(UNDO_BAR_MS - (clockNow - action.at))
        visible = false
    }
    if (!visible) return
    val c = Ward.c
    Row(
        modifier.fillMaxWidth().heightIn(min = TvsSize.TouchLarge)
            .background(c.card, TvsShape.Card)
            .border(TvsSize.Edge, c.ink, TvsShape.Card)
            .padding(start = TvsSpace.s12, end = TvsSpace.s4, top = TvsSpace.s4, bottom = TvsSpace.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(action.label, color = c.ink, style = WardType.BodyBold, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(TvsSpace.s8))
        WardButton(
            "Undo", { onUndo(action.id) },
            modifier = Modifier.width(TvsSize.Stepper),
            fill = c.primary, content = c.onPrimary, edge = c.primaryEdge,
            minHeight = TvsSize.Touch, style = WardType.ButtonSmall, shape = TvsShape.Control,
        )
    }
}
