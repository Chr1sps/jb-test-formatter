package io.github.chr1sps

/**
 * A class representing a range in the modified text. This class doesn't perform
 * any semantic checks of the data passed to it.
 */
data class RangeInResult(val start: PositionInResult, val end: PositionInResult)