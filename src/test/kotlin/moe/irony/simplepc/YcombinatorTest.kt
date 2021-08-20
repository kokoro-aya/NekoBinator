package moe.irony.simplepc

import moe.irony.simplepc.utils.yCombinator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class YcombinatorTest {
    @Test
    fun testFactorial() {
        fun fac(f: (Long) -> Long): (Long) -> Long = { x ->
            if (x <= 1) 1 else x * f(x - 1)
        }
        assertEquals(121645100408832000, yCombinator(::fac)(19))
    }

    @Test
    fun testFib() {
        fun fib(f: (Int) -> Int): (Int) -> Int = { x ->
            if (x <= 2) 1 else f(x - 1) + f(x - 2)
        }
        assertEquals(1597, yCombinator(::fib)(17))
    }

    @Test
    fun testRev() {
        fun reverse(f: (String) -> String): (String) -> String = { s ->
            if (s.isEmpty()) "" else s.last() + f(s.dropLast(1))
        }
        assertEquals("noisruceR", yCombinator(::reverse)("Recursion"))
    }
}