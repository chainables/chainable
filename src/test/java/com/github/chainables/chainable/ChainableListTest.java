/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.chainables.chainable;

import static com.github.chainables.chainable.Chainable.chain;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.chainables.chainable.ChainableList.Unmodifiable;

/**
 * Unit tests
 */
public class ChainableListTest {

    @Test
    void testSubList() {
        // Given
        final int subStart = 1, subLength = 3, subSubStart = 1, subSubLength = 2;
        String[] array = { "a", "b", "c", "d", "e" };
        ChainableList<String> list = Chainable.from(array).toList();

        // When
        ChainableList<String> subList = list.subList(subStart, subStart + subLength);
        Unmodifiable<String> subSubList = subList.subList(subSubStart, subSubLength + subSubStart);

        // Then
        assertEquals(subList.size(), subLength);
        for (int i = 0; i < subLength; i++) {
            assertEquals(subList.get(i), list.get(subStart + i));
        }

        assertThrows(IndexOutOfBoundsException.class, () -> list.subList(-1, array.length));
        assertThrows(IndexOutOfBoundsException.class, () -> list.subList(0, array.length + 1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.subList(3, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.subList(1, 1));

        assertThrows(IndexOutOfBoundsException.class, () -> subList.subList(0, subList.size() + 1));
        assertThrows(IndexOutOfBoundsException.class, () -> subList.subList(-1, subList.size() - 1));
        assertThrows(IndexOutOfBoundsException.class, () -> subList.subList(2, 1));

        assertNotNull(subSubList);
        assertEquals(subSubLength, subSubList.size());
        for (int i = 0; i < subSubLength; i++) {
            assertEquals(subSubList.get(i), list.get(subStart + subSubStart + i));
        }

        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(array.length));
        assertThrows(IndexOutOfBoundsException.class, () -> subList.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> subList.get(subLength));
        assertThrows(IndexOutOfBoundsException.class, () -> subSubList.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> subSubList.get(subSubLength));
    }

    @Test
    void testUnmodifiable() {
        // Given
        String[] items = { "a", "b", "c" };
        ChainableList<String> list = chain(items).toList();
        List<String> additions = Arrays.asList("d", "e");

        // When
        ChainableList<String> unmodifiable = list.unmodifiable();

        // Then
        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.add("d"));
        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.add(1, additions.get(0)));
        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.addAll(additions));
        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.addAll(1, additions));
        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.clear());

        for (int i = 0; i < items.length; i++) {
            assertEquals(items[i], unmodifiable.get(i));
        }

        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.remove(0));
        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.remove("a"));
        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.removeAll(list));
        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.retainAll(list));
        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.set(1, "d"));
        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.replaceAll(s -> s = "x"));
        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.sort((a, b) -> b.compareTo(a)));
    }

    @Test
    void testToArray() {
        // Given
        String[] array = {"a", "b", "c", "d", "e"};
        String[] expected = { "b", "c", "d" };
        ChainableList<String> list = Chainable.from(array).toList();
        ChainableList<String> subList = list.subList(1, 4);

        // When
        Object[] actual = subList.toArray();

        // Then
        assertArrayEquals(expected, actual);
    }

    @Test
    void testContainsAll() {
        // Given
        String[] array = {"a", "b", "c", "d", "e"};
        List<String> contents = Arrays.asList("b", "c", "d");
        ChainableList<String> list = Chainable.from(array).toList();
        ChainableList<String> subList = list.subList(1, 4);

        // Then
        assertTrue(list.containsAll(contents));
        assertTrue(subList.containsAll(contents));
        assertFalse(subList.containsAll(list));
    }

    @Test
    void testContains() {
        // Given
        String[] array = {"a", "b", "c", "d", "e"};
        int subListStart = 1, subListSize = 3;
        ChainableList<String> list = Chainable.from(array).toList();
        ChainableList<String> subList = list.subList(subListStart, subListStart + subListSize);

        // Then
        for (int i = 0; i < array.length; i++) {
            assertTrue(list.contains(array[i]));
        }

        for (int i = 0; i < subListSize; i++) {
            assertTrue(subList.contains(array[subListStart + i]));
        }
    }

    @Test
    void testIndexOf() {
        // Given
        String[] array = {"a", "b", "c", "d", "e"};
        int subListStart = 1, subListSize = 3;
        ChainableList<String> list = Chainable.from(array).toList();
        ChainableList<String> subList = list.subList(subListStart, subListStart + subListSize);

        // Then
        for (int i = 0; i < array.length; i++) {
            String item = array[i];
            assertEquals(i, list.indexOf(item));
            if (i < subListStart || i >= subListSize + subListStart) {
                assertEquals(-1, subList.indexOf(item));
            } else {
                assertEquals(i - subListStart, subList.indexOf(item));
            }
        }
    }

    @Test
    void testLastIndexOf() {
        // Given
        int subChainStart = 1, subChainSize = 4;
        String[] array = {"a", "a", "b", "b", "c", "c"};
        List<String> list = Arrays.asList(array);
        List<String> subList = list.subList(subChainStart, subChainSize + subChainStart);
        ChainableList<String> chain = Chainable.from(array).toList();
        ChainableList<String> subChain = chain.subList(subChainStart, subChainStart + subChainSize);

        // Tnen
        for (int i = 0; i < array.length; i++) {
            String item = array[i];
            assertEquals(list.lastIndexOf(item), chain.lastIndexOf(item));
            assertEquals(subList.lastIndexOf(item), subChain.lastIndexOf(item));
        }
    }

    @Test
    void testIterator() {
        // Given
        String[] array = {"a", "b", "c", "d", "e"};
        int subListStart = 1, subListSize = 3;
        ChainableList<String> list = Chainable.from(array).toList().unmodifiable();
        ChainableList<String> subList = list.subList(subListStart, subListStart + subListSize);

        // When
        int i = 0;
        for (String item : list) {
            // Then
            assertEquals(array[i], item);
            i++;
        }

        // When
        i = subListStart;
        for (String item : subList) {
            // Then
            assertEquals(array[i], item);
            i++;
        }
    }
}
