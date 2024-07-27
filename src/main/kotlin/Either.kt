sealed class Either<E, out R> : Monad<Either<E, *>, R> {
    companion object {
        private fun <E> eitherReturnScope() = object : ReturnScope<Either<E, *>> {
            @Suppress("UNCHECKED_CAST")
            override fun <A> returns(a: A): Either<E, A> = Right(a) as Either<E, A>
        }

        fun <E, R> right(item: R): Either<E, R> = eitherReturnScope<E>().returns(item)
        fun <E> left(item: E): Either<E, Nothing> = Left(item)
    }

    override fun <B> map(f: (R) -> B): Either<E, B> = when(this) {
        is Left -> this
        is Right -> right(f(item))
    }

    override fun <B> bind(f: Binder<Either<E, *>, R, B>): Either<E, B> = when(this) {
        is Left -> this
        is Right -> f(eitherReturnScope(), item).self()
    }

    fun <B> flatMap(f: (R) -> Either<E, B>): Either<E, B> = super.flatMap(f).self()

    inline fun fold(err: (E) -> @UnsafeVariance R): R = when (this) {
        is Left -> err(item)
        is Right -> item
    }

    data class Left<E>(val item: E) : Either<E, Nothing>()
    data class Right<R>(val item: R) : Either<Nothing, R>()
}

fun <E, R> Functor<Either<E, *>, R>.self() = this as Either<E, R>

typealias EitherScope<E> = BindScope<Either<E, *>>

suspend fun <E> EitherScope<E>.raise(err: E): Nothing = Either.left(err).bind()

fun <E, T> either(block: suspend EitherScope<E>.() -> T) =
    composeBinder<Either<E, *>, Unit, T> { returns(block()) }
        .let { Either.right<E, _>(Unit).bind(it) }
