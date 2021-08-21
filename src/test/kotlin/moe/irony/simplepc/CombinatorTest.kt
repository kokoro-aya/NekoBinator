package moe.irony.simplepc

import moe.irony.simplepc.instances.Option
import moe.irony.simplepc.instances.Result
import moe.irony.simplepc.parser.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CombinatorTest {
    // TODO: 20/08/2021 ("not yet completed")

    @Test
    fun testIdentity() {
        assertEquals(Result.Success('a'), identity().parse("a"))
        assertEquals(Result.Success('z'), identity().parse("z"))
        assertEquals(Result.Success('b'), identity().parse("bcd"))
    }

    @Test
    fun testSatisfy() {
        assertEquals(Result.Success('a'), satisfy { it in 'a' .. 'f' }.parse("abc"))
        assertTrue(satisfy { it in 'a' .. 'f' }.parse("zzz") is Result.Failure)
    }

    @Test
    fun testChars() {
        ('a' .. 'z').forEach {
            assertEquals(Result.Success(it), anyChar().parse(it + "foo"))
            assertTrue(isDigit().parse(it + "foo") is Result.Failure)
        }
        ('0' .. '9').forEach {
            assertEquals(Result.Success(it), isDigit().parse(it + "foo"))
        }
        ('a' .. 'z').forEach {
            assertEquals(Result.Success(it), matchChar(it).parse(it + "foo"))
            assertTrue(matchChar('0').parse(it + "foo") is Result.Failure)
        }
        assertEquals(Result.Success("foo"), Parser.narrow(matchString("foo")).parse("foofoo"))
        assertTrue(Parser.narrow(matchString("bar")).parse("foofoo") is Result.Failure)

    }

    @Test
    fun testReplicate() {
        assertEquals(Result.Success(listOf<String>()), Parser.narrow(replicate(0, matchString("foo"))).parse(""))
        assertEquals(Result.Success(listOf("foo")), Parser.narrow(replicate(1, matchString("foo"))).parse("foo"))
        assertEquals(Result.Success(listOf("foo", "foo")), Parser.narrow(replicate(2, matchString("foo"))).parse("foofoo"))

        assertEquals(Result.Success(listOf<String>()), Parser.narrow(replicate(0, matchString("foo"))).parse("abc"))
        assertTrue(Parser.narrow(replicate(1, matchString("foo"))).parse("bar") is Result.Failure)
        assertTrue(Parser.narrow(replicate(2, matchString("foo"))).parse("foobar") is Result.Failure)

    }

    @Test
    fun testMany() {
        assertEquals(Result.Success(listOf<String>()), Parser.narrow(many(matchString("foo"))).parse(""))
        assertEquals(Result.Success(listOf<String>()), Parser.narrow(many(matchString("foo"))).parse("baz"))
        assertEquals(Result.Success(listOf("foo")), Parser.narrow(many(matchString("foo"))).parse("foobaz"))
        assertEquals(Result.Success(listOf("foo", "foo")), Parser.narrow(many(matchString("foo"))).parse("foofoobaz"))
    }

    @Test
    fun testMany1() {
        assertTrue(Parser.narrow(many1(matchString("foo"))).parse("") is Result.Failure)
        assertTrue(Parser.narrow(many1(matchString("foo"))).parse("baz") is Result.Failure)
        assertEquals(Result.Success(listOf("foo")), Parser.narrow(many1(matchString("foo"))).parse("foobaz"))
        assertEquals(Result.Success(listOf("foo", "foo")), Parser.narrow(many1(matchString("foo"))).parse("foofoobaz"))
    }

    @Test
    fun testChainl1() {
        fail<Nothing>()
    }

    @Test
    fun testHigherOrderCombinations() {
        fail<Nothing>()
    }
}
