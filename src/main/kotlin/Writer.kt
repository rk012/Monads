data class Writer<out T>(val item: T, val log: String = "") : Monad<Writer<*>, T> {
    companion object WriterReturnScope : ReturnScope<Writer<*>> {
        override fun <A> returns(a: A) = Writer(a, "")
    }

    override fun <B> map(f: (T) -> B): Writer<B> = Writer(f(item), log)

    override fun <B> bind(f: Binder<Writer<*>, T, B>): Writer<B> = f(WriterReturnScope, item).self().let { w ->
        Writer(w.item, log + w.log)
    }

    fun <B> flatMap(f: (T) -> Writer<B>) = super.flatMap(f).self()
}

fun <T> Functor<Writer<*>, T>.self() = this as Writer<T>

typealias WriterScope = BindScope<Writer<*>>

suspend fun WriterScope.log(msg: String) = Writer(Unit, msg).bind()

fun <T> writer(block: suspend WriterScope.() -> T) =
    composeBinder<Writer<*>, Unit, T> { returns(block()) }
        .let { Writer.returns(Unit).bind(it) }
