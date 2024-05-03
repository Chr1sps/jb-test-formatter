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
