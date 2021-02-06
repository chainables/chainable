/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.chainables.chainable;

import static com.github.chainables.chainable.Chainable.chain;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

public class Examples {
    @SuppressWarnings("unchecked")
    @Test
    public void initialExample() {
        Chainable<String> chain =
                chain(0, 0, 0, 2, 3, 7, 0, 1, 8, 3, 13, 14, 0, 2)   // Integers
                    .notAsLongAs(i -> i == 0)                       // Ignore leading sub chain of 0s
                    .notAfter(i -> i == 13)                         // Stop after finding 13
                    .whereEither(                                   // Choose only those that...
                        i -> i % 2 == 0,                            // ...are even
                        i -> i > 6)                                 // ...or greater than 6
                    .transform(i -> Character.toString((char) (i + 65)));   // Transform into letters

        String text = chain.join();                                 // Merge into a string
        String textBackwards = chain.reverse().join();              // Reverse and merge into a string

        assertEquals("CHAIN", text);
        assertEquals("NIAHC", textBackwards);
    }

    public interface Foo {};
    public static final List<Foo> existingIterable = new ArrayList<>();
    public static final Stream<Foo> existingStream = Stream.empty();

    @SuppressWarnings("unchecked")
    @Test
    public void gettingStarted() {
        // From pre-defined values
        Chainable<String> chain1 = chain("a", "b", "c");

        // Empty but expecting String items
        Chainable<String> chain2 = chain(String.class);

        // From an existing Iterable<Foo>
        Chainable<Foo> chain3 = chain(existingIterable);

        // From an existing Stream<Foo>
        Chainable<Foo> chain4 = chain(existingStream);

        // Example tree of String values
        ChainableTree<String> tree = ChainableTree.withRoot("root");

        // Assign explicit child subtrees
        tree.withChildren(
                ChainableTree
                    .withRoot("1")
                    .withChildValues("1.1", "1.2"),
                ChainableTree
                    .withRoot("2")
                    .withChildValues("2.1", "2.2"));
    }

    @Test
    public void infiniteTrees() {
        char[] alphabet = { 'a', 'b', 'c' };                // Define alphabet to take letters from
        ChainableTree<String> permutations = ChainableTree
                .withRoot("")                               // Blank string at the root
                .withChildValueExtractor(p -> Chainable
                        .empty(String.class)                // Start with an empty chain of strings
                        .chainIndexed((s, i) -> p + alphabet[i.intValue()]) // Append each alphabet item to the parent
                        .first(alphabet.length));           // Limit the children chain to the size of the alphabet

        String text = permutations
                .notBelowWhere(t -> t.value().length() >= 3) // Limit permutation length to 3 letters
                .breadthFirst()                              // Create chain from breadth-first traversal
                .afterFirst()                                // Skip the empty root
                .join(", ");

        System.out.println(text);
        assertEquals(
                "a, b, c, aa, ab, ac, ba, bb, bc, ca, cb, cc, "
                + "aaa, aab, aac, aba, abb, abc, aca, acb, acc, "
                + "baa, bab, bac, bba, bbb, bbc, bca, bcb, bcc, "
                + "caa, cab, cac, cba, cbb, cbc, cca, ccb, ccc",
                text);
    }

    @Test
    public void fibonacci() {
        String fibonacciFirst8 = chain(0l, 1l)  // Starting values for Fibonacci
                .chain((i0, i1) -> i0 + i1)     // Generate next Fibonacci number
                .first(8)                       // Take first 8 items
                .join(", ");                    // Merge into a string

        assertEquals("0, 1, 1, 2, 3, 5, 8, 13", fibonacciFirst8);        
    }

    @Test
    public void interleaveNaturals() {
        // Define infinite chain of odd numbers starting with 1
        final Chainable<Long> odds = chain(1l).chain(o -> o + 2);

        // Define infinite chain of even numbers starting with 2
        final Chainable<Long> evens = chain(2l).chain(o -> o + 2);

        String naturals = odds
            .interleave(evens) // Interleave odds with evens
            .first(10)         // Take the first 10 items 
            .join(", ");       // Merge into a string

        assertEquals("1, 2, 3, 4, 5, 6, 7, 8, 9, 10", naturals);
    }
}
