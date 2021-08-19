package moe.irony.simplepc.types

interface Functor<F> {
    /**
     * map :: f a -> (a -> b) -> f b
     */
    fun <A, B> map(ma: HKT<F, A>, f: (A) -> B): HKT<F, B>
}