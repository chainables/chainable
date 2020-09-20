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
import java.util.stream.Stream;

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
    public void testContains() {
        // Given
        Chainable<String> items = Chainable.from("a", "b", "c");

        // When/Then
        assertTrue(items.contains("b"));
        assertFalse(items.contains("d"));
        assertTrue(items.containsAny("b", "d"));
        assertFalse(items.containsAny("x", "y"));
        assertTrue(items.containsAll("c", "b"));
        assertFalse(items.containsAll("b", "d"));
    }

    @Test
    public void testContainsAll() {
        // Given
        String items[] = { "a", "b", "c", "d" };
        Chainable<String> chain = Chainable.from(items);

        // When / Then
        assertTrue(chain.containsAll(items));
    }

    @Test
    public void testContainsSubarray() {
        // Given
        Chainable<String> items1 = Chainable.from("a", "b", "x", "a", "b", "c", "d");
        Iterable<String> subarray1 = Arrays.asList("a", "b", "c");
        Iterable<String> notSubarray1 = Arrays.asList("a", "b", "x", "y");

        // When/Then
        assertTrue(items1.containsSubarray(subarray1));
        assertFalse(items1.containsSubarray(notSubarray1));
        //TODO: More tests
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
    public void testStreamBasics() {
        // Given
        Integer inputs[] = { 1, 2, 3, 4, 5, 6, 7, 8 };
        String expected = Chainables.join("", Chainable.from(inputs).where(i -> i % 2 != 0));

        // When
        Chainable<Integer> chain = Chainable.from(Stream
                .of(inputs)
                .filter(i -> i % 2 != 0));
        String actual = Chainables.join("", chain);

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testStreamGeneration() {
        // Given
        Chainable<Integer> ints = Chainable.from(1, 3, 2, 5, 2);
        String expected = "22";

        // When
        Stream<Integer> stream = ints.stream().filter(i -> i % 2 == 0);
        StringBuilder info = new StringBuilder();
        stream.forEach(o -> info.append(o.toString()));
        String actual = info.toString();

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testStreamReEntry() {
        // Given
        Integer inputs[] = { 1, 2, 3, 4, 5, 6, 7, 8 };
        String expected = Chainables.join("", Chainable.from(inputs).where(i -> i % 2 != 0));

        // When
        Chainable<Integer> chain = Chainable.from(Stream
                .of(inputs)
                .filter(i -> i % 2 != 0));
        String actual = Chainables.join("", chain);
        actual = Chainables.join("", chain); // Again, since streams are normally traversable only once

        // Then
        assertEquals(expected, actual);
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

    @Test
    public void testWhere() {
        // Given
        Iterable<Integer> testList = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        String expected = "aabac";

        // When
        Iterable<Integer> greaterThan4 = Chainables.whereEither(testList, o -> o > 4);

        Chainable<String> where = Chainable
                .from("a", "b", "c", "ab", "ac", "bc")
                .where(s -> s.startsWith("a"));
        String actual = Chainables.join("", where);

        // Then
        assertEquals(5, Chainables.count(greaterThan4));
        assertEquals(expected, actual);
    }

    @Test
    public void testWithoutNull() {
        // Given
        Iterable<String> items = Arrays.asList("a", null, "b", null);
        String expected = "ab";

        // When
        Chainable<String> withoutNull = Chainables.withoutNull(items);
        String actual = Chainables.join("", withoutNull);

        // Then
        assertEquals(expected, actual);
    }
}
