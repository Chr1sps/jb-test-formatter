import io.github.chr1sps.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
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
    fun testChangesNoMerging() {
        testChange(
            "Some text",
            inOriginal(0) upTo inOriginal(0),
            " ",
            inChange(TextChange(0, 0, " "), 0) upTo inOriginal(0),
            " Some text"
        )
        testChange(
            "Some text",
            inOriginal(4) upTo inOriginal(5),
            "",
            inOriginal(5) upTo inOriginal(5),
            "Sometext"
        )
        testChange(
            "Some text",
            inOriginal(0) upTo inOriginal(0),
            "",
            inOriginal(0) upTo inOriginal(0),
            "Some text"
        )
        testChange(
            "Some text ",
            inOriginal(9) upTo inOriginal(10),
            "  ",
            inChange(TextChange(9, 10, "  "), 0) upTo inOriginal(10),
            "Some text  "
        )
    }

    @Test
    fun testChangesWithMerging() {
        testChange(
            "\n \na".addChanges(
                inOriginal(0) upTo inOriginal(1) replacedWith "\t",
                inOriginal(2) upTo inOriginal(3) replacedWith "\t",
            ),
            inOriginal(1) upTo inChange(TextChange(2, 3, "\t"), 0),
            "  ",
            TextChange(0, 3, "\t  \t").range(1, 3),
            "\t  \ta"
        )
    }


    @Test
    fun testChangesInsideChanges() {
        testChange(
            "\n\na".addChanges(
                inOriginal(0) upTo inOriginal(2) replacedWith "\t\t",
            ),
            TextChange(0, 2, "\t\t").range(1, 1),
            "  ",
            TextChange(0, 2, "\t  \t").range(1, 3),
            "\t  \ta"
        )
        testChange(
            "\n\na".addChanges(
                inOriginal(0) upTo inOriginal(2) replacedWith "\t\t",
            ),
            inChange(TextChange(0, 2, "\t\t"), 0) upTo inOriginal(2),
            "  ",
            inChange(TextChange(0, 2, "  "), 0) upTo inOriginal(2),
            "  a"
        )

    }

    @Test
    fun testChangesCoveringChanges() {
        testChange(
            "\n\n\na".addChanges(
                inOriginal(1) upTo inOriginal(2) replacedWith "\t",
            ),
            inOriginal(0) upTo inOriginal(3),
            "   ",
            inChange(TextChange(0, 3, "   "), 0) upTo inOriginal(3),
            "   a"
        )
        testChange(
            "\n\na".addChanges(
                inOriginal(0) upTo inOriginal(1) replacedWith "\t",
            ),
            inChange(TextChange(0, 1, "\t"), 0) upTo inOriginal(2),
            "  ",
            inChange(TextChange(0, 2, "  "), 0) upTo inOriginal(2),
            "  a"
        )
        testChange(
            "\n\na".addChanges(
                inOriginal(1) upTo inOriginal(2) replacedWith "",
            ),
            inOriginal(0) upTo inOriginal(2),
            "  ",
            inChange(TextChange(0, 2, "  "), 0) upTo inOriginal(2),
            "  a"
        )

    }

    private fun testChange(
        original: TextWithChanges,
        changeRange: RangeInResult,
        changeString: String,
        expectedRange: RangeInResult,
        expectedString: String
    ) {
        val newRange = original.addChange(changeRange, changeString)
        assertAll(
            {
                assertEquals(expectedRange, newRange)
            },
            {
                val result = original.applyChanges()
                assertEquals(
                    expectedString,
                    result,
                    "Expected: \"${expectedString.withEscapes()}\", actual: \"${result.withEscapes()}\""
                )
            }
        )
    }

    private fun testChange(
        original: String,
        changeRange: RangeInResult,
        changeString: String,
        expectedRange: RangeInResult,
        expectedString: String
    ) = testChange(
        TextWithChanges(original),
        changeRange,
        changeString,
        expectedRange,
        expectedString
    )


    @Test
    fun validatingWsChars() {
        assertThrows<TextException.NonWhitespace> {
            "Some text".addChanges(
                inOriginal(0) upTo inOriginal(0) replacedWith "text"
            )
        }
        assertThrows<TextException.NonWhitespace> {
            "Some text".addChanges(
                inOriginal(0) upTo inOriginal(1) replacedWith " "
            )
        }
    }

    @Test
    fun detectingIntersections() {
        assertThrows<TextException.IntersectingChanges> {
            val base = "\t\t\t".addChanges(
                inOriginal(0) upTo inOriginal(1) replacedWith "\n\n"
            )
            base.addChanges(
                inChange(
                    TextChange(0, 1, "\n\n"),
                    1
                ) upTo inOriginal(2) replacedWith " "
            )
        }
    }

}

private data class Change(
    val range: RangeInResult,
    val text: String
)

private fun TextWithChanges.addChanges(
    vararg changes: Change
): TextWithChanges {
    for (change in changes) {
        val (range, text) = change
        this.addChange(range, text)
    }
    return this
}

private fun String.addChanges(
    vararg changes: Change
) = TextWithChanges(this).addChanges(*changes)

private infix fun RangeInResult.replacedWith(text: String) = Change(this, text)

private fun TextChange.range(from: Int, to: Int) =
    RangeInResult(inChange(this, from), inChange(this, to))