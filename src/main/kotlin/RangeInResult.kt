package io.github.chr1sps


/**
 * A class representing a range in the modified text. This class doesn't perform
 * any semantic checks of the data passed to it.
 */
data class RangeInResult(
    val start: PositionInResult,
    val end: PositionInResult
) {
    override fun equals(other: Any?) = when {
        this === other -> true
        javaClass != other?.javaClass -> false
        else -> {
            other as RangeInResult
            start == other.start && end == other.end
        }
    }

    override fun hashCode() = start.hashCode() * 31 + end.hashCode()
    override fun toString() = "RangeInResult(from: $start, to: $end)"
}

infix fun PositionInResult.upTo(other: PositionInResult) =
    RangeInResult(this, other)