import io.github.chr1sps.TextChange
import org.junit.jupiter.api.Test

class TextChangeTests {
//    private fun testChange(
//        from: PositionInResult,
//        to: PositionInResult,
//        text: String
//    ) {
//        val range = RangeInResult(from, to)
//        assertDoesNotThrow { TextChange(range, text) }
//    }
//
//    @Test
//    fun testEmptyText() {
//        testChange(
//            PositionInResult.InOriginal(0),
//            PositionInResult.InOriginal(1),
//            ""
//        )
//    }

    @Test
    fun textComparison() {
        val text = ""
        val first = TextChange(0, 1, text)
        val second = TextChange(2, 3, text)
        val third = TextChange(0, 1, text)
        assert(first < second)
        assert(first == third)
    }
}