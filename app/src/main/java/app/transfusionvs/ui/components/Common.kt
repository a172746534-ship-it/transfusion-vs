package app.transfusionvs.ui.components

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import app.transfusionvs.R
import app.transfusionvs.schedule.StatusKind
import app.transfusionvs.schedule.StatusView
import app.transfusionvs.ui.theme.TvsShape
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.Ward
import app.transfusionvs.ui.theme.WardType
import kotlinx.coroutines.delay

/**
 * While more content waits below, the region's bottom edge fades out. A hard cut under a pinned action
 * reads as the end of the list, and a bed could be missed that way.
 */
fun Modifier.moreBelow(active: Boolean): Modifier =
    if (!active) {
        this
    } else {
        graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithContent {
            drawContent()
            val h = TvsSize.Fade.toPx()
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = size.height - h, endY = size.height),
                topLeft = Offset(0f, size.height - h),
                size = Size(size.width, h),
                blendMode = BlendMode.DstOut,
            )
        }
    }

/** At large font sizes layouts stack instead of squeezing a word until it breaks. */
@Composable
fun largeFont(): Boolean = LocalDensity.current.fontScale >= 1.3f

/**
 * Rapid repeats do nothing: the second tap of a double tap, even if it lands on whatever the first
 * tap just opened underneath the finger.
 */
private object TapGate {
    private const val WINDOW_MS = 400L
    private var last = -WINDOW_MS

    fun pass(): Boolean {
        val t = SystemClock.uptimeMillis()
        if (t - last < WINDOW_MS) return false
        last = t
        return true
    }
}

/** A click that ignores rapid repeats. Every control in the app goes through this. */
fun Modifier.wardClick(enabled: Boolean = true, role: Role = Role.Button, onClick: () -> Unit): Modifier =
    clickable(enabled = enabled, role = role) { if (TapGate.pass()) onClick() }

@Composable
fun WardButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fill: Color,
    content: Color,
    edge: Color? = null,
    minHeight: Dp = TvsSize.TouchLarge,
    style: TextStyle = WardType.Button,
    shape: Shape = TvsShape.Card,
    enabled: Boolean = true,
    icon: Int? = null,
) {
    Row(
        modifier.heightIn(min = minHeight).widthIn(min = minHeight)
            .background(fill, shape)
            .let { if (edge != null) it.border(TvsSize.Edge, edge, shape) else it }
            .wardClick(enabled = enabled, onClick = onClick)
            .padding(horizontal = TvsSpace.s12, vertical = TvsSpace.s6),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(TvsSize.IconSmall))
            Spacer(Modifier.width(TvsSpace.s8))
        }
        Text(text, color = content, style = style, textAlign = TextAlign.Center)
    }
}

/**
 * A patient's MRN or any one-line label: shrinks to fit rather than breaking a number in two, down to
 * [min]. [style] is the size it wants.
 */
@Composable
fun LabelText(text: String, color: Color, style: TextStyle, modifier: Modifier = Modifier, min: TextUnit = WardType.Body.fontSize) {
    BasicText(
        text, modifier, style = style.copy(color = color), maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = min, maxFontSize = style.fontSize),
    )
}

/** The one main action of a screen, in the thumb zone. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    icon: Int? = null,
    minHeight: Dp = TvsSize.TouchLarge,
    style: TextStyle = WardType.Button,
) {
    val c = Ward.c
    WardButton(
        text, onClick, modifier,
        fill = if (enabled) c.primary else c.bg,
        content = if (enabled) c.onPrimary else c.muted,
        edge = if (enabled) c.primaryEdge else c.muted,
        enabled = enabled, icon = icon, minHeight = minHeight, style = style,
    )
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth(), minHeight: Dp = TvsSize.Touch) {
    val c = Ward.c
    WardButton(text, onClick, modifier, fill = c.card, content = c.ink, edge = c.muted, minHeight = minHeight, style = WardType.ButtonSmall)
}

/** Acknowledge in place: amber like the alarm's action, and it always says which bed. */
@Composable
fun AckButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth(), minHeight: Dp = TvsSize.Touch) {
    val c = Ward.c
    WardButton(text, onClick, modifier, fill = c.primary, content = c.onPrimary, minHeight = minHeight, style = WardType.ButtonSmall)
}

