package moe.irony.simplepc.applications.calculator

import moe.irony.simplepc.applications.matchChar
import moe.irony.simplepc.applications.natural
import moe.irony.simplepc.applications.skipSpaces
import moe.irony.simplepc.applications.symbol
import moe.irony.simplepc.parser.*
import moe.irony.simplepc.parser.Parser.Companion.`*≻`
import moe.irony.simplepc.parser.Parser.Companion.`≺$≻`
import moe.irony.simplepc.parser.Parser.Companion.`≺*`
import moe.irony.simplepc.parser.Parser.Companion.`≺|≻`
import moe.irony.simplepc.parser.Parser.Companion.`≻≻`
import moe.irony.simplepc.types.HKT

private fun integer(): HKT<Parser<*>, Int> =
    natural() `≺|≻` ((Int::unaryMinus) `≺$≻` (matchChar('-') `≻≻` natural()))

// Expr ::= Term | Expr '+' Term | Expr '-' Term
// Expr ::= chainl1 (Term, '+' | '-')
internal fun expr(): HKT<Parser<*>, Int> =
    recur { chainl1(term(), (symbol("+") `*≻` Parser.pure { a: Int, b: Int -> a + b }) `≺|≻`
            (symbol("-") `*≻` Parser.pure { a: Int, b: Int -> a - b })) }

// Term ::= Factor | Term '*' Factor | Term '/' Factor
// Term ::= chainl1 (Factor, '*' | '/')
internal fun term(): HKT<Parser<*>, Int> =
    recur { chainl1(factor(), (symbol("*") `*≻` Parser.pure { a: Int, b: Int -> a * b }) `≺|≻`
            (symbol("/") `*≻` Parser.pure { a: Int, b: Int -> a / b })) }

// Factor ::= ( Expr ) | Number
internal fun factor(): HKT<Parser<*>, Int> =
    recur { (symbol("(") `*≻` expr() `≺*` symbol(")")) `≺|≻` integer() }

fun Calculator(): Parser<Int> = Parser.narrow(skipSpaces() `*≻` expr() `≺*` skipSpaces())
