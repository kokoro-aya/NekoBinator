package moe.irony.simplepc.parser

import moe.irony.simplepc.instances.Option
import moe.irony.simplepc.instances.Result

class Context<A>(val state: ParseState, val consumed: Boolean, val result: A) {

}

class FailContext(val state: ParseState, val consumed: Boolean, val halted: Boolean): Throwable() {
    override fun toString(): String = "Context:: $state, consumed: $consumed, halted: $halted"
}

fun <A> Result<Context<A>>.getParseState(): ParseState = when (this) {
    is Result.Success -> this.value.state
    is Result.Failure -> when (val ctx = this.coerceFailure()) {
        is FailContext -> ctx.state
        else -> throw IllegalStateException("FailContext:: getConsumed :: coerce failure returned another Throwable instance, this should never happen")
    }
}

fun <A> Result<Context<A>>.getConsumed(): Boolean = when (this) {
    is Result.Success -> this.value.consumed
    is Result.Failure -> when (val ctx = this.coerceFailure()) {
        is FailContext -> ctx.consumed
        else -> throw IllegalStateException("FailContext:: getConsumed :: coerce failure returned another Throwable instance, this should never happen")
    }
}

fun <A> Result<Context<A>>.getResult(): Option<A> = when (val ctx = this.toOption()) {
    is Option.Some -> Option.Some(ctx.value.result)
    is Option.None -> Option.None
}

fun <A> Result<Context<A>>.coerceAbort(): Boolean = when (val ctx = this.coerceFailure()) {
    is FailContext -> ctx.halted
    else -> throw IllegalStateException("FailContext:: coerceAbort :: coerce failure returned another Throwable instance, this should never happen")
}