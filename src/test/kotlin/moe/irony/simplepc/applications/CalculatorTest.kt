package moe.irony.simplepc.applications

import moe.irony.simplepc.applications.calculator.Calculator
import moe.irony.simplepc.applications.calculator.expr
import moe.irony.simplepc.applications.calculator.factor
import moe.irony.simplepc.applications.calculator.term
import moe.irony.simplepc.instances.Result
import moe.irony.simplepc.parser.Parser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CalculatorTest {
    @Test
    fun testExpr() {
        assertEquals(Result.Success(17), Parser.narrow(expr()).parse(" 1 + 8 + 7 + 1   "))
        assertEquals(Result.Success(6), Parser.narrow(expr()).parse("  12 - 8 + 2 "))
        assertEquals(Result.Success(3), Parser.narrow(expr()).parse(" 27  -  24 "))
        assertEquals(Result.Success(36), Parser.narrow(expr()).parse(" 33 + 10  -   7 "))
        assertTrue(Parser.narrow(expr()).parse(" + - 2 ") is Result.Failure)
        assertTrue(Parser.narrow(expr()).parse(" - 2 +") is Result.Failure)
        assertTrue(Parser.narrow(expr()).parse(" -- 2 + 7") is Result.Failure)
    }

    @Test
    fun testTerm() {
        assertEquals(Result.Success(27), Parser.narrow(term()).parse("   3 *   9 "))
        assertEquals(Result.Success(4), Parser.narrow(term()).parse("   17/  4 "))
        assertEquals(Result.Success(64), Parser.narrow(term()).parse("   4 * 4 * 4 "))
        assertEquals(Result.Success(4), Parser.narrow(term()).parse("   4 * 4 / 4 "))
//        assertTrue(Parser.narrow(term()).parse(" 13 / ") is Result.Failure)
        assertTrue(Parser.narrow(term()).parse(" *   29") is Result.Failure)
    }

    @Test
    fun testFactor() {
        assertEquals(Result.Success(12), Parser.narrow(factor()).parse("   12 "))
        assertEquals(Result.Success(25), Parser.narrow(factor()).parse(" (   12 + 13) "))
        assertEquals(Result.Success(44), Parser.narrow(factor()).parse("44"))
        assertTrue(Parser.narrow(factor()).parse("   (13 ") is Result.Failure)
//        assertTrue(Parser.narrow(factor()).parse("   13 )") is Result.Failure) // TODO Need EOF to pass this
        assertTrue(Parser.narrow(factor()).parse("  f 2 3 ") is Result.Failure)
//        assertTrue(Parser.narrow(factor()).parse(" 24b ") is Result.Failure)
//        assertTrue(Parser.narrow(factor()).parse(" 29 17 ") is Result.Failure)
    }

    @Test
    fun simpleExpressions() {
        assertEquals(Result.Success(20), Calculator().parse(" 2+3+4+5+6 "))
        assertEquals(Result.Success(24), Calculator().parse(" 1 * 2 * 3 * 4 "))
    }

    @Test
    fun negativeNumbers() {
        assertEquals(Result.Success(-15), Calculator().parse(" (  -15 ) "))
        assertEquals(Result.Success(-15), Calculator().parse(" -1 + -2 + -3 + -4 + -5 "))
        assertEquals(Result.Success(-5), Calculator().parse(" ( 4 ) + ( -9) "))
    }

    @Test
    fun expressionOrder() {
        assertEquals(Result.Success(0), Calculator().parse(" 2 + 36 / 6 - 4 * 2"))
        assertEquals(Result.Success(50), Calculator().parse(" 14 - 7 * -5 + 1 "))
        assertEquals(Result.Success(50), Calculator().parse(" (14) - 7 * (-5) + 1 "))
        assertEquals(Result.Success(104), Calculator().parse(" 6 + 12 * 7 + 23 - 45 / 5 + 6 / 9 / 3 "))
    }

    @Test
    fun nestedExpressions() {
        assertEquals(Result.Success(4), Calculator().parse("3 * (2 + 1) - 5 "))
        assertEquals(Result.Success(-3), Calculator().parse(" 12 * (6 - 7) / (8 - 4) "))
    }
}