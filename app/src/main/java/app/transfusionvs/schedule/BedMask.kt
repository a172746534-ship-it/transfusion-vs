package app.transfusionvs.schedule

/**
 * Patients go by MRN. Outside the app (notifications, the alarm over the lock screen, the widget) it
 * shows as "•••4821": the shortest suffix, at least 4 digits, that is unique among the active labels.
 * The whole number is only inside the app, on an unlocked phone.
 */
object BedMask {
    private const val SUFFIX_MIN = 4

    /** Any script's digits. A bed label from an older version is shorter than this and stays as it is. */
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
