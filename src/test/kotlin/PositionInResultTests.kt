import io.github.chr1sps.PositionInResult
import io.github.chr1sps.TextChange
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test

class PositionInResultTests {
    @Test
    fun testInOriginal() {
        assertDoesNotThrow { PositionInResult.InOriginal(0) }
        assertDoesNotThrow { PositionInResult.InOriginal(1) }
        assertThrows<IllegalArgumentException> { PositionInResult.InOriginal(-1) }
    }

    @Test
    fun testInChange() {
        assertDoesNotThrow {
            PositionInResult.InChange(
                TextChange(0, 2, "  "),
                0
            )
        }
        assertThrows<IllegalArgumentException> {
            PositionInResult.InChange(
                TextChange(0, 2, "  "),
                -1
            )
        }
        assertThrows<IllegalArgumentException> {
            PositionInResult.InChange(
                TextChange(0, 2, "  "),
                3
            )
        }
    }
}