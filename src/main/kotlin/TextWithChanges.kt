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
                val change = findChange(pos.change)
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

    /**
     * Handles the change addition case, where the starting position is based on
     * the original text. This encompasses the two cases, where merging occurs.
     */
    private fun handleFromOriginal(
        from: PositionInResult.InOriginal,
        to: PositionInResult,
        text: String
    ): Triple<TextChange, PositionInResult, PositionInResult> {
        var newTo = to
        val nextChange =
            if (to is PositionInResult.InChange) to.change else null
        val end = when (to) {
            is PositionInResult.InOriginal -> to.position
            is PositionInResult.InChange -> {
                guard(to.position == 0) { throw TextException.InvalidPosition() }
                to.change.from
            }
        }
        val changes = getChangesInRange(from.position..<end)
        var newChange = when (changes.size) {
            0 -> TextChange(from.position, end, text)
            1 -> {
                val change = changes.first()
                changeSet.remove(change)
                TextChange(from.position, end, text)
            }

            else -> throw IllegalArgumentException()
        }
        var shift = 0
        val prevChange = changeSet.floor(newChange)
        if (prevChange != null && prevChange.to == newChange.from) {
            changeSet.remove(prevChange)
            newChange = prevChange merge newChange
            shift = prevChange.text.length
        }
        if (nextChange != null) {
            val oldLength = newChange.text.length
            newChange = newChange merge nextChange
            changeSet.remove(nextChange)
            newTo = inChange(newChange, oldLength)
        }
        val newFrom =
            if (newChange.text.isNotEmpty()) inChange(newChange, shift) else to
        return Triple(newChange, newFrom, newTo)
    }

    /**
     * Handles the change addition case, where the start position is inside
     * another change and the end position is based on the original text. In
     * this case there is no merging.
     */
    private fun handleChangeToOriginal(
        from: PositionInResult.InChange,
        to: PositionInResult.InOriginal,
        text: String
    ): Triple<TextChange, PositionInResult, PositionInResult> {
        guard(from.position == 0) { throw TextException.IntersectingChanges() }
        val change = changeSet.ceiling(from.change)
        guard(change == from.change) { throw TextException.IntersectingChanges() }
        val oldChange = from.change
        changeSet.remove(oldChange)
        val newChange = TextChange(from.change.from, to.position, text)
        val newFrom = if (text.isNotEmpty()) inChange(newChange, 0) else to
        return Triple(newChange, newFrom, to)
    }

    /**
     * Handles the change addition case, where both of the positions given in
     * the RangeInResult object are relative to the same change. Because of the
     * constraints on addressing text positions, this effectively means that any
     * situation where the RangeInResult object contains two
     * PositionInResult.InChange objects then it relates to a situation where
     * the incoming change is inside another one (as long as the given info is
     * valid).
     */
    private fun handleInsideChange(
        from: PositionInResult.InChange,
        to: PositionInResult.InChange,
        text: String
    ): Triple<TextChange, PositionInResult.InChange, PositionInResult.InChange> {
        guard(from.change == to.change) { throw TextException.IntersectingChanges() }
        val oldChange = from.change
        val oldString = oldChange.text
        val newString =
            oldString.replaceRange(from.position..<to.position, text)
        changeSet.remove(oldChange)
        val newChange =
            TextChange(oldChange.from, oldChange.to, newString)
        val newFrom = inChange(newChange, from.position)
        val newTo = inChange(newChange, to.position + text.length)
        return Triple(newChange, newFrom, newTo)
    }

    /**
     * Given a RangeInResult object and a replacement String, adds a new change
     * to this object.
     */
    fun addChange(range: RangeInResult, text: String): RangeInResult {
        validateRange(range)
        guard(text.isBlank()) { throw TextException.NonWhitespace() }
        validateOriginalText(range)
        val (from, to) = range
        if (from == to && text.isEmpty())
            return range
        val (newChange, newFrom, newTo) = when {
            from is PositionInResult.InOriginal -> handleFromOriginal(
                from,
                to,
                text
            )

            from is PositionInResult.InChange && to is PositionInResult.InOriginal -> handleChangeToOriginal(
                from,
                to,
                text
            )

            else -> handleInsideChange(
                from as PositionInResult.InChange,
                to as PositionInResult.InChange,
                text
            )
        }
        changeSet.add(newChange)
        val newRange = newFrom upTo newTo
        return newRange
    }


    private fun searchInOriginal(
        range: IntRange, predicate: (Char) -> Boolean, fromStart: Boolean
    ): Pair<PositionInResult, ResultType>? {
        val result = searchInString(original, range, predicate, fromStart)
        return if (result == null) null else {
            val (index, type) = result
            Pair(inOriginal(index), type)
        }
    }


    private fun searchLastInMiddle(
        range: IntRange, predicate: (Char) -> Boolean
    ): Pair<PositionInResult, ResultType>? {

        val changes =
            changeSet.filter { it.from >= range.first + 1 && it.to <= range.last + 1 }
        if (changes.isEmpty()) return searchInOriginal(
            range, predicate, false
        ) else {
            val iter = changes.reversed().iterator()
            var change: TextChange? = iter.next()
            var i = if (change != null && range.last + 1 == change.to) {
                searchInChange(change, predicate, false)?.let { return it }
                val initValue = change.from
                change = if (iter.hasNext()) iter.next() else null
                initValue
            } else range.last

            while (i >= range.first || change != null) {
                if (change != null) {
                    if (i == change.from) {
                        searchInChange(change, predicate, false)
                    } else {
                        searchInOriginal(
                            change.to..<i, predicate, false
                        )
                    }?.let { return it }
                    val newI =
                        if (i == change.to) change.from else change.to
                    if (i == change.from)
                        change =
                            if (iter.hasNext()) iter.next() else null
                    i = newI


                } else {
                    return searchInOriginal(
                        range.first..<i,
                        predicate,
                        false
                    )
                }
            }
            return null
        }
    }

    private fun searchInMiddle(
        range: IntRange, predicate: (Char) -> Boolean,
    ): Pair<PositionInResult, ResultType>? {
        val changes =
            changeSet.filter { it.from >= range.first && it.to <= range.last }
        if (changes.isEmpty()) return searchInOriginal(
            range, predicate, true
        ) else {
            val iter = changes.iterator()
            var change: TextChange? = iter.next()
            var i = range.first
            while (i <= range.last || change != null) {
                if (change != null) {
                    if (i == change.from) {
                        searchInChange(change, predicate, true)
                    } else {
                        searchInOriginal(
                            i..<change.from, predicate, true
                        )
                    }?.let { return it }
                    val newI =
                        if (i == change.from) change.to else change.from
                    if (i == change.from)
                        change = if (iter.hasNext()) iter.next() else null
                    i = newI


                } else {
                    return searchInOriginal(
                        i..<range.last,
                        predicate,
                        true
                    )
                }
            }
            return null
        }
    }

    /*
    * Given a RangeInResult, returns a pair of Ints that represents the middle
    * part for searching/counting in the given text range. The idea is that if
    * one of the extremities is a position relative to a change then we perform
    * a search/count in a part of the change that is encompassed by the range
    * and return the original text position after/before the change. So, if the
    * search range looks like this: <change>one<middle>two<change>, then the
    * return value is equal to Pair(one, two)
    */
    private fun getMiddleSearchPositions(range: RangeInResult): Pair<Int, Int> {
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

    fun search(
        range: RangeInResult,
        searchType: SearchType
    ): Pair<PositionInResult, ResultType>? {
        try {
            validateRange(range)
            val predicate = searchType.getPredicate()
            val (start, end) = getMiddleSearchPositions(range)
            if (range.start is PositionInResult.InChange) {
                searchInChange(
                    range.start.change,
                    range.start.position..<range.start.change.text.length,
                    predicate,
                    true
                )?.let { return it }
            }
            if (start <= end) {
                searchInMiddle(
                    start..<end,
                    predicate,
                )?.let { return it }
            }
            if (range.end is PositionInResult.InChange) {
                searchInChange(
                    range.end.change,
                    0..<range.end.position,
                    predicate,
                    true
                )?.let { return it }
            }
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }
    }

    fun searchFirst(
        range: RangeInResult,
        searchType: SearchType
    ): Pair<PositionInResult, ResultType>? =
        search(range, searchType)

    /**
     * Given a RangeInResult object and a SearchType, returns a
     * Pair<PositionInResult, ResultType> object that indicates the position and
     * the type of the last occurrence of the searched value
     */
    fun searchLast(
        range: RangeInResult,
        searchType: SearchType
    ): Pair<PositionInResult, ResultType>? {
        try {
            validateRange(range)
            val predicate = searchType.getPredicate()
            val (start, end) = getMiddleSearchPositions(range)
            if (range.end is PositionInResult.InChange) {
                searchInChange(
                    range.end.change,
                    0..<range.end.position,
                    predicate,
                    false
                )?.let { return it }
            }
            if (start <= end) {
                searchLastInMiddle(
                    start..<end,
                    predicate,
                )?.let { return it }
            }
            if (range.start is PositionInResult.InChange) {
                searchInChange(
                    range.start.change,
                    range.start.position..<range.start.change.text.length,
                    predicate,
                    false
                )?.let { return it }
            }
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }
    }

    /**
     * If the given position relative to the original text has a change applied
     * to it, returns the adequate TextChange object. Otherwise, returns null.
     */
    private fun findChange(originalPos: Int): TextChange? =
        this.changeSet.find { it.checkInRange(originalPos) }

    /**
     * If the given TextChange object is present in this instance, returns it.
     * Otherwise, returns null.
     */
    private fun findChange(change: TextChange): TextChange? =
        this.changeSet.find { it == change }


    /**
     * Given a TextChange and a weight function returns an Int value that
     * corresponds to applying the weight function to each of the Chars in the
     * given RangeInResult.
     */
    private fun countInChange(
        change: TextChange, weightFunc: (Char) -> Int
    ): Int = change.text.sumOf(weightFunc)

    /**
     * Given a TextChange, a range within the change and a weight function
     * returns an Int that corresponds to applying the weight function to each
     * of the Chars in the given RangeInResult.
     */
    private fun countInChange(
        change: TextChange, range: IntRange, weightFunc: (Char) -> Int
    ): Int = change.text.substring(range).sumOf(weightFunc)

    private fun countInOriginal(
        range: IntRange, weightFunc: (Char) -> Int
    ) = original.substring(range).sumOf(weightFunc)


    /**
     * Performs the middle part count.
     */
    private fun countInMiddle(
        range: IntRange, weightFunc: (Char) -> Int
    ): Int {
        val changes =
            changeSet.filter { it.from >= range.first + 1 && it.to <= range.last + 1 }
        if (changes.isEmpty()) return countInOriginal(range, weightFunc) else {
            var result = 0
            val iter = changes.iterator()
            var change: TextChange? = iter.next()
            var i = range.first
            while (i <= range.last || change != null) {
                if (change != null) {
                    result += if (i == change.from) countInChange(
                        change, weightFunc
                    ) else countInOriginal(i..<change.from, weightFunc)
                    val newI = if (i == change.from) change.to else change.from
                    if (i == change.from)
                        change = if (iter.hasNext()) iter.next() else null
                    i = newI
                } else {
                    result += countInOriginal(i..range.last, weightFunc)
                    break
                }
            }
            return result
        }
    }

    /**
     * Given a RangeInResult and a weight function, returns an Int value that
     * corresponds to applying the weight function to each of the Chars in the
     * RangeInResult.
     */
    private fun countInText(
        range: RangeInResult, weightFunc: (Char) -> Int
    ): Int {
        validateRange(range)
        val (start, end) = getMiddleSearchPositions(range)
        var result = 0
        if (range.start is PositionInResult.InChange &&
            range.end is PositionInResult.InChange &&
            range.start.change == range.end.change
        ) {
            return this.countInChange(
                range.start.change,
                range.start.position..<range.end.position,
                weightFunc
            )
        }
        if (range.start is PositionInResult.InChange) {
            result += this.countInChange(
                range.start.change,
                range.start.position..<range.start.change.text.length,
                weightFunc
            )
        }
        if (start < end)
            result += this.countInMiddle(start..<end, weightFunc)
        if (range.end is PositionInResult.InChange) {
            result += this.countInChange(
                range.end.change,
                0..<range.end.position,
                weightFunc,
            )
        }
        return result
    }

    /**
     * Calculates and returns the amount of line breaks for a given range.
     */
    fun countBreaks(range: RangeInResult): Int =
        countInText(range) { if (it == '\n') 1 else 0 }


    /**
     * Counts the amount of visual spaces in a given range. The result value
     * takes into account the visual offset of other whitespace characters, such
     * as tabs. The visual space per tab character amount is passed as an
     * additional argument of this function.
     */
    fun countSpaces(range: RangeInResult, spacesPerTab: Int): Int {
        var col = 0
        return countInText(range) {
            val result = when (it) {
                ' ' -> 1
                '\t' -> {
                    val result = spacesPerTab - col
                    col = spacesPerTab - 1
                    result
                }

                else -> 0
            }
            col = (col + 1) % spacesPerTab
            result
        }
    }

    /**
     * Returns the string that results from applying the changes present in this
     * object.
     */
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


fun searchInString(
    string: String,
    range: IntRange,
    predicate: (Char) -> Boolean,
    fromStart: Boolean
): Pair<Int, ResultType>? {
    val index = string.substring(range).run {
        if (fromStart) indexOfFirst(predicate) else indexOfLast(predicate)
    }
    return if (index == -1) null else {
        Pair(
            index + range.first,
            if (string[index] == '\n') {
                ResultType.LINE_BREAK
            } else {
                ResultType.NON_WHITESPACE
            }
        )
    }
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
    searchInChange(change, 0..<change.text.length, predicate, fromStart)

fun String.withChanges() = TextWithChanges(this)