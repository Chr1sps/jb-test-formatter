package io.github.chr1sps

class TextWithChanges(private val original: String) {
    private val changeSet = sortedSetOf<TextChange>()

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

    /**
     * Validates that the given position points to a correct position, that is
     * either to a position unaffected by changes or to a position in a change.
     * Throws an IllegalArgumentException when that is not the case.
     */
    private fun validatePos(pos: PositionInResult): AggregatePos {
        when (pos) {
            is PositionInResult.InOriginal -> {
                guard(pos.position in 0..original.length) { throw TextException.InvalidPosition() }
                val change = findChange(pos.position)
                guard(change == null) { throw TextException.InvalidPosition() }
                return AggregatePos(pos.position, null)
            }

            is PositionInResult.InChange -> {
                val change = findChange(pos.change.from)
                guard(change != null) { throw TextException.InvalidPosition() }
                guard(pos.position in 0..<change.text.length) { throw TextException.InvalidPosition() }
                return AggregatePos(change.from, pos.position)
            }
        }
    }

    /**
     * Validates the range by first validating its positions and then checking
     * if the starting position precedes / is equal to the end position. Throws
     * an IllegalArgumentException otherwise.
     */
    private fun validateRange(range: RangeInResult) {
        val (from, to) = range
        val aggFrom = validatePos(from)
        val aggTo = validatePos(to)
        guard(aggTo >= aggFrom) { throw TextException.InvalidRange() }
    }

    private fun getChangesInRange(range: IntRange) =
        changeSet.filter { it.from >= range.first && it.to <= range.last + 1 }

