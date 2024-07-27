sealed class Option<out T> : Monad<Option<*>, T> {
    companion object OptionReturnScope : ReturnScope<Option<*>> {
        override fun <A> returns(a: A): Option<A> = Some(a)

        fun <A> some(a: A) = returns(a)
        fun none() = None
    }

    override fun <B> map(f: (T) -> B): Option<B> = when (this) {
        is Some -> Some(f(item))
        None -> None
    }

    override fun <B> bind(f: Binder<Option<*>, T, B>): Option<B> = when (this) {
        is Some -> f(OptionReturnScope, item).self()
        None -> None
    }

    fun <B> flatMap(f: (T) -> Option<B>) = super.flatMap(f).self()

    inline fun fold(default: () -> @UnsafeVariance T): T = when (this) {
        is Some -> item
        None -> default()
    }

    data class Some<T>(val item: T) : Option<T>()
    data object None : Option<Nothing>()
}

fun <T> Functor<Option<*>, T>.self() = this as Option<T>

typealias OptionScope = BindScope<Option<*>>

suspend fun OptionScope.raise(): Nothing = Option.None.bind()

fun <T> option(block: suspend OptionScope.() -> T) =
    composeBinder<Option<*>, Unit, T> { returns(block()) }
    .let { Option.some(Unit).bind(it) }