/** An action for one patient: who it is for, big enough to check before pressing, then what it records. */
@Composable
fun PatientButton(
    patient: String,
    action: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    fill: Color = Ward.c.primary,
    content: Color = Ward.c.onPrimary,
    key: TextStyle = WardType.CardBed,
    minHeight: Dp = TvsSize.TouchLarge,
    edge: Color? = null,
    icon: Int? = null,
) {
    Row(
        modifier.heightIn(min = minHeight).background(fill, TvsShape.Card)
            .let { if (edge != null) it.border(TvsSize.Edge, edge, TvsShape.Card) else it }
            .wardClick(onClick = onClick).padding(horizontal = TvsSpace.s12, vertical = TvsSpace.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(TvsSize.IconMedium))
            Spacer(Modifier.width(TvsSpace.s10))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LabelText(patient, content, key)
            Text(action, color = content, style = WardType.ButtonSmall, textAlign = TextAlign.Center)
        }
    }
}

/** Text fields in the theme's own ink, so they can be found in a dark room too. */
@Composable
fun wardFieldColors(): TextFieldColors {
    val c = Ward.c
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = c.ink, unfocusedTextColor = c.ink, cursorColor = c.ink,
        focusedBorderColor = c.ink, unfocusedBorderColor = c.muted,
        focusedLabelColor = c.ink, unfocusedLabelColor = c.muted,
    )
}

/** The band on top of every screen: navy like the card by day, a dark card by night. */
@Composable
fun WardHeader(
    caps: String,
    title: String,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    icon: Int? = null,
    background: Color = Ward.c.header,
    content: Color = Ward.c.onHeader,
    capsColor: Color = Ward.c.headerCaps,
    /** The title is the patient: a key number, on one line. */
    titleIsKey: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
) {
    val large = largeFont()
    Column(
        Modifier.fillMaxWidth().background(background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = TvsSpace.s4, end = TvsSpace.s12, bottom = TvsSpace.s6),
    ) {
        if (onBack != null) {
            Row(
                Modifier.padding(start = TvsSpace.s4, top = TvsSpace.s4).heightIn(min = TvsSize.Touch).widthIn(min = TvsSize.Stepper)
                    .background(Ward.c.headerTint, TvsShape.Control).wardClick(onClick = onBack).padding(horizontal = TvsSpace.s8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_back), contentDescription = null, tint = content, modifier = Modifier.size(TvsSize.IconMedium))
                Spacer(Modifier.width(TvsSpace.s4))
                Text("Back", color = content, style = WardType.Lead)
            }
        }
        // At large font sizes the clock gets a row of its own, so the words don't break around it.
        if (large && trailing != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) { trailing() }
        }
        Row(Modifier.padding(start = TvsSpace.s12, top = if (onBack == null) TvsSpace.s6 else TvsSpace.s2), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(painterResource(icon), contentDescription = null, tint = capsColor, modifier = Modifier.size(TvsSize.IconLarge))
                Spacer(Modifier.width(TvsSpace.s10))
            }
            Column(Modifier.weight(1f)) {
                if (caps.isNotBlank()) Text(caps.uppercase(), color = capsColor, style = WardType.Caps)
                if (titleIsKey) {
                    LabelText(title, content, WardType.Hero, min = WardType.Title.fontSize)
                } else {
                    Text(title, color = content, style = WardType.Title)
                }
                if (subtitle != null) Text(subtitle, color = content, style = WardType.Body)
            }
            if (!large) trailing?.invoke()
        }
    }
}

/**
 * The card's red bar: "SUSPECTED REACTION? STOP the transfusion & call". Opens the guide.
 * On transfusion screens only.
 */
@Composable
fun ReactionBar(onClick: () -> Unit, modifier: Modifier = Modifier, minHeight: Dp = TvsSize.TouchLarge, quiet: Boolean = false, short: Boolean = false) {
    val c = Ward.c
    // Always there, with its red kept for when it matters: a mark and an edge, never a slab on a routine screen.
    val ink = if (quiet) c.ink else c.alarmInk
    val mark = if (quiet) c.danger else c.alarmDanger
    Row(
        modifier.fillMaxWidth().heightIn(min = minHeight)
            .background(if (quiet) c.card else c.alarmBg, TvsShape.Card)
            .border(TvsSize.Edge, if (quiet) c.muted else c.alarmDanger, TvsShape.Card)
            .wardClick(onClick = onClick)
            .padding(horizontal = TvsSpace.s12, vertical = TvsSpace.s6),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_warning), contentDescription = null, tint = mark, modifier = Modifier.size(TvsSize.IconMedium))
        Spacer(Modifier.width(TvsSpace.s12))
        Column {
            Text("SUSPECTED REACTION?", color = ink, style = WardType.Tag)
            if (!short) Text("STOP the transfusion & call", color = ink, style = WardType.Body)
        }
    }
}

