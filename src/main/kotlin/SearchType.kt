package io.github.chr1sps

enum class SearchType {
    NON_WHITESPACE,
    LINE_BREAK,
    BOTH;

    fun getPredicate(): (Char) -> Boolean = when (this) {
        NON_WHITESPACE -> { it -> !it.isWhitespace() }
        LINE_BREAK -> { it -> it == '\n' }
        BOTH -> { it -> it == '\n' || !it.isWhitespace() }
    }
}