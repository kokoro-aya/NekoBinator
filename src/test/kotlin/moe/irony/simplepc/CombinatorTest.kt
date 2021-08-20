package moe.irony.simplepc

import moe.irony.simplepc.instances.Option
import moe.irony.simplepc.parser.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CombinatorTest {
    // TODO: 20/08/2021 ("not yet completed")

    @Test
    fun testIdentity() {
        assertEquals(Option.Some('a'), identity().parse("a"))
        assertEquals(Option.Some('z'), identity().parse("z"))
        assertEquals(Option.Some('b'), identity().parse("bcd"))
    }

    @Test
    fun testSatisfy() {
        assertEquals(Option.Some('a'), satisfy { it in 'a' .. 'f' }.parse("abc"))
        assertEquals(Option.None, satisfy { it in 'a' .. 'f' }.parse("zzz"))
    }

    @Test
    fun testChars() {
        ('a' .. 'z').forEach {
            assertEquals(Option.Some(it), anyChar().parse(it + "foo"))
            assertEquals(Option.None, isDigit().parse(it + "foo"))
        }
        ('0' .. '9').forEach {
            assertEquals(Option.Some(it), isDigit().parse(it + "foo"))
        }
        ('a' .. 'z').forEach {
            assertEquals(Option.Some(it), matchChar(it).parse(it + "foo"))
            assertEquals(Option.None, matchChar('0').parse(it + "foo"))
        }
        assertEquals(Option.Some("foo"), Parser.narrow(matchString("foo")).parse("foofoo"))
        assertEquals(Option.None, Parser.narrow(matchString("bar")).parse("foofoo"))

    }

    @Test
    fun testReplicate() {
        assertEquals(Option.Some(listOf<String>()), Parser.narrow(replicate(0, matchString("foo"))).parse(""))
        assertEquals(Option.Some(listOf("foo")), Parser.narrow(replicate(1, matchString("foo"))).parse("foo"))
        assertEquals(Option.Some(listOf("foo", "foo")), Parser.narrow(replicate(2, matchString("foo"))).parse("foofoo"))

        assertEquals(Option.Some(listOf<String>()), Parser.narrow(replicate(0, matchString("foo"))).parse("abc"))
        assertEquals(Option.None, Parser.narrow(replicate(1, matchString("foo"))).parse("bar"))
        assertEquals(Option.None, Parser.narrow(replicate(2, matchString("foo"))).parse("foobar"))

    }

    @Test
    fun testMany() {
        assertEquals(Option.Some(listOf<String>()), Parser.narrow(many(matchString("foo"))).parse(""))
        assertEquals(Option.Some(listOf<String>()), Parser.narrow(many(matchString("foo"))).parse("baz"))
        assertEquals(Option.Some(listOf("foo")), Parser.narrow(many(matchString("foo"))).parse("foobaz"))
        assertEquals(Option.Some(listOf("foo", "foo")), Parser.narrow(many(matchString("foo"))).parse("foofoobaz"))
    }

    @Test
    fun testMany1() {
        assertEquals(Option.None, Parser.narrow(many1(matchString("foo"))).parse(""))
        assertEquals(Option.None, Parser.narrow(many1(matchString("foo"))).parse("baz"))
        assertEquals(Option.Some(listOf("foo")), Parser.narrow(many1(matchString("foo"))).parse("foobaz"))
        assertEquals(Option.Some(listOf("foo", "foo")), Parser.narrow(many1(matchString("foo"))).parse("foofoobaz"))
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
