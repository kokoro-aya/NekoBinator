package moe.irony.simplepc.types

interface Monad<M> {
    /**
     * pure :: a -> m a
     */
    fun <A> pure(v: A): HKT<M, A>

    /**
     * flatMap :: m a -> (a -> m b) -> m b
     */
    fun <A, B> flatMap(ma: HKT<M, A>, f: (A) -> HKT<M, B>): HKT<M, B>

}

interface Monad2<M> {
    fun <A, B> pure(v: B): HKT2<M, A, B>
    fun <A, B, C> flatMap(ma: HKT2<M, A, B>, f: (B) -> HKT2<M, A, C>): HKT2<M, A, C>
}

fun <M, A, B> Monad<M>.liftM1(f: (A) -> B): (HKT<M, A>) -> HKT<M, B> = {
    this.flatMap(it) { a ->
        pure(f.invoke(a))
    }
}

fun <M, A, B, C> Monad<M>.liftM2(f: (A, B) -> C): (HKT<M, A>, HKT<M, B>) -> HKT<M, C> = { ma, mb ->
    this.flatMap(ma) { a ->
        this.flatMap(mb) { b ->
            pure(f.invoke(a, b))
        }
    }
}