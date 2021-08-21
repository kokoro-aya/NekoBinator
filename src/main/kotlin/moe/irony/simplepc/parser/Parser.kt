package moe.irony.simplepc.parser

import moe.irony.simplepc.instances.Option
import moe.irony.simplepc.instances.Result
import moe.irony.simplepc.types.HKT
import moe.irony.simplepc.types.Monad
import moe.irony.simplepc.utils.Trampoline
import moe.irony.simplepc.utils.`≻≻=`

// Mainly referenced from: https://academy.realm.io/posts/tryswift-yasuhiro-inami-parser-combinator/

class Parser<A>(val parser: (ParseState) -> Trampoline<Result<Context<A>>>): HKT<Parser<*>, A> {

    fun runParser(s: ParseState) = parser.invoke(s)

    fun parse(s: String): Result<A> = Result.narrow(
        Result.flatMap((runParser(ParseState(s)).run())) { Result.pure(it.result) }
    )

    companion object: Monad<Parser<*>> {

        fun <A> narrow(v: HKT<Parser<*>, A>): Parser<A> = v as Parser<A>

        // Applicative pure
        override fun <A> pure(v: A): HKT<Parser<*>, A> = Parser { Trampoline.done(Result.Success(Context(it, consumed = false, v))) }

        // Alternative empty
        fun <A> empty(): HKT<Parser<*>, A> = Parser { Trampoline.done(Result.Failure(FailContext(it, consumed = false, halted = false))) }

        fun <A> attempt(p: HKT<Parser<*>, A>): HKT<Parser<*>, A> = // FIXED
            Parser { ps ->
                Parser.narrow(p).runParser(ps) `≻≻=` { res ->
                    when (res) {
                        is Result.Success -> Trampoline.done(res)
                        is Result.Failure -> Trampoline.done(Result.Failure(FailContext(ps, consumed = false, res.coerceAbort())))
                    }
                }
            }

        // Monad bind (>>=)
        override fun <A, B> flatMap(ma: HKT<Parser<*>, A>, f: (A) -> HKT<Parser<*>, B>): HKT<Parser<*>, B> =
            Parser { ps ->
                narrow(ma).runParser(ps) `≻≻=` { res1 ->
                    when (res1) {
                        is Result.Success -> { val (pps, cc, rs) = res1.value
                            narrow(f.invoke(rs)).runParser(pps) `≻≻=` { res2 ->
                                Trampoline.more {
                                    when (res2) {
                                        is Result.Success -> Trampoline.done(
                                            Result.Success(res2.let { val (p2s, cc2, rs2) = res2.value
                                                Context(p2s, cc || cc2, rs2)
                                            }))
                                        is Result.Failure -> Trampoline.done(res2.let { val (p2s, cc2, hlt) = it.failure as FailContext
                                            Result.Failure(FailContext(p2s, cc2, hlt)) // Should we do this?
                                        })
                                    }
                                }
                            }
                        }
                        is Result.Failure -> Trampoline.done(Result.Failure(res1.failure))
                    }
                }
            }

        infix fun <A, B> HKT<Parser<*>, A>.`≻≻=`(f: (A) -> HKT<Parser<*>, B>): HKT<Parser<*>, B> =
            flatMap(this, f)

        // Functor fmap (<$>)
        fun <A, B> fmap(ma: HKT<Parser<*>, A>, f: (A) -> B): HKT<Parser<*>, B> =
            flatMap(ma) {
                pure(f.invoke(it))
            }

        infix fun <A, B> ((A) -> B).`≺$≻`(p: HKT<Parser<*>, A>): HKT<Parser<*>, B> =
            fmap(p, this)

        fun <A, B, C> (HKT<Parser<*>, A>).combine(p: HKT<Parser<*>, B>, f: (A, B) -> C): HKT<Parser<*>, C> =
            this `≻≻=` { a -> p `≻≻=` { b -> pure(f.invoke(a, b)) }}

        // Alternative choice (associative operation)
        fun <A> alt(p: HKT<Parser<*>, A>, q: HKT<Parser<*>, A>): HKT<Parser<*>, A> =
            Parser { ps ->
                narrow(p).runParser(ps) `≻≻=` { res1 ->
                    when (res1) {
                        is Result.Success -> Trampoline.done(res1)
                        is Result.Failure -> {
                            if (res1.getConsumed() || res1.coerceAbort())
                                Trampoline.done(res1)
                            else
                                Trampoline.more {
                                    narrow(q).runParser(ps) `≻≻=` { res2 ->
                                        when (res2) {
                                            is Result.Success -> Trampoline.done(res2)
                                            is Result.Failure ->
                                                Trampoline.done(Result.Failure(FailContext(res2.getParseState(), res2.getConsumed(), res2.coerceAbort())))
                                        }
                                    }
                                }
                        }
                    }
                }
            }

        infix fun <A> HKT<Parser<*>, A>.`≺|≻`(q: HKT<Parser<*>, A>): HKT<Parser<*>, A> =
            alt(this, q)

        // Applicative sequential application
        fun <A, B> ap(p: HKT<Parser<*>, A>, f: HKT<Parser<*>, (A) -> B>): HKT<Parser<*>, B> =
            f `≻≻=` { it `≺$≻` p }

        infix fun <A, B> HKT<Parser<*>, (A) -> B>.`≺*≻`(f: HKT<Parser<*>, A>): HKT<Parser<*>, B> =
            ap(f, this)

        // Sequence actions, discarding the value of the second argument
        infix fun <A, B> HKT<Parser<*>, A>.`≺*`(q: HKT<Parser<*>, B>): HKT<Parser<*>, A> =
            this `≻≻=` { a -> q `≻≻=` { _ -> Parser.pure(a) } }

        // Sequence actions, discarding the value of the first argument
        infix fun <A, B> HKT<Parser<*>, A>.`*≻`(q: HKT<Parser<*>, B>): HKT<Parser<*>, B> =
            this `≻≻=` { _ -> q `≻≻=` { b -> Parser.pure(b) } }

        fun <A> sum(vararg p: HKT<Parser<*>, A>): HKT<Parser<*>, A> =
            p.reduce(Companion::alt)
    }
}


fun <A> recur(f: () -> HKT<Parser<*>, A>): HKT<Parser<*>, A> = Parser { ps ->
    Trampoline.more { Parser.narrow(f.invoke()).parser.invoke(ps) }
}