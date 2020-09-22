# Chainable

> :warning: Under construction / Work in progress / Coming soon...

## Summary

`Chainable` is a fluent interface-style sub type of `java.lang.Iterable` with additional convenience methods facilitating the use of the
**iterator pattern**, **functional programming** and **lazy evaluation**. It is intended for achieving code that is more succinct, readable,
simpler to implement, and sometimes faster than its non-lazy/non-functional equivalent. Its implementation is very lightweight and self-contained, i.e. it has
no external dependencies, so it would not cause any dependency versioning hell.

`Chainable` is partly analogous to and inspired by C#'s `Enumerable` (LINQ), but it is not a port and it is taken further.

Although`Chainable` overlaps with Java 8's `Stream` in terms of its functionality, the design of `Chainable` is optimized for a somewhat different set of goals and aligns more with C#'s `Enumerable`'s LINQ-style methods than Java streams.

For example, one of the key differences from `Stream` is that `Chainable` fully preserves the functional and re-entrancy semantics of `Iterable`, i.e. it can be traversed multiple times, with multiple iterator instantiations, whereas a stream can be traversed only once.

The `Chainable` API surface also exposes various additional convenience methods for chain processing with functional programming, and is not so much oriented toward the parallelism that was a key guiding design principle behind Java's `Stream`.

Having said that, a basic level of interoperability between `Stream` and `Chainable` exists: a chain can be created from a stream (see `Chainable#from(Stream)`, and vice-versa (see `Chainable#stream()`).

(A note on the vocabulary: `Chainable` is the interface, whereas the word *"chain"* is used throughout the documentation to refer to specific instances of a `Chainable`).

Overall, the current highlights of `Chainable` include:

> :warning: Section under construction as the API is currently under active development/at pre-release stage.

- **disjunctive filtering** - you can specify one or more filter predicates at the same time (see `Chainable#whereEither), with disjuctive (logical-OR) semantics. This means you can define specific filtering predicates for specific purposes and then just supply them all as parameters, rather than having to create yet another predicate that's an *OR* of the others.

- predicate-based **skipping** of the leading sub-chain of items under various scenarios, e.g.:
  - skip *as long as* they satisfy a condition (`Chainable#notAsLongAs()`)
  - skip *before* they satisfy a condition (`Chainable#notBefore()`)
  
- predicate-based **trimming** of the trailing sub-chain of items under various scenario, e.g.:
  - stop *as soon as* the specified conditions are satisfied (`Chainable#asLongAs()`) or are no longer satisfied (`Chainable#before()`.)

- **equality and sub-array containment** checks, but evaluated lazily, i.e. chains failing the equality (`Chainable#equals`) or sub-array containment (`Chainable#containsSubarray) tests return quickly, without traversing/evaluating the rest of the chain.

## Examples

> :triangular_flag_on_post: To be continued...