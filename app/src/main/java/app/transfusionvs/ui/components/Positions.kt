package app.transfusionvs.ui.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import app.transfusionvs.R
import app.transfusionvs.data.Position
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.WardType

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

/**
 * "◻ Left 30°  →  ◻ Back": the turn to make, with icons. When space is short it wraps between
 * whole parts, never inside a word.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PositionChange(
    from: Position,
    to: Position,
    color: Color,
    iconSize: Dp = TvsSize.IconMedium,
    style: TextStyle = WardType.Body,
) {
    FlowRow(itemVerticalAlignment = Alignment.CenterVertically) {
        PositionLabel(from, color, iconSize, style)
        Text("  →  ", color = color, style = style, softWrap = false)
        PositionLabel(to, color, iconSize, WardType.Lead)
    }
}

@Composable
private fun PositionLabel(p: Position, color: Color, iconSize: Dp, style: TextStyle) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PositionIcon(p, color, iconSize)
        Spacer(Modifier.width(TvsSpace.s6))
        Text(p.label, color = color, style = style, softWrap = false, maxLines = 1)
    }
}
