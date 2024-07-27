fun interface State<S, out T> : Monad<State<S, *>, T> {
    companion object {
        private fun <S> stateReturnScope() = object : ReturnScope<State<S, *>> {
            override fun <A> returns(a: A): State<S, A> = State { s -> s to a }
        }

        fun <S, T> returns(item: T) = stateReturnScope<S>().returns(item)
    }

    fun update(state: S): Pair<S, T>

    override fun <B> map(f: (T) -> B): State<S, B> = State { s -> update(s).let { (s1, t) -> s1 to f(t) } }

    override fun <B> bind(f: Binder<State<S, *>, T, B>): State<S, B> = State { s ->
        update(s).let { (s1, t) ->
            f(stateReturnScope(), t).self().update(s1)
        }
    }
}

fun <S, T> Functor<State<S, *>, T>.self() = this as State<S, T>

typealias StateScope<S> = BindScope<State<S, *>>

suspend fun <S> StateScope<S>.getState() = State { s: S -> s to s }.bind()
suspend fun <S> StateScope<S>.setState(newState: S) = State { _: S -> newState to Unit }.bind()

fun <S, T> state(block: suspend StateScope<S>.() -> T) =
    composeBinder<State<S, *>, Unit, T> { returns(block()) }
        .let { State.returns<S, _>(Unit).bind(it) }