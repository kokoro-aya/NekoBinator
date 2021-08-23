package moe.irony.simplepc

import moe.irony.simplepc.parser.*
import moe.irony.simplepc.parser.Parser.Companion.`≺$≻`
import moe.irony.simplepc.parser.Parser.Companion.`≺*`
import moe.irony.simplepc.parser.Parser.Companion.`≺|≻`
import moe.irony.simplepc.parser.Parser.Companion.`≻≻=`
import moe.irony.simplepc.parser.Parser.Companion.empty
import moe.irony.simplepc.parser.Parser.Companion.pure
import moe.irony.simplepc.utils.`≻≻=`
import moe.irony.simplepc.utils.constructString

fun isEven(): Parser<Boolean> = TODO("Not implemented yet")

fun bcString(): Parser<String> = Parser.narrow(
    (List<Char>::constructString) `≺$≻` ((natural() `≺*` symbol(":")) `≻≻=` { count ->
        replicate(count, anyChar())
    }))

fun main() {
//    (0 until 100).forEach {
//        println(isEven().parse(it.toString()))
//    }
//    val p = Parser.narrow(tryAndThen(matchString("foo"), matchString("bar"),
//        String::plus) { it })
//
//    println(p.parse("bar"))
//    println(p.parse("foobar"))

//    println(Parser.narrow(many1(isDigit())).parse("1234abc"))
//    println(Parser.narrow(many1(isDigit())).parse("4567abc"))
//    println(Parser.narrow(many1(isDigit())).parse("89111abc"))
//
//    println(Parser.narrow(bcString()).parse("5:abcd"))
//    println(Parser.narrow(bcString()).parse("4:abcd"))
//    println(Parser.narrow(bcString()).parse("3:abcd"))
//
//    println()
//
//    println(Parser.narrow(tryParse(matchString("abc"))).parse("abcdefg"))
//    println(Parser.narrow(tryParse(matchString("abc"))).parse("defg"))
//
//    println()
//
//    println(Parser.narrow(tryParse(matchString(""))).parse("abcdefg"))
//
//    println()
//
//    println(Parser.narrow((matchString("abc") `≺|≻` matchString("")) `≻≻=` { i -> matchString("def") `≻≻=` { j -> pure(i + j) }}).parse("abcdefg"))
//
//    println(Parser.narrow((matchString("abc") `≺|≻` matchString("")) `≻≻=` { i -> matchString("def") `≻≻=` { j -> pure(i + j) }}).parse("defg"))
//
//    println(Parser.narrow((tryParse(matchString("abc")) `≺|≻` matchString("")) `≻≻=` { i -> matchString("def") `≻≻=` { j -> pure(i + j) }}).parse("defg"))
//
//    println()
//
//    println(Parser.narrow(asterisk(matchString("abc")) `≻≻=` { i -> matchString("def") `≻≻=` { j -> pure(i + j) }}).parse("abcdefg"))
//    println(Parser.narrow(asterisk(matchString("abc")) `≻≻=` { i -> matchString("def") `≻≻=` { j -> pure(i + j) }}).parse("defg"))


    println(Parser.narrow(many(isDigit())).parse("1234"))
    println(Parser.narrow(tryParse(many(isDigit()))).parse("1234abc"))
}

// TODO: Test * == (+)? or * == (?)+ and others
// TODO: Should we change every signature with HKT to narrowed Parser?