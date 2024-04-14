package io.github.chr1sps

/*

Test task 2
We have a text in a programming language, represented by a string. The first
pass of the code formatter made some changes to the white spaces in this text.
White space is defined as a sequence of space, tab, and line break characters.
Each change is described by:

- A range in the text, defined by a start and end position (inclusive start,
  exclusive end), which indicates the portion to be replaced or removed. A range
  could also be empty (with the start offset equal to the end offset),
  indicating an insertion.
- A string containing whitespace characters to replace
  or insert in the specified range. An empty string indicates removal.
The text was parsed into an abstract syntax tree (AST), and this tree contains
positions corresponding to the original text. As reparsing is a costly operation
and the formatter does not alter the structure of the syntax tree (only adding
or removing whitespace tokens), we require a second pass of the code formatter
to work with the same AST while accommodating the changes made by the first pass.

Example:

//Before formatter
while( true){foo( );}

//After formatter
while (true)
{
    foo();
}
You need to implement a class TextWithChanges that represents the text and
formatter changes. The list of changes should always be sorted. In Kotlin, you
can use TreeSet to ensure sorting, and utilize its methods floor, higher, and
contains for optimization when needed. Additionally, you need to create a class
PositionInResult that represents a position in the resulting text. We cannot
implement it simply as an integer offset in the resulting text because it would
be costly to translate it to an offset in the original text, which we need to
work with the AST. So, if the position points to an unchanged part of the text,
then it should be described by an offset in the original text. Otherwise, the
position should be described by a reference to a change and an offset in a
replacement string. Furthermore, you need to create a class RangeInResult that
represents a range in the resulting text as a start and an end position (start
inclusive, end exclusive).

You need to implement the following methods in the TextWithChanges class:

- Add a new change: Accept a RangeInResult and a string as arguments. Check and
  assert that a change only modifies white spaces (both the old text and the new
  text must consist of only white spaces). Convert a RangeInResult to a range in
  the original text. If a new change completely replaces some old change, remove
  that old change before adding the new one. If a new change is completely
  inside some old change, update that old change instead of adding. Otherwise,
  check and assert that change ranges do not intersect with each other. If
  changes touch each other (when the end of one range is equal to the start of
  the next range), merge them (be cautious in a situation when one or both of
  the ranges are empty).
- Search: Given a RangeInResult, a direction (from start to end or from end to
  start), and a flag “what to search” (non-whitespace characters, line breaks,
  or both), return a result (not found, found a non-whitespace character, found
  a line break) and the position of the found character.
- Count line breaks: Given a RangeInResult, calculate and return the number of
  line breaks.
- Count simple spaces: Given a RangeInResult and a tab width, calculate and
  return the numbers of spaces and tabs and the length of a visual
  representation of the characters in the range. The length of a visual
  representation could differ from the number of characters in the range in
  cases where tab characters are present in it. Assume that the start character
  in the range is placed in the first column. Refer to this discussion about
  rendering tab characters
- Apply the changes and return the resulting text as a string.

Write tests for all corner cases. It's preferable to implement everything in
Kotlin.

*/

class TextWithChanges(original: String) {
    val original = original
    val changeSet = sortedSetOf<TextChange>()
    fun search(
        range: RangeInResult,
        type: SearchType,
        from_start: Boolean = true
    ): Pair<PositionInResult, SearchType>? {
        TODO()
    }

    fun addChange(range: RangeInResult, text: String) {
        TODO()
    }

    /**
     * Calculates and returns the amount of line breaks for a given range.
     */
    fun countBreaks(range: RangeInResult): UInt {
        TODO()
    }

    /**
     * Counts the amount of visual spaces in a given range. The result value
     * takes into account the visual offset of other whitespace characters, such
     * as tabs.
     */
    fun countSpaces(range: RangeInResult): UInt {
        TODO()
    }

    fun applyChanges(): String {
        return original
    }
}