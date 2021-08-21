package moe.irony.simplepc

import moe.irony.simplepc.parser.*
import moe.irony.simplepc.parser.Parser.Companion.`≺$≻`
import moe.irony.simplepc.parser.Parser.Companion.`≺*`
import moe.irony.simplepc.parser.Parser.Companion.`≻≻=`
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

    println(Parser.narrow(bcString()).parse("5:abcd"))
    println(Parser.narrow(bcString()).parse("4:abcd"))
    println(Parser.narrow(bcString()).parse("3:abcd"))

}