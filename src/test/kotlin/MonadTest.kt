import org.junit.jupiter.api.assertThrows
import kotlin.math.sqrt
import kotlin.properties.Delegates
import kotlin.test.Test
import kotlin.test.assertEquals

class MonadTest {
    sealed interface NumErr {
        data object Imag : NumErr
        data object DBZ : NumErr
    }

    @Test
    fun eitherListTest() {
        val nums = ListMonad((-2..2).toList())
        val expected = listOf(NumErr.Imag, NumErr.Imag, NumErr.DBZ, 1.0, 1/sqrt(2.0))

        fun esqrt(n: Double) = either<NumErr, _> { if (n < 0) raise(NumErr.Imag) else sqrt(n) }
        fun ediv(n: Double) = either<NumErr, _> { if (n == 0.0) raise(NumErr.DBZ) else 1/n }

        val res = nums.map { n ->
            either<_, Any> {
                ediv(esqrt(n.toDouble()).bind()).bind()
            }.fold { it }
        }

        assertEquals(expected, res.list)
    }

    @Test
    fun futureTest() {
        val base = CompletableFuture<Int>()

        val f = future {
            2 * base.bind()
        }

        assertEquals(Option.None, f.tryGet())
        base.completeWith(3)
        assertEquals(6, f.tryGet().fold { -1 })
    }

    @Test
    fun ioTest() {
        var src by Delegates.notNull<Int>()
        val getSrc = io { src }
        val it = io { getSrc.bind() * 2 }
        src = 4
        assertEquals(8, it.run())
        src = 5
        assertEquals(10, it.run())
    }

    @Test
    fun listFlatMapTest() {
        val nums = ListMonad(listOf(0, 1))
        val expected = setOf(0 to 0, 0 to 1, 1 to 0, 1 to 1)

        val out =
            nums.bind { a ->
            nums.bind { b ->
            returns(a to b)
        } }.list.toSet()

        assertEquals(expected, out)

        assertThrows<IllegalStateException> {
            composeBinder<ListMonad<*>, Unit, Unit> {
                assert(false)
                returns(Unit)
            }
        }
    }

    @Test
    fun optionTest() {
        var src: Option<Int> = option { raise() }

        fun res() = option {
            2 * src.bind() * src.bind()
        }

        assertEquals(-1, res().fold { -1 })
        src = option { 2 }
        assertEquals(8, res().fold { -1 })
    }

    @Test
    fun readerTest() {
        val a = reader<Int, _> { peek() }
        val r = reader<Int, _> {
            2 * peek() * a.bind()
        }

        assertEquals(8, r.read(2))
        assertEquals(32, r.read(4))
    }

    @Test
    fun stateTest() {
        var state = 1

        val s = state<Int, _> {
            val n = getState()
            setState(n*2)
            n+1
        }

        fun <T> update(s: State<Int, T>): T {
            val (newState, res) = s.update(state)
            state = newState
            return res
        }

        assertEquals(2, update(s))
        assertEquals(2, state)
        assertEquals(3, update(s))
        assertEquals(4, state)
    }

    @Test
    fun writerTest() {
        val a = writer { log("A") }
        val b = writer { log("B") }
        val c = writer {
            a.bind()
            b.bind()
            log("C")
        }

        assertEquals("ABC", c.log)
    }
}