package moe.irony.simplepc.instances

import moe.irony.simplepc.types.HKT2
import moe.irony.simplepc.types.Monad2

sealed class Either<A, B>: HKT2<Either<*, *>, A, B> {

    class Left<A, B>(val a: A): Either<A, B>()
    class Right<A, B>(val b: B): Either<A, B>()

    fun isLeft(): Boolean = when (this) {
        is Left -> true
        is Right -> false
    }

    fun isRight(): Boolean = when (this) {
        is Left -> false
        is Right -> true
    }

    fun coerceLeft(): A = when (this) {
        is Left -> a
        is Right -> throw IllegalStateException("Either:: coerceLeft() called on Right hand side")
    }


    fun coerceRight(): B = when (this) {
        is Right -> b
        is Left -> throw IllegalStateException("Either:: coerceRight() called on Left hand side")
    }

    companion object: Monad2<Either<*, *>> {
        fun <A, B> narrow(v: HKT2<Either<*, *>, A, B>): Either<A, B> = v as Either<A, B>

        override fun <A, B> pure(v: B): HKT2<Either<*, *>, A, B> = Right(v)

        override fun <A, B, C> flatMap(ma: HKT2<Either<*, *>, A, B>, f: (B) -> HKT2<Either<*, *>, A, C>)
                : HKT2<Either<*, *>, A, C> = when (val v = narrow(ma)) {
            is Left<*, *> -> Left(v.a as A)
            is Right<*, *> -> f.invoke(v.b as B)
        }
    }
}