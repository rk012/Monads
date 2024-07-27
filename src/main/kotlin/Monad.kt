import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.reflect.jvm.jvmErasure

interface Functor<out Self, out A> {
    fun <B> map(f: (A) -> B): Functor<Self, B>
}

interface ReturnScope<M> {
    fun <A> returns(a: A): Monad<M, A>
}

typealias Binder<M, A, B> = ReturnScope<M>.(A) -> Monad<M, B>

interface MultiMap

interface Monad<Self, out A> : Functor<Self, A> {
    fun <B> bind(f: Binder<Self, A, B>): Monad<Self, B>

    fun <B> flatMap(f: (A) -> Monad<Self, B>) = bind { a -> f(a) }
}

infix fun <M, A, B, C> Binder<M, A, B>.compose(next: Binder<M, B, C>): Binder<M, A, C> =
    { a -> this@compose.invoke(this, a).bind(next) }

fun <M, A> mIdentity(): Binder<M, A, A> = { a -> returns(a) }

@RestrictsSuspension
class BindScope<M>(r: ReturnScope<M>) : ReturnScope<M> by r {
    lateinit var acc: Monad<M, *>

    suspend fun <A> Monad<M, A>.bind(): A = suspendCoroutineUninterceptedOrReturn { cont ->
        acc = flatMap { a ->
            cont.resume(a)
            acc
        }

        COROUTINE_SUSPENDED
    }
}

inline fun <reified M, A, B> composeBinder(noinline block: suspend BindScope<M>.(A) -> Monad<M, B>): Binder<M, A, B> {
    if (MultiMap::class in M::class.supertypes.map { it.jvmErasure }) error("Not supported for MultiMap Monads")

    return { a ->
        val bindScope = BindScope(this)
        val handle: suspend BindScope<M>.() -> Monad<M, B> = { block(a) }

        handle.startCoroutine(bindScope, Continuation(EmptyCoroutineContext) {
            bindScope.acc = it.getOrThrow()
        })

        @Suppress("UNCHECKED_CAST")
        bindScope.acc as Monad<M, B>
    }
}
