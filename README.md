# Chainable

> :warning: Under construction / Work in progress / Coming soon...

## Summary

`Chainable` is a fluent interface-style sub type of `java.lang.Iterable` with additional convenience methods facilitating the use of the
**iterator pattern**, **functional programming** and **lazy evaluation**. It is intended for achieving code that is more succinct, readable,
simpler to implement, and sometimes faster than its non-lazy/non-functional equivalent.

```java
        Chainable<String> chain = Chainable
                .from(0, 0, 0, 2, 3, 7, 0, 1, 8, 3, 13, 14, 0, 2)     // Integers
                .notAsLongAs(i -> i == 0)                             // Ignore leading sub chain of 0s
                .notAfter(i -> i == 13)                               // Stop after finding 13
                .whereEither(                                         // Choose only those that...
                        i -> i % 2 == 0,                              // ...are even
                        i -> i > 6)                                   // ...or greater than 6
                .transform(i -> Character.toString((char) (i + 65))); // Transform into letters

        String text = chain.join(); // Merge into a string
        String textBackwards = chain.reverse().join(); // Reverse and merge into a string

        assertEquals("CHAIN", text);
        assertEquals("NIAHC", textBackwards);
```

`Chainable` is partly analogous to and inspired by C#'s `Enumerable` (LINQ), but it is not a port, and the core ideas behind this kind of approach are taken further.

The implementation is very lightweight and self-contained, i.e. it has no external dependencies, so as not to contribute to any sub-dependency versioning challenges.

Although `Chainable` overlaps with Java 8's `Stream` in terms of functionality, the design of `Chainable` is optimized for a somewhat different set of goals and aligns more with C#'s `Enumerable`'s LINQ-style methods than Java streams.

For example, one of the key differences from `Stream` is that `Chainable` fully preserves the functional and re-entrancy semantics of `Iterable`, i.e. it can be traversed multiple times, with multiple iterator instantiations, whereas Java's built-in `Stream` can be traversed only once.

The `Chainable` API surface also exposes various additional convenience methods for chain processing with functional programming, and is not so much oriented toward the parallelism that was a key guiding design principle behind Java's `Stream`.

Having said that, a basic level of interoperability between `Stream` and `Chainable` exists: a chain can be created from a stream (see `Chainable#from(Stream)`, and vice-versa (see `Chainable#stream()`).

(A note on the vocabulary: `Chainable` is the interface, whereas the word *"chain"* is used throughout the documentation to refer to specific instances of `Chainable`).

Overall, the current highlights of `Chainable` include:

> :warning: Section under construction as the API is currently under active development/at pre-release stage.

- **disjunctive filtering** - you can specify one or more filter predicates at the same time (see `Chainable#whereEither`), with disjuctive (logical-OR) semantics. This means you can define specific filtering predicates for specific purposes and then just supply them all as parameters, rather than having to create yet another predicate that's an *OR* of the others.

- predicate-based **skipping** of the leading sub-chain of items under various scenarios, e.g.:
  - skip *as long as* they satisfy a condition (`Chainable#notAsLongAs()`)
  - skip *before* they satisfy a condition (`Chainable#notBefore()`)
  
- predicate-based **trimming** of the trailing sub-chain of items under various scenario, e.g.:
  - stop *as soon as* the specified conditions are satisfied (`Chainable#asLongAs()`)
  - or are no longer satisfied (`Chainable#before()`.)

- **equality and sub-array containment** checks, but evaluated lazily, i.e. chains failing the equality (`Chainable#equals`) or sub-array containment (`Chainable#containsSubarray`) tests return quickly, without traversing/evaluating the rest of the chain.

- chainable **string joining/splitting** operations - you can quickly get a chain of tokens or characters out of a string (see `Chainables#split()`, process it using `Chainable` APIs and go back to a string (see `Chainable#join()`).

## System requirements

- Java 8+

## Examples

> :triangular_flag_on_post: To be continued...