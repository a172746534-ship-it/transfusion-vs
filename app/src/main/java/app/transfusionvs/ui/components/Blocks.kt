package app.transfusionvs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.text.TextStyle
import app.transfusionvs.ui.theme.Inter
import app.transfusionvs.ui.theme.Tvs
import app.transfusionvs.ui.theme.TvsShape
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.TvsFont

/** The card's "① MEASURE → ② DOCUMENT" blocks. */
@Composable
fun MeasureDocumentBlocks(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepBlock(
            number = "1", title = "MEASURE", body = "T · HR · RR · BP · SpO₂",
            background = Tvs.Blue, content = Tvs.White, badge = Tvs.White, badgeText = Tvs.Blue,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        Text("›", color = Tvs.Outline, fontSize = TvsFont.Glyph, fontWeight = FontWeight.Bold)
        StepBlock(
            number = "2", title = "DOCUMENT", body = "in Cerner, right away",
            background = Tvs.Amber, content = Tvs.Navy, badge = Tvs.Navy, badgeText = Tvs.White,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

@Composable
private fun StepBlock(
    number: String,
    title: String,
    body: String,
    background: Color,
    content: Color,
    badge: Color,
    badgeText: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(background, TvsShape.Card)
            .padding(TvsSpace.s14),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(TvsSize.BadgeSmall).background(badge, CircleShape), contentAlignment = Alignment.Center) {
                Text(number, color = badgeText, fontWeight = FontWeight.Black, fontSize = TvsFont.BodySmall)
            }
            Spacer(Modifier.size(TvsSpace.s8))
            // One line always: on narrow phones the word shrinks a little instead of breaking ("DOCUMEN-T").
            BasicText(
                title,
                style = TextStyle(color = content, fontWeight = FontWeight.Black, fontSize = TvsFont.Lead, fontFamily = Inter),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = TvsFont.Label, maxFontSize = TvsFont.Lead),
            )
        }
        Spacer(Modifier.height(TvsSpace.s6))
        Text(body, color = content, fontSize = TvsFont.BodySmall)
    }
}

/** Small-caps, letter-spaced label like "TIME FROM START". */
@Composable
fun CapsLabel(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier,
        color = color,
        fontSize = TvsFont.Label,
        fontWeight = FontWeight.Black,
        letterSpacing = TvsFont.CapsTracking,
    )
}
