@JvmInline
value class ListMonad<out T>(val list: List<T>) : Monad<ListMonad<*>, T>, MultiMap {
    companion object ListReturnScope : ReturnScope<ListMonad<*>> {
        override fun <A> returns(a: A) = ListMonad(listOf(a))
    }

    override fun <B> map(f: (T) -> B) = ListMonad(list.map(f))

    override fun <B> bind(f: Binder<ListMonad<*>, T, B>) = ListMonad(list.flatMap { f(ListReturnScope, it).self().list })

    fun <B> flatMap(f: (T) -> ListMonad<B>) = super.flatMap(f).self()

    fun <R> fold(initial: R, combine: (acc: R, current: T) -> R) = list.fold(initial, combine)
}

fun <T> Functor<ListMonad<*>, T>.self() = this as ListMonad<T>