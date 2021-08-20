# A Simple Parser Combinator

An attempt to implement parser combinator from scratch after having read the "十分钟魔法练习"

Main references:

- [Parser Combinators in Swift](https://academy.realm.io/posts/tryswift-yasuhiro-inami-parser-combinator/)
- Haskell documentation and source codes for implementations of several combinators and instances
- [Recursion, Tail Recursion and Y Combinator](https://proandroiddev.com/kotlin-pearls-8-recursion-tailrecursion-and-ycombinator-in-kotlin-3ec6975e9b6)
- [Monadium](https://github.com/yuxuanchiadm/monadium/blob/5715d7c6f3f99d620b8eaf57c9b4088bd1c7c8ed/monadium-core/src/main/java/org/monadium/core/control/Trampoline.java) for the idea and implementation of trampoline
- Advices from @90

Currently there is a simple calculator on Int with add/minus/multiply/divide, more application will be coming soon.

## TODO

- More elementary parser combinators
- Error handling (change Option to Either)
- A Json Parser
- A STLC Parser
- A converter from chinese number to numeric representation and vice-versa
- Test coverage
- Implementation of do-notation