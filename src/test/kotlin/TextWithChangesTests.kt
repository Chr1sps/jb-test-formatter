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
}