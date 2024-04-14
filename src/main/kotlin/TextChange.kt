package io.github.chr1sps

data class TextChange(val range: RangeInResult, val text: String)

infix fun TextChange.merge(other: TextChange): TextChange {
    TODO()
}