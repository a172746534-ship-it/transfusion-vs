package app.transfusionvs.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import app.transfusionvs.R
import app.transfusionvs.data.Position
import app.transfusionvs.ui.theme.TvsFont
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace

/** One vector per position: a top-down figure on a bed (same grid and stroke as the other icons). */
fun Position.iconRes(): Int = when (this) {
    Position.LEFT_30 -> R.drawable.ic_pos_left30
    Position.BACK -> R.drawable.ic_pos_back
    Position.RIGHT_30 -> R.drawable.ic_pos_right30
    Position.PRONE -> R.drawable.ic_pos_prone
    Position.HOB_UP -> R.drawable.ic_pos_hob_up
}

@Composable
fun PositionIcon(position: Position, tint: Color, size: Dp = TvsSize.IconMedium) {
    Icon(painterResource(position.iconRes()), contentDescription = position.label, tint = tint, modifier = Modifier.size(size))
}

/** "◻ Left 30°  →  ◻ Back": the turn to make, with icons. */
@Composable
fun PositionChange(
    from: Position,
    to: Position,
    color: Color,
    iconSize: Dp = TvsSize.IconMedium,
    fontSize: TextUnit = TvsFont.Body,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PositionIcon(from, color, iconSize)
        Spacer(Modifier.width(TvsSpace.s6))
        Text(from.label, color = color, fontSize = fontSize)
        Text("  →  ", color = color, fontSize = fontSize, fontWeight = FontWeight.Black)
        PositionIcon(to, color, iconSize)
        Spacer(Modifier.width(TvsSpace.s6))
        Text(to.label, color = color, fontSize = fontSize, fontWeight = FontWeight.Black)
    }
}
