package moe.irony.simplepc.utils

internal data class LazyFix<A, B>(val call: (LazyFix<A, B>) -> (A) -> B)

internal fun <A, B> fix(lazyFix: LazyFix<A, B>): (A) -> B =
    lazyFix.call(lazyFix)

internal fun <A, B> yCombinator(recur: ((A) -> B) -> (A) -> B): (A) -> B =
    fix(LazyFix { rec -> recur{ x -> fix(rec)(x) }})