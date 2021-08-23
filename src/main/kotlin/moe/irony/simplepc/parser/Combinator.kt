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
import moe.irony.simplepc.parser.Parser.Companion.combine
import moe.irony.simplepc.parser.Parser.Companion.empty
import moe.irony.simplepc.parser.Parser.Companion.narrow
import moe.irony.simplepc.parser.Parser.Companion.pure
import moe.irony.simplepc.types.HKT
import moe.irony.simplepc.types.Tuple0
import moe.irony.simplepc.types.liftM1
import moe.irony.simplepc.utils.*

// Basic Parsers

fun identity(): Parser<Char> = Parser {
    Trampoline.done(
        if (it.hasNext())
            Result.Success(Context(it.next(), true, it.peek()))
        else
            Result.Failure(FailContext(it, consumed = false, halted = false)))
}

fun eof(): Parser<Char> = TODO("Not yet implemented")

fun satisfy(pred: (Char) -> Boolean): Parser<Char> = Parser.narrow(
    identity() `≻≻=` {
        if (pred.invoke(it)) Parser.pure(it) else Parser.empty()
    }
)

// Text Parsers

fun anyChar(): Parser<Char> = satisfy { true }
fun isDigit(): Parser<Char> = satisfy { it.isDigit() }
fun isLetter(): Parser<Char> = satisfy { it.isLetter() }
fun matchChar(c: Char): Parser<Char> = satisfy { it == c }
fun spaces(): Parser<Char> = satisfy { it.isWhitespace() }

fun matchString(str: String): HKT<Parser<*>, String> =
    if (str.isEmpty()) Parser.pure(str) // 这里不能用Parser.empty()否则会出现None导致整个调用栈被污染 // TODO，似乎有问题，如果我想要匹配一个字符串但不需要用到它的值呢？
    else matchChar(str.first()) `≻≻=` { c -> matchString(str.drop(1)) `≻≻=` { cs -> Parser.pure(c + cs) } }

fun anyString(): HKT<Parser<*>, String> =
    (List<Char>::constructString) `≺$≻` many1(isLetter())

fun asterisk(p: HKT<Parser<*>, String>): HKT<Parser<*>, String> =
    tryParse(p) `≺|≻` pure("")

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

fun skipSpaces(): HKT<Parser<*>, Tuple0> =
    skipMany(spaces())

fun symbol(str: String): HKT<Parser<*>, String> =
    skipSpaces() `*≻` matchString(str) `≺*` skipSpaces()

fun natural(): HKT<Parser<*>, Int> =
    skipSpaces() `*≻` ((List<Char>::constructInt) `≺$≻` many1(isDigit())) `≺*` skipSpaces()

fun real(): HKT<Parser<*>, Double> =
    skipSpaces() `*≻` natural().combine({ ll: List<Char> ->
        ll.foldRight(0.0 to 0.1) { i, (x, y) -> x + y * (i - '0').toDouble() to y * 0.1 }.first } `≺$≻`
            (matchChar('.') `*≻` many1(isDigit())) `≺*` skipSpaces(), Int::plus) `≺|≻` ((Int::toDouble) `≺$≻` natural())
