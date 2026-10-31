package app.transfusionvs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.transfusionvs.ui.theme.TvsShape
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.WardType

/**
 * The card's "① MEASURE → ② DOCUMENT". Outlined with text inside: instructions must not look like
 * buttons, so only the action is a filled block on the screen.
 */
@Composable
fun MeasureDocumentBlocks(ink: Color, edge: Color, measure: Color, document: Color, onBadge: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().border(TvsSize.Edge, edge, TvsShape.Card).padding(TvsSpace.s12),
        verticalArrangement = Arrangement.spacedBy(TvsSpace.s10),
    ) {
        Step("1", "MEASURE", "T · HR · RR · BP · SpO₂", measure, ink, onBadge)
        Step("2", "DOCUMENT", "in Cerner, right away", document, ink, onBadge)
    }
}

@Composable
private fun Step(number: String, title: String, body: String, accent: Color, ink: Color, onBadge: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.sizeIn(minWidth = TvsSize.Badge, minHeight = TvsSize.Badge).background(accent, CircleShape), contentAlignment = Alignment.Center) {
            Text(number, color = onBadge, style = WardType.Lead)
        }
        Spacer(Modifier.width(TvsSpace.s12))
        Column {
            Text(title, color = accent, style = WardType.Lead)
            Text(body, color = ink, style = WardType.Body)
        }
    }
}
