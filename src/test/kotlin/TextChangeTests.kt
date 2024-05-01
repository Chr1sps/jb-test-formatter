import io.github.chr1sps.TextChange
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class TextChangeTests {
    @Test
    fun testEmptyText() {
        assertDoesNotThrow { TextChange(0, 1, "") }
    }

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