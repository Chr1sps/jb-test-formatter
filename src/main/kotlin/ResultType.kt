package io.github.chr1sps

enum class ResultType {
    NON_WHITESPACE,
    LINE_BREAK
}

fun Char?.getResultType() =
    if (this?.isWhitespace() == true) ResultType.NON_WHITESPACE else ResultType.LINE_BREAK