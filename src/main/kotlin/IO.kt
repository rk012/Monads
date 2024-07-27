fun interface IO<out T> : Monad<IO<*>, T> {
    companion object IOReturnScope : ReturnScope<IO<*>> {
        override fun <A> returns(a: A) = IO { a }

        fun <A> of(a: A) = returns(a)
    }

    fun run(): T

    override fun <B> bind(f: Binder<IO<*>, T, B>): IO<B> = IO { f(IOReturnScope, run()).self().run() }
    override fun <B> map(f: (T) -> B): IO<B> = IO { f(run()) }

    fun <B> flatMap(f: (T) -> IO<B>) = super.flatMap(f).self()
}

fun <A> Functor<IO<*>, A>.self() = this as IO<A>

typealias IOScope = BindScope<IO<*>>

fun <T> io(block: suspend IOScope.() -> T) =
    composeBinder<IO<*>, Unit, T> { returns(block()) }
    .let { IO.of(Unit).bind(it) }
