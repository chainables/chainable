# Chainable

> :warning: Under construction / Work in progress / Coming soon...

## Overview

`Chainable` is an `Iterable`-based alternative to Java's `Stream`, focused on delivering richer fluent API for achieving sequential access logic. It is heavily inspired by the **iterator pattern**, **functional programming**, **lazy evaluation** and C#'s `Enumerable`, but also extended into areas of functionality not yet addressed by older approaches. It is intended to enable writing code that is more succinct, readable, simpler to implement, and sometimes faster than its non-lazy/non-functional equivalents.

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

The implementation is lightweight and self-contained, i.e. it has no external dependencies, so as not to contribute to any sub-dependency versioning challenges.

(A note on the terminology: `Chainable` is the interface, whereas the word *"chain"* is used throughout the documentation to refer to specific instances of `Chainable`).

### Chainable vs Java Stream

Although `Chainable` overlaps with Java's `Stream` in some areas of functionality, the design of `Chainable` is optimized for a somewhat different set of goals and aligns more with C#'s `Enumerable`'s LINQ-style methods than Java streams.

For example, one of the key differences from `Stream` is that `Chainable` fully preserves the functional and re-entrancy semantics of `Iterable`, that is it can be traversed multiple times, with multiple iterator instantiations, whereas Java's built-in `Stream` can be traversed only once.

The `Chainable` API surface also exposes various additional convenience methods for sequential chain processing with functional programming, and not so much oriented toward the parallelism that was a key guiding design principle behind Java's `Stream`. Also, some of the overlapping APIs are only available in Java streams starting with Java 9, whereas `Chainable` is fully functional from Java 8 on.

Having said that, a level of interoperability between `Stream` and `Chainable` exists: a chain can be created from a stream (see `Chainable#from(Stream)`, and vice-versa (see `Chainable#stream()`).

### Highlights

Besides the part of the API overlapping with streams, current highlights of `Chainable` include:

> :warning: Section under construction as the API is under active development/at pre-release stage.

- **interleaving** (see `Chainable#interleave`) - two or more chains that have their own evaluation logic can be interleaved,
so that subsequent chain can apply to their outputs in a quasi-parallel (or sequential round-robin) fashion, so as not to have a bias toward one chain first, while still not actually being concurrent.

- **breadth-first/depth-first traversal** - enabling tree-like traversals of a chain of items, where children of an item are dynamically added by the caller-specified child extractor and traversed either breadth-first (queue-like, `Chainable#breadthFirst()`) or depth-first (stack-like, `Chainable#depthFirst()`), both in a lazy fashion.

- **single pass caching** - by default, each re-iteration over a given chain re-evaluates the lambdas, just like in a typical `Iterable`. But it is possible to create a chain that is lazy-evaluated only on the first pass, i.e. when it is iterated all the way to the end (see `Chainable#cached()`). From then on, subsequent iterations over the same chain would only navigate through the internally cached outputs of that initial pass, no longer evaluating the provided lambdas. That means the cached chain, upon subsequent traversals, starts behaving like a collection. 

- **disjunctive filtering** - you can specify one or more filter predicates at the same time (see `Chainable#whereEither`), with disjuctive (logical-OR) semantics. This means you can define specific filtering predicates for specific purposes and then just supply them all as parameters, rather than having to create yet another predicate that's an *OR* of the others.

- **skipping** of the leading sub-chain of items under various scenarios, e.g.:
  - skip *as long as* they satisfy a condition (`Chainable#notAsLongAs()`)
  - skip *before* they satisfy a condition (`Chainable#notBefore()`)
  
- **trimming** of the trailing sub-chain of items under various scenario, e.g.:
  - stop *as soon as* the specified conditions are satisfied (`Chainable#asLongAs()`)
  - or are no longer satisfied (`Chainable#before()`.)

- **equality and sub-array containment** checks, but evaluated lazily, i.e. chains failing the equality (`Chainable#equals`) or sub-array containment (`Chainable#containsSubarray`) tests return quickly, without traversing/evaluating the rest of the chain.

- chainable **string joining/splitting** operations - you can quickly get a chain of tokens or characters out of a string (see `Chainables#split()`, process it using `Chainable` APIs and go back to a string (see `Chainable#join()`).

### Performance Considerations

In general, although Java (as of v8) supports a notion of functional programming with lambdas, Java is known not to be optimized for functional programming's performance. Hence, in scenarios where code speed is a topmost priority, this style of programming may not be preferable, regardless of whether `Chainable` or Java's `Stream` is considered.

However, if most of the processing time is spent by the application on fetching data from external sources (an inherently slower process), or where the evaluation of each item in a sequence is time-consuming in general, then lazy evaluation enabled by `Chainable` can result in significant performance gains. It is in those scenarios where the benefits of functional programming begin to greatly outweigh the performance costs of Java's lambda support. (Not to mention the improved code readability and succinctness that can be achieved.)

With this in mind, based on some initial testing in Java 8 so far, `Chainable` currently appears comparable in performance to `Streams`, outperforming it slightly in some circumstances and under-performing it slightly in some others. Although under the hood `Chainable` works quite differently from `Stream`, the biggest factor in the performance of either approach is ultimately the JVM's handling of lambdas, arguably more than anything else. This likely explains why the performance differences between the two so far are not significant (see existing benchmarks in [PerfTest.java](https://github.com/martinsawicki/chainable/blob/dev/src/test/java/com/github/martinsawicki/chainable/PerfTest.java)). 

## System requirements

- Java 8+

## Usage (Maven)

> :triangular_flag_on_post: To do...

## Examples

> :triangular_flag_on_post: To do...