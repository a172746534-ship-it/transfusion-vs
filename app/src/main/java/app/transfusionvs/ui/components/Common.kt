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
) {
    Column(
        Modifier.fillMaxWidth().background(background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
    ) {
        if (onBack != null) {
            Box(
                Modifier.heightIn(min = 56.dp).clickable(onClick = onBack).padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) { Text("‹  Back", color = Tvs.White, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
        }
        Row(Modifier.padding(start = 8.dp, top = if (onBack == null) 12.dp else 0.dp), verticalAlignment = Alignment.CenterVertically) {
            if (showDrop) {
                Icon(painterResource(R.drawable.ic_drop), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(36.dp))
                Spacer(Modifier.width(12.dp))
            }
            Column {
                CapsLabel(caps, capsColor)
                Text(title, color = Tvs.White, fontSize = 24.sp, lineHeight = 29.sp, fontWeight = FontWeight.Black)
                if (subtitle != null) Text(subtitle, color = Tvs.White.copy(alpha = 0.85f), fontSize = 15.sp)
            }
        }
    }
}

/** The card's red bar: "SUSPECTED REACTION? STOP the transfusion & call". Opens the reaction screen. */
@Composable
fun ReactionBanner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp)
            .background(Tvs.Red, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_warning), contentDescription = null, tint = Tvs.White, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text("SUSPECTED REACTION? STOP the transfusion & call", color = Tvs.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Text("Warning signs and what to do — tap here", color = Tvs.White, fontSize = 14.sp)
        }
    }
}

/** Big glove-friendly choice chip (min 56 dp). */
@Composable
fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier.heightIn(min = 56.dp)
            .background(if (selected) Tvs.Navy else Tvs.White, shape)
            .border(1.5.dp, if (selected) Tvs.Navy else Tvs.Outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (selected) Tvs.White else Tvs.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

/** White rounded panel. */
@Composable
fun Panel(modifier: Modifier = Modifier, background: Color = Tvs.White, content: @Composable () -> Unit) {
    Column(
        modifier.fillMaxWidth().background(background, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}

/** The card's "◷ Hourly = same minute as start" pill. */
@Composable
fun HourlyPill() {
    Row(
        Modifier.background(Tvs.Navy, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_clock), contentDescription = null, tint = Tvs.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Hourly = same minute as start", color = Tvs.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

/** Rounded status label: on time / late +N min / missed / ringing / upcoming. */
@Composable
fun StatusPill(status: StatusView) {
    val (bg, fg) = when (status.kind) {
        StatusKind.ON_TIME -> Tvs.Green to Tvs.White
        StatusKind.LATE, StatusKind.SNOOZED -> Tvs.Amber to Tvs.Navy
        StatusKind.MISSED, StatusKind.RINGING, StatusKind.DUE -> Tvs.Red to Tvs.White
        StatusKind.UPCOMING, StatusKind.CANCELLED -> Tvs.Panel to Tvs.Muted
    }
    StatusPill(status.text, bg, fg)
}

@Composable
fun StatusPill(text: String, bg: Color, fg: Color) {
    Box(Modifier.background(bg, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 5.dp)) {
        Text(text, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
