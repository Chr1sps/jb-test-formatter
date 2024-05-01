package io.github.chr1sps

sealed class TextException : IllegalArgumentException() {
    class InvalidPosition : TextException()
    class InvalidRange : TextException()
    class NonWhitespace : TextException()
    class IntersectingChanges : TextException()
}

