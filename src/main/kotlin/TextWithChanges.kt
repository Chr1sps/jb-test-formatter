package io.github.chr1sps

import java.util.*


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
while (true)\n{\n\tfoo();\n}
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

class TextWithChanges(private val original: String) {
    private val _changeSet = sortedSetOf<TextChange>()
    val changeSet get() = TreeSet(_changeSet)

    /**
     * A class for comparing relative positions of PositionInResult objects.
     */
    data class AggregatePos(val pos: Int, val innerPos: Int?) :
        Comparable<AggregatePos> {
        override fun compareTo(other: AggregatePos): Int =
            if (this.pos == other.pos) {
                when {
                    this.innerPos == null && other.innerPos == null -> 0
                    this.innerPos == null && other.innerPos != null -> -1
                    this.innerPos != null && other.innerPos == null -> 1
                    else -> this.innerPos!! - other.innerPos!!
                }
            } else this.pos - other.pos
    }

    sealed class PositionStatus {
        class WrappedBy(val change: TextChange) : PositionStatus()
        class Wrapping(val changes: List<TextChange>) : PositionStatus()
        data object Unaffected : PositionStatus()
    }

    /**
     * Validates that the given position points to a correct position, that is
     * either to a position unaffected by changes or to a position in a change.
     * Throws an IllegalArgumentException when that is not the case.
     */
    private fun validatePos(pos: PositionInResult): AggregatePos {
        when (pos) {
            is PositionInResult.InOriginal -> {
                require(pos.position in 0..original.length)
                val change = findChange(pos.position)
                require(change == null)
                return AggregatePos(pos.position, null)
            }

            is PositionInResult.InChange -> {
                val change = findChange(pos.position)
                require(change != null)
                require(pos.position in 0..change.text.length)
                return AggregatePos(change.from, pos.position)
            }
        }
    }

    /**
     * Validates the range by first validating its positions and then checking
     * if the starting position precedes / is equal to the end position. Throws
     * an IllegalArgumentException otherwise.
     */
    private fun validateRange(range: RangeInResult): Pair<AggregatePos, AggregatePos> {
        val (from, to) = range
        val aggFrom = validatePos(from)
        val aggTo = validatePos(to)
        require(aggTo >= aggFrom)
        return Pair(aggFrom, aggTo)
    }

    private fun validateAffectedChanges(range: RangeInResult): PositionStatus {
        val (from, to) = range

        if (from is PositionInResult.InChange && to is PositionInResult.InChange && from.change == to.change) {
            return PositionStatus.WrappedBy(from.change)
        } else {
            if (from is PositionInResult.InChange)
                require(from.position == from.change.from)
            if (to is PositionInResult.InChange)
                require(to.position == to.change.to)
        }
        val startBound = when (from) {
            is PositionInResult.InOriginal -> from.position
            is PositionInResult.InChange -> from.change.from
        }
        val endBound = when (to) {
            is PositionInResult.InOriginal -> to.position
            is PositionInResult.InChange -> to.change.to
        }
        val changes =
            _changeSet.filter { it.from >= startBound && it.to < endBound }
        return if (changes.isEmpty()) PositionStatus.Unaffected else PositionStatus.Wrapping(
            changes
        )
    }

    /*
    TODO
    - check and assert that the old and new texts contain only ws chars
    - convert a RangeInResult to a range in original text
    - if a new change replaced an old one, remove the old one and add the new
    one - that means that both of the positions must be relative to the original
    text
    - if a new change is completely inside an old one, update the old one - that
    means that both of the edge positions must be relative to the same change
    - otherwise, check and assert that the ranges don't intersect (intersection
    means that the position types aren't equal (one relative to text, another to
    change) or that they are relative to different changes
    - when the changes touch, merge them (be wary of empty ranges)
    */
    fun addChange(range: RangeInResult, text: String): TextChange {
        val (aggFrom, aggTo) = validateRange(range)
        val (from, to) = range
        when {
            from is PositionInResult.InOriginal && to is PositionInResult.InOriginal -> {}
            from is PositionInResult.InChange && to is PositionInResult.InChange -> {}
            from is PositionInResult.InOriginal && to is PositionInResult.InChange -> {}
            from is PositionInResult.InChange && to is PositionInResult.InOriginal -> {}
        }
        TODO()
    }

