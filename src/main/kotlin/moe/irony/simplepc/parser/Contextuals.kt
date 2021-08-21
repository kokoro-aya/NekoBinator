package moe.irony.simplepc.parser

class Context<A>(val state: ParseState, val consumed: Boolean, val result: A)

class FailContext(val state: ParseState, val consumed: Boolean, val halted: Boolean): Throwable()