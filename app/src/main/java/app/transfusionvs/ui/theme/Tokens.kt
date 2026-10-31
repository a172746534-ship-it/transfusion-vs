package app.transfusionvs.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// The design system's size tokens. Screens use ONLY these and WardType (CI fails on raw numbers
// outside ui/theme and ui/components).

/** Spacing scale (padding, gaps). */
object TvsSpace {
    val s2 = 2.dp
    val s4 = 4.dp
    val s6 = 6.dp
    val s8 = 8.dp
    val s10 = 10.dp
    val s12 = 12.dp
    val s16 = 16.dp
    val s20 = 20.dp
}

/** Fixed sizes: touch targets, icons, strokes. */
object TvsSize {
    /** Minimum touch target (gloves). */
    val Touch = 56.dp
    /** Minimum on alarm screens, and for primary actions everywhere. */
    val TouchLarge = 64.dp
    /** The one big action of an alarm screen. */
    val AckHuge = 96.dp

    /** The type strip along a Home card. */
    val TypeStrip = 7.dp
    /** Home card's rail. */
    val Rail = 16.dp
    val RailLarge = 30.dp

    val IconSmall = 20.dp
    val IconMedium = 28.dp
    val IconLarge = 34.dp
    val Badge = 34.dp

    val Stepper = 92.dp
    val TimelineColumn = 60.dp
    /** The clock time at the start of a Detail row. */
    val TimeColumn = 68.dp
    val TimelineDot = 20.dp

    val Edge = 2.dp
    val EdgeStrong = 3.dp
}

/** Corner shapes. */
object TvsShape {
    /** Bed cards, panels, big buttons. */
    val Card = RoundedCornerShape(14.dp)
    val CardStart = RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)
    /** Chips, small buttons, inner blocks. */
    val Control = RoundedCornerShape(12.dp)
    /** The urgency pill on a Home card. */
    val Pill = RoundedCornerShape(8.dp)
}
