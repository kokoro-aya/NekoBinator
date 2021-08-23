package moe.irony.simplepc.utils

import moe.irony.simplepc.instances.Either
import kotlin.random.Random

sealed class Trampoline<A> {
    class Done<A>(val a: A): Trampoline<A>() {
        interface Case<A, R> {
            fun caseDone(a: A): R
        }

        override fun <R> caseOf(caseDone: Case<A, R>, caseMore: More.Case<A, R>, caseFlatMap: FlatMap.Case<A, R>): R =
            caseDone.caseDone(a)
    }
    class More<A>(val ka: () -> Trampoline<A>): Trampoline<A>() {
        interface Case<A, R> {
            fun caseMore(ka: () -> Trampoline<A>): R
        }

        override fun <R> caseOf(caseDone: Done.Case<A, R>, caseMore: Case<A, R>, caseFlatMap: FlatMap.Case<A, R>): R =
            caseMore.caseMore(ka)
    }
    class FlatMap<X, A>(val tx: Trampoline<X>, val ka: (X) -> Trampoline<A>): Trampoline<A>() {
        interface Case<A, R> {
            fun <X> caseFlatMap(tx: Trampoline<X>, ka: (X) -> Trampoline<A>): R
        }

        override fun <R> caseOf(caseDone: Done.Case<A, R>, caseMore: More.Case<A, R>, caseFlatMap: Case<A, R>): R =
            caseFlatMap.caseFlatMap(tx, ka)
    }

    interface Match<A, R>: Done.Case<A, R>, More.Case<A, R>, FlatMap.Case<A, R>
    fun <R> match(match: Match<A, R>): R = caseOf(match, match, match)
    abstract fun <R> caseOf(caseDone: Done.Case<A, R>, caseMore: More.Case<A, R>, caseFlatMap: FlatMap.Case<A, R>): R

    fun <B> map(f: (A) -> B): Trampoline<B> = match(object: Match<A, Trampoline<B>> {
        override fun caseDone(a: A): Trampoline<B> = FlatMap(this@Trampoline, f.andThen(::Done))

        override fun caseMore(ka: () -> Trampoline<A>): Trampoline<B> = FlatMap(this@Trampoline, f.andThen(::Done))

        override fun <X> caseFlatMap(tx: Trampoline<X>, ka: (X) -> Trampoline<A>): Trampoline<B> =
            FlatMap(tx) { x -> ka.invoke(x).map(f) }
    })

    fun <B> applyMap(fab: Trampoline<(A) -> B>): Trampoline<B> = fab.match(object: Match<(A) -> B, Trampoline<B>> {
        override fun caseDone(a: (A) -> B): Trampoline<B> = FlatMap(fab, this@Trampoline::map)

        override fun caseMore(ka: () -> Trampoline<(A) -> B>): Trampoline<B> = FlatMap(fab, this@Trampoline::map)

        override fun <X> caseFlatMap(tx: Trampoline<X>, kf: (X) -> Trampoline<(A) -> B>): Trampoline<B> =
            FlatMap(tx) { x -> kf.invoke(x).flatMap(this@Trampoline::map) }
    })

    fun <B> flatMap(f: (A) -> Trampoline<B>): Trampoline<B> = match(object: Match<A, Trampoline<B>> {
        override fun caseDone(a: A): Trampoline<B> = FlatMap(this@Trampoline, f)

        override fun caseMore(ka: () -> Trampoline<A>): Trampoline<B> = FlatMap(this@Trampoline, f)

        override fun <X> caseFlatMap(tx: Trampoline<X>, ka: (X) -> Trampoline<A>): Trampoline<B> =
            FlatMap(tx) { x -> ka.invoke(x).flatMap(f) }
    })

    fun <A> pure(a: A): Trampoline<A> = done(a)
    fun <A, B> replace(fa: Trampoline<A>, b: B): Trampoline<B> = fa.map { b }
    fun <A> discard(fa: Trampoline<A>): Trampoline<Unit> = fa.map { }

