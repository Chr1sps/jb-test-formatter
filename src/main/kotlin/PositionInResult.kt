package io.github.chr1sps

sealed class PositionInResult {
    class InOriginal(val position: Int) : PositionInResult() {
        init {
            require(position >= 0)
        }

        override fun equals(other: Any?) = when {
            this === other -> true
            javaClass != other?.javaClass -> false
            else -> {
                other as InOriginal
                position == other.position
            }
        }

        override fun hashCode() = position
        override fun toString() =
            "InOriginal(position: $position)"
    }

    class InChange(val change: TextChange, val position: Int) :
        PositionInResult() {
        init {
            require(position in 0..change.text.length)
        }

        override fun equals(other: Any?) = when {
            this === other -> true
            javaClass != other?.javaClass -> false
            else -> {
                other as InChange
                change == other.change && position == other.position
            }
        }

        override fun hashCode() = 31 * change.hashCode() + position
        override fun toString() =
            "InChange(change: $change, position: $position)"
    }
}

fun inOriginal(position: Int) = PositionInResult.InOriginal(position)

fun inChange(change: TextChange, position: Int) =
    PositionInResult.InChange(change, position)
