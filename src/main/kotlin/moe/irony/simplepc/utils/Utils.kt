package moe.irony.simplepc.utils

fun <A, B> const(): (A) -> (B) -> A = { x -> { _ -> x } }
fun <A> id(): (A) -> A = { it }

fun <A> cons(): (A) -> (List<A>) -> List<A> = { x -> { xx ->
    val zz = xx.toMutableList()
    zz.add(x)
    zz
}}