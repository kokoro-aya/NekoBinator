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