    private fun searchInString(
        string: String,
        range: IntRange,
        predicate: (Char) -> Boolean,
        fromStart: Boolean
    ): Pair<Int, ResultType>? {
        val substring = string.substring(range)
        val index = if (fromStart) substring.indexOfFirst(predicate)
        else substring.indexOfLast(predicate)
        return if (index == -1) null else Pair(
            index, string[index].getResultType()
        )
    }

    private fun searchInChange(
        change: TextChange,
        range: IntRange,
        predicate: (Char) -> Boolean,
        fromStart: Boolean
    ): Pair<PositionInResult, ResultType>? {
        val result = searchInString(change.text, range, predicate, fromStart)
        return if (result == null) null else {
            val (index, type) = result
            Pair(PositionInResult.InChange(change, index), type)
        }
    }

    private fun searchInChange(
        change: TextChange,
        predicate: (Char) -> Boolean,
        fromStart: Boolean
    ): Pair<PositionInResult, ResultType>? =
        searchInChange(change, change.from..change.to, predicate, fromStart)

    private fun searchInOriginal(
        range: IntRange,
        predicate: (Char) -> Boolean,
        fromStart: Boolean
    ): Pair<PositionInResult, ResultType>? {
        val result = searchInString(original, range, predicate, fromStart)
        return if (result == null) null else {
            val (index, type) = result
            Pair(PositionInResult.InOriginal(index), type)
        }
    }


    private fun searchInOriginalWithChanges(
        range: IntRange,
        predicate: (Char) -> Boolean,
        fromStart: Boolean
    ): Pair<PositionInResult, ResultType>? {
        val changes =
            _changeSet.filter { it.from >= range.first && it.to <= range.last }
        if (changes.isEmpty()) return searchInOriginal(
            range,
            predicate,
            fromStart
        ) else {
            val iter = changes.iterator()
            var change: TextChange? = iter.next()
            var i = range.first
            while (i <= range.last) {
                if (change != null) {
                    val found: Pair<PositionInResult, ResultType>? =
                        if (i == change.from) {
                            searchInChange(change, predicate, fromStart)
                        } else {
                            searchInOriginal(
                                i..change.from,
                                predicate,
                                fromStart
                            )
                        }
                    if (found != null)
                        return found
                    else {
                        i = if (i == change.from) change.to else change.from
                        change = if (iter.hasNext())
                            iter.next()
                        else
                            null
                    }

                } else {
                    return searchInOriginal(i..range.last, predicate, fromStart)
                }
            }
            return null
        }
    }

    // - Search: Given a RangeInResult, a direction (from start to end or from end to
    // start), and a flag “what to search” (non-whitespace characters, line breaks,
    // or both), return a result (not found, found a non-whitespace character, found
    // a line break) and the position of the found character.
    fun search(
        range: RangeInResult,
        type: SearchType,
        fromStart: Boolean = true
    ): Pair<PositionInResult, ResultType>? {
        try {
            validateRange(range)
            val predicate: (Char) -> Boolean = when (type) {
                SearchType.NON_WHITESPACE -> { it -> !it.isWhitespace() }
                SearchType.LINE_BREAK -> { it -> it == '\n' }
                SearchType.BOTH -> { it -> !it.isWhitespace() || it == '\n' }
            }
            val start = when (range.start) {
                is PositionInResult.InOriginal -> range.start.position
                is PositionInResult.InChange -> range.start.change.to
            }
            val end = when (range.end) {
                is PositionInResult.InOriginal -> range.end.position
                is PositionInResult.InChange -> range.end.change.from
            }
            var found: Pair<PositionInResult, ResultType>?
            if (range.start is PositionInResult.InChange) {
                found = searchInChange(
                    range.start.change,
                    range.start.position..range.start.change.text.length,
                    predicate,
                    fromStart
                )
                if (found != null) return found
            }
            found =
                searchInOriginalWithChanges(start..end, predicate, fromStart)
            if (found != null) return found
            if (range.end is PositionInResult.InChange) {
                found = searchInChange(
                    range.end.change,
                    0..range.end.position,
                    predicate,
                    fromStart
                )
                if (found != null) return found
            }
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }
    }

