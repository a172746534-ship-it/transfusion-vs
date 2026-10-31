package app.transfusionvs.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BedMaskTest {

    @Test
    fun aLongNumberShowsItsLastFourDigits() {
        val mask = BedMask.masker(listOf("7654821", "Bed 12"))
        assertEquals("•••4821", mask("7654821"))
        assertEquals("Bed •••4821", BedMask.masker(listOf("Bed 7654821"))("Bed 7654821"))
    }

    @Test
    fun theSuffixGrowsOnlyAsFarAsItMustToStayUnique() {
        val mask = BedMask.masker(listOf("1234821", "9994821", "5550000"))
        assertEquals("•••34821", mask("1234821"))
        assertEquals("•••94821", mask("9994821"))
        assertEquals("•••0000", mask("5550000"))
    }

    @Test
    fun realBedLabelsAreLeftAlone() {
        val mask = BedMask.masker(listOf("Bed 12", "Isolation Bed 23", "ICU 3", "Bed 1204"))
        listOf("Bed 12", "Isolation Bed 23", "ICU 3", "Bed 1204").forEach { assertEquals(it, mask(it)) }
    }

    @Test
    fun itWorksInsideASentenceAndInArabicDigits() {
        val mask = BedMask.masker(listOf("7654821"))
        assertEquals("15-min check — •••4821", mask("15-min check — 7654821"))
        assertEquals("•••٤٨٢١", BedMask.masker(listOf("٧٦٥٤٨٢١"))("٧٦٥٤٨٢١"))
    }

    @Test
    fun theWholeNumberNeverShows() {
        // One number ends with the other: no suffix can be unique, and it still isn't shown in full.
        val mask = BedMask.masker(listOf("123456", "9123456"))
        assertFalse(mask("123456").contains("123456"))
        assertFalse(mask("9123456").contains("9123456"))
        assertFalse(BedMask.masker(listOf("12345"))("12345").contains("12345"))
    }
}
