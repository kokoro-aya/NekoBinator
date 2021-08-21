package moe.irony.simplepc.parser

class ParseState(val s: String, val p: Int = 0) {

    fun hasNext(): Boolean = p in s.indices

    fun peek(): Char = s[p]

    fun next(): ParseState = ParseState(s, p + 1)

    override fun toString(): String = "State { $s, at: $p }"
}