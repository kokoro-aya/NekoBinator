package moe.irony.simplepc.calculator

import moe.irony.simplepc.instances.Option
import moe.irony.simplepc.parser.*
import moe.irony.simplepc.parser.Parser.Companion.`*≻`
import moe.irony.simplepc.parser.Parser.Companion.`≺*`
import moe.irony.simplepc.parser.Parser.Companion.`≺|≻`
import moe.irony.simplepc.types.HKT

// Expr ::= Term | Expr '+' Term | Expr '-' Term
// Expr ::= chainl1 (Term, '+' | '-')
fun expr(): HKT<Parser<*>, Int> =
    recur { chainl1(term(), (symbol("+") `*≻` Parser.pure { a: Int, b: Int -> a + b })  `≺|≻`
            (symbol("-") `*≻` Parser.pure { a: Int, b: Int -> a - b })) }

// Term ::= Factor | Term '+' Factor | Term '-' Factor
// Term ::= chainl1 (Factor, '+' | '-')
fun term(): HKT<Parser<*>, Int> =
    recur { chainl1(factor(), (symbol("*") `*≻` Parser.pure { a: Int, b: Int -> a * b })  `≺|≻`
            (symbol("/") `*≻` Parser.pure { a: Int, b: Int -> a / b })) }

// Factor ::= ( Expr ) | Number
fun factor(): HKT<Parser<*>, Int> =
    recur { (symbol("(") `*≻` expr() `≺*` symbol(")")) `≺|≻` natural() }

fun parseInt(s: String): Int? = when (val p = Parser.narrow(expr()).parse(s)) {
    is Option.Some -> p.value
    is Option.None -> null
}

