package moe.irony.simplepc

import moe.irony.simplepc.calculator.parseInt
import moe.irony.simplepc.parser.Parser
import moe.irony.simplepc.parser.many
import moe.irony.simplepc.parser.matchString
import moe.irony.simplepc.parser.replicate

fun main() {
    println(parseInt("11 + 45"))
    println(parseInt("27 - 69"))
    println(parseInt("33 * 7"))
    println(parseInt("48 / 20"))

    println(Parser.narrow(replicate(3, matchString("foo"))).parse("foofoofoo"))
    println(Parser.narrow(many(matchString("foo"))).parse("foo"))
    println(Parser.narrow(many(matchString("foo"))).parse("foofoo"))
    println(Parser.narrow(many(matchString("foo"))).parse("bar"))
}