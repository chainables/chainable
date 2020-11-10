# Chainable

> :warning: Under construction / Work in progress / Coming soon...

## Overview

`Chainable` is intended to be a rich, `Iterable`-based alternative to Java's `Stream` and Google's *guava*, but focused on sequence and tree processing specifically using lambdas and command chaining. It is heavily inspired by the iterator pattern, functional programming, lazy evaluation and C#'s `Enumerable`, but also extended into areas of functionality not addressed by older approaches. It is designed to enable writing powerful yet readable code quickly, succinctly, and performing sometimes faster than its non-lazy/non-functional equivalents.

The implementation is lightweight and self-contained, i.e. it has no external dependencies, so as not to contribute to any sub-dependency versioning challenges.

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

### Chainable vs Java Stream

Although `Chainable` overlaps with Java's `Stream` in some areas of functionality, the design of `Chainable` is optimized for a somewhat different set of goals, manifested in a number of design and functional differences:

- Unlike streams, `Chainable` derives from `Iterable` to simplify usage inside the *for-each* flavor of the `for` loop.

- By virtue of deriving from `Iterable`, chainables are "re-entrant" in the sense that multiple iterators can be instantiated against the same chain.

- Unlike Java's streams, the *Chainables* library also provides functional programming-based API for trees (or more specifically, *tries*), seamlessly integrated with the `Chainable` sequence processing API, as well as other (future) basic data structures.

- `Chainable` supports caching of the already evaluated items in the chain, if that is what the programmer chooses to enable (see `cached()`). This can be especially useful if the underlying sequence is not expected to change before subsequent traversals, or if it is wrapping a Java `Stream` (which itself by definition is not re-entrant.)

- `Chainable` exposes various additional convenience methods for sequential processing with functional programming that are not present in streams

- While some of the overlapping APIs in Java's `Stream` are only available starting with Java 9, `Chainable` is fully functional starting with Java 8.

- `Chainable` is not (currently) oriented toward the parallelism that was a key guiding design principle behind Java's `Stream`.

