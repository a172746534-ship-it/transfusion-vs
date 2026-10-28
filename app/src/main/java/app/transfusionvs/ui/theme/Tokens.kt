package app.transfusionvs.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// The design system's size tokens. Screens use ONLY these (CI fails on raw numbers outside
// ui/theme and ui/components). Values are the ones the screens already used, so nothing moved
// when they were introduced; change one here and every screen follows.

/** Font sizes, line heights and tracking. */
object TvsFont {
    /** The big "started at" clock on Start. */
    val Hero = 40.sp
    /** Alarm screen title. */
    val AlarmTitle = 34.sp
    /** "›" chevrons. */
    val Glyph = 28.sp
    /** Top-bar titles, bed names, stepper − / +. */
    val Title = 24.sp
    /** Timeline numbers, bed label field, bed rows on the alarm. */
    val Number = 22.sp
    /** Empty-state title, the large primary buttons. */
    val Heading = 20.sp
    /** Primary action buttons, the baseline checkbox. */
    val Button = 19.sp
    /** Key lines: warning signs, DO NOW steps, snooze, stepper values. */
    val Lead = 18.sp
    val BodyLarge = 17.sp
    val Body = 16.sp
    val BodySmall = 15.sp
    val Caption = 14.sp
    /** Caps labels, pills, chips. */
    val Label = 13.sp
    val Footnote = 12.sp
    /** Compact timeline chips and units. */
    val Tiny = 11.sp
    val Micro = 9.sp

    val AlarmTitleLine = 40.sp
    val TitleLine = 29.sp
    val StepLine = 24.sp
    /** Letter spacing of the small-caps labels ("TIME FROM START"). */
    val CapsTracking = 1.6.sp
}

/** Spacing scale (padding, gaps). s8 / s12 / s16 carry most layouts. */
object TvsSpace {
    val s2 = 2.dp
    val s3 = 3.dp
    val s4 = 4.dp
    val s5 = 5.dp
    val s6 = 6.dp
    val s7 = 7.dp
    val s8 = 8.dp
    val s9 = 9.dp
    val s10 = 10.dp
    val s12 = 12.dp
    val s14 = 14.dp
    val s16 = 16.dp
    val s18 = 18.dp
    val s20 = 20.dp
}

/** Fixed sizes: touch targets, dots, badges, icons, strokes. */
object TvsSize {
    /** Minimum touch target (gloves). */
    val Touch = 56.dp
    val TouchLarge = 64.dp
    val TouchXL = 72.dp
    /** The single huge "Measured & documented ✓" button. */
    val AckHuge = 96.dp

    val Bullet = 9.dp
    val DotSmall = 12.dp
    val DotCompact = 14.dp
    val Dot = 16.dp
    val DotLarge = 18.dp
    val TimelineDot = 20.dp
    val BadgeSmall = 26.dp
    val Badge = 28.dp
    val BadgeLarge = 36.dp
    val IconSmall = 16.dp
    val IconMedium = 30.dp
    val IconLarge = 34.dp
    val IconXL = 36.dp

    val TimeColumn = 64.dp
    val StepperValue = 92.dp
    val TimelineColumn = 58.dp
    val TimelineColumnCompact = 50.dp

    val Border = 1.5.dp
    val Stroke = 3.dp
}

/** Corner shapes. */
object TvsShape {
    /** Cards, panels, big buttons. */
    val Card = RoundedCornerShape(16.dp)
    /** Chips, small buttons, inner blocks. */
    val Control = RoundedCornerShape(12.dp)
    val Pill = RoundedCornerShape(50)
    /** Timeline time chips. */
    val Chip = RoundedCornerShape(9.dp)
    val ChipCompact = RoundedCornerShape(7.dp)
}
