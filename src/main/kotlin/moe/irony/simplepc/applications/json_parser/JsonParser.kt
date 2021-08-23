package moe.irony.simplepc.applications.json_parser

import moe.irony.simplepc.parser.*
import moe.irony.simplepc.parser.Parser.Companion.`*≻`
import moe.irony.simplepc.parser.Parser.Companion.`≺$≻`
import moe.irony.simplepc.parser.Parser.Companion.`≺*`
import moe.irony.simplepc.parser.Parser.Companion.`≺|≻`
import moe.irony.simplepc.parser.Parser.Companion.`≻≻=`
import moe.irony.simplepc.parser.Parser.Companion.`≻≻`
import moe.irony.simplepc.parser.Parser.Companion.pure
import moe.irony.simplepc.types.HKT
import moe.irony.simplepc.utils.`≻≻=`
import moe.irony.simplepc.utils.constructInt
import moe.irony.simplepc.utils.constructLong
import moe.irony.simplepc.utils.constructString

// ------------ //
//  Whitespace  //
// ------------ //

// Whitespace ::= '' | ' ' | '\n' | '\r' | '\t'

fun Whitespace(): HKT<Parser<*>, Char> = tryParse(satisfy { it in " \n\r\t" })

// -------- //
//  Number  //
// -------- //

// Number ::= NumberPart Fraction? Exponent?

fun Number() = NumberPart() `≻≻=` { left ->
    orElse(Fraction(), 0.0) `≻≻=` { right ->
        orElse(Exponent(), 1) `≻≻=` { exp ->
            pure((0 until exp).fold(0.0) { acc, _ -> acc * 10 } * (left + right))
        }
    }
}

// NumberPart ::= ('-')? 0 | [1-9] Digit*

fun NumberPart(): HKT<Parser<*>, Long> = orElse(matchChar('-'), '+') `≻≻=` { ch ->
    ({ c: Char -> (c - '0').toLong() } `≺$≻` matchChar('0')) `≺|≻`
            ((List<Char>::constructLong) `≺$≻` many(satisfy { it in '0' .. '9' })) `≻≻=` { num ->
        if (ch == '-') pure(num.unaryMinus()) else pure(num)
    }
}

// Fraction ::= '.' Digit+

fun Fraction(): HKT<Parser<*>, Double> = (matchChar('.') `*≻`
        ({ xs: List<Char> -> xs
            .foldRight(0.0 to 0.1) { i, (a, b) -> a + b * (i - '0') to b * 0.1 }.first } `≺$≻`
                (  many(satisfy { it in '0' .. '9' }))))

// Exponent ::= ('E' | 'e') ('-' | '+')? Digit+

fun Exponent(): HKT<Parser<*>, Int> = (matchChar('E') `≺|≻` matchChar('e')) `*≻`
        ((matchChar('+') `≺|≻` matchChar('-')) `≻≻=` { sig ->
            ((List<Char>::constructInt) `≺$≻` many(satisfy { it in '0' .. '9' })) `≻≻=` { num ->
                if (sig == '-') pure(num.unaryMinus()) else pure(num)
        } } )

// -------- //
//  String  //
// -------- //

// String ::= '"' StringFragment? '"'

fun String(): HKT<Parser<*>, String> = (List<Char>::constructString) `≺$≻` (matchChar('"') `*≻`
        many(StringFragment()) `≺*` matchChar('"'))

// StringFragment ::= ~('"' | '\') | '\' (
//     '"' | '\' | '/' | '\b' | '\f' | '\n' | '\r' | '\t' | ('\u' HexDigit{4})
// )

fun matchHex() = satisfy { it in "0123456789abcdefABCDEF" }

fun StringFragment() = satisfy { it !in "\"\\" } `≺|≻` (matchChar('\\') `*≻`
        satisfy { it in listOf('"', '\\', '/', '\b', '\n', '\r', '\t') } `≺|≻`
            ({ xx: List<Char> ->
                xx.foldRight(0) { a, b -> (a - '0') + b * 10 }.toChar()

            } `≺$≻` replicate(4, matchHex()))
        )

// ------- //
//  Value  //
// ------- //

// Value ::= Whitespace (String | Number | Object | Array | 'true' | 'false' | 'null') Whitespace

fun Value(): HKT<Parser<*>, JsonValue> =
    recur {
        Whitespace() `*≻` // 这里如果调用的Parser是比如说Parser<JsonObject>，一个JsonValue的子类，会转子型失败
        // 解决方案是把所有转为数据结构的步骤都放在这一层处理
        (Parser.empty<JsonValue>() `≺|≻` ((::JsonString) `≺$≻` String()) `≺|≻` ((::JsonNumber) `≺$≻` Number()) `≺|≻`
        ({ xx: List<Pair<String, JsonValue>> ->
            JsonObject(xx.associate { it }) } `≺$≻` Object()) `≺|≻`
        ((::JsonArray) `≺$≻` Array()) `≺|≻`
        (matchString("true") `≻≻` pure(JsonBool(true))) `≺|≻`
        (matchString("false") `≻≻` pure(JsonBool(false))) `≺|≻` // TODO 测试这里matchString的行为
        (matchString("null") `≻≻` pure(JsonNull))) `≺*` Whitespace()
    }

// ------- //
//  Array  //
// ------- //

// Array ::= '[' (Whitespace | Value (',' Value)*) ']'

fun Array(): HKT<Parser<*>, List<JsonValue>> = matchChar('[') `*≻`
        (((Value() sepBy1 matchChar(',')) `≺|≻` (Whitespace() `≻≻` pure(listOf())))) `≺*` // 把顺序调过来就不用显式注明类型了
        matchChar(']')

// -------- //
//  Object  //
// -------- //

// Object ::= '{' (Whitespace | ObjectFragment (',' ObjectFragment)*) ']'

fun Object(): HKT<Parser<*>, List<Pair<String, JsonValue>>> = matchChar('{') `*≻`
        ((ObjectFragment() sepBy1 matchChar(',')) `≺|≻` (Whitespace() `≻≻` pure(listOf()))) `≺*`
        matchChar('}')

// ObjectFragment ::= Whitespace String Whitespace ':' Value

fun ObjectFragment(): HKT<Parser<*>, Pair<String, JsonValue>> =
    Whitespace() `*≻` (String() `≺*` Whitespace() `≺*` matchChar(':')) `≻≻=` { label ->
        Value() `≻≻=` { value ->
            pure(label to value)
        }
    }