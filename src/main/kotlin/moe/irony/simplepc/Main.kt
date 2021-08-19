package moe.irony.simplepc

import moe.irony.simplepc.calculator.parse
import moe.irony.simplepc.parser.Parser
import moe.irony.simplepc.parser.many
import moe.irony.simplepc.parser.matchString
import moe.irony.simplepc.parser.replicate

fun main() {
    println(parse("11 + 45"))
    println(parse("27 - 69"))
    println(parse("33 * 7"))
    println(parse("48 / 20"))

    println(Parser.narrow(replicate(3, matchString("foo"))).parse("foofoofoo"))
    println(Parser.narrow(many(matchString("foo"))).parse("foo"))
    println(Parser.narrow(many(matchString("foo"))).parse("foofoo"))
    println(Parser.narrow(many(matchString("foo"))).parse("bar"))
}