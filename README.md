# Chainable

> :warning: Under construction / Work in progress / Coming soon...

## Overview

`Chainable` is intended to be a richer, `Iterable`-based alternative to Java's `Stream` and Google's *guava*, but focused on sequence and tree processing specifically. It is heavily inspired by the **iterator pattern**, **functional programming**, **lazy evaluation** and C#'s `Enumerable`, but also extended into areas of functionality not addressed by older approaches. It is designed to enable writing code that is more succinct, readable, simpler to implement, and sometimes faster than its non-lazy/non-functional equivalents.

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

Although `Chainable` overlaps with Java's `Stream` in some areas of functionality, the design of `Chainable` is optimized for a somewhat different set of goals, manifested in a number of desing and functional differences:

- Unlike streams, `Chainable` derives from `Iterable` to enable easy use inside of the "for-each" flavor of the for loop.
- Since it is `Iterable`, it is "re-entrant" in the sense that multiple iterators can be instantiated against the same chain
- Unlike streams, *chainable* also provides functional programming-based API for trees (or more specifically, *tries*), seamlessly integrated with its sequence processing API, as well as other (future) basic data structures.
- Since `Chainanable` chains are  re-entrant as mentioned earlier, they support caching of the already evaluated items in the chain, if that is what the programmer chooses to enable (see `Chainable#cached()`). This may be especially useful if the underlying sequence is not expected to change upon subsequent traversals, or if it is based on a Stream (which by definition is not re-entrant).
- `Chainable` API surface exposes various additional convenience methods for sequential chain processing with functional programming that are not present in streams
- `Chainable` is not (currently) oriented toward the parallelism that was a key guiding design principle behind Java's `Stream`.
- Some of the overlapping APIs in streams are only available starting with Java 9, whereas `Chainable` is fully functional starting with Java 8.

Having said that, a level of interoperability between `Stream` and `Chainable` exists: a chain can be created from a stream (see `Chainable#from(Stream)`, and vice-versa (see `Chainable#stream()`). A stream wrapped in a Chainable appears reentrant, even though it is traversed only once: the chain wrapper automatically caches the already evaluated stream members and only accesses the underlying stream for not yet visited items.

### Highlights

In general, some of the current key highlights of `Chainable` include:

#### Sequence processing
> :warning: Section under construction as the API is under active development/at pre-release stage.

- **interleaving** (see `Chainable#interleave`) - two or more chains that have their own evaluation logic can be interleaved,
so that subsequent chain can apply its logic to their outputs in a quasi-parallel (or sequential round-robin) fashion, so as not to have a bias toward one chain first, while still not actually being concurrent.
![Interleave](./src/main/java/doc-files/img/interleave.png)

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

#### Tree processing

> :warning: Section under construction as the API is under active development/at pre-release stage.

The functional programming-based tree (trie) support (`ChainableTree`) is a particularly unique feature of the chainable API, built on top of `Chainable` sequences. A chainable tree can be defined using a lazily-evaluated lambda that returns the children of a given tree node only when requested. A number of capabilities stem from this:

- **infinite trees**, that is trees of infinite depth, can be easily defined in terms of a lambda that dyamically generates children based on the parent. For example, the code below defines a lazily-evaluated tree that consists of all the possible permutations of the letters *a*, *b* and *c*, without a length limit:

```java
        char[] alphabet = { 'a', 'b', 'c' };        // Define alphabet to take letters from
        ChainableTree<String> permutations = ChainableTree
                .withRoot("")                       // Blank string at the root
                .withChildValueExtractor(p -> Chainable
                        .empty(String.class)        // Start with empty chain of strings
                        .chainIndexed((s, i) -> p + alphabet[i.intValue()]) // Append each alphabet item to the parent
                        .first(alphabet.length)); // Limit the children chain to the size of the alphabet
```

  If you were to traverse this tree, its initial few layers would look like this:

```
- (blank root)
 - a
  - aa
   - aaa
     ...
  - ab
   - aba
     ...
  - ac
   - aca
     ...
 - b
  - ba
    ...
 - c
  - ca
    ...
```

- ***tree trimming** - (especially useful for infinite trees) a chainable tree can be limited in depth based on either a lazily-evaluated condition that the lowermost descendants are to meet (see `notBelowWhere()`, where the condition is specified as a predicate lambda) or a numerical maximum depth of the tree. This enables methods that may result in a full traversal of the tree (such as `firstWhere()`) to eventually return.

For example, the following code, which builds on the `infinitePermutations` tree from the previous example, results in a tree that is eventually limited to 3 layers of depth:

```java
        // Limit the depth of the infinite tree to the level of 3-letter strings
        String text = permutationsUptoLength5 = permutations
                .notBelowWhere(t -> t.value().length() >= maxLength) // Limit tree depth to 5 levels
                .breadthFirst()                                      // Create chain from breadth-first traversal
                .afterFirst()                                        // Skip the empty root
                .join(", ");

        System.out.println(text);
```

The beginning of the output text will be: `a, b, c, aa, ab, ac, ba, bb, bc, ca, cb, cc, aaa, aab, ...`


> :triangular_flag_on_post: To do...

## System requirements

- Java 8+

## Usage (Maven)

> :triangular_flag_on_post: To do...

## Examples

### Chains

#### Fibonacci Sequence

In this example, each next item is the sum of the previous two preceding it in the chain:

```java
    // When
    String fibonacciFirst8 = Chainable
        .from(0l, 1l)   // Starting values for Fibonacci
        .chain((i0, i1) -> i0 + i1) // Generate next Fibonacci number
        .first(8)       // Take first 8 items
        .join(", ");    // Merge into a string

    assertEquals(
        "0, 1, 1, 2, 3, 5, 8, 13",
        fibonacciFirst8);
```

The flavor of the `chain()` method used above feeds the user-specified lambda with the two preceding items.

#### Interleaving odd numbers with evens

In this examples, a chain of odd numbers is interleaved with a chain of even numbers to produce a chain of natural numbers:

```java
    final Chainable<Long> odds = Chainable
        .from(1l)           // Start with 1
        .chain(o -> o + 2); // Generate infinite chain of odd numbers

    final Chainable<Long> evens = Chainable
        .from(2l)           // Start with 2
        .chain(o -> o + 2); // Generate infinite chain of even numbers

    String naturals = odds
        .interleave(evens) // Interleave odds with evens
        .first(10)         // Take the first 10 items 
        .join(", ");       // Merge into a string

    assertEquals(
        "1, 2, 3, 4, 5, 6, 7, 8, 9, 10"
        naturals);
```

> :triangular_flag_on_post: To do...

### Trees

> :triangular_flag_on_post: To do...