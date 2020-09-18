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
