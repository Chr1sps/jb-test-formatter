package io.github.chr1sps


class TextChange(val from: Int, val to: Int, val text: String) :
    Comparable<TextChange> {
    private val id = genID()
    val offset: Int
        get() = to - from - text.length

    init {
        require(from >= 0)
        require(to >= 0)
        require(to >= from)
        require(text.isBlank())
    }

    companion object {
        private var id = 0L
        fun genID() = id++
    }

    //    override fun compareTo(other: TextChange) = (this.id - other.id).toInt()
    override fun compareTo(other: TextChange) = (this.from - other.from)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return if (other !is TextChange) false else
            from == other.from && to == other.to && text == other.text
    }

    override fun hashCode(): Int {
        var result = from
        result = 31 * result + to
        result = 31 * result + text.hashCode()
        return result
    }

    fun checkInRange(position: Int): Boolean = position in from..to
}