# NekoBinator

> The project is under revision, it might be dropped or totally rewritten in the Future. Also, codes in the repo need to be cleaned

A simple parser combinator written in Kotlin.

An attempt to implement parser combinator from scratch after having read the "十分钟魔法练习".

Main references:

- [Parser Combinators in Swift](https://academy.realm.io/posts/tryswift-yasuhiro-inami-parser-combinator/)
- Haskell documentation and source codes for implementations of several combinators and instances
- [Recursion, Tail Recursion and Y Combinator](https://proandroiddev.com/kotlin-pearls-8-recursion-tailrecursion-and-ycombinator-in-kotlin-3ec6975e9b6)
- [Monadium](https://github.com/yuxuanchiadm/monadium/blob/5715d7c6f3f99d620b8eaf57c9b4088bd1c7c8ed/monadium-core/src/main/java/org/monadium/core/control/Trampoline.java) for the idea and implementation of trampoline
- Advices and ideas from @90, @yuxuanchiadm and other friends

Currently there is a simple calculator on Int with add/minus/multiply/divide and a bencode parser as examples, more application will be coming soon.

## Achieved

- A minimum FP infrastructure for building a parser combinator:
  - HKT typeclasses and instances (Option and Either)
  - Trampoline for lazy evaulation
  - Other utilities functions

- A monadic parser
- Several combinators
- A simple calculator, a Bencode Parser and a Json Parser as a demo (some bugs exist)

## Notice

- Windows platform is not supported as special characters are used in infix methods' names.
- The project may encounter performance issues.

## Introduction

The project is build with a built-in minimum FP environment which consists of:

- Higher Kinded Type definitions including a Monad typeclass.
  - It defines the monadic behaviors (`pure` and `flatMap`) for its instances.
- Some Monad instances such as `Either`, `Option`, and `Result`.
- A `Trampoline` class which offers the possibility to evaluate recursive functions lazily, allowing for a place of mutual recursion and prevents stack overflow. `Trampoline` itself is also a Monad as it conforms to the monadic laws.
- Several utilities functions.

### Parser Monad

The `Parser<A>` class is an instance of Monad. Therefore it implements `pure` and `flatMap`.

As it's implemented with HKT, it's often necessary to convert from `HKT<Parser<*>, A>` to `Parser<A>`, there is a `narrow` function for this purpose. The reverse conversion could be done automatically.

The `Parser<A>` is indeed a State Monad that contains a function from `ParseState` to `Trampoline<Result<Context<A>>>`, which is stateful and contains a deterministic action that's wrapped in `Trampoline<>`.

The `ParseState` in this version is just a string with a cursus pointing to a certain position. It works like an iterator.

Here are methods of an object of `Parser` class:

- `Parser<A>.runParser(s:)` receives a `ParseState`, call it and returns a `Trampoline<>`.

  It's used for constructing combinators. You should never use this method unless you want to construct your own combinators. This method should be wrapped inside a lambda that receives a `ParseState` and its result should be sent to a `Trampoline` constructor method.

- `Parser<A>.parse(s:)` receives a `String` and parse it. Returns a `Result<>` which will be `Success` if parsing is completed, or `Failure` otherwise.

  This is the main entry of calling a Parser.

Here are all methods of `Parser` class's companion:

- `narrow(v:)` converts a HKT to its `Parser` type.

- `pure(v:)` lift a value of type `A` to `Parser<A>`.

  In the context of `Parser`, this means a Parser which will always emit the input value `v`, regardless the context.

- `empty()` creates a Parser that will always fail.

- `attempt(p:)` given a Parser `p`, produces a Parser which has a similar behavior excepts that it won't consume any input.

- `flatMap(ma:f:)` given a Parser `ma` and a function `f`, produces a combinator which runs first `ma`, feed the result to `f`. If the parse succeeds, consume the inputs, otherwise it will consume the input or not according to the last Parser within.

  The function `flatMap` is often used to chain multiple Parsers sequentially, as `f` has a type `(A) -> HKT<Parser<*>, B>`, this makes operations like `p flatMap { x -> q flatMap { y -> r flatMap { z -> ... } } }` possible.

- `fmap(ma:f:)` given a Parser `ma` and a transformer `f` from `A` to `B`, produces a Parser of type `B`. It's the conformance to `Functor` typeclass.

- `Parser<A>.combine(p:f:)` given a Parser of type `A` and another of type `B` and a transformer from `A` and `B` to `C`, produces a Parser of type `C`.

  This combinator could be useful for linking two Parsers of sequential order with a function that apply on their results. It could be simply implemented with `flatMap`.

- `alt(p:q:)` given two Parsers of type `A`, produces a Parser of the same type that will try to parse with the first one. If the parse fails and no input was consumed then it parses with the second one.

  This operator conforms to `Alternative` typeclass. It could be used to represent grammars such as `S ::= A | B`.

- `ap(p:f:)` given a Parser of type `A` and another Parser of type `(A) -> B`, apply the lifted function on first one and obtains a Parser of type `B`.

  This operator conforms to `Applicative` sequential application.

- `sum(...p:)` given several Parsers of type `A`, produces a Parser that's the sum of them.

  This operator could be understand for grammars such as `S ::= A | B | C | D ...`

Several infix operators have been defined. The symbol `≺` and `≻` has been used to replace the `<` and `>` symbols which cannot be used in names in Kotlin.

-   `≻≻=` alias for `flatMap`
-   `≻≻`  `flatMap` with `Parser<B>` as right hand side instead of a function
-   `≺$≻` alias for `fmap`
-   `≺|≻` alias for `alt`
-   `≺*≻` alias for `ap`
-   `≺*` sequence action that discarding the value of the second argument
-   `*≻` sequence action that discarding the value of the first argument

-   `recur(f:)`  The last function defined in `Parser` class is quite imporant as it creates a `Trampoline<>` to encapsulate a Parser within a lambda to create a Parser that will be evaluated lazily. This make mutual recursions possible.

### Combinators

#### Basic Parsers

-   `identity()`
-   `satisfy(pred:)`

#### Text Parsers

-   `anyChar()`
-   `isDigit()`
-   `isLetter()`
-   `matchChar(c:)`
-   `space()`
-   `matchString(str:)`
-   `anyString()`
-   `asterisk(p:)`

#### Combinators

-   `tryParse(p:)`
-   `choice(desc:ps:)`
-   `between(open:p:close:)`
-   `orElse(p:opt:)`
-   `tryDiscard(p:)`
-   `replicate(n:p:)`
-   `runLoop(a:f:)`
-   `many(p:)` and `many1(p:)`
-   `p sepBy s` and `p sepBy1 s`
-   `skipMany(p:)` and `skipMany1(p:)`
-   `p endBy sep` and `p endBy1 sep`
-   `p endByOptional sep` and `p endByOptional1 sep`
-   `chainl(p:op:x:)` and `chainl1(p:op:)`
-   `lookAhead(p:)`

#### Several Applications

-   `skipSpaces()`
-   `symbol(str:)`
-   `natural()`
-   `real()`

### Examples

Here's a sample calculator implemented using NekoBinator:

```kotlin
fun expr(): HKT<Parser<*>, Int> =
    recur { chainl1(term(), (symbol("+") `*≻` Parser.pure { a: Int, b: Int -> a + b })  `≺|≻`
        (symbol("-") `*≻` Parser.pure { a: Int, b: Int -> a - b })) }

fun term(): HKT<Parser<*>, Int> =
    recur { chainl1(factor(), (symbol("*") `*≻` Parser.pure { a: Int, b: Int -> a * b })  `≺|≻`
        (symbol("/") `*≻` Parser.pure { a: Int, b: Int -> a / b })) }

fun factor(): HKT<Parser<*>, Int> =
    recur { (symbol("(") `*≻` expr() `≺*` symbol(")")) `≺|≻` natural() }

fun eval(str: String) = Parser.narrow(expr()).parse(str)
```


