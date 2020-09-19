/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.martinsawicki.collections;

import org.junit.jupiter.api.Test;

import com.github.martinsawicki.collections.Chainables.Chainable;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

/**
 * Unit tests
 */
public class ChainableTest {
    @Test
    public void testApply() {
        // Given
        Iterable<String> items = Arrays.asList("a", "b", "c");
        String expected = "axbxcx";

        // When
        Iterable<String> transformed = Chainables
                .transform(items, s -> new StringBuilder(s))
                .apply(sb -> sb.append("x"))
                .transform(sb -> sb.toString());
        String actual = Chainables.join("", transformed);

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testEmpty() {
        // Given
        Chainable<String> emptyChain = Chainable.empty();
        Chainable<String> nonEmptyChain = Chainable.from("a");

        // When/Then
        assertTrue(emptyChain.isEmpty());
        assertFalse(nonEmptyChain.isEmpty());
        assertFalse(emptyChain.any());
        assertTrue(nonEmptyChain.any());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEquals() {
        // Given
        Chainable<String> items1 = Chainable.from("A", "B", "C");
        Chainable<String> equal = Chainable.from("A", "B", "C");
        Chainable<String> superset = Chainable.from("A", "B", "C", "D");
        Chainable<String> empty = Chainable.from();
        Chainable<String> different = Chainable.from("A", "C", "X");

        // When/Then
        assertTrue(Chainables.equal(items1, equal));
        assertFalse(Chainables.equal(items1, superset));
        assertFalse(Chainables.equal(superset, items1));
        assertFalse(Chainables.equal(items1, empty));
        assertFalse(Chainables.equal(empty, items1));
        assertFalse(Chainables.equal(items1, different));
        assertFalse(Chainables.equal(empty, items1));
        assertTrue(Chainables.equal(empty,  empty));
        assertTrue(items1.equalsEither(different, equal));
        assertFalse(items1.equalsEither(different, superset));
    }

    @Test
    public void testFrom() {
        // Given
        String inputs[] = { "A", "B", "C", "D" };
        String expected = String.join("", inputs);
        String expectedTransformed = "abcd";

        // When
        Chainable<String> items = Chainable.from("A", "B", "C", "D");
        String actual = Chainables.join("", items);

        Chainable<String> itemsTransformed = items.transform(s -> s.toLowerCase());
        String actualTransformed = Chainables.join("", itemsTransformed);

        // Then
        assertEquals(expected, actual);
        assertEquals(expectedTransformed, actualTransformed);
    }

    @Test
    public void testJoin() {
        // Given
        List<String> items = Arrays.asList("a", "b", "c", "d");
        Chainable<String> chain = Chainable.from(items);
        String expected = String.join(", ", items);

        // When
        String actual = Chainables.join(", ", chain);

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testSize() {
        // Given
        List<Integer> items = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
        int expectedItemsSize = items.size();
        Chainable<Integer> itemsChain = Chainable.from(items);
        Chainable<Integer> emptyChain = Chainable.empty();
        Chainable<String> transformedChain = itemsChain.transform(o -> o.toString());

        // When
        int actualItemsChainSize = itemsChain.size();
        int actualEmptyChainSize = emptyChain.size();
        int actualTransformedChainSize = transformedChain.size();

        // Then
        assertEquals(expectedItemsSize, actualItemsChainSize);
        assertEquals(0, actualEmptyChainSize);
        assertEquals(expectedItemsSize, actualTransformedChainSize);
    }

    @Test
    public void testToList() {
        // Given
        Chainable<String> chain = Chainable.from("a", "b", "c");
        String expected = "abc";

        // When
        List<String> list = chain.toList();
        String actual = String.join("", list);

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testTransform() {
        // Given
        Iterable<String> items = Arrays.asList("a", "aa", "aaa", "aaaa");

        // When
        Iterable<Integer> lengths = Chainables.transform(items, String::length);
        int sum = 0;
        for (int l : lengths) {
            sum += l;
        }

        // Then
        assertEquals(10, sum);
    }

    @Test
    public void testTransformAndFlatten() {
        // Given
        String[][] items = { { "a", "b" }, { "c", "d", "e" }, null, { "f" }};
        Iterable<String[]> list = Arrays.asList(items);
        String expected = "abcdef";

        // When
        Iterable<String> transformed = Chainables.transformAndFlatten(list, o -> (o != null) ? Arrays.asList(o) : null);
        String actual = Chainables.join("", transformed);

        // Then
        assertEquals(expected, actual);
    }
}
