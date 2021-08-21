package moe.irony.simplepc

import moe.irony.simplepc.instances.Option
import moe.irony.simplepc.instances.Result
import moe.irony.simplepc.parser.ParseState
import moe.irony.simplepc.parser.Parser
import moe.irony.simplepc.parser.Parser.Companion.`≻≻=`
import moe.irony.simplepc.parser.Parser.Companion.empty
import moe.irony.simplepc.parser.Parser.Companion.pure
import moe.irony.simplepc.utils.Trampoline
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ParserTest {
    @Test
    fun basicParserTest() {
        assertEquals(Result.Success(1024), Parser.narrow(pure(1024)).parse(""))
        assertTrue(Parser.narrow(empty<Int>()).parse("") is Result.Failure)
    }

    @Test
    fun monadicParserTest() {
        assertEquals(Result.Success(3), Parser.narrow(
            Parser.pure(1) `≻≻=` { x ->
                Parser.pure(2) `≻≻=` { y ->
                    Parser.pure(x + y)
            } }).parse(""))
        assertTrue(Parser.narrow(
            Parser.pure(1) `≻≻=` { x ->
                Parser.empty<Int>() `≻≻=` { y ->
                    Parser.pure(x + y)
            } }).parse("") is Result.Failure)

        // TODO: 20/08/2021 Tests on more monadic operations
    }
}
