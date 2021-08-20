package moe.irony.simplepc

import moe.irony.simplepc.calculator.parseInt
import moe.irony.simplepc.parser.*
import moe.irony.simplepc.parser.Parser.Companion.`*≻`
import moe.irony.simplepc.parser.Parser.Companion.`≻≻=`
import moe.irony.simplepc.parser.Parser.Companion.pure
import moe.irony.simplepc.types.HKT
import moe.irony.simplepc.utils.`≻≻=`

fun isEven(): Parser<Boolean> = TODO("Not implemented yet")

fun main() {
//    (0 until 100).forEach {
//        println(isEven().parse(it.toString()))
//    }
    val p = Parser.narrow(tryAndThen(matchString("foo"), matchString("bar"),
        String::plus) { it })

    println(p.parse("bar"))
    println(p.parse("foobar"))
}