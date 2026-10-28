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
import app.transfusionvs.ui.theme.Tvs

/** The card's "① MEASURE → ② DOCUMENT" blocks. */
@Composable
fun MeasureDocumentBlocks(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepBlock(
            number = "1", title = "MEASURE", body = "T · HR · RR · BP · SpO₂",
            background = Tvs.Blue, content = Tvs.White, badge = Tvs.White, badgeText = Tvs.Blue,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        Text("›", color = Tvs.Outline, fontSize = 28.sp, fontWeight = FontWeight.Bold)
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
            .background(background, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(26.dp).background(badge, CircleShape), contentAlignment = Alignment.Center) {
                Text(number, color = badgeText, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
            Spacer(Modifier.size(8.dp))
            Text(title, color = content, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(body, color = content, fontSize = 15.sp)
    }
}

/** Small-caps, letter-spaced label like "TIME FROM START". */
@Composable
fun CapsLabel(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier,
        color = color,
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.6.sp,
    )
}
