fun interface Reader<R, out T> : Monad<Reader<R, *>, T> {
    companion object {
        private fun <R> readerReturnScope() = object : ReturnScope<Reader<R, *>> {
            override fun <A> returns(a: A) = Reader { _: R -> a }
        }

        fun <R, T> returns(item: T) = readerReturnScope<R>().returns(item)
    }

    fun read(r: R): T

    override fun <B> map(f: (T) -> B): Reader<R, B> = Reader { r -> f(read(r)) }

    override fun <B> bind(f: Binder<Reader<R, *>, T, B>): Reader<R, B> =
        Reader { r -> f(readerReturnScope(), read(r)).self().read(r) }

    fun <B> flatMap(f: (T) -> Reader<R, B>): Reader<R, B> = super.flatMap(f).self()
}

fun <R, T> Functor<Reader<R, *>, T>.self() = this as Reader<R, T>

typealias ReaderScope<R> = BindScope<Reader<R, *>>

suspend fun <R> ReaderScope<R>.peek() = Reader { r: R -> r }.bind()

fun <R, T> reader(block: suspend ReaderScope<R>.() -> T) =
    composeBinder<Reader<R, *>, Unit, T> { returns(block()) }
        .let { Reader.returns<R, _>(Unit).bind(it) }