    fun resume(): Either<A, () -> Trampoline<A>> {
        var tco = Either.narrow(Either.pure<Either<A, () -> Trampoline<A>>, Trampoline<A>>(this))
        while (tco.isRight()) {
            tco = tco.coerceRight().match(object: Match<A, Either<Either<A, () -> Trampoline<A>>, Trampoline<A>>> {
                override fun caseDone(a: A): Either<Either<A, () -> Trampoline<A>>, Trampoline<A>> =
                    Either.Left(Either.Left(a))

                override fun caseMore(ka: () -> Trampoline<A>): Either<Either<A, () -> Trampoline<A>>, Trampoline<A>> =
                    Either.Left(Either.Right(ka))

                override fun <X> caseFlatMap(
                    tx: Trampoline<X>, ka: (X) -> Trampoline<A>,
                ): Either<Either<A, () -> Trampoline<A>>, Trampoline<A>> =
                    tx.match(object: Match<X, Either<Either<A, () -> Trampoline<A>>, Trampoline<A>>> {
                        override fun caseDone(a: X): Either<Either<A, () -> Trampoline<A>>, Trampoline<A>> =
                            Either.Right(ka.invoke(a))

                        override fun caseMore(kx: () -> Trampoline<X>): Either<Either<A, () -> Trampoline<A>>, Trampoline<A>> =
                            Either.Left(Either.Right { kx.invoke().flatMap(ka) })

                        override fun <Y> caseFlatMap(
                            ty: Trampoline<Y>,
                            kx: (Y) -> Trampoline<X>
                        ): Either<Either<A, () -> Trampoline<A>>, Trampoline<A>> =
                            Either.Right(ty.flatMap { kx.invoke(it).flatMap(ka) })

                    })

            })
        }
        return tco.coerceLeft()
    }

    private fun run1(): A {
        var tco = Either.narrow(Either.pure<A, Trampoline<A>>(this))
        while (tco.isRight()) {
            tco = when (val ta = tco.coerceRight().resume()) {
                is Either.Left -> Either.Left(ta.a)
                is Either.Right -> Either.Right(ta.b.invoke())
            }
        }
        return tco.coerceLeft()
    }

    private tailrec fun recursiveRun_(tco: Either<A, Trampoline<A>>): A =
        if (tco.isRight())
            recursiveRun_(when (val ta = tco.coerceRight().resume()) {
                is Either.Left -> Either.Left(ta.a)
                is Either.Right -> Either.Right(ta.b.invoke())
            })
        else
            tco.coerceLeft()

    fun run(): A = recursiveRun_(Either.narrow(Either.pure(this)))

    companion object {
        fun <A> done(a: A): Trampoline<A> = Done(a)
        //        fun <A> more(ka: () -> Trampoline<A>): Trampoline<A> = More(ka)
        fun <A> more(ka: () -> Trampoline<A>): Trampoline<A> = More {  ka.invoke() }
    }
}

infix fun <A, B> Trampoline<A>.`≻≻=`(f: (A) -> Trampoline<B>): Trampoline<B> =
    this.flatMap(f)

infix fun <A, B> Trampoline<A>.`≻≻=`(fb: () -> Trampoline<B>): Trampoline<B> =
    this.flatMap { fb.invoke() }

private fun iterate(i: Int): Trampoline<String> =
    if (i <= 0) Trampoline.done("")
    else Trampoline.more { iterate(i - 1) } `≻≻=` { x ->
        Trampoline.done(x + Random.nextInt('a'.toInt(), 'z'.toInt()).toChar() )
    }

private fun slowFib(i: Int): Trampoline<Int> =
    if (i <= 1) Trampoline.done(i)
    else Trampoline.more { slowFib(i - 1) } `≻≻=` { x ->
        Trampoline.more { slowFib(i - 2) } `≻≻=` { y ->
            Trampoline.done(x + y)
        }
    }

fun main() {
//    println(iterate(50).run())

//    val t1 = System.currentTimeMillis()
//    println(slowFib(20).run1())
//    println("Elapsed time: ${System.currentTimeMillis() - t1}")
//
//    val t2 = System.currentTimeMillis()
//    println(slowFib(20).run())
//    println("Elapsed time: ${System.currentTimeMillis() - t2}")
//
//    val t11 = System.currentTimeMillis()
//    println(slowFib(25).run1())
//    println("Elapsed time: ${System.currentTimeMillis() - t11}")
//
//    val t21 = System.currentTimeMillis()
//    println(slowFib(25).run())
//    println("Elapsed time: ${System.currentTimeMillis() - t21}")
//
//    val t12 = System.currentTimeMillis()
//    println(slowFib(30).run1())
//    println("Elapsed time: ${System.currentTimeMillis() - t12}")
//
//    val t22 = System.currentTimeMillis()
//    println(slowFib(30).run())
//    println("Elapsed time: ${System.currentTimeMillis() - t22}")


}