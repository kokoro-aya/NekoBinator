package moe.irony.simplepc.types

interface HKT<F, A> // F<A>

typealias HKT2<FF, A, B> = HKT<HKT<FF, A>, B> // F<A, B>