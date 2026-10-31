package app.transfusionvs.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.transfusionvs.R

/**
 * Red Hat Display and Red Hat Text (SIL Open Font License, bundled in res/font; licence in
 * assets/licenses). Static files, one per weight, so phones and screenshot tests draw the same.
 */
val RedHatDisplay = FontFamily(
    Font(R.font.rh_display_bold, FontWeight.Bold),
    Font(R.font.rh_display_black, FontWeight.Black),
)

val RedHatText = FontFamily(
    Font(R.font.rh_text_regular, FontWeight.Normal),
    Font(R.font.rh_text_bold, FontWeight.Bold),
)

/** Key numbers (times, beds): tabular figures and the card's slashed zero. */
private const val KEY_FIGURES = "tnum, zero"

/** Times at body size: tabular only, a slashed zero can read as an 8 this small. */
private const val SMALL_FIGURES = "tnum"

/** Every text style in the app. The smallest is 16 sp. */
object WardType {
    val AlarmKey = TextStyle(fontFamily = RedHatDisplay, fontWeight = FontWeight.Black, fontSize = 48.sp, lineHeight = 52.sp, fontFeatureSettings = KEY_FIGURES)
    val AlarmKeySmall = TextStyle(fontFamily = RedHatDisplay, fontWeight = FontWeight.Black, fontSize = 40.sp, lineHeight = 44.sp, fontFeatureSettings = KEY_FIGURES)
    val AlarmTitle = TextStyle(fontFamily = RedHatDisplay, fontWeight = FontWeight.Black, fontSize = 34.sp, lineHeight = 40.sp)
    val Hero = TextStyle(fontFamily = RedHatDisplay, fontWeight = FontWeight.Black, fontSize = 40.sp, lineHeight = 44.sp, fontFeatureSettings = KEY_FIGURES)
    val CardTime = TextStyle(fontFamily = RedHatDisplay, fontWeight = FontWeight.Black, fontSize = 26.sp, lineHeight = 30.sp, fontFeatureSettings = KEY_FIGURES)
    val CardBed = TextStyle(fontFamily = RedHatDisplay, fontWeight = FontWeight.Black, fontSize = 24.sp, lineHeight = 28.sp, fontFeatureSettings = KEY_FIGURES)
    val Title = TextStyle(fontFamily = RedHatDisplay, fontWeight = FontWeight.Black, fontSize = 24.sp, lineHeight = 29.sp)
    val Heading = TextStyle(fontFamily = RedHatDisplay, fontWeight = FontWeight.Black, fontSize = 22.sp, lineHeight = 27.sp)
    val ButtonLarge = TextStyle(fontFamily = RedHatDisplay, fontWeight = FontWeight.Black, fontSize = 24.sp, lineHeight = 29.sp)
    val Button = TextStyle(fontFamily = RedHatDisplay, fontWeight = FontWeight.Black, fontSize = 20.sp, lineHeight = 24.sp)
    val ButtonSmall = TextStyle(fontFamily = RedHatDisplay, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 22.sp)
    val Caps = TextStyle(fontFamily = RedHatDisplay, fontWeight = FontWeight.Black, fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = 1.6.sp)
    val Tag = TextStyle(fontFamily = RedHatDisplay, fontWeight = FontWeight.Black, fontSize = 16.sp, lineHeight = 20.sp)
    val Lead = TextStyle(fontFamily = RedHatText, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp)
    val LeadPlain = TextStyle(fontFamily = RedHatText, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 24.sp)
    val Time = TextStyle(fontFamily = RedHatDisplay, fontWeight = FontWeight.Black, fontSize = 18.sp, lineHeight = 24.sp, fontFeatureSettings = SMALL_FIGURES)
    val BodyBold = TextStyle(fontFamily = RedHatText, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp, fontFeatureSettings = SMALL_FIGURES)
    val Body = TextStyle(fontFamily = RedHatText, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp, fontFeatureSettings = SMALL_FIGURES)
}

private val base = Typography()

val TvsTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = RedHatDisplay),
    displayMedium = base.displayMedium.copy(fontFamily = RedHatDisplay),
    displaySmall = base.displaySmall.copy(fontFamily = RedHatDisplay),
    headlineLarge = base.headlineLarge.copy(fontFamily = RedHatDisplay),
    headlineMedium = base.headlineMedium.copy(fontFamily = RedHatDisplay),
    headlineSmall = base.headlineSmall.copy(fontFamily = RedHatDisplay),
    titleLarge = base.titleLarge.copy(fontFamily = RedHatDisplay),
    titleMedium = base.titleMedium.copy(fontFamily = RedHatText),
    titleSmall = base.titleSmall.copy(fontFamily = RedHatText),
    bodyLarge = base.bodyLarge.copy(fontFamily = RedHatText),
    bodyMedium = base.bodyMedium.copy(fontFamily = RedHatText, fontSize = 16.sp),
    bodySmall = base.bodySmall.copy(fontFamily = RedHatText, fontSize = 16.sp),
    labelLarge = base.labelLarge.copy(fontFamily = RedHatText, fontSize = 16.sp),
    labelMedium = base.labelMedium.copy(fontFamily = RedHatText, fontSize = 16.sp),
    labelSmall = base.labelSmall.copy(fontFamily = RedHatText, fontSize = 16.sp),
)
