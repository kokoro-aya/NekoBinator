package moe.irony.simplepc.instances

import moe.irony.simplepc.types.Functor
import moe.irony.simplepc.types.HKT
import moe.irony.simplepc.types.Monad

sealed class Result<A>: HKT<Result<*>, A> {
    class Success<A>(val value: A): Result<A>() {
        override fun toString(): String = "[Success: $value]"

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is Success<*>) return false
            return this.value == other.value
        }

        override fun hashCode(): Int {
            return value?.hashCode() ?: 0
        }
    }

    class Failure<A>(val failure: Throwable): Result<A>() {
        override fun toString(): String = "[Fail: $failure]"

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is Failure<*>) return false
            return this.failure == other.failure
        }

        override fun hashCode(): Int {
            return failure.hashCode()
        }
    }

    fun isSuccess(): Boolean = when (this) {
        is Success -> true
        is Failure -> false
    }

    fun isFailure(): Boolean = when (this) {
        is Success -> false
        is Failure -> true
    }

    fun toOption(): Option<A> = when (this) {
        is Success -> Option.Some(this.value)
        is Failure -> Option.None
    }

    fun coerceResult(): A = when (this) {
        is Success -> this.value
        is Failure -> throw IllegalStateException("Result:: Attempt to coerce result from Failure")
    }

    fun coerceFailure(): Throwable = when (this) {
        is Success -> throw IllegalStateException("Result:: Attempt to coerce failure from Success")
        is Failure -> this.failure
    }

    companion object: Monad<Result<*>>, Functor<Result<*>> {

        fun <A> narrow(ma: HKT<Result<*>, A>): Result<A> = ma as Result<A>

        override fun <A> pure(v: A): HKT<Result<*>, A> = Success(v)

        override fun <A, B> map(ma: HKT<Result<*>, A>, f: (A) -> B): HKT<Result<*>, B> = when (val a = narrow(ma)) {
            is Success -> Success(f(a.value))
            is Failure -> Failure(a.failure)
        }

        override fun <A, B> flatMap(ma: HKT<Result<*>, A>, f: (A) -> HKT<Result<*>, B>): HKT<Result<*>, B> = when (val a = narrow(ma)) {
            is Success -> f(a.value)
            is Failure -> Failure(a.failure)
        }

    }
}