    /**
     * Checks if a given position in original text has a change applied to it
     * and returns a corresponding TextChange object if it does so, otherwise
     * returns null.
     */
    fun findChange(originalPos: Int): TextChange? =
        this._changeSet.find { it.checkInRange(originalPos) }


    fun countInText(range: RangeInResult, predicate: (Char) -> Int): Int {
        validateRange(range)
        val start = when (range.start) {
            is PositionInResult.InOriginal -> range.start.position
            is PositionInResult.InChange -> range.start.change.to
        }
        val end = when (range.end) {
            is PositionInResult.InOriginal -> range.end.position
            is PositionInResult.InChange -> range.end.change.from
        }
        var result = 0
        if (range.start is PositionInResult.InChange) {
            result += this.countInChange(
                range.start.change,
                range.start.position..range.start.change.text.length,
                predicate
            )
        }
        result += this.countInOriginalWithChanges(start..end, predicate)
        if (range.end is PositionInResult.InChange) {
            result += this.countInChange(
                range.end.change,
                0..range.end.position,
                predicate,
            )
        }
        return result
    }

    /**
     * Calculates and returns the amount of line breaks for a given range.
     */
    fun countBreaks(range: RangeInResult): Int =
        countInText(range) { if (it == '\n') 1 else 0 }

    private fun countInOriginalWithChanges(
        range: IntRange,
        predicate: (Char) -> Int
    ): Int {
        val changes =
            _changeSet.filter { it.from >= range.first && it.to <= range.last }
        if (changes.isEmpty()) return countInOriginal(range, predicate) else {
            var result = 0
            val iter = changes.iterator()
            var change: TextChange? = iter.next()
            var i = range.first
            while (i <= range.last) {
                if (change != null) {
                    result += if (i == change.from) countInChange(
                        change,
                        predicate
                    ) else countInOriginal(i..change.from, predicate)
                    i = if (i == change.from) change.to else change.from
                    change = if (iter.hasNext()) iter.next() else null
                } else {
                    result += countInOriginal(i..range.last, predicate)
                }
            }
            return result
        }
    }

    private fun countInChange(
        change: TextChange,
        predicate: (Char) -> Int
    ): Int = change.text.sumOf(predicate)

    private fun countInChange(
        change: TextChange,
        range: IntRange,
        predicate: (Char) -> Int
    ): Int = change.text.substring(range).sumOf(predicate)

    private fun countInOriginal(
        range: IntRange,
        predicate: (Char) -> Int
    ) = original.substring(range).sumOf(predicate)

    /**
     * Counts the amount of visual spaces in a given range. The result value
     * takes into account the visual offset of other whitespace characters, such
     * as tabs.
     */
    fun countSpaces(range: RangeInResult, spacesPerTab: Int): Int {
        var col = 0
        return countInText(range) {
            val result = when (it) {
                ' ' -> 1
                '\t' -> spacesPerTab - col
                else -> 0
            }
            col = (col + 1) % spacesPerTab
            result
        }
    }

    fun applyChanges(): String {
        var offset = 0
        val result = original
        for (change in _changeSet) {
            result.replaceRange(
                change.from + offset,
                change.to + offset,
                change.text
            )
            offset += change.offset
        }
        return result
    }
}