import moe.irony.simplepc.instances.Option
import moe.irony.simplepc.parser.ParseState
import moe.irony.simplepc.parser.Parser
import moe.irony.simplepc.parser.Parser.Companion.`≻≻=`
import moe.irony.simplepc.utils.Trampoline
import moe.irony.simplepc.utils.`≻≻=`
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ParserTest {
    @Test
    fun basicParserTest() {
        assertEquals(Option.Some(1024), Parser { Trampoline.done(Option.Some(1024 to it)) }.parse(""))
        assertEquals(Option.None, Parser { Trampoline.done<Option<Pair<*, ParseState>>>(Option.None) }.parse(""))
    }

    @Test
    fun monadicParserTest() {
        assertEquals(Option.Some(3), Parser.narrow(
            Parser.pure(1) `≻≻=` { x ->
                Parser.pure(2) `≻≻=` { y ->
                    Parser.pure(x + y)
            } }).parse(""))
        assertEquals(Option.None, Parser.narrow(
            Parser.pure(1) `≻≻=` { x ->
                Parser.empty<Int>() `≻≻=` { y ->
                    Parser.pure(x + y)
            } }).parse(""))

        // TODO: 20/08/2021 Tests on more monadic operations
    }
}
