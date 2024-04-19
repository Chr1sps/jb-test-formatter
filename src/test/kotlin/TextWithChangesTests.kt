import io.github.chr1sps.PositionInResult
import io.github.chr1sps.RangeInResult
import io.github.chr1sps.TextWithChanges
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TextWithChangesTests {
    @Test
    fun testNoChanges() {
        val original = "Some text"
        val withChanges = TextWithChanges(original)
        val result = withChanges.applyChanges()
        assertEquals(original, result)
    }

    @Test
    fun testAddSpace() {
        val original = "Some text"
        val withChanges = TextWithChanges(original)
        val from = PositionInResult.InOriginal(0)
        val to = PositionInResult.InOriginal(0)
        val range = RangeInResult(from, to)
        withChanges.addChange(range, " ")
        val result = withChanges.applyChanges()
        assertEquals(" Some text", result)
    }

    @Test
    fun testRemoveSpace() {
        val original = "Some text"
        val withChanges = TextWithChanges(original)
        val from = PositionInResult.InOriginal(4)
        val to = PositionInResult.InOriginal(5)
        val range = RangeInResult(from, to)
        withChanges.addChange(range, "")
        val result = withChanges.applyChanges()
        assertEquals("Sometext", result)
    }
}