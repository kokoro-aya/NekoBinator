package moe.irony.simplepc.applications

import moe.irony.simplepc.instances.Result
import moe.irony.simplepc.parser.*
import moe.irony.simplepc.parser.Parser.Companion.`*≻`
import moe.irony.simplepc.parser.Parser.Companion.combine
import moe.irony.simplepc.parser.Parser.Companion.`≺$≻`
import moe.irony.simplepc.parser.Parser.Companion.`≺*`
import moe.irony.simplepc.parser.Parser.Companion.`≺|≻`
import moe.irony.simplepc.parser.Parser.Companion.`≻≻=`
import moe.irony.simplepc.types.HKT
import moe.irony.simplepc.types.Tuple0
import moe.irony.simplepc.utils.Trampoline
import moe.irony.simplepc.utils.constructInt
import moe.irony.simplepc.utils.constructString

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
fun space(): Parser<Char> = satisfy { it.isWhitespace() }

fun matchString(str: String): HKT<Parser<*>, String> =
    if (str.isEmpty()) Parser.pure(str) // 这里不能用Parser.empty()否则会出现None导致整个调用栈被污染
    else matchChar(str.first()) `≻≻=` { c -> matchString(str.drop(1)) `≻≻=` { cs -> Parser.pure(c + cs) } }

fun matchStringStrict(str: String): HKT<Parser<*>, String> = // 如果我想要匹配一个字符串但不需要用到它的值
    if (str.isEmpty()) Parser.empty()
    else matchChar(str.first()) `≻≻=` { c -> matchString(str.drop(1)) `≻≻=` { cs -> Parser.pure(c + cs) } }

fun anyString(): HKT<Parser<*>, String> =
    (List<Char>::constructString) `≺$≻` many1(isLetter())

// Applications

fun asterisk(p: HKT<Parser<*>, String>): HKT<Parser<*>, String> =
    tryParse(p) `≺|≻` Parser.pure("")

fun skipSpaces(): HKT<Parser<*>, Tuple0> =
    skipMany(space())

fun symbol(str: String): HKT<Parser<*>, String> =
    skipSpaces() `*≻` matchString(str) `≺*` skipSpaces()

fun natural(): HKT<Parser<*>, Int> =
    skipSpaces() `*≻` ((List<Char>::constructInt) `≺$≻` many1(isDigit())) `≺*` skipSpaces()

fun real(): HKT<Parser<*>, Double> =
    skipSpaces() `*≻` natural().combine({ ll: List<Char> ->
        ll.foldRight(0.0 to 0.1) { i, (x, y) -> x + y * (i - '0').toDouble() to y * 0.1 }.first } `≺$≻`
            (matchChar('.') `*≻` many1(isDigit())) `≺*` skipSpaces(), Int::plus) `≺|≻` ((Int::toDouble) `≺$≻` natural())
