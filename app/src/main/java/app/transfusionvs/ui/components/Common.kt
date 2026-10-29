package app.transfusionvs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.transfusionvs.R
import app.transfusionvs.schedule.StatusKind
import app.transfusionvs.schedule.StatusView
import app.transfusionvs.ui.theme.Tvs
import kotlinx.coroutines.delay
import app.transfusionvs.ui.theme.TvsShape
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.TvsFont

/** Navy top bar like the card's header: optional back, optional drop icon, amber caps label, title. */
@Composable
fun TopBar(
    caps: String,
    title: String,
    onBack: (() -> Unit)? = null,
    showDrop: Boolean = false,
    subtitle: String? = null,
    background: Color = Tvs.Navy,
    capsColor: Color = Tvs.Amber,
    // The header icon: the blood drop for transfusions, the rotation arrow for repositioning.
    icon: Int? = if (showDrop) R.drawable.ic_drop else null,
) {
    Column(
        Modifier.fillMaxWidth().background(background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = TvsSpace.s8, end = TvsSpace.s16, top = TvsSpace.s6, bottom = TvsSpace.s16),
    ) {
        if (onBack != null) {
            Box(
                Modifier.heightIn(min = TvsSize.Touch).clickable(onClick = onBack).padding(horizontal = TvsSpace.s8),
                contentAlignment = Alignment.CenterStart,
            ) { Text("‹  Back", color = Tvs.White, fontSize = TvsFont.BodyLarge, fontWeight = FontWeight.Bold) }
        }
        Row(Modifier.padding(start = TvsSpace.s8, top = if (onBack == null) TvsSpace.s12 else 0.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(painterResource(icon), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(TvsSize.IconXL))
                Spacer(Modifier.width(TvsSpace.s12))
            }
            Column {
                CapsLabel(caps, capsColor)
                Text(title, color = Tvs.White, fontSize = TvsFont.Title, lineHeight = TvsFont.TitleLine, fontWeight = FontWeight.Black)
                if (subtitle != null) Text(subtitle, color = Tvs.OnNavyMuted, fontSize = TvsFont.BodySmall)
            }
        }
    }
}

/** The card's red bar: "SUSPECTED REACTION? STOP the transfusion & call". Opens the reaction screen. */
@Composable
fun ReactionBanner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().heightIn(min = TvsSize.TouchLarge)
            .background(Tvs.Red, TvsShape.Card)
            .clickable(onClick = onClick)
            .padding(horizontal = TvsSpace.s16, vertical = TvsSpace.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_warning), contentDescription = null, tint = Tvs.White, modifier = Modifier.size(TvsSize.IconMedium))
        Spacer(Modifier.width(TvsSpace.s14))
        Column {
            Text("SUSPECTED REACTION? STOP the transfusion & call", color = Tvs.White, fontSize = TvsFont.BodySmall, fontWeight = FontWeight.Black)
            Text("Warning signs and what to do — tap here", color = Tvs.White, fontSize = TvsFont.Caption)
        }
    }
}

/** Big glove-friendly choice chip (min 56 dp). */
@Composable
fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = TvsShape.Control
    Box(
        modifier.heightIn(min = TvsSize.Touch)
            .background(if (selected) Tvs.Navy else Tvs.White, shape)
            .border(TvsSize.Border, if (selected) Tvs.Navy else Tvs.Outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = TvsSpace.s16),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (selected) Tvs.White else Tvs.Navy, fontSize = TvsFont.Body, fontWeight = FontWeight.Bold)
    }
}

/** White rounded panel. */
@Composable
fun Panel(modifier: Modifier = Modifier, background: Color = Tvs.White, content: @Composable () -> Unit) {
    Column(
        modifier.fillMaxWidth().background(background, TvsShape.Card).padding(TvsSpace.s16),
        verticalArrangement = Arrangement.spacedBy(TvsSpace.s12),
    ) { content() }
}

/** The card's "◷ Hourly = same minute as start" pill. */
@Composable
fun HourlyPill(text: String = "Hourly = same minute as start") {
    Row(
        Modifier.background(Tvs.Navy, TvsShape.Pill).padding(horizontal = TvsSpace.s12, vertical = TvsSpace.s6),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_clock), contentDescription = null, tint = Tvs.White, modifier = Modifier.size(TvsSize.IconSmall))
        Spacer(Modifier.width(TvsSpace.s6))
        Text(text, color = Tvs.White, fontSize = TvsFont.Label, fontWeight = FontWeight.Bold)
    }
}

/** Rounded status label: on time / late +N min / missed / ringing / upcoming. */
@Composable
fun StatusPill(status: StatusView) {
    val (bg, fg) = when (status.kind) {
        StatusKind.ON_TIME -> Tvs.Green to Tvs.White
        StatusKind.LATE, StatusKind.SNOOZED -> Tvs.Amber to Tvs.Navy
        StatusKind.MISSED, StatusKind.RINGING, StatusKind.DUE -> Tvs.Red to Tvs.White
        StatusKind.UPCOMING, StatusKind.SKIPPED, StatusKind.CANCELLED -> Tvs.Panel to Tvs.Muted
    }
    StatusPill(status.text, bg, fg)
}

@Composable
fun StatusPill(text: String, bg: Color, fg: Color) {
    Box(Modifier.background(bg, TvsShape.Pill).padding(horizontal = TvsSpace.s10, vertical = TvsSpace.s5)) {
        Text(text, color = fg, fontSize = TvsFont.Label, fontWeight = FontWeight.Bold)
    }
}

/** Current time, refreshed every [periodMs] — for countdowns. */
@Composable
fun rememberNow(periodMs: Long = 15_000): State<Long> {
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(periodMs) {
        while (true) {
            delay(periodMs)
            now.longValue = System.currentTimeMillis()
        }
    }
    return now
}