    private fun validateOriginalText(range: RangeInResult) =
        guard(countInText(range) { if (it.isWhitespace()) 0 else 1 } == 0) { throw TextException.NonWhitespace() }

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
    change)) or that they are relative to different changes
    - when the changes touch, merge them (be wary of empty ranges)
    - throw an exception when a new change spans over multiple other changes
    - return an original RangeInResult object that may additionally be altered
    to reflect the new changes
    */
    fun addChange(range: RangeInResult, text: String): RangeInResult {
        // TODO: Check if the original string is ws only.
        validateRange(range)
        guard(text.isBlank()) { throw TextException.NonWhitespace() }
        validateOriginalText(range)
        val (from, to) = range
        if (from == to && text.isEmpty()) // the change does nothing, just return the original range
            return range
        val newFrom: PositionInResult
        var newTo: PositionInResult = to
        val newChange = when {
            from is PositionInResult.InOriginal -> {
                val end = when (to) {
                    is PositionInResult.InOriginal -> to.position
                    is PositionInResult.InChange -> {
                        guard(to.position == 0) { throw TextException.InvalidPosition() }
                        to.change.from
                    }
                }
                val changes = getChangesInRange(from.position..<end)
                val newChange = when (changes.size) {
                    0 -> TextChange(from.position, end, text)
                    1 -> {
                        val change = changes.first()
                        changeSet.remove(change)
                        TextChange(from.position, end, text)
                    }

                    else -> throw IllegalArgumentException()
                }
                newFrom = if (text.isNotEmpty()) inChange(newChange, 0) else to
                newChange
            }

            from is PositionInResult.InChange && to is PositionInResult.InOriginal -> {
                guard(from.position == 0) { throw TextException.IntersectingChanges() }
                val changes = getChangesInRange(from.change.from..<to.position)
                guard(changes.size == 1) { throw TextException.IntersectingChanges() }
                val oldChange = from.change
                changeSet.remove(oldChange)
                val newChange = TextChange(from.change.from, to.position, text)
                newFrom = if (text.isNotEmpty()) inChange(newChange, 0) else to
                newChange
            }

            else -> {
                from as PositionInResult.InChange
                to as PositionInResult.InChange
                guard(from.change == to.change) { throw TextException.IntersectingChanges() }
                guard(changeSet.contains(from.change)) { throw TextException.OutdatedChange() }
                val oldChange = from.change
                val oldString = oldChange.text
                val newString =
                    oldString.replaceRange(from.position..<to.position, text)
                changeSet.remove(oldChange)
                val newChange =
                    TextChange(oldChange.from, oldChange.to, newString)
                newFrom = inChange(newChange, from.position)
                newTo = inChange(newChange, to.position + text.length)
                newChange
            }
        }
        val merged = mergeChanges(newChange)
        changeSet.add(merged)
        // TODO: Updating the range after merging.
        val newRange = newFrom upTo newTo
        return newRange
    }

    /**
     * Given the new change, searches for changes that may touch it. If there
     * are any then removes them from the set and merges them with the given
     * change. The resulting range is then returned from the function. If there
     * were no adjacent changes then the set isn't altered and the original
     * change is returned.
     */
    private fun mergeChanges(change: TextChange): TextChange {
        var newChange = change
        val nextChange = changeSet.find { it.from == change.to }
        if (nextChange != null) {
            changeSet.remove(nextChange)
            newChange = newChange merge nextChange
        }
        val prevChange = changeSet.find { it.to == change.from }
        if (prevChange != null) {
            changeSet.remove(prevChange)
            newChange = prevChange merge newChange
        }
        changeSet.add(newChange)
        return newChange
    }

    private fun searchInString(
        string: String,
        range: IntRange,
        predicate: (Char) -> Boolean,
        fromStart: Boolean
    ): Pair<Int, ResultType>? {
        val index = string.substring(range).run {
            if (fromStart) indexOfFirst(predicate) else indexOfLast(predicate)
        }
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
            Pair(inChange(change, index), type)
        }
    }

    private fun searchInChange(
        change: TextChange, predicate: (Char) -> Boolean, fromStart: Boolean
    ): Pair<PositionInResult, ResultType>? =
        searchInChange(change, change.from..change.to, predicate, fromStart)

    private fun searchInOriginal(
        range: IntRange, predicate: (Char) -> Boolean, fromStart: Boolean
    ): Pair<PositionInResult, ResultType>? {
        val result = searchInString(original, range, predicate, fromStart)
        return if (result == null) null else {
            val (index, type) = result
            Pair(inOriginal(index), type)
        }
    }


    private fun searchInOriginalWithChanges(
        range: IntRange, predicate: (Char) -> Boolean, fromStart: Boolean
    ): Pair<PositionInResult, ResultType>? {
        val changes =
            changeSet.filter { it.from >= range.first && it.to <= range.last }
        if (changes.isEmpty()) return searchInOriginal(
            range, predicate, fromStart
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
                                i..change.from, predicate, fromStart
                            )
                        }
                    if (found != null) return found
                    else {
                        i = if (i == change.from) change.to else change.from
                        change = if (iter.hasNext()) iter.next()
                        else null
                    }

                } else {
                    return searchInOriginal(i..range.last, predicate, fromStart)
                }
            }
            return null
        }
    }

    private fun getPositions(range: RangeInResult): Pair<Int, Int> {
        val start = when (range.start) {
            is PositionInResult.InOriginal -> range.start.position
            is PositionInResult.InChange -> range.start.change.to
        }
        val end = when (range.end) {
            is PositionInResult.InOriginal -> range.end.position
            is PositionInResult.InChange -> range.end.change.from
        }
        return Pair(start, end)
    }

    // - Search: Given a RangeInResult, a direction (from start to end or from end to
    // start), and a flag “what to search” (non-whitespace characters, line breaks,
    // or both), return a result (not found, found a non-whitespace character, found
    // a line break) and the position of the found character.
    fun search(
        range: RangeInResult, type: SearchType, fromStart: Boolean = true
    ): Pair<PositionInResult, ResultType>? {
        try {
            validateRange(range)
            val predicate: (Char) -> Boolean = when (type) {
                SearchType.NON_WHITESPACE -> { it -> !it.isWhitespace() }
                SearchType.LINE_BREAK -> { it -> it == '\n' }
                SearchType.BOTH -> { it -> !it.isWhitespace() || it == '\n' }
            }
            val (start, end) = getPositions(range)
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
    private fun findChange(originalPos: Int): TextChange? =
        this.changeSet.find { it.checkInRange(originalPos) }


    private fun countInText(
        range: RangeInResult, predicate: (Char) -> Int
    ): Int {
        validateRange(range)
        val (start, end) = getPositions(range)
        var result = 0
        if (range.start is PositionInResult.InChange) {
            result += this.countInChange(
                range.start.change,
                range.start.position..<range.start.change.text.length,
                predicate
            )
        }
        if (start < end)
            result += this.countInOriginalWithChanges(start..<end, predicate)
        if (range.end is PositionInResult.InChange) {
            result += this.countInChange(
                range.end.change,
                0..<range.end.position,
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
        range: IntRange, predicate: (Char) -> Int
    ): Int {
        val changes =
            changeSet.filter { it.from >= range.first && it.to <= range.last }
        if (changes.isEmpty()) return countInOriginal(range, predicate) else {
            var result = 0
            val iter = changes.iterator()
            var change: TextChange? = iter.next()
            var i = range.first
            while (i <= range.last) {
                if (change != null) {
                    result += if (i == change.from) countInChange(
                        change, predicate
                    ) else countInOriginal(i..change.from, predicate)
                    i = if (i == change.from) change.to else change.from
                    change = if (iter.hasNext()) iter.next() else null
                } else {
                    result += countInOriginal(i..range.last, predicate)
                    break
                }
            }
            return result
        }
    }

    private fun countInChange(
        change: TextChange, predicate: (Char) -> Int
    ): Int = change.text.sumOf(predicate)

    private fun countInChange(
        change: TextChange, range: IntRange, predicate: (Char) -> Int
    ): Int = change.text.substring(range).sumOf(predicate)

    private fun countInOriginal(
        range: IntRange, predicate: (Char) -> Int
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
        var result = original
        for (change in changeSet) {
            result = result.replaceRange(
                change.from + offset, change.to + offset, change.text
            )
            offset += change.offset
        }
        return result
    }
}