/** Big glove-friendly choice. Selected also carries a ✓, so it never rests on colour alone. */
@Composable
fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, minHeight: Dp = TvsSize.Touch) {
    val c = Ward.c
    val shape = TvsShape.Control
    Box(
        modifier.heightIn(min = minHeight).widthIn(min = minHeight)
            .background(if (selected) c.primary else c.card, shape)
            .border(TvsSize.Edge, if (selected) c.primaryEdge else c.muted, shape)
            .wardClick(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = TvsSpace.s8, vertical = TvsSpace.s6),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (selected) "✓ $text" else text, color = if (selected) c.onPrimary else c.ink, style = WardType.BodyBold, textAlign = TextAlign.Center)
    }
}

/** Chips that share a row evenly. At large font sizes they wrap, so no word gets squeezed apart. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChoiceRow(options: List<String>, selected: Int, onPick: (Int) -> Unit, minHeight: Dp = TvsSize.Touch) {
    if (largeFont()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8), verticalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
            options.forEachIndexed { i, text -> ChoiceChip(text, i == selected, { onPick(i) }, minHeight = minHeight) }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
            options.forEachIndexed { i, text -> ChoiceChip(text, i == selected, { onPick(i) }, Modifier.weight(1f), minHeight) }
        }
    }
}

/**
 * What a Start screen will do unless changed, next to the action, and the way to change it. Tapping
 * scrolls to the options.
 */
@Composable
fun ChangeRow(text: String, onClick: () -> Unit) {
    val c = Ward.c
    Row(
        Modifier.fillMaxWidth().heightIn(min = TvsSize.Touch).wardClick(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = c.ink, style = WardType.BodyBold, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(TvsSpace.s8))
        Box(
            Modifier.heightIn(min = TvsSize.Touch).widthIn(min = TvsSize.Stepper).border(TvsSize.Edge, c.muted, TvsShape.Control).padding(horizontal = TvsSpace.s8),
            contentAlignment = Alignment.Center,
        ) {
            Text("Change", color = c.ink, style = WardType.BodyBold)
        }
    }
}

/** Rounded panel on the page background. */
@Composable
fun Panel(modifier: Modifier = Modifier, background: Color = Ward.c.card, content: @Composable () -> Unit) {
    Column(
        modifier.fillMaxWidth().background(background, TvsShape.Card).padding(TvsSpace.s12),
        verticalArrangement = Arrangement.spacedBy(TvsSpace.s10),
    ) { content() }
}

/** Small-caps, letter-spaced label like "TIME FROM START". */
@Composable
fun CapsLabel(text: String, color: Color = Ward.c.ink, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier = modifier, color = color, style = WardType.Caps)
}

/** on time / late +N min / missed / ringing / upcoming, on a Detail row. */
@Composable
fun StatusPill(status: StatusView) {
    val c = Ward.c
    when (status.kind) {
        StatusKind.ON_TIME -> PlainStatus("✓ ${status.text}", c.ink)
        StatusKind.LATE, StatusKind.SNOOZED -> OutlinedStatus(status.text, c.soon)
        // Still actionable or gone for good: the two read differently, in shape as well as in the word.
        StatusKind.MISSED -> if ("overdue" in status.text) {
            FilledStatus("◆ ${status.text}", c.overdue, c.onOverdue, solid = true)
        } else {
            OutlinedStatus("✕ ${status.text}", c.overdue)
        }
        StatusKind.RINGING, StatusKind.DUE -> FilledStatus(status.text, c.now, c.onNow)
        StatusKind.UPCOMING, StatusKind.SKIPPED, StatusKind.CANCELLED -> PlainStatus(status.text, c.muted)
    }
}

@Composable
fun FilledStatus(text: String, bg: Color, fg: Color, solid: Boolean = false, icon: Int? = null) {
    // A bright pill would be the brightest thing in a dark room: a tint and an edge say the same.
    val tinted = Ward.c.isNight && !solid
    val ink = if (tinted) bg else fg
    Row(
        (if (tinted) Modifier.background(bg.copy(alpha = 0.18f), TvsShape.Pill).border(TvsSize.Edge, bg, TvsShape.Pill) else Modifier.background(bg, TvsShape.Pill))
            .padding(horizontal = TvsSpace.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, tint = ink, modifier = Modifier.size(TvsSize.IconTiny))
            Spacer(Modifier.width(TvsSpace.s4))
        }
        Text(text, color = ink, style = WardType.BodyBold)
    }
}

/** Coming, not here yet: an outline, quieter than the filled "due now". */
@Composable
fun OutlinedStatus(text: String, edge: Color) {
    Box(Modifier.border(TvsSize.Edge, edge, TvsShape.Pill).padding(horizontal = TvsSpace.s8)) {
        Text(text, color = Ward.c.ink, style = WardType.BodyBold)
    }
}

@Composable
fun PlainStatus(text: String, color: Color) {
    Text(text, color = color, style = WardType.BodyBold)
}

/** Current time, refreshed every [periodMs] — for countdowns and the rail. */
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
