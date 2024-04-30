package io.github.chr1sps


class TextChange(val from: Int, val to: Int, val text: String) :
    Comparable<TextChange> {
    private val id = genID()
    val offset: Int
        get() = to - from - text.length

    init {
        require(to >= 0)
        require(from in 0..to)
        require(text.isBlank())
    }

    companion object {
        private var id = 0L
        fun genID() = id++
    }

    override fun compareTo(other: TextChange) = (this.from - other.from)
    override fun equals(other: Any?) = when {
        this === other -> true
        javaClass != other?.javaClass -> false
        else -> {
            other as TextChange
            from == other.from && to == other.to && text == other.text
        }
    }

    override fun hashCode() = (from * 31 + to) * 31 + text.hashCode()
    override fun toString() =
        "TextChange(from: $from, to: $to, text: \"${text.withEscapes()}\")"

    infix fun merge(other: TextChange): TextChange {
        require(this.to == other.from)
        return TextChange(this.from, other.to, this.text + other.text)
    }

    fun checkInRange(position: Int): Boolean = position in from..<to
}
