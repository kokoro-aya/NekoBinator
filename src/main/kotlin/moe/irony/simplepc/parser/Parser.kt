package moe.irony.simplepc.parser

import moe.irony.simplepc.instances.Option
import moe.irony.simplepc.types.HKT
import moe.irony.simplepc.types.Monad
import moe.irony.simplepc.utils.Trampoline
import moe.irony.simplepc.utils.`≻≻=`

// Mainly referenced from: https://academy.realm.io/posts/tryswift-yasuhiro-inami-parser-combinator/

class Parser<A>(val parser: (ParseState) -> Trampoline<Option<Pair<A, ParseState>>>): HKT<Parser<*>, A> {

    fun runParser(s: ParseState) = parser.invoke(s)

    fun parse(s: String): Option<A> = Option.narrow(
        Option.flatMap((runParser(ParseState(s)).run())) { Option.pure(it.first) }
    )

    companion object: Monad<Parser<*>> {

        fun <A> narrow(v: HKT<Parser<*>, A>): Parser<A> = v as Parser<A>

        // Applicative pure
        override fun <A> pure(v: A): HKT<Parser<*>, A> = Parser { Trampoline.done(Option.Some(v to it)) }

        // Alternative empty
        fun <A> empty(): HKT<Parser<*>, A> = Parser { Trampoline.done(Option.None) }

        // Monad bind (>>=)
        override fun <A, B> flatMap(ma: HKT<Parser<*>, A>, f: (A) -> HKT<Parser<*>, B>): HKT<Parser<*>, B> =
            Parser { ps ->
                narrow(ma).runParser(ps) `≻≻=` { res1 ->
                    when (res1) {
                        is Option.Some -> {
                            narrow(f.invoke(res1.value.first)).runParser(res1.value.second) `≻≻=` { res2 ->
                                Trampoline.more {
                                    when (res2) {
                                        is Option.Some -> Trampoline.done(Option.Some(res2.value))
                                        is Option.None -> Trampoline.done(Option.None)
                                    }
                                }
                            }
                        }
                        is Option.None -> Trampoline.done(Option.None)
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
                        is Option.Some -> Trampoline.done(res1)
                        is Option.None -> narrow(q).runParser(ps)
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
    }
}


fun <A> recur(f: () -> HKT<Parser<*>, A>): HKT<Parser<*>, A> = Parser { ps ->
    Trampoline.more { Parser.narrow(f.invoke()).parser.invoke(ps) }
}