package app.transfusionvs.schedule

/**
 * The app has no MRN field: bed only. An MRN can still be typed as a bed label by mistake, so outside
 * the app (notifications, the alarm over the lock screen, the widget) a long number in a label shows as
 * "•••4821": the shortest suffix, at least 4 digits, that is unique among the active labels.
 */
object BedMask {
    private const val SUFFIX_MIN = 4

    /** Any script's digits: a phone in Arabic may type ١٢٣٤٥. Real bed numbers are shorter than this. */
    private val MRN_LIKE = Regex("""\p{Nd}{5,}""")

    fun masker(active: Collection<String>): (String) -> String {
        val numbers = active.flatMap { label -> MRN_LIKE.findAll(label).map { it.value } }.distinct()
        return { text -> MRN_LIKE.replace(text) { m -> "•••" + suffix(m.value, numbers) } }
    }

    private fun suffix(number: String, all: List<String>): String {
        for (n in SUFFIX_MIN until number.length) {
            val s = number.takeLast(n)
            if (all.none { it != number && it.endsWith(s) }) return s
        }
        return number.takeLast(number.length - 1) // never the whole number
    }
}
