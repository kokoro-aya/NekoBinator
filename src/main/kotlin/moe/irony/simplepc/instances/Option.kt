package moe.irony.simplepc.instances

import moe.irony.simplepc.types.Functor
import moe.irony.simplepc.types.HKT
import moe.irony.simplepc.types.Monad

sealed class Option<out A>: HKT<Option<*>, @UnsafeVariance A> {

    object None : Option<Nothing>() {
        override fun toString(): String = "[None]"
    }

    class Some<A>(val value: A) : Option<A>() {
        override fun toString(): String = "[Some: $value]"

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is Some<*>) return false
            return other.value == this.value
        }

        override fun hashCode(): Int {
            return value?.hashCode() ?: 0
        }
    }

    companion object: Monad<Option<*>>, Functor<Option<*>> {
        fun <T> narrow(v: HKT<Option<*>, T>): Option<T> = v as Option<T>

        override fun <A> pure(v: A): HKT<Option<*>, A> {
            return Some(v)
        }

        override fun <A, B> map(ma: HKT<Option<*>, A>, f: (A) -> B): HKT<Option<*>, B> {
            return when (val a = narrow(ma)) {
                is Some -> Some(f.invoke(a.value))
                is None -> None as HKT<Option<*>, B>
            }
        }

        override fun <A, B> flatMap(ma: HKT<Option<*>, A>, f: (A) -> HKT<Option<*>, B>): HKT<Option<*>, B> {
            return when (val a = narrow(ma)) {
                is Some -> f.invoke(a.value)
                is None -> None as HKT<Option<*>, B>
            }
        }
    }
}