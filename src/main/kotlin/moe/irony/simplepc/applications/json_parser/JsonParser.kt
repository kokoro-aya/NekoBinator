package moe.irony.simplepc.applications.json_parser

import moe.irony.simplepc.parser.*
import moe.irony.simplepc.parser.Parser.Companion.`*≻`
import moe.irony.simplepc.parser.Parser.Companion.`≺$≻`
import moe.irony.simplepc.parser.Parser.Companion.`≺*`
import moe.irony.simplepc.parser.Parser.Companion.`≺|≻`
import moe.irony.simplepc.parser.Parser.Companion.`≻≻=`
import moe.irony.simplepc.parser.Parser.Companion.`≻≻`
import moe.irony.simplepc.parser.Parser.Companion.empty
import moe.irony.simplepc.parser.Parser.Companion.pure
import moe.irony.simplepc.types.HKT
import moe.irony.simplepc.types.Tuple0
import moe.irony.simplepc.utils.*
import moe.irony.simplepc.utils.cons
import moe.irony.simplepc.utils.constructInt
import moe.irony.simplepc.utils.constructLong
import moe.irony.simplepc.utils.constructString

// ------------ //
//  Whitespace  //
// ------------ //

// Whitespace ::= '' | ' ' | '\n' | '\r' | '\t'

private fun Whitespace(): HKT<Parser<*>, Tuple0> = skipMany(satisfy { it in " \n\r\t" })

// -------- //
//  Number  //
// -------- //

// Number ::= NumberPart Fraction? Exponent?

private fun Number() = NumberPart() `≻≻=` { left ->
    orElse(Fraction(), 0.0) `≻≻=` { right ->
        orElse(Exponent(), 0) `≻≻=` { exp ->
            if (exp >= 0)
                pure((0 until exp).fold(1.0) { acc, _ -> acc * 10 } * (left + right))
            else
                pure((left + right) / (exp until 0).fold(1.0) { acc, _ -> acc * 10 })
        }
    }
}

// NumberPart ::= ('-')? 0 | [1-9] Digit*

private fun NumberPart(): HKT<Parser<*>, Long> = orElse(matchChar('-'), '+') `≻≻=` { ch ->
    ({ c: Char -> (c - '0').toLong() } `≺$≻` matchChar('0')) `≺|≻`
        ((List<Char>::constructLong) `≺$≻` (satisfy { it in '0' .. '9' } `≻≻=` { car ->
            many(isDigit()) `≻≻=` { cdr ->
                pure(cons<Char>()(car)(cdr))
            }})) `≻≻=` { num ->
        if (ch == '-') pure(num.unaryMinus()) else pure(num)
    }
}

// Fraction ::= '.' Digit+

private fun Fraction(): HKT<Parser<*>, Double> = matchChar('.') `*≻`
    ({ xs: List<Char> -> xs
        .foldRight(0.0 to 0.1) { i, (a, b) -> a + b * (i - '0') to b * 0.1 }.first } `≺$≻`
            many1(satisfy { it in '0' .. '9' }))

// Exponent ::= ('E' | 'e') ('-' | '+')? Digit+

private fun Exponent(): HKT<Parser<*>, Int> = (matchChar('E') `≺|≻` matchChar('e')) `*≻`
        (orElse(matchChar('+') `≺|≻` matchChar('-'), '+') `≻≻=` { sig ->
            ((List<Char>::constructInt) `≺$≻` many(satisfy { it in '0' .. '9' })) `≻≻=` { num ->
                if (sig == '-') pure(num.unaryMinus()) else pure(num)
        } } )

// -------- //
//  String  //
// -------- //

// String ::= '"' StringFragment? '"'

private fun String(): HKT<Parser<*>, String> = (List<Char>::constructString) `≺$≻` (matchChar('"') `*≻`
        many(StringFragment()) `≺*` matchChar('"'))

// StringFragment ::= ~('"' | '\') | '\' (
//     '"' | '\' | '/' | '\b' | '\f' | '\n' | '\r' | '\t' | ('\u' HexDigit{4})
// )

private fun matchHex() = satisfy { it in "0123456789abcdefABCDEF" }

private fun StringFragment() = satisfy { it != '"' }

