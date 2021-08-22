package moe.irony.simplepc.utils

internal fun <A, B> const(): (A) -> (B) -> A = { x -> { _ -> x } }
internal fun <A> id(): (A) -> A = { it }

internal fun <T, U, V> ((T) -> U).andThen(after: (U) -> V): (T) -> V = { x ->
    after(this.invoke(x))
}

internal fun <A> cons(): (A) -> (List<A>) -> List<A> = { x -> { xx ->
    val zz = xx.toMutableList()
    zz.add(x)
    zz
}}

internal fun List<Char>.constructString() =
    this.foldRight("") { ch, acc -> acc + ch } // reversed order

internal fun List<Char>.constructInt(): Int =
    this.foldRight(0) { ch, acc -> acc * 10 + (ch - '0') }

internal fun List<Char>.constructLong(): Long =
    this.foldRight(0L) { ch, acc -> acc * 10 + (ch - '0') }

fun <A, B, C> curry2(f: (A, B) -> C): (A) -> (B) -> C = { x -> { y -> f(x, y) } }

fun <A, B, C> uncurry2(f: (A) -> (B) -> C): (A, B) -> C = { x, y -> f(x)(y) }