/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.chainables.map;

import org.junit.jupiter.api.Test;

import com.github.chainables.chainable.Chainables;
import com.github.chainables.map.HashMap2DMultiValued;
import com.github.chainables.map.Map2DMultiValued;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests
 */
public class Map2DMultiValuedTest {
    @Test
    public void testMap2DMultiValue() {
        // Given
        Map2DMultiValued<String, String, String> map = new HashMap2DMultiValued<>();

        // When
        map
            .putInValues("1", "1", "1.1.1", "1.1.2")
            .putInValues("1", "2", "1.2.1", "1.2.2")
            .putInValues("2", "1", "2.1.1", "2.1.2")
            .putInValues("2", "2", "2.2.1", "2.2.2");

        // Then
        assertTrue(map.containsValue("1", "1", "1.1.1"));
        assertTrue(map.containsValue("1", "1", "1.1.2"));
        assertFalse(map.containsValue("1", "1", "1.1.3"));
        assertTrue(map.containsValue("2", "2", "2.2.2"));
        assertFalse(map.containsValue("2", "3", "2.3.1"));

        assertNotNull(map.get("x", "y"));
        assertTrue(map.removeFromValues("1", "2", "1.2.1"));
        assertFalse(map.removeFromValues("1", "2", "1.2.1"));
        assertFalse(map.removeFromValues("1", "3", "1.3.1"));

        assertEquals(7, Chainables.count(map.valuesFlattened()));
    }
}
