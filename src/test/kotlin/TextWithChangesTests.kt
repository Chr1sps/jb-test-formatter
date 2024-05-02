import io.github.chr1sps.*
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals


class TextWithChangesTests {

    data class TestData(
        val original: TextWithChanges,
        val changeRange: RangeInResult,
        val changeText: String,
        val expectedRange: RangeInResult,
        val expectedString: String
    )

    data class ErrorTestData(
        val original: TextWithChanges,
        val changeRange: RangeInResult,
        val changeText: String,
    )

    companion object {
        @JvmStatic
        fun dataNoMerging() = listOf(
            TestData(
                "Some text".withChanges(),
                inOriginal(0) upTo inOriginal(0),
                " ",
                inChange(TextChange(0, 0, " "), 0) upTo inOriginal(0),
                " Some text"
            ),
            TestData(
                "Some text".withChanges(),
                inOriginal(4) upTo inOriginal(5),
                "",
                inOriginal(5) upTo inOriginal(5),
                "Sometext"
            ),
            TestData(
                "Some text".withChanges(),
                inOriginal(0) upTo inOriginal(0),
                "",
                inOriginal(0) upTo inOriginal(0),
                "Some text"
            ),
            TestData(
                "Some text ".withChanges(),
                inOriginal(9) upTo inOriginal(10),
                "  ",
                inChange(TextChange(9, 10, "  "), 0) upTo inOriginal(10),
                "Some text  "
            )
        ).stream()

        @JvmStatic
        fun dataWithMerging() = listOf(
            TestData(
                "\n \na".addChanges(
                    inOriginal(0) upTo inOriginal(1) replacedWith "\t",
                ),
                inOriginal(1) upTo inOriginal(2),
                "  ",
                inChange(TextChange(0, 2, "\t  "), 1) upTo inOriginal(2),
                "\t  \na"
            ),
            TestData(
                "\n \na".addChanges(
                    inOriginal(2) upTo inOriginal(3) replacedWith "\t",
                ),
                inOriginal(1) upTo inChange(TextChange(2, 3, "\t"), 0),
                "  ",
                TextChange(1, 3, "  \t").range(0, 2),
                "\n  \ta"
            ),
            TestData(
                "\n \na".addChanges(
                    inOriginal(0) upTo inOriginal(1) replacedWith "\t",
                    inOriginal(2) upTo inOriginal(3) replacedWith "\t",
                ),
                inOriginal(1) upTo inChange(TextChange(2, 3, "\t"), 0),
                "  ",
                TextChange(0, 3, "\t  \t").range(1, 3),
                "\t  \ta"
            ),
        ).stream()

        @JvmStatic
        fun dataInside() = listOf(
            TestData(
                "\n\na".addChanges(
                    inOriginal(0) upTo inOriginal(2) replacedWith "\t\t",
                ),
                TextChange(0, 2, "\t\t").range(1, 1),
                "  ",
                TextChange(0, 2, "\t  \t").range(1, 3),
                "\t  \ta"
            ),
            TestData(
                "\n\na".addChanges(
                    inOriginal(0) upTo inOriginal(2) replacedWith "\t\t",
                ),
                inChange(TextChange(0, 2, "\t\t"), 0) upTo inOriginal(2),
                "  ",
                inChange(TextChange(0, 2, "  "), 0) upTo inOriginal(2),
                "  a"
            )
        ).stream()

        @JvmStatic
        fun dataCovering() = listOf(
            TestData(
                "\n\n\na".addChanges(
                    inOriginal(1) upTo inOriginal(2) replacedWith "\t",
                ),
                inOriginal(0) upTo inOriginal(3),
                "   ",
                inChange(TextChange(0, 3, "   "), 0) upTo inOriginal(3),
                "   a"
            ), TestData(
                "\n\na".addChanges(
                    inOriginal(0) upTo inOriginal(1) replacedWith "\t",
                ),
                inChange(TextChange(0, 1, "\t"), 0) upTo inOriginal(2),
                "  ",
                inChange(TextChange(0, 2, "  "), 0) upTo inOriginal(2),
                "  a"
            ), TestData(
                "\n\na".addChanges(
                    inOriginal(1) upTo inOriginal(2) replacedWith "",
                ),
                inOriginal(0) upTo inOriginal(2),
                "  ",
                inChange(TextChange(0, 2, "  "), 0) upTo inOriginal(2),
                "  a"
            )
        ).stream()

        @JvmStatic
        fun dataInvalidPositions() =
            listOf(
                ErrorTestData(
                    "\t\t\t".addChanges(
                        inOriginal(0) upTo inOriginal(1) replacedWith "\n"
                    ),
                    inOriginal(0) upTo inOriginal(2),
                    " "
                ),
                ErrorTestData(
                    "\t\t\t".withChanges(),
                    inOriginal(0) upTo inChange(
                        TextChange(1, 2, "  "),
                        0
                    ),
                    "\n"
                ),
                ErrorTestData(
                    "\t\t\t".withChanges(),
                    inOriginal(0) upTo inOriginal(4),
                    "\n"
                ),
            ).stream()

        @JvmStatic
        fun dataWhitespace() = listOf(
            ErrorTestData(
                "Some text".withChanges(),
                inOriginal(0) upTo inOriginal(0),
                "text"
            ),
            ErrorTestData(
                "Some text".withChanges(),
                inOriginal(0) upTo inOriginal(1),
                "\n"
            ),
        ).stream()

        @JvmStatic
        fun dataInvalidRanges() = listOf(
            ErrorTestData(
                "\t\t\t".withChanges(),
                inOriginal(1) upTo inOriginal(0),
                "\n"
            ),
            ErrorTestData(
                "\t\t\t".addChanges(
                    inOriginal(0) upTo inOriginal(1) replacedWith "\n"
                ),
                inOriginal(2) upTo inChange(
                    TextChange(0, 1, "\n"),
                    0
                ),
                " "
            ),
        ).stream()

        @JvmStatic
        fun dataIntersections() = listOf(
            ErrorTestData(
                "\t\t\t".addChanges(
                    inOriginal(0) upTo inOriginal(1) replacedWith "\n\n"
                ),
                inChange(
                    TextChange(0, 1, "\n\n"),
                    1
                ) upTo inOriginal(2), " "
            )
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

    private inline fun <reified T : Throwable> testThrows(
        original: TextWithChanges,
        changeRange: RangeInResult,
        changeText: String
    ) {
        assertThrows<T> { original.addChange(changeRange, changeText) }
    }

    private inline fun <reified T : Throwable> testThrows(data: ErrorTestData) =
        testThrows<T>(
            data.original,
            data.changeRange,
            data.changeText
        )

    private fun testChange(data: TestData) = testChange(
        data.original,
        data.changeRange,
        data.changeText,
        data.expectedRange,
        data.expectedString
    )

    @Test
    fun testNoChanges() {
        val original = "Some text"
        val withChanges = TextWithChanges(original)
        val result = withChanges.applyChanges()
        assertEquals(original, result)
    }

    @ParameterizedTest
    @MethodSource("dataNoMerging")
    fun testChangesNoMerging(data: TestData) {
        testChange(data)
    }

    @ParameterizedTest
    @MethodSource("dataWithMerging")
    fun testChangesWithMerging(data: TestData) {
        testChange(data)
    }


    @ParameterizedTest
    @MethodSource("dataInside")
    fun testChangesInsideChanges(data: TestData) {
        testChange(data)
    }

    @ParameterizedTest
    @MethodSource("dataCovering")
    fun testChangesCoveringChanges(data: TestData) {
        testChange(data)
    }

    @ParameterizedTest
    @MethodSource("dataWhitespace")
    fun testValidatingWS(data: ErrorTestData) {
        testThrows<TextException.NonWhitespace>(data)
    }

    @ParameterizedTest
    @MethodSource("dataIntersections")
    fun testIntersections(data: ErrorTestData) {
        testThrows<TextException.IntersectingChanges>(data)
    }

    @ParameterizedTest
    @MethodSource("dataInvalidPositions")
    fun testInvalidPositions(data: ErrorTestData) {
        testThrows<TextException.InvalidPosition>(data)
    }

    @ParameterizedTest
    @MethodSource("dataInvalidRanges")
    fun testInvalidRanges(data: ErrorTestData) {
        testThrows<TextException.InvalidRange>(data)
    }

    @Nested
    inner class AddingTests {
        @ParameterizedTest
        @MethodSource("TextWithChangesTests#dataInvalidRanges")
        fun testInvalidRanges(data: ErrorTestData) {
            dataInvalidRanges()
            testThrows<TextException.InvalidRange>(data)
        }
    }

    @Nested
    inner class SearchTests {

        @Test
        fun testEmptyString() {
            val original = "".withChanges()
            assertNull(
                original.search(
                    inOriginal(0) upTo inOriginal(0),
                    SearchType.NON_WHITESPACE
                )
            )
            assertNull(
                original.search(
                    inOriginal(0) upTo inOriginal(0),
                    SearchType.LINE_BREAK
                )
            )
            assertNull(
                original.search(
                    inOriginal(0) upTo inOriginal(0),
                    SearchType.BOTH
                )
            )
            assertNull(
                original.search(
                    inOriginal(0) upTo inOriginal(0),
                    SearchType.NON_WHITESPACE,
                    false
                )
            )
            assertNull(
                original.search(
                    inOriginal(0) upTo inOriginal(0),
                    SearchType.LINE_BREAK,
                    false
                )
            )
            assertNull(
                original.search(
                    inOriginal(0) upTo inOriginal(0),
                    SearchType.BOTH,
                    false
                )
            )
        }

        @Test
        fun testSearching() {

        }
    }

    @Nested
    inner class BreakCountTests {
        @Test
        fun testEmptyString() {
            assert(
                "".withChanges()
                    .countBreaks(inOriginal(0) upTo inOriginal(0)) == 0
            )
        }

        @Test
        fun testBreaksInOriginal() {
            val original = "1\n2\n3".withChanges()
            assert(original.countBreaks(inOriginal(0) upTo inOriginal(1)) == 0)
            assert(original.countBreaks(inOriginal(1) upTo inOriginal(1)) == 0)
            assert(original.countBreaks(inOriginal(1) upTo inOriginal(2)) == 1)
            assert(original.countBreaks(inOriginal(2) upTo inOriginal(2)) == 0)
            assert(original.countBreaks(inOriginal(0) upTo inOriginal(5)) == 2)
        }

        @Test
        fun testBreaksInChanges() {
            val original = "123".addChanges(
                inOriginal(0) upTo inOriginal(0) replacedWith "\n",
                inOriginal(1) upTo inOriginal(1) replacedWith "\n",
                inOriginal(2) upTo inOriginal(2) replacedWith "\n",
                inOriginal(3) upTo inOriginal(3) replacedWith "\n",
            )
            assert(original.countBreaks(inOriginal(0) upTo inOriginal(1)) == 1)
            assert(original.countBreaks(inOriginal(0) upTo inOriginal(3)) == 3)
            assert(
                original.countBreaks(
                    inChange(
                        TextChange(0, 0, "\n"),
                        0
                    ) upTo inOriginal(3)
                ) == 4
            )
        }

        @Test
        fun testRemovedBreaks() {
            val original = "\n1\n2\n3\n".addChanges(
                inOriginal(0) upTo inOriginal(1) replacedWith "",
                inOriginal(2) upTo inOriginal(3) replacedWith "",
                inOriginal(4) upTo inOriginal(5) replacedWith "",
                inOriginal(6) upTo inOriginal(7) replacedWith "",
            )
            assert(original.countBreaks(inOriginal(1) upTo inOriginal(7)) == 0)
        }
    }

    @Nested
    inner class SpaceCountTests {
        @Test
        fun testEmptyString() {
            assert(
                TextWithChanges("").countSpaces(
                    inOriginal(0) upTo inOriginal(0),
                    4
                ) == 0
            )
        }

        @Test
        fun testOriginalSpace() {
            assert(
                TextWithChanges(" ").countSpaces(
                    inOriginal(0) upTo inOriginal(1),
                    4
                ) == 1
            )
        }

        @Test
        fun testOriginalTabs() {
            assert(
                TextWithChanges("\t").countSpaces(
                    inOriginal(0) upTo inOriginal(1),
                    4
                ) == 4
            )
            assert(
                TextWithChanges("a\t").countSpaces(
                    inOriginal(0) upTo inOriginal(2),
                    4
                ) == 3
            )
            assert(
                TextWithChanges("aaa\t").countSpaces(
                    inOriginal(0) upTo inOriginal(4),
                    4
                ) == 1
            )
            assert(
                TextWithChanges("aaaa\t").countSpaces(
                    inOriginal(0) upTo inOriginal(5),
                    4
                ) == 4
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