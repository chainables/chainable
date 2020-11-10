/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.chainables.chainable;

import org.junit.jupiter.api.Test;

import com.github.chainables.chainable.Chainable;
import com.github.chainables.chainable.ChainableQueue;
import com.github.chainables.chainable.Chainables;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
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
    public void testAfterFirst() {
        // Given
        Chainable<String> items = Chainable.from("a", "b", "c", "d", "e");
        String expectedAfterFirst = "bcde";
        String expectedAfterSecond = "cde";

        // When
        Chainable<String> afterFirst = items.afterFirst();
        String actualAfterFirst = Chainables.join("", afterFirst);
        String actualAfterSecond = afterFirst.afterFirst().join();
        String actualAfterSecondNum = items.afterFirst(2).join();

        // Then
        assertEquals(expectedAfterFirst, actualAfterFirst);
        assertEquals(expectedAfterSecond, actualAfterSecond);
        assertEquals(expectedAfterSecond, actualAfterSecondNum);
    }

    @Test
    public void testAllWhereEither() {
        // Given
        final Chainable<Integer> odds = Chainable.from(1, 3, 5, 7);
        final Chainable<Integer> evens = Chainable.from(2, 4, 6, 8);
        final Chainable<Integer> mixed = Chainable.from(1, 2, 3, 4);

        // When + Then
        assertTrue(Chainables.allWhereEither(odds,
                o -> o % 2 != 0,
                o -> o > 0));
        assertTrue(Chainables.allWhereEither(evens,
                o -> o % 2 != 0,
                o -> o > 0));
        assertFalse(Chainables.allWhereEither(mixed,
                o -> o % 2 != 0,
                o -> o < 0));
        assertFalse(Chainables.allWhereEither(odds,
                o -> o % 2 == 0,
                o -> o < 0));
    }

    @Test
    public void testAny() {
        // Given
        Iterable<String> items = Arrays.asList("a", "b", "c");

        // When/Then
        assertTrue(Chainables.anyWhereEither(items, o -> o.equals("b")));
        assertFalse(Chainables.anyWhereEither(items, o -> o.equals("d")));
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
        String actual = Chainables.asLongAsEquals(testList, "a").join();

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testAsQueue() {
        // Given
        String[] items = { "a", "b", "c", "d", "e" };
        String expected = String.join("", items);

        // Adding individual items to end of queue
        ChainableQueue<String> queue = Chainable.from(items[0]).toQueue();
        for (int i = 1; i < items.length; i++) {
            queue.withLast(items[i]);
        }

        String actual = String.join("", queue);
        assertEquals(expected, actual);

        // Removing from front of queue
        StringBuilder sb = new StringBuilder();
        while (queue.any()) {
            sb.append(queue.removeFirst());
        }

        actual = sb.toString();
        assertEquals(expected, actual);

        // Adding and removing in one loop
        sb.setLength(0);
        for (String item : items) {
            queue.withLast(item);
            if (queue.any()) {
                sb.append(queue.removeFirst());
            }
        }

        actual = sb.toString();
        assertEquals(expected, actual);

        // Adding entire set to end
        queue
            .withLast(items)
            .withLast(items);

        sb.setLength(0);
        while (queue.any()) {
            sb.append(queue.removeFirst());
        }

        actual = sb.toString();
        expected = String.join("", items) + String.join("", items);
        assertEquals(expected, actual);

        // Larger initial iterable
        queue = Chainable.from(items[0], items[1]).toQueue();
        sb.setLength(0);
        for (int i = 2; i < items.length; i++) {
            queue.withLast(items[i]);
            sb.append(queue.removeFirst());
        }

        while (queue.any()) {
            sb.append(queue.removeFirst());
        }

        expected = String.join("", items);
        actual = sb.toString();
        assertEquals(expected, actual);
    }

    @Test
    public void testAtLeastMost() {
        // Given
        Iterable<Integer> items = Arrays.asList(1, 2, 3);

        // When/Then
        assertTrue(Chainables.isCountAtLeast(items, Chainables.count(items)));
        assertTrue(Chainables.isCountAtLeast(items, Chainables.count(items) - 1));
        assertTrue(Chainables.isCountAtLeast(items, 0));
        assertFalse(Chainables.isCountAtLeast(items, Chainables.count(items) + 1));
        assertFalse(Chainables.isCountAtLeast(Collections.emptyList(), 1));
        assertTrue(Chainables.isCountAtLeast(Collections.emptyList(), 0));

        assertTrue(Chainables.isCountAtMost(items, Chainables.count(items)));
        assertTrue(Chainables.isCountAtMost(items, Chainables.count(items) + 1));
        assertFalse(Chainables.isCountAtMost(items, Chainables.count(items) - 1));
        assertFalse(Chainables.isCountAtMost(items, 0));
        assertTrue(Chainables.isCountAtMost(Collections.emptyList(), 0));
        assertTrue(Chainables.isCountAtMost(Collections.emptyList(), 1));
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
    public void testBreadthFirstNotBelow() {
        // Given
        Chainable<String> roots = Chainable.from("a", "b", "c");
        String expected = Chainable
                .from("a", "b", "c", "aa", "ab", "ac", "ba", "bb", "bc", "ca", "cb", "cc")
                .join(",");

        // When
        String actual = roots.breadthFirstNotBelow(
                s -> roots.transform(o -> s + o),
                s -> s.length() == 2)
                .join(",");

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testBreadthFirstAsLongAs() {
        // Given
        Chainable<String> roots = Chainable.from("a", "b", "c");
        Chainable<String> expectedResults = Chainable.from("a", "b", "c", "aa", "ab", "ac", "ba", "bb", "bc", "ca", "cb", "cc");
        String expected = String.join(",", expectedResults);

        // When
        String actual = roots.breadthFirstAsLongAs(
                s -> roots.transform(o -> s + o),
                s -> s.length() < 3)
                .join(",");

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testCached() {
        // Given
        Chainable<Long> randomInts = Chainable
                .from((Long)null)
                .chain(o -> Math.round(Math.random() * 100.0))
                .withoutNull()
                .first(10)
                .cached();

        // When
        String partialTraversal = Chainables.join(", ", randomInts.first(5));
        String fullTraversal = Chainables.join(", ", randomInts);
        String secondTraversal = Chainables.join(", ", randomInts);

        // Then
        assertFalse(fullTraversal.startsWith(partialTraversal));
        assertEquals(fullTraversal, secondTraversal);
    }

    @Test
    public void testChainIndexed() {
        // Given
        String expected = "01234";

        // When
        String actual = Chainable
                .empty()
                .chainIndexed((v, i) -> Long.toString(i))
                .first(5)
                .join();

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testChain() {
        // Given
        Iterable<String> items = Arrays.asList("a", "b", "c", "d");
        final Iterator<String> iter1 = items.iterator();
        String expected = Chainables.join("", items);

        Chainable<String> initial = Chainable.from("1", "2", "3");
        final Iterator<String> iter2 = items.iterator();
        String expected2 = String.join("", Chainables.concat(initial, items));

        long length = 10, expected3 = length;
        int unseededChainLength = 5;

        // When
        String actual = Chainables
                .chain(iter1.next(), s -> (iter1.hasNext()) ? iter1.next() : null)
                .join();

        String actual2 = initial
                .chain(i -> (iter2.hasNext()) ? iter2.next() : null)
                .join();

        Long actual3 = Chainable
                .from(1l)
                .chain(i -> i + 1)
                .first(length)
                .last();

        Chainable<Long> unseededChain = Chainable
                .empty() // Test chaining without a seed
                .chain(o -> Math.round(Math.random() * 10))
                .first(5)
                .cast(Long.class);

        Chainable<String> trulyEmptyChain = Chainable
                .empty()
                .chain(i -> (i == null) ? null : "A")
                .first(5)
                .cast(String.class);

        // Then
        assertEquals(expected, actual);
        assertEquals(expected2, actual2);
        assertTrue(unseededChain.isCountExactly(unseededChainLength));
        assertTrue(trulyEmptyChain.isEmpty());
        assertEquals(expected3, actual3);
    }

    // Note this is a test case for a README example
    @Test void testChainLastTwo() {
        // Given
        String expected = "0, 1, 1, 2, 3, 5, 8, 13";

        // When
        String actual = Chainable
                .from(0l, 1l)
                .chain((i0, i1) -> i0 + i1) // Fibonacci sequence
                .first(8)
                .join(", ");

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testChainIf() {
        // Given/When
        Chainable<String> items = Chainable.from("a", "b", "c");
        String expectedText = "abcd";
        String expectedText2 = "d";

        Chainable<String> itemsEmpty = Chainable.from(new ArrayList<String>());

        Chainable<Boolean> bools = Chainable
                .from(false, false, false, true, false, false)
                .transform(b -> Boolean.TRUE.equals(b) ? true : null)
                .notAfter(b -> Boolean.TRUE.equals(b))
                .chainIf(b -> b == null, b -> false);

        Chainable<Boolean> bools2 = Chainable.from(false, false, true)
                .transform(b -> Boolean.TRUE.equals(b) ? true : null)
                .notAfter(b -> Boolean.TRUE.equals(b))
                .chainIf(b -> b == null, b -> false);

        Chainable<Boolean> bools3 = Chainable.from(false, false, false)
                .transform(b -> Boolean.TRUE.equals(b) ? true : null)
                .notAfter(b -> Boolean.TRUE.equals(b))
                .chainIf(b -> b == null, b -> false);

        Chainable<Boolean> bools4 = Chainable.from(new ArrayList<Boolean>())
                .transform(b -> Boolean.TRUE.equals(b) ? true : null)
                .notAfter(b -> Boolean.TRUE.equals(b))
                .chainIf(b -> b == null, b -> false);

        Chainable<String> actual = items.chainIf(null, i -> "c".equals(i) ? "d" : null);
        String actualText = String.join("", actual);

        Chainable<String> actual2 = itemsEmpty.chainIf(null, i -> "d".equals(i) ? null : "d");
        String actualText2 = String.join("", actual2);

        // Then
        assertTrue(Boolean.TRUE.equals(bools.last()));
        assertTrue(Boolean.TRUE.equals(bools2.last()));
        assertTrue(Boolean.FALSE.equals(bools3.last()));
        assertTrue(Boolean.FALSE.equals(bools4.last()));
        assertEquals(expectedText, actualText);
        assertEquals(expectedText2, actualText2);
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
        assertTrue(oddsLessThan6.isCountExactly(expected.length()));
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
    public void testCount() {
        // Given
        List<Integer> items = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
        int expectedItemsSize = items.size();
        Chainable<Integer> itemsChain = Chainable.from(items);
        Chainable<Integer> emptyChain = Chainable.empty();
        Chainable<String> transformedChain = itemsChain.transform(o -> o.toString());

        // When
        long actualItemsChainSize = itemsChain.count();
        long actualEmptyChainSize = emptyChain.count();
        long actualTransformedChainSize = transformedChain.count();

        // Then
        assertEquals(expectedItemsSize, actualItemsChainSize);
        assertEquals(0, actualEmptyChainSize);
        assertEquals(expectedItemsSize, actualTransformedChainSize);
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
    public void testDepthFirstNotBelow() {
        // Given
        final int depth = 4;
        Chainable<String> initial = Chainable.from("1");
        Function<String, Iterable<String>> childExtractor = (s) -> (s.length() < (depth - 1)  * 2) ? Chainable.from(s + ".1", s + ".2") : null;
        String expected = "1, 1.1, 1.1.1, 1.1.2, 1.2, 1.2.1, 1.2.2";

        // When
        String actual = initial.depthFirstNotBelow(childExtractor, i -> i.length() == 5).join(", ");

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testDirectoryList() {
        File currentDir = Paths.get("").toAbsolutePath().toFile();
        Chainable<File> directories = Chainables.directoriesFromPath(currentDir);
        for (File dir : directories) {
            assertNotNull(dir);
            assertTrue(dir.isDirectory());
        }

        currentDir = new File("nonexistentDir");
        directories = Chainables.directoriesFromPath(currentDir);
        assertNotNull(directories);
        assertFalse(directories.any());
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
    public void testFirst() {
        // Given
        Iterable<String> items = Arrays.asList("a", "b", "c");
        Iterable<String> itemsEmpty = new ArrayList<>();

        // When
        String first = Chainables.first(items);

        // Then
        assertNotNull(first);
        assertEquals("a", first);
        assertNull(Chainables.first(itemsEmpty));
        assertNull(Chainables.first(null));
        assertNotNull(Chainables.firstWhereEither(items, i -> i.equals("b")));
        assertNull(Chainables.firstWhereEither(items, i -> i.equals("d")));
    }

    @Test
    public void testFirstNumber() {
        // Given
        final Chainable<String> items = Chainable.from("a", "b", "c", "d", "e", "f", "g", "h", "i");

        // When
        String actualFirst5 = items.first(5).join();
        String actualFirst11 = items.first(11).join();
        Chainable<String> first0 = items.first(0);

        // Then
        assertEquals("abcde", actualFirst5);
        assertEquals("abcdefghi", actualFirst11);
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
    public void testGet() {
        // Given
        Chainable<Integer> infiniteChain = Chainable
                .from(0)
                .chain(i -> i + 1);

        Chainable<Integer> listChain = Chainable
                .from(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

        Chainable<Long> cachedChain = Chainable
                .empty(Long.class)
                .chain(i -> Math.round(Math.random() * 1000))
                .first(10)
                .cached();

        // When
        int item5FromInfinite = infiniteChain.get(5);
        int item5FromList = listChain.get(5);
        long cachedCount = cachedChain.count();
        long item5FromCache = cachedChain.get(5);
        long item5FromCacheAgain = cachedChain.get(5);

        // Then
        assertEquals(5, item5FromInfinite);
        assertEquals(5, item5FromList);
        assertEquals(10, cachedCount);
        assertEquals(item5FromCache, item5FromCacheAgain);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInterleave() {
        // Given
        final Chainable<Long> odds = Chainable.from(1l).chain(o -> o + 2);
        final Chainable<Long> oddsFirst4 = odds.first(4);
        final Chainable<Long> evens = Chainable.from(2l).chain(o -> o + 2);
        final Chainable<Long> evensFirst6 = evens.first(6);
        final Chainable<Long> zeros = Chainable.from(0l, 0l, 0l);
        String expected = "123456781012";
        String expected2 = "102304506781012";
        String expectedNaturals = "1, 2, 3, 4, 5, 6, 7, 8, 9, 10";

        // When
        String actual = oddsFirst4.interleave(evensFirst6).join();
        String actual2 = Chainables.interleave(oddsFirst4, zeros, evensFirst6).join();
        String actualNaturals = odds.interleave(evens).first(10).join(", ");

        // Then
        assertEquals(expected, actual);
        assertEquals(expected2, actual2);
        assertEquals(expectedNaturals, actualNaturals);
    }

    @Test
    public void testIsCount() {
        // Given
        Chainable<String> chainInfinite = Chainable.empty(String.class).chain(i -> "a");
        long expected = 10;
        Chainable<String> chain10 = chainInfinite.first(expected);

        // When / Then
        assertTrue(chainInfinite.isCountAtLeast(expected));
        assertFalse(chainInfinite.isCountAtMost(expected));
        assertFalse(chainInfinite.isCountExactly(expected));

        assertTrue(chain10.isCountAtLeast(expected));
        assertTrue(chain10.isCountAtLeast(expected - 1));
        assertFalse(chain10.isCountAtLeast(expected + 1));

        assertTrue(chain10.isCountAtMost(expected));
        assertFalse(chain10.isCountAtMost(expected - 1));
        assertTrue(chain10.isCountAtMost(expected + 1));

        assertTrue(chain10.isCountExactly(expected));
        assertFalse(chain10.isCountExactly(expected - 1));
        assertFalse(chain10.isCountExactly(expected + 1));

        assertTrue(Chainable.empty().isCountExactly(0));
        assertTrue(Chainable.empty().isCountAtMost(0));
        assertTrue(Chainable.empty().isCountAtLeast(0));

        assertEquals(expected, chain10.count());
    }

    @Test
    public void testIterativeContains() {
        // Given
        Chainable<String> items = Chainable.from("a", "b", "c", "d", "e");

        // When/Then
        assertTrue(Boolean.TRUE.equals(items.iterativeContains("a").last()));
        assertTrue(Boolean.TRUE.equals(items.iterativeContains("c").last()));
        assertTrue(Boolean.TRUE.equals(items.iterativeContains("e").last()));
        assertTrue(Boolean.FALSE.equals(items.iterativeContains("x").last()));

        items = Chainable.from(new ArrayList<String>());
        assertTrue(Boolean.FALSE.equals(items.iterativeContains("x").last()));
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
    public void testLast() {
        // Given
        Iterable<String> items = Arrays.asList("a", "b", "c");
        Iterable<String> itemsEmpty = new ArrayList<>();

        // When
        String last = Chainables.last(items);
        String actualLastOneNonEmpty = Chainables.last(items, 1).join();
        String actualLastOneEmpty = Chainables.last(itemsEmpty, 1).join();
        String actualTooMany = Chainables.last(items, 4).join();
        String actualAll = Chainables.last(items, Chainables.count(items)).join();
        String actualLastTwo = Chainables.last(items, 2).join();

        // Then
        assertNotNull(last);
        assertEquals("c", last);
        assertNull(Chainables.last(null));
        assertNull(Chainables.last(itemsEmpty));
        assertEquals("c", actualLastOneNonEmpty);
        assertTrue(actualLastOneEmpty.isEmpty());
        assertTrue(actualTooMany.isEmpty());
        assertEquals("abc", actualAll);
        assertEquals("bc", actualLastTwo);
    }

    @Test
    public void testMap() {
        // Given
        final Chainable<String> items = Chainable.from("a", "b", "c", "d", "e");

        // When
        Map<String, String> map = items.toMap(i -> i);

        // Then
        assertTrue(items.isCountExactly(map.size()));
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
        String actual = Chainables.notBeforeEquals(testList, "c").join();

        // Then
        assertEquals(expected, actual);
    }

    private class TypeA {};
    private class TypeB {};

    @Test
    public void testOfType() {
        // Given
        TypeA typeAExample = new TypeA();
        TypeB typeBExample = new TypeB();
        Chainable<Object> chain = Chainable.from(new TypeA(), new TypeB(), new TypeA(), new TypeA(), new TypeB());
        long expectedACount = 3, expectedBCount = 2;

        // When
        Chainable<TypeA> aTypes = chain.ofType(typeAExample);
        Chainable<TypeB> bTypes = chain.ofType(typeBExample);

        // Then
        assertTrue(aTypes.isCountExactly(expectedACount));
        assertTrue(bTypes.isCountExactly(expectedBCount));
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
    public void testSorting() {
        // Given
        Chainable<String> textChain = Chainable.from(Arrays.asList("c", "b", "a", "d"));
        Chainable<Long> numericalChain = Chainable.from(Arrays.asList(3l, 2l, 1l, 4l));
        Chainable<Double> decimalChain = Chainable.from(1.0, 3.0, 2.0, 5.0, 4.0);

        // When
        String actualTextAlphaAsc = textChain.ascending().join();
        String actualTextAlphaDesc = textChain.descending().join();
        String actualNumbersAsc = numericalChain.ascending().join();
        String actualNumbersDesc = numericalChain.descending().join();
        String actualDecAsc = decimalChain.ascending().join(",");
        String actualDecDesc = decimalChain.descending().join(",");

        // Then
        assertEquals("abcd", actualTextAlphaAsc);
        assertEquals("dcba", actualTextAlphaDesc);
        assertEquals("1234", actualNumbersAsc);
        assertEquals("4321", actualNumbersDesc);
        assertEquals("1.0,2.0,3.0,4.0,5.0", actualDecAsc);
        assertEquals("5.0,4.0,3.0,2.0,1.0", actualDecDesc);
    }

    @Test
    public void testSplit() {
        // Given
        String text = "Hello World! This is Mr. Johnson speaking... Listen, how are you?";
        int expectedTokens = 28;
        int expectedChars = text.length();

        // When
        Chainable<String> tokens = Chainable.split(text, " ,'\"!?.()[]{};:-+=");
        Chainable<String> chars = Chainable.split(text);
        String mergedTokens = tokens.join();
        String mergedChars = chars.join();

        // Then
        assertTrue(tokens.isCountExactly(expectedTokens));
        assertTrue(chars.isCountExactly(expectedChars));
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

    @Test void testStreamPartialTraversal() {
        // Given
        Integer inputs[] = { 1, 2, 3, 4, 5, 6, 7, 8 };
        Chainable<Integer> chain = Chainable.from(Stream.of(inputs));
        String first4Expected = Chainable.from(inputs).first(4).join();
        String first3Expected = Chainable.from(inputs).first(3).join();
        String first6Expected = Chainable.from(inputs).first(6).join();
        String allExpected = Chainable.from(inputs).join();

        // When
        Chainable<Integer> first4 = chain.first(4);
        String first4Actual = first4.join();
        Chainable<Integer> first3 = first4.first(3);
        String first3Actual = first3.join();
        Chainable<Integer> first6 = chain.first(6);
        String first6Actual = first6.join();
        String allActual = chain.join();
        String allAgainActual = chain.join();

        // Then
        assertEquals(first4Expected, first4Actual);
        assertEquals(first3Expected, first3Actual);
        assertEquals(first6Expected, first6Actual);
        assertEquals(allExpected, allActual);
        assertEquals(allExpected, allAgainActual);
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
