package app.transfusionvs.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import app.transfusionvs.R
import app.transfusionvs.ui.components.TopBar
import app.transfusionvs.ui.theme.Tvs
import app.transfusionvs.ui.theme.TvsFont
import app.transfusionvs.ui.theme.TvsShape
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace

/** Start → what are you starting? The two types differ only by icon and wording. */
@Composable
fun ChooserContent(onTransfusion: () -> Unit, onRepositioning: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Tvs.Panel)) {
        TopBar(caps = "Start", title = "What are you starting?", onBack = onBack)
        Column(Modifier.padding(TvsSpace.s16), verticalArrangement = Arrangement.spacedBy(TvsSpace.s16)) {
            Choice(
                icon = R.drawable.ic_drop,
                title = "Transfusion",
                body = "Vital signs at 15 min, 30 min, then hourly — from the start time.",
                onClick = onTransfusion,
            )
            Choice(
                icon = R.drawable.ic_turn,
                title = "Repositioning",
                body = "Turns on the fixed ward clock — every 2 h at 08:00, 10:00, 12:00 …",
                onClick = onRepositioning,
            )
        }
    }
}

@Composable
private fun Choice(icon: Int, title: String, body: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = TvsSize.AckHuge)
            .background(Tvs.White, TvsShape.Card)
            .clickable(onClick = onClick)
            .padding(TvsSpace.s16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(TvsSize.IconXL))
        Spacer(Modifier.width(TvsSpace.s16))
        Column(Modifier.weight(1f)) {
            Text(title, color = Tvs.Navy, fontSize = TvsFont.Title, fontWeight = FontWeight.Black)
            Text(body, color = Tvs.Muted, fontSize = TvsFont.Caption)
        }
        Text("›", color = Tvs.Muted, fontSize = TvsFont.Glyph)
    }
}
