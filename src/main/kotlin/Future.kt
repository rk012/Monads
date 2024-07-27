interface Future<out T> : Monad<Future<*>, T> {
    companion object FutureReturnScope : ReturnScope<Future<*>> {
        override fun <A> returns(a: A) = object : Future<A> {
            override fun tryGet() = Option.some(a)
        }
    }

    fun tryGet(): Option<T>

    override fun <B> bind(f: Binder<Future<*>, T, B>): Future<B> = object : Future<B> {
        override fun tryGet() = this@Future.tryGet().flatMap { x -> f(FutureReturnScope, x).self().tryGet() }
    }

    override fun <B> map(f: (T) -> B): Future<B> = object : Future<B> {
        override fun tryGet() = this@Future.tryGet().map(f)
    }

    fun <B> flatMap(f: (T) -> Future<B>) = super.flatMap(f).self()
}

fun <T> Functor<Future<*>, T>.self() = this as Future<T>

fun <A, B> merge(fa: Future<A>, fb: Future<B>) = fa.flatMap { a -> fb.map { b -> Pair(a, b) } }

typealias FutureScope = BindScope<Future<*>>

fun <T> future(block: suspend FutureScope.() -> T) =
    composeBinder<Future<*>, Unit, T> { returns(block()) }
        .let { Future.returns(Unit).bind(it) }

class CompletableFuture<T> : Future<T> {
    private var result: Option<T> = Option.none()

    fun completeWith(x: T) {
        result = Option.some(x)
    }

    override fun tryGet() = result
}
