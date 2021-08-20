package moe.irony.simplepc.parser

import moe.irony.simplepc.instances.Option
import moe.irony.simplepc.parser.Parser.Companion.`*≻`
import moe.irony.simplepc.parser.Parser.Companion.`≺$≻`
import moe.irony.simplepc.parser.Parser.Companion.`≺*`
import moe.irony.simplepc.parser.Parser.Companion.`≺*≻`
import moe.irony.simplepc.parser.Parser.Companion.`≺|≻`
import moe.irony.simplepc.parser.Parser.Companion.`≻≻=`
import moe.irony.simplepc.parser.Parser.Companion.combine
import moe.irony.simplepc.parser.Parser.Companion.pure
import moe.irony.simplepc.types.HKT
import moe.irony.simplepc.types.Tuple0
import moe.irony.simplepc.utils.Trampoline
import moe.irony.simplepc.utils.`≻≻=`
import moe.irony.simplepc.utils.cons
import moe.irony.simplepc.utils.yCombinator

// Basic Parsers

fun identity(): Parser<Char> = Parser {
    Trampoline.done(
        if (it.hasNext())
            Option.Some(it.peek() to it.next())
        else
            Option.None)
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
    if (str.isEmpty()) Parser.pure(str) // 这里不能用Parser.empty()否则会出现None导致整个调用栈被污染
    else matchChar(str.first()) `≻≻=` { c -> matchString(str.drop(1)) `≻≻=` { cs -> Parser.pure(c + cs) } }

fun anyString(): HKT<Parser<*>, String> =
    { ll: List<Char> -> ll.foldRight("") { i, x -> x + i } } `≺$≻` many1(isLetter())

// Combinators

fun <A, B, C> tryAndThen(p: HKT<Parser<*>, A>, q: HKT<Parser<*>, B>, f: (A, B) -> C, or: (B) -> C): HKT<Parser<*>, C> =
    (p `≻≻=` { x -> q `≻≻=` { y -> pure(f(x, y)) }}) `≺|≻` (q `≻≻=` { pure(or(it)) })

fun <A> tryParse(p: HKT<Parser<*>, A>): HKT<Parser<*>, A> = // TODO("Encountered a problem")
    Parser { ps ->
        Parser.narrow(p).runParser(ps) `≻≻=` { res ->
            when (res) {
                is Option.Some -> Trampoline.done(res)
                is Option.None -> Trampoline.done(Option.None)
            }
        }
    } `≺|≻` p

fun <A> choice(desc: String, ps: List<HKT<Parser<*>, A>>): HKT<Parser<*>, A> = TODO("Not yet implemented")

fun <A> count(n: Int, p: HKT<Parser<*>, A>): HKT<Parser<*>, List<A>> = TODO("Not yet implemented")

fun <A, B, C> between(open: HKT<Parser<*>, B>, p: HKT<Parser<*>, A>, close: HKT<Parser<*>, C>): HKT<Parser<*>, A> = TODO("Not yet implemented")

fun <A> orElse(p: HKT<Parser<*>, A>, opt: A): HKT<Parser<*>, A> =
    p `≺|≻` pure(opt)

fun <A> optional(p: HKT<Parser<*>, A>): HKT<Parser<*>, Option<A>> = TODO("Not yet implemented")

fun <A> tryDiscard(p: HKT<Parser<*>, A>): HKT<Parser<*>, Tuple0> = TODO("Not yet implemented")

fun <A> replicate(n: Int, p: HKT<Parser<*>, A>): HKT<Parser<*>, List<A>> =
    if (n <= 0) Parser.pure(listOf<A>())
    else Parser.narrow(p) `≻≻=` { a ->
        recur { replicate(n - 1, p) } `≻≻=` { ax ->
            Parser.pure(cons<A>()(a)(ax))
        }
    }

fun <A> many(p: HKT<Parser<*>, A>): HKT<Parser<*>, List<A>> =
    recur { many1(p) `≺|≻` Parser.pure(listOf()) }

fun <A> many1(p: HKT<Parser<*>, A>): HKT<Parser<*>, List<A>> =
    recur { cons<A>() `≺$≻` p `≺*≻` many(p) }

fun <A, S> sepBy(p: HKT<Parser<*>, A>, s: HKT<Parser<*>, S>): HKT<Parser<*>, List<A>> = TODO("Not yet implemented")

fun <A, S> sepBy1(p: HKT<Parser<*>, A>, s: HKT<Parser<*>, S>): HKT<Parser<*>, List<A>> = TODO("Not yet implemented")

fun <A> skipMany(p: HKT<Parser<*>, A>): HKT<Parser<*>, Tuple0> =
    recur { Parser.pure(Tuple0) `≺*` many(p) }

fun <A> skipMany1(p: HKT<Parser<*>, A>): HKT<Parser<*>, Tuple0> =
    recur { p `*≻` skipMany(p) }

fun <A, B> endBy(p: HKT<Parser<*>, A>, sep: HKT<Parser<*>, B>): HKT<Parser<*>, List<A>> = TODO("Not yet implemented")

fun <A, B> endBy1(p: HKT<Parser<*>, A>, sep: HKT<Parser<*>, B>): HKT<Parser<*>, List<A>> = TODO("Not yet implemented")

fun <A, B> endByOptional(p: HKT<Parser<*>, A>, sep: HKT<Parser<*>, B>): HKT<Parser<*>, List<A>> = TODO("Not yet implemented")

fun <A, B> endByOptional1(p: HKT<Parser<*>, A>, sep: HKT<Parser<*>, B>): HKT<Parser<*>, List<A>> = TODO("Not yet implemented")

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

fun <A> lookAhead(p: HKT<Parser<*>, A>): HKT<Parser<*>, A> = TODO("Not yet implemented")

// Applications

fun skipSpaces(): HKT<Parser<*>, Tuple0> =
    skipMany(spaces())

fun symbol(str: String): HKT<Parser<*>, String> =
    skipSpaces() `*≻` matchString(str) `≺*` skipSpaces()

fun natural(): HKT<Parser<*>, Int> =
    skipSpaces() `*≻` ({
            ll: List<Char> -> ll.foldRight(0) { i, x -> x * 10 + (i - '0') } } `≺$≻` many1(isDigit())) `≺*` skipSpaces()

fun real(): HKT<Parser<*>, Double> =
    skipSpaces() `*≻` natural().combine({ ll: List<Char> ->
        ll.foldRight(0.0 to 0.1) { i, (x, y) -> x + y * (i - '0').toDouble() to y * 0.1 }.first } `≺$≻`
            (matchChar('.') `*≻` many1(isDigit())) `≺*` skipSpaces(), Int::plus) `≺|≻` ((Int::toDouble) `≺$≻` natural())
