/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.martinsawicki.collections;

import org.junit.jupiter.api.Test;

import com.github.martinsawicki.collections.Chainables.Chainable;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Unit tests
 */
public class ChainableTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testExample() {
        // Given / When
        Chainable<String> chain = Chainable
                .from(0, 0, 0, 2, 3, 7, 0, 1, 8, 3, 13, 14, 0, 2) // Integers
                .notAsLongAs(i -> i == 0) // Ignore leading sub chain of 0s
                .notAfter(i -> i == 13) // Stop after finding 13
                .whereEither( // Choose only those that...
                        i -> i % 2 == 0, // ...are even
                        i -> i > 6) // ...or greater than 6
                .transform(i -> Character.toString((char) (i + 65))); // Transform into letters

        String text = chain.join(); // Merge into a string
        String textBackwards = chain.reverse().join(); // Reverse and merge into a string

        // Then
        assertEquals("CHAIN", text);
        assertEquals("NIAHC", textBackwards);
    }

    @Test
    public void testApply() {
        // Given
        Iterable<String> items = Arrays.asList("a", "b", "c");
        String expected = "axbxcx";

        // When
        String actual = Chainables
                .transform(items, s -> new StringBuilder(s))
                .apply(sb -> sb.append("x"))
                .transform(sb -> sb.toString())
                .join();

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testAsLongAs() {
        // Given
        Chainable<Integer> integers = Chainable.from(1, 1, 1, 2, 1, 1);
        String expectedText = "111";

        // When
        String actualText  = integers.asLongAs(i -> i == 1).join();
        String actualTextAsLongAs0 = integers.asLongAs(i -> i == 0).join();

        // Then
        assertEquals(expectedText, actualText);
        assertEquals("", actualTextAsLongAs0);
    }

    @Test
    public void testAsLongAsValue() {
        // Given
        Iterable<String> testList = Arrays.asList("a", "a", "c", "d", "e");
        String expected = "aa";

        // When
        String actual = Chainables.asLongAsValue(testList, "a").join();

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testAtLeastMost() {
        // Given
        Iterable<Integer> items = Arrays.asList(1, 2, 3);

        // When/Then
        assertTrue(Chainables.atLeast(items, Chainables.count(items)));
        assertTrue(Chainables.atLeast(items, Chainables.count(items) - 1));
        assertTrue(Chainables.atLeast(items, 0));
        assertFalse(Chainables.atLeast(items, Chainables.count(items) + 1));
        assertFalse(Chainables.atLeast(Collections.emptyList(), 1));
        assertTrue(Chainables.atLeast(Collections.emptyList(), 0));

        assertTrue(Chainables.atMost(items, Chainables.count(items)));
        assertTrue(Chainables.atMost(items, Chainables.count(items) + 1));
        assertFalse(Chainables.atMost(items, Chainables.count(items) - 1));
        assertFalse(Chainables.atMost(items, 0));
        assertTrue(Chainables.atMost(Collections.emptyList(), 0));
        assertTrue(Chainables.atMost(Collections.emptyList(), 1));
    }

    @Test
    public void testBeforeValue() {
        // Given
        Iterable<String> testList = Arrays.asList("a", "a", "c", "d", "e");
        String expected = "aac";

        // When
        String actual = Chainables.beforeValue(testList, "d").join();
        Chainable<String> until2 = Chainables.beforeValue(testList, "a");

        // Then
        assertEquals(expected, actual);
        assertTrue(Chainables.isNullOrEmpty(until2));
    }

    @Test
    public void testBreadthFirst() {
        // Given
        final int depth = 4;
        Chainable<String> initial = Chainable.from("1");
        Function<String, Iterable<String>> childExtractor = (s) -> (s.length() < (depth - 1)  * 2) ? Chainable.from(s + ".1", s + ".2") : null;
        String expectedBreadthFirst = "1, 1.1, 1.2, 1.1.1, 1.1.2, 1.2.1, 1.2.2, 1.1.1.1, 1.1.1.2, 1.1.2.1, 1.1.2.2, 1.2.1.1, 1.2.1.2, 1.2.2.1, 1.2.2.2";

        // When
        String actualBreadthFirst = initial.breadthFirst(childExtractor).join(", ");

        // Then
        assertEquals(expectedBreadthFirst, actualBreadthFirst);
    }

    @Test
    public void testBreadthFirstUntil() {
        // Given
        Chainable<String> roots = Chainable.from("a", "b", "c");
        String expected = Chainable
                .from("a", "b", "c", "aa", "ab", "ac", "ba", "bb", "bc", "ca", "cb", "cc")
                .join(",");

        // When
        String actual = roots.breadthFirstUntil(
                s -> roots.transform(o -> s + o),
                s -> s.length() == 2)
                .join(",");

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testBreadthFirstWhile() {
        // Given
        Chainable<String> roots = Chainable.from("a", "b", "c");
        Chainable<String> expectedResults = Chainable.from("a", "b", "c", "aa", "ab", "ac", "ba", "bb", "bc", "ca", "cb", "cc");
        String expected = String.join(",", expectedResults);

        // When
        Chainable<String> results = roots.breadthFirstWhile(
                s -> roots.transform(o -> s + o),
                s -> s.length() < 3);
        String actual = String.join(",", results);

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testCollectInto() {
        // Given
        final Chainable<Integer> items = Chainable.from(1, 2, 3, 4, 5, 6, 7);
        String expected = "135";
        List<String> collection = new ArrayList<>();

        // When
        Chainable<String> oddsLessThan6 = items
                .where(i -> i < 6 && i%2 != 0)
                .transform(i -> i.toString())
                .collectInto(collection)
                .apply();
        String actual = String.join("", collection);

        // Then
        assertEquals(expected.length(), oddsLessThan6.size());
        assertEquals(expected, actual);
    }

    @Test
    public void testConcatFunctional() {
        // Given
        Iterable<String> items1 = Arrays.asList("a", "b", "c");

        // When
        String actual = Chainable
                .from(items1)
                .concat(i -> (i != "b") ? Chainable.from("1", "2", "3") : Chainable.from())
                .join();

        // Then
        assertEquals("a123bc123", actual);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConcatSimple() {
        // Given
        Iterable<String> items1 = Arrays.asList("a", "b", "c");
        Iterable<String> items2 = null;
        Iterable<String> items3 = Arrays.asList("d", "e", "f");
        Iterable<String> items4 = Arrays.asList("g", "h", "i");

        // When
        Chainable<String> combined = Chainables
                .concat(items1, items2)
                .concat(items3)
                .concat(items4);

        String list = combined.join();
        String list2 = Chainable
                .from(items1)
                .concat(items2, items3, items4)
                .join();

        // Then
        assertEquals(9, Chainables.count(combined));
        assertEquals("abcdefghi", list);
        assertEquals("abcdefghi", list2);
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
    public void testDepthFirst() {
        // Given
        final int depth = 4;
        Chainable<String> initial = Chainable.from("1");
        Function<String, Iterable<String>> childExtractor = (s) -> (s.length() < (depth - 1)  * 2) ? Chainable.from(s + ".1", s + ".2") : null;
        String expectedDepthFirst = "1, 1.1, 1.1.1, 1.1.1.1, 1.1.1.2, 1.1.2, 1.1.2.1, 1.1.2.2, 1.2, 1.2.1, 1.2.1.1, 1.2.1.2, 1.2.2, 1.2.2.1, 1.2.2.2";

        // When
        String actualDepthFirst = initial.depthFirst(childExtractor).join(", ");

        // Then
        assertEquals(expectedDepthFirst, actualDepthFirst);
    }

    @Test
    public void testDistinct() {
        // Given
        Chainable<String> items = Chainable.from("a", "b", "c", "a", "d", "b");
        String expected = "abcd";

        // When
        String actual = items.distinct().join();

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testDistinctKey() {
        // Given
        Chainable<Integer> numbers = Chainable.from(1, 3, 4, 5, 8);
        String expected = "14";

        // When
        String actual = numbers.distinct(i -> i % 2).join();

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

    @Test
    public void testEndsWith() {
        // Given
        Iterable<String> items = Arrays.asList("a", "b", "c", "d", "e");
        Iterable<String> suffix = Arrays.asList("d", "e");
        Iterable<String> copy = Arrays.asList("a", "b", "c", "d", "e");
        Iterable<String> nonSuffix = Arrays.asList("a", "b");
        Iterable<String> empty = Arrays.asList();

        // When/Then
        assertTrue(Chainables.endsWithEither(items, suffix));
        assertTrue(Chainables.endsWithEither(items, copy));
        assertFalse(Chainables.endsWithEither(items, nonSuffix));
        assertTrue(Chainables.endsWithEither(items, empty));
        assertTrue(Chainables.endsWithEither(items, nonSuffix, suffix));
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
    public void testFirstNumber() {
        // Given
        final Chainable<String> items = Chainable.from("a", "b", "c", "d", "e", "f", "g", "h", "i");

        // When
        Chainable<String> first5 = items.first(5);
        Chainable<String> first11 = items.first(11);
        Chainable<String> first0 = items.first(0);

        String first5Text = String.join("", first5);
        String first11Text = String.join("", first11);

        // Then
        assertEquals("abcde", first5Text);
        assertEquals("abcdefghi", first11Text);
        assertTrue(first0.isEmpty());
    }

    @Test
    public void testFrom() {
        // Given
        String inputs[] = { "A", "B", "C", "D" };
        String expected = String.join("", inputs);
        String expectedTransformed = "abcd";

        // When
        Chainable<String> items = Chainable.from("A", "B", "C", "D");
        String actual = items.join();
        String actualTransformed = items.transform(s -> s.toLowerCase()).join();

        // Then
        assertEquals(expected, actual);
        assertEquals(expectedTransformed, actualTransformed);
    }

    @Test
    public void testJoin() {
        // Given
        List<String> items = Arrays.asList("a", "b", "c", "d");
        Chainable<String> chain = Chainable.from(items);
        String expected = String.join("", items);

        // When
        String actual = chain.join();

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testMap() {
        // Given
        final Chainable<String> items = Chainable.from("a", "b", "c", "d", "e");

        // When
        Map<String, String> map = items.toMap(i -> i);

        // Then
        assertEquals(items.size(), map.size());
        for (String item : items) {
            String mappedItem = map.get(item);
            assertNotNull(mappedItem);
        }
    }

    @Test
    public void testMaxMin() {
        // Given
        Chainable<Integer> ints = Chainable.from(1, 3, 2, 5, 2);
        int expectedMax = 5;
        int expectedMin = 1;

        // When
        int max = ints.max(o -> o.doubleValue());
        int min = ints.min(o -> o.doubleValue());

        // Then
        assertEquals(expectedMax, max);
        assertEquals(expectedMin, min);
    }

    @Test
    public void testNotAfter() {
        // Given
        Chainable<Integer> integers = Chainable.from(1, 1, 1, 2, 3, 4);
        String expectedTextNotAfter2 = "1112";
        String expectedTextNotAfter4 = "111234";

        // When
        String actualTextNotAfter2 = integers.notAfter(i -> i == 2).join();
        String actualTextNotAfter4 = integers.notAfter(i -> i == 4).join();
        String actualTextNotAfter5 = integers.notAfter(i -> i == 5).join();

        // Then
        assertEquals(expectedTextNotAfter2, actualTextNotAfter2);
        assertEquals(expectedTextNotAfter4, actualTextNotAfter4);
        assertEquals(expectedTextNotAfter4, actualTextNotAfter5);
    }

    @Test
    public void testNotAsLongAs() {
        // Given
        Iterable<String> testList = Arrays.asList("a", "b", "c", "d", "e");
        String expected = "cde";

        // When
        String actual = Chainables.notAsLongAs(testList, o -> "a".equals(o) || "b".equals(o)).join();

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testNotAsLongAsValue() {
        // Given
        Iterable<String> testList = Arrays.asList("a", "b", "c", "d", "e");
        String expected = "bcde";

        // When
        String actual = Chainables.notAsLongAsValue(testList, "a").join();

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testNotBeforeValue() {
        // Given
        Iterable<String> testList = Arrays.asList("a", "b", "c", "d", "e");
        String expected = "cde";

        // When
        String actual = Chainables.notBeforeValue(testList, "c").join();

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testReplace() {
        // Given
        Iterable<String> items = Arrays.asList("a", "b", "c", "d");
        String expected = "123123123";

        // When
        String actual  = Chainables.replace(items, i -> (i != "b") ? Arrays.asList("1", "2", "3") : null).join();

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testReverse() {
        // Given
        Chainable<String> items = Chainable.from("a", "b", "c", "d");
        String expected = "dcba";

        // When
        String actual = items.reverse().join();

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
    public void testSplit() {
        // Given
        String text = "Hello World! This is Mr. Johnson speaking... Listen, how are you?";
        int expectedTokens = 28;
        int expectedChars = text.length();

        // When
        Chainable<String> tokens = Chainables.split(text, " ,'\"!?.()[]{};:-+=");
        int actualTokens = tokens.size();
        Chainable<String> chars = Chainables.split(text);
        int actualChars = chars.size();
        String mergedTokens = tokens.join();
        String mergedChars = chars.join();

        // Then
        assertEquals(expectedTokens, actualTokens);
        assertEquals(expectedChars, actualChars);
        assertEquals(text, mergedTokens);
        assertEquals(text, mergedChars);
    }

    @Test
    public void testStartsWith() {
        // Given
        Iterable<String> items = Arrays.asList("a", "b", "c", "d");
        Iterable<String> prefixMatching = Arrays.asList("a", "b");
        Iterable<String> prefixEmpty = Arrays.asList();
        Iterable<String> prefixNonMatching = Arrays.asList("b", "c");
        Iterable<String> prefixSuperset = Arrays.asList("a", "b", "c", "d", "e");
        Iterable<String> prefixSame = items;

        // When/Then
        assertTrue(Chainables.startsWithEither(items, prefixMatching));
        assertTrue(Chainables.startsWithEither(items, prefixEmpty));
        assertFalse(Chainables.startsWithEither(items, prefixNonMatching));
        assertFalse(Chainables.startsWithEither(items, prefixSuperset));
        assertTrue(Chainables.startsWithEither(items, prefixSame));
    }

    @Test
    public void testStreamBasics() {
        // Given
        Integer inputs[] = { 1, 2, 3, 4, 5, 6, 7, 8 };
        String expected = Chainable.from(inputs).where(i -> i % 2 != 0).join();

        // When
        String actual = Chainable.from(Stream
                .of(inputs)
                .filter(i -> i % 2 != 0))
                .join();

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
        String expected = Chainable.from(inputs).where(i -> i % 2 != 0).join();

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
    public void testSum() {
        // Given
        Chainable<Integer> ints = Chainable.from(1, 2, 3, 4);
        long expected = 1 + 2 + 3 + 4;

        // When
        long actual = ints.sum(o -> o.longValue());

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
        String actual = Chainables.transformAndFlatten(list, o -> (o != null) ? Arrays.asList(o) : null).join();

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

        String actual = Chainable
                .from("a", "b", "c", "ab", "ac", "bc")
                .where(s -> s.startsWith("a"))
                .join();

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
        String actual  = Chainables.withoutNull(items).join();

        // Then
        assertEquals(expected, actual);
    }
}
