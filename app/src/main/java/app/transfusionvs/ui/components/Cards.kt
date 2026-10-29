package app.transfusionvs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.transfusionvs.ui.theme.TimeFigures
import app.transfusionvs.ui.theme.Tvs
import app.transfusionvs.ui.theme.TvsFont
import app.transfusionvs.ui.theme.TvsShape
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace

// The Home building blocks, shared by transfusion and repositioning cards so both look identical.

/** Full-width tappable banner ("Alarm ringing — tap to open", "Setup check: …"). */
@Composable
fun Alert(text: String, color: Color, textColor: Color = Tvs.White, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = TvsSize.Touch).background(color, TvsShape.Card)
            .clickable(onClick = onClick).padding(TvsSpace.s16),
        contentAlignment = Alignment.CenterStart,
    ) { Text(text, color = textColor, fontSize = TvsFont.BodyLarge, fontWeight = FontWeight.Black) }
}

/** Coloured block with a message and one big action ("Overdue: …" + "Measured & documented ✓"). */
@Composable
fun ActionBlock(
    text: String,
    background: Color,
    textColor: Color,
    buttonText: String = "Measured & documented ✓",
    onAck: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().background(background, TvsShape.Control).padding(TvsSpace.s12),
        verticalArrangement = Arrangement.spacedBy(TvsSpace.s8),
    ) {
        Text(text, color = textColor, fontSize = TvsFont.Body, fontWeight = FontWeight.Black)
        Button(
            onClick = onAck,
            modifier = Modifier.fillMaxWidth().heightIn(min = TvsSize.Touch),
            shape = TvsShape.Control,
            colors = ButtonDefaults.buttonColors(containerColor = Tvs.White, contentColor = Tvs.Navy),
        ) { Text(buttonText, fontSize = TvsFont.BodyLarge, fontWeight = FontWeight.Black) }
    }
}

/**
 * The Home card shell: bed colour dot, bed label, subtitle and chevron; red border when overdue.
 * [content] follows below (timeline, next line, action blocks).
 */
@Composable
fun BedCard(
    colorIndex: Int,
    title: String,
    subtitle: String,
    overdue: Boolean,
    onOpen: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = TvsShape.Card
    Column(
        Modifier.fillMaxWidth()
            .background(Tvs.White, shape)
            .border(if (overdue) TvsSize.Stroke else 0.dp, if (overdue) Tvs.Red else Tvs.White, shape)
            .clickable(onClick = onOpen)
            .padding(TvsSpace.s16),
        verticalArrangement = Arrangement.spacedBy(TvsSpace.s12),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(TvsSize.DotLarge).background(Tvs.bedColor(colorIndex), CircleShape))
            Spacer(Modifier.width(TvsSpace.s10))
            Column(Modifier.weight(1f)) {
                Text(title, color = Tvs.Navy, fontSize = TvsFont.Title, fontWeight = FontWeight.Black)
                Text(subtitle, color = Tvs.Muted, fontSize = TvsFont.Caption)
            }
            Text("›", color = Tvs.Muted, fontSize = TvsFont.Glyph)
        }
        content()
    }
}

/** "Next: 1-h check at 11:20  in 15 min". */
@Composable
fun NextLine(what: String, countdown: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Next: ", color = Tvs.Muted, fontSize = TvsFont.Body)
        Text(what, color = Tvs.Navy, fontSize = TvsFont.Body, fontWeight = FontWeight.Bold, style = TimeFigures, modifier = Modifier.weight(1f, fill = false))
        Spacer(Modifier.width(TvsSpace.s8))
        Text(countdown, color = Tvs.Blue, fontSize = TvsFont.Body, fontWeight = FontWeight.Black, style = TimeFigures)
    }
}

/** Home section label ("TRANSFUSIONS", "REPOSITIONING"). */
@Composable
fun SectionLabel(text: String) {
    CapsLabel(text, Tvs.Navy, Modifier.padding(top = TvsSpace.s4))
}

/** Clean empty state for a Home section. */
@Composable
fun EmptyCard(title: String, body: String) {
    Column(
        Modifier.fillMaxWidth().background(Tvs.White, TvsShape.Card).padding(TvsSpace.s20),
        verticalArrangement = Arrangement.spacedBy(TvsSpace.s6),
    ) {
        Text(title, color = Tvs.Navy, fontSize = TvsFont.Heading, fontWeight = FontWeight.Black)
        Text(body, color = Tvs.Muted, fontSize = TvsFont.BodySmall)
    }
}
