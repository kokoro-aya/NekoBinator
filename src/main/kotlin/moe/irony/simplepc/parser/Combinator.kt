package moe.irony.simplepc.parser

import moe.irony.simplepc.instances.Either
import moe.irony.simplepc.instances.Option
import moe.irony.simplepc.instances.Result
import moe.irony.simplepc.parser.Parser.Companion.`*≻`
import moe.irony.simplepc.parser.Parser.Companion.`≺$≻`
import moe.irony.simplepc.parser.Parser.Companion.`≺*`
import moe.irony.simplepc.parser.Parser.Companion.`≺*≻`
import moe.irony.simplepc.parser.Parser.Companion.`≺|≻`
import moe.irony.simplepc.parser.Parser.Companion.`≻≻=`
import moe.irony.simplepc.parser.Parser.Companion.`≻≻`
import moe.irony.simplepc.parser.Parser.Companion.attempt
import moe.irony.simplepc.parser.Parser.Companion.pure
import moe.irony.simplepc.types.HKT
import moe.irony.simplepc.types.Tuple0
import moe.irony.simplepc.utils.*

// Combinators

@Deprecated("Use asterisk() instead")
fun <A, B, C> tryAndThen(p: HKT<Parser<*>, A>, q: HKT<Parser<*>, B>, f: (A, B) -> C, or: (B) -> C): HKT<Parser<*>, C> =
    (p `≻≻=` { x -> q `≻≻=` { y -> pure(f(x, y)) }}) `≺|≻` (q `≻≻=` { pure(or(it)) })

fun <A> tryParse(p: HKT<Parser<*>, A>): HKT<Parser<*>, A> = attempt(p) // FIXED

fun <A> choice(desc: String, ps: List<HKT<Parser<*>, A>>): HKT<Parser<*>, A> =
    ps.reduce(Parser.Companion::alt)

fun <A, B, C> between(open: HKT<Parser<*>, B>, p: HKT<Parser<*>, A>, close: HKT<Parser<*>, C>): HKT<Parser<*>, A> =
    open `*≻` p `≺*` close

fun <A> orElse(p: HKT<Parser<*>, A>, opt: A): HKT<Parser<*>, A> =
    p `≺|≻` pure(opt)

fun <A> optional(p: HKT<Parser<*>, A>): HKT<Parser<*>, Option<A>> = TODO("no idea how to implement")
//    orElse(empty(), Option.narrow(Option.liftM1(Option.Some(p))))

fun <A> tryDiscard(p: HKT<Parser<*>, A>): HKT<Parser<*>, Tuple0> =
    p `≻≻=` { _ ->
        pure(Tuple0)
    } `≺|≻` pure(Tuple0)

fun <A> replicate(n: Int, p: HKT<Parser<*>, A>): HKT<Parser<*>, List<A>> =
    if (n <= 0) Parser.pure(listOf<A>())
    else Parser.narrow(p) `≻≻=` { a ->
        recur { replicate(n - 1, p) } `≻≻=` { ax ->
            Parser.pure(cons<A>()(a)(ax))
        }
    }

fun <A, B> runLoop(a: A, f: (A) -> HKT<Parser<*>, Either<B, A>>): HKT<Parser<*>, B> =
    f.invoke(a) `≻≻=` { ei -> when (ei) {
        is Either.Left -> pure(ei.a)
        is Either.Right -> recur { runLoop(ei.b, f) }
    } }

fun <A> many(p: HKT<Parser<*>, A>): HKT<Parser<*>, List<A>> =
    recur { many1(p) } `≺|≻` Parser.pure(listOf())

fun <A> many1(p: HKT<Parser<*>, A>): HKT<Parser<*>, List<A>> =
    cons<A>() `≺$≻` p `≺*≻` recur { many(p) }

infix fun <A, S> (HKT<Parser<*>, A>).sepBy(s: HKT<Parser<*>, S>): HKT<Parser<*>, List<A>> =
    recur { this sepBy1 s } `≺|≻` Parser.pure(listOf())

infix fun <A, S> (HKT<Parser<*>, A>).sepBy1(s: HKT<Parser<*>, S>): HKT<Parser<*>, List<A>> =
    this `≻≻=` { a ->
        recur {
            many(s `≻≻` this) `≻≻=` { xs ->
                pure(cons<A>()(a)(xs))
        } }
    }

fun <A> skipMany(p: HKT<Parser<*>, A>): HKT<Parser<*>, Tuple0> =
    Parser.pure(Tuple0) `≺*` recur { many(p) }

fun <A> skipMany1(p: HKT<Parser<*>, A>): HKT<Parser<*>, Tuple0> =
    p `*≻` recur { skipMany(p) }

infix fun <A, B> (HKT<Parser<*>, A>).endBy(sep: HKT<Parser<*>, B>): HKT<Parser<*>, List<A>> =
    recur { many(this `≻≻=` { a ->
        sep `≻≻=` { _ ->
            pure(a)
        }
    }) }

infix fun <A, B> (HKT<Parser<*>, A>).endBy1(sep: HKT<Parser<*>, B>): HKT<Parser<*>, List<A>> =
    recur { many1(this `≻≻=` { a ->
        sep `≻≻=` { _ ->
            pure(a)
        }
    }) }

infix fun <A, B> (HKT<Parser<*>, A>).endByOptional(sep: HKT<Parser<*>, B>): HKT<Parser<*>, List<A>> =
    recur { this endByOptional1 sep } `≺|≻` pure(listOf<A>())

infix fun <A, B> (HKT<Parser<*>, A>).endByOptional1(sep: HKT<Parser<*>, B>): HKT<Parser<*>, List<A>> =
    this `≻≻=` { x ->
        sep `≻≻=` { _ ->
            recur { this endByOptional sep } `≻≻=` { xs ->
                pure(cons<A>()(x)(xs))
            }
        } `≺|≻` pure(listOf(x))
    }

fun <A> chainl(p: HKT<Parser<*>, A>, op: HKT<Parser<*>, (A, A) -> A>, x: A): HKT<Parser<*>, A> =
    chainl1(p, op) `≺|≻` pure(x)

fun <A> chainl1(p: HKT<Parser<*>, A>, op: HKT<Parser<*>, (A, A) -> A>): HKT<Parser<*>, A> =
    p `≻≻=` { x ->
        yCombinator<A, HKT<Parser<*>, A>> { recur ->
            { x ->
                (op `≻≻=` { f ->
                    p `≻≻=` { y ->
                        recur(f(x, y))
                    }
                }) `≺|≻` Parser.pure(x)
            } }(x)
    }

fun <A> notFollowedBy(p: HKT<Parser<*>, A>): HKT<Parser<*>, Tuple0> = TODO("Not yet implemented")

fun <A, B> manyTill(p: HKT<Parser<*>, A>, end: HKT<Parser<*>, B>): HKT<Parser<*>, List<A>> = TODO("Not yet implemented")

fun <A> lookAhead(p: HKT<Parser<*>, A>): HKT<Parser<*>, A> =
    Parser { ps ->
        Parser.narrow(p).runParser(ps) `≻≻=` { res ->
            when (res) {
                is Result.Success -> Trampoline.done(Result.Success(Context(ps, false, res.value.result)));
                is Result.Failure -> Trampoline.done(Result.Failure(res.failure))
            }
        }
    }

// Applications

