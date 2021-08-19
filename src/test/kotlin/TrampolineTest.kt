import moe.irony.simplepc.utils.Trampoline
import moe.irony.simplepc.utils.`$$`
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTimeout
import org.junit.jupiter.api.Test
import java.time.Duration

class TrampolineTest {
    @Test
    fun testBasic() {
        assertEquals(1, object {
            fun one(): Trampoline<Int> = Trampoline.done(1)
        }.one().run())
        assertEquals(2, object {
            fun two(): Trampoline<Int> = Trampoline.more { Trampoline.done(2) }
        }.two().run())
    }

    @Test
    fun testRecursion() {
        assertEquals(832040, object {
            fun fib(i: Int): Trampoline<Int> =
                if (i <= 1) Trampoline.done(i)
                else Trampoline.more { fib(i - 1) } `$$` { x ->
                    Trampoline.more { fib(i - 2) } `$$` { y ->
                        Trampoline.done(x + y)
                    }
                }
        }.fib(30).run())
        assertEquals(2147450880, object {
            fun sum(i: Int): Trampoline<Int> =
                if (i <= 0) Trampoline.done(i)
                else Trampoline.more { sum(i - 1) } `$$` { x ->
                    Trampoline.done(i + x)
                }
        }.sum(65535).run())
        assertTimeout(Duration.ofSeconds(1)) {
            object {
                fun fibtail(i: Long, a: Long, b: Long): Trampoline<Long> =
                    if (i <= 0) Trampoline.done(a)
                    else if (i <= 1) Trampoline.done(b)
                    else Trampoline.more { fibtail(i - 1, b, a + b) }
            }.fibtail(65535, 0, 1).run()
        }
    }
}