//private fun StringFragment() = satisfy { it !in "\"\\" } `≺|≻` (matchChar('\\') `*≻`
//        ((satisfy { it in listOf('"', '\\', '/', 'b', 'n', 'r', 't') } `≻≻=` { ch: Char ->
//            when (ch) {
//                '"' -> pure('"')
//                '\\' -> pure('\\')
//                '/' -> pure('/')
//                'b' -> pure('\b')
//                'n' -> pure('\n')
//                'r' -> pure('\r')
//                't' -> pure('\t')
//                else -> empty()
//            } }) `≺|≻`
//            (matchChar('u') `*≻` ({ xx: List<Char> ->
//                xx.foldRight(0) { a, b -> a.digitToInt(16) + b * 10 }.toChar()
//            } `≺$≻` replicate(4, matchHex()))))
//        )

// ------- //
//  Value  //
// ------- //

// Value ::= Whitespace (String | Number | Object | Array | 'true' | 'false' | 'null') Whitespace

private fun Value(): HKT<Parser<*>, JsonValue> =
    recur {
        Whitespace() `*≻` // 这里如果调用的Parser是比如说Parser<JsonObject>，一个JsonValue的子类，会转子型失败
        // 解决方案是把所有转为数据结构的步骤都放在这一层处理
        (Parser.empty<JsonValue>() `≺|≻` ((::JsonString) `≺$≻` String()) `≺|≻` ((::JsonNumber) `≺$≻` Number()) `≺|≻`
        ({ xx: List<Pair<String, JsonValue>> ->
            JsonObject(xx.associate { it }) } `≺$≻` Object()) `≺|≻`
        ((::JsonArray) `≺$≻` Array()) `≺|≻`
        (matchString("true") `*≻` pure(JsonBool(true))) `≺|≻`
        (matchString("false") `*≻` pure(JsonBool(false))) `≺|≻` // TODO 测试这里matchString的行为，maybe matchStringStrict
        (matchString("null") `*≻` pure(JsonNull))) `≺*` Whitespace()
    }

// ------- //
//  Array  //
// ------- //

// Array ::= '[' (Whitespace | Value (',' Value)*) ']'

private fun Array(): HKT<Parser<*>, List<JsonValue>> = matchChar('[') `*≻`
        (((Value() sepBy1 matchChar(',')) `≺|≻` (Whitespace() `*≻` pure(listOf())))) `≺*` // 把顺序调过来就不用显式注明类型了
        matchChar(']')

// -------- //
//  Object  //
// -------- //

// Object ::= '{' (Whitespace | ObjectFragment (',' ObjectFragment)*) ']'

private fun Object(): HKT<Parser<*>, List<Pair<String, JsonValue>>> = matchChar('{') `*≻`
        ((ObjectFragment() sepBy1 matchChar(',')) `≺|≻` (Whitespace() `*≻` pure(listOf()))) `≺*`
        matchChar('}')

// ObjectFragment ::= Whitespace String Whitespace ':' Value

private fun ObjectFragment(): HKT<Parser<*>, Pair<String, JsonValue>> =
    Whitespace() `*≻` (String() `≺*` Whitespace() `≺*` matchChar(':')) `≻≻=` { label ->
        Value() `≻≻=` { value ->
            pure(label to value)
        }
    }

fun JsonParser(): Parser<JsonValue> = Parser.narrow(Value())

fun main() {
    println(Parser.narrow(Number()).parse("-0"))
    println(Parser.narrow(Number()).parse("123.45"))
    println(Parser.narrow(Number()).parse("23E2"))
    println(Parser.narrow(Number()).parse("23E+2"))
    println(Parser.narrow(Number()).parse("35.6e-3"))
    println(Parser.narrow(Number()).parse("0.025E3"))

    println(Parser.narrow(Number()).parse("-.25"))
    println(Parser.narrow(Number()).parse("-.E3"))

    println(Parser.narrow(String()).parse("\"abcdefoobar123\""))
    println(Parser.narrow(String()).parse("\"abc\\zzzz\bzzffff\n\teeeee\r3333\""))
    println(Parser.narrow(String()).parse("\"\""))
    println(Parser.narrow(String()).parse("\"\u227a\""))
    println(Parser.narrow(String()).parse("\"abc\u227a\u227B\""))
    println(Parser.narrow(String()).parse("\""))

    println(Parser.narrow(Value()).parse("\"foo\""))
    println(Parser.narrow(Value()).parse("1234e2"))
    println(Parser.narrow(Value()).parse("true"))
    println(Parser.narrow(Value()).parse("false"))
    println(Parser.narrow(Value()).parse("null"))

    println(Parser.narrow(Array()).parse("[123, 45e2, true, null]"))
    println(Parser.narrow(Array()).parse("[[]]"))

    println(Parser.narrow(Object()).parse("{\"foo\":3, \"bar\": true}"))
    println(Parser.narrow(Object()).parse("{\"foo\":[3,false,4.5], \"bar\": {\"foo\": \"bar\"}}"))

}