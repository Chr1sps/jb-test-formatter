package io.github.chr1sps

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun String.withEscapes(): String {
    val builder = StringBuilder()
    this.forEach {
        builder.append(
            when (it) {
                '\t' -> "\\t"
                '\r' -> "\\t"
                '\b' -> "\\b"
                '\n' -> "\\n"
                '\'' -> "\\\'"
                '\"' -> "\\\""
                '\\' -> "\\\\"
                in '\u0000'..<' ' -> "\\u${it.code}"
                else -> it
            }
        )
    }
    return builder.toString()
}

@OptIn(ExperimentalContracts::class)
inline fun guard(condition: Boolean, handler: () -> Nothing) {
    contract {
        returns() implies condition
    }
    if (!condition) handler()
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
    return if (index == -1) null else Pair(
        index, string[index].getResultType()
    )
}

fun searchInChange(
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

fun searchInChange(
    change: TextChange, predicate: (Char) -> Boolean, fromStart: Boolean
): Pair<PositionInResult, ResultType>? =
    searchInChange(change, change.from..change.to, predicate, fromStart)
