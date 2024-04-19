package io.github.chr1sps

sealed class PositionInResult {
    class InOriginal(val position: Int) : PositionInResult() {
        init {
            require(position >= 0)
        }
    }

    class InChange(val change: TextChange, val position: Int) :
        PositionInResult() {
        init {
            require(position >= 0)
        }
    }
}