Functional and design differences with streams aside, a level of interoperability between `Stream` and `Chainable` exists: a chain can be created from a stream (see `Chainable#from(Stream)`, and vice-versa (see `Chainable#stream()`). A chain wrapping a stream makes the stream *appear* reentrant, even though in reality it is traversed only once under the hood. That is because the chain wrapper for the stream automatically caches the already evaluated stream items and only accesses the underlying stream for not yet visited items.

### Highlights

In general, some of the current key highlights of `Chainable` include:

#### Sequence processing
> :warning: Section under construction as the API is under active development/at pre-release stage.

- **single pass caching** - By default, each re-iteration over a given chain re-evaluates the specified lambdas. But using the `chached()` method, you can create a chain that is lazily evaluated only on the first complete pass, when it is iterated all the way to the end, assuming it is not infinite. From then on, subsequent iterations over the same chain would only navigate through the internally cached outputs of that initial pass, no longer evaluating the provided lambdas. This means the cached chain, upon subsequent traversals, starts behaving de-facto like a `List`.

- **interleaving** - Two or more chains that have their own evaluation logic can be interleaved using the `interleave()` method,
so that a subsequent chain can apply its logic to their outputs in a quasi-parallel (or sequential round-robin) fashion, while still not actually being concurrent.

  ![Interleave](./src/main/java/doc-files/img/interleave.png)

- **breadth-first / depth-first traversal** - You can achieve a tree-like traversal of a chain, where children of each item extracted by the child-extracting lambda are inserted immediately ahead (`depthFirst()`) or appended to the end of the chain (`breadthFirst()`), thereby resulting in a pre-order/depth-first or breadth-first traversal respectively.

- **disjunctive filtering** - Using the `whereEither()` method, you can specify one or more filter predicates at the same time, with disjunctive (logical-OR) semantics. This means you can define specific filtering predicates for specific purposes and then just supply them all as parameters, rather than having to create yet another predicate that's an *OR* of the others.

- **skipping** of the leading sub-chain of items under various scenarios, e.g.:
  - skip *as long as* they satisfy a condition (`notAsLongAs()`)
  - skip *before* they satisfy a condition (`notBefore()`)
  
- **trimming** of the trailing sub-chain of items under various scenario, e.g.:
  - stop *as soon as* the specified conditions are satisfied (`asLongAs()`)
  - or are no longer satisfied (`before()`.)

- **equality and sub-array containment checks**, evaluated lazily. Chains failing the equality (`equals()`) or sub-array containment (`containsSubarray()`) tests return quickly, without traversing/evaluating the rest of the chain.

- chainable **string joining/splitting** operations - You can get a chain of tokens or characters out of a string with Chainable's `split()` method, process it using various `Chainable` APIs and go back to a string using Chainable's `join()`.

#### Tree processing

> :warning: Section under construction as the API is under active development/at pre-release stage.

Tightly integrated with `Chainable`, the functional programming-based tree (trie) support (`ChainableTree`) is a particularly distinguishing feature of the *Chainables* library. A chainable tree can be defined using a lazily-evaluated lambda, which does not evaluate/traverse the children of a given parent until necessary. A number of capabilities stem from this:

- **infinite trees**, or trees of infinite depth, can be easily defined in terms of children-generating lambdas. For example, the code below defines a lazily evaluated infinite tree made of all the possible permutations of the letters *a*, *b* and *c*, where each layer of the tree consists of nodes of increasingly longer strings:

```java
        char[] alphabet = { 'a', 'b', 'c' };        // Define alphabet to take letters from
        ChainableTree<String> permutations = ChainableTree
                .withRoot("")                       // Blank string at the root
                .withChildValueExtractor(p -> Chainable
                        .empty(String.class)        // Start with an empty chain of strings
                        .chainIndexed((s, i) -> p + alphabet[i.intValue()]) // Append each alphabet item to the parent
                        .first(alphabet.length)); // Limit the children chain to the size of the alphabet
```

  If you were to begin to traverse this infinite tree, its initial few layers would look like this:

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

- **tree trimming** - Especially useful for infinite trees, a chainable tree can be limited in depth based on either a lazily evaluated predicate condition that the lowermost descendants are to meet (see `notBelowWhere()`) or a numerical maximum depth of the tree. This enables methods that may result in a full traversal of the tree (such as `firstWhere()`) to *eventually* return, which they might not otherwise do if the tree is infinite.

For example, the following code, which builds on the previous example, results in a view of the previously defined tree that is limited to de-facto 3 layers of depth for any subsequent logic applied to it:

```java
        String text = permutationsUptoLength3 = permutations
                .notBelowWhere(t -> t.value().length() >= 3) // Limit permutation length to 3 letters
                .breadthFirst()                              // Create chain from breadth-first traversal
                .afterFirst()                                // Skip the empty root
                .join(", ");

        System.out.println(text);
```

The beginning of the output text, which lists all the permutations of *a*, *b*, and *c*, up to 3 letters in length, will be: `a, b, c, aa, ab, ac, ba, bb, bc, ca, cb, cc, aaa, aab, ...`


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

#### Interleaving two chains

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

#### Breadth-first traversal

> :triangular_flag_on_post: To do...

#### Finding an item matching some criteria

> :triangular_flag_on_post: To do...

#### Infinite tree of permutations of 3 letters

In this example, an infinite tree is defined with a child extracting lambda that generates strings as permutations of letters from the specified alphabet (*a, b, c*) of increasingly greater length. Then, a "view" of the tree is defined, limiting its depth to 4 layers (including the empty root). Finally, it is transformed into a string listing of all the permutations:

  ```java
        char[] alphabet = { 'a', 'b', 'c' };        // Define alphabet to take letters from
        ChainableTree<String> permutations = ChainableTree
                .withRoot("")                       // Blank string at the root
                .withChildValueExtractor(p -> Chainable
                        .empty(String.class)        // Start with empty chain of strings
                        .chainIndexed((s, i) -> p + alphabet[i.intValue()]) // Append each alphabet item to the parent
                        .first(alphabet.length));   // Limit the children chain to the size of the alphabet

        // Prepare a listing of the permutations
        String text = permutationsUptoLength3 = permutations
            .notBelowWhere(t -> t.value().length() >= 3)    // Limit permutation length to 3 letters
            .breadthFirst()                                 // Create chain from breadth-first traversal
            .afterFirst()                                   // Skip the empty root
            .join(", ");

        System.out.println(text);
  ```

The beginning of the output text here, which lists all the permutations of *a*, *b*, and *c* up to 3 letters in length, will be: `a, b, c, aa, ab, ac, ba, bb, bc, ca, cb, cc, aaa, aab, ...`

> :triangular_flag_on_post: To do...