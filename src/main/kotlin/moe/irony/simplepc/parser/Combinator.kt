package moe.irony.simplepc.parser

import moe.irony.simplepc.instances.Option
import moe.irony.simplepc.parser.Parser.Companion.`*≻`
import moe.irony.simplepc.parser.Parser.Companion.`≺$≻`
import moe.irony.simplepc.parser.Parser.Companion.`≺*`
import moe.irony.simplepc.parser.Parser.Companion.`≺*≻`
import moe.irony.simplepc.parser.Parser.Companion.`≺|≻`
import moe.irony.simplepc.parser.Parser.Companion.`≻≻=`
import moe.irony.simplepc.types.HKT
import moe.irony.simplepc.types.Tuple0
import moe.irony.simplepc.utils.Trampoline
import moe.irony.simplepc.utils.cons
import moe.irony.simplepc.utils.yCombinator

fun identity(): Parser<Char> = Parser {
    Trampoline.done(
        if (it.hasNext())
            Option.Some(it.peek() to it.next())
        else
            Option.None)
}

fun satisfy(pred: (Char) -> Boolean): Parser<Char> = Parser.narrow(
    identity() `≻≻=` {
        if (pred.invoke(it)) Parser.pure(it) else Parser.empty()
    }
)

fun anyChar(): Parser<Char> = satisfy { true }
fun isDigit(): Parser<Char> = satisfy { it in '0' .. '9' }
fun matchChar(c: Char): Parser<Char> = satisfy { it == c }
fun spaces(): Parser<Char> = satisfy { it.isWhitespace() }

fun matchString(str: String): HKT<Parser<*>, String> =
    if (str.isEmpty()) Parser.pure(str)
    else matchChar(str.first()) `≻≻=` { c -> matchString(str.drop(1)) `≻≻=` { cs -> Parser.pure(c + cs) } }


fun <A> replicate(n: Int, p: HKT<Parser<*>, A>): HKT<Parser<*>, List<A>> =
    if (n <= 0) Parser.narrow(Parser.pure(listOf<A>()))
    else Parser.narrow(Parser.narrow(p) `≻≻=` { a ->
        recur { replicate(n - 1, p) } `≻≻=` { ax ->
            Parser.pure(cons<A>()(a)(ax))
        }
    })

fun <A> many(p: HKT<Parser<*>, A>): HKT<Parser<*>, List<A>> =
    recur { many1(p) `≺|≻` Parser.pure(listOf()) }

fun <A> many1(p: HKT<Parser<*>, A>): HKT<Parser<*>, List<A>> =
    recur { cons<A>() `≺$≻` p `≺*≻` many(p) }

fun <A> skipMany(p: HKT<Parser<*>, A>): HKT<Parser<*>, Tuple0> =
    recur { Parser.pure(Tuple0) `≺*` many(p) }

fun <A> skipMany1(p: HKT<Parser<*>, A>): HKT<Parser<*>, Tuple0> =
    recur { p `*≻` skipMany(p) }

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

fun skipSpaces(): HKT<Parser<*>, Tuple0> =
    skipMany(spaces())

fun symbol(str: String): HKT<Parser<*>, String> =
    skipSpaces() `*≻` matchString(str) `≺*` skipSpaces()

fun natural(): HKT<Parser<*>, Int> =
    skipSpaces() `*≻` ({
            ll: List<Char> -> ll.foldRight(0) { i, x -> x * 10 + (i - '0') } } `≺$≻` many1(isDigit())) `≺*` skipSpaces()
