/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.chainables.map;

import static com.github.chainables.chainable.Chainable.chain;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.chainables.chainable.Chainable;
import com.github.chainables.chainable.Chainables;
import com.github.chainables.map.HashMap2D;
import com.github.chainables.map.Map2D;
import com.github.chainables.map.Map2D.Entry2D;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests
 */
public class Map2DTest {
    final static String[][] TEST_DATA = {
            { "a", "a", "aa" },
            { "a", "b", "ab" },
            { "b", "b", "bb" },
            { "b", "c", "bc" },
            { "c", "c", "cc" },
            { "c", "d", "cd" }
    };

    @Test
    public void testClear() {
        // Given
        Map2D<String, String, String> map = HashMap2D.from(TEST_DATA);
        assertTrue(map.values().any());
        assertTrue(map.reverse().values().any());

        // When
        map.clear();

        // Then
        assertTrue(map.values().isEmpty());
        assertTrue(map.reverse().values().isEmpty());
    }

    @Test
    public void testPutGet() {
        // Given
        Map2D<String, String, String> map = new HashMap2D<>();

        // When
        for (String[] testEntry : TEST_DATA) {
            assertEquals(3, testEntry.length);
            String key1 = testEntry[0];
            String key2 = testEntry[1];
            String value = testEntry[2];
            map.put(key1, key2, value);
        }

        // Then
        for (String[] testEntry : TEST_DATA) {
            String key1 = testEntry[0];
            String key2 = testEntry[1];
            String expectedValue = testEntry[2];
            String actualValue = map.get(key1, key2);
            assertEquals(expectedValue, actualValue);
        }
    }

    @Test
    public void testFrom() {
        // When
        Map2D<String, String, String> map = HashMap2D.from(TEST_DATA);

        // Then
        assertNotNull(map);
        assertEquals(TEST_DATA.length, Chainables.count(map.values()));
        for (String[] row : TEST_DATA) {
            assertEquals(3, row.length);
            String key1 = row[0];
            String key2 = row[1];
            String value = row[2];
            assertEquals(value, map.get(key1, key2));
        }
    }

    @Test
    public void testReverse() {
        // Given
        Map2D<String, String, String> table = new HashMap2D<String, String, String>("").putAll(TEST_DATA);
        Map<String, String> outgoingFromA = table.mapFrom("a");

        // When / Then
        for (String[] testCase : TEST_DATA) {
            assertEquals(3, testCase.length);
            String key1 = testCase[0];
            String key2 = testCase[1];
            String value = testCase[2];
            assertTrue(table.containsKeys(key1, key2));
            assertTrue(table.reverse().containsKeys(key2, key1));
            assertEquals(value, table.get(key1, key2));
            assertEquals(value, table.reverse().get(key2, key1));
        }

        Map<String, String> incomingToB = table.reverse().mapFrom("b");
        assertNotNull(outgoingFromA);
        assertEquals(2, outgoingFromA.size());
        assertTrue(outgoingFromA.containsKey("b"));
        assertTrue(outgoingFromA.containsValue("ab"));
        assertNotNull(incomingToB);
        assertEquals(2,  incomingToB.size());
        assertTrue(incomingToB.containsKey("a"));
        assertTrue(incomingToB.containsValue("ab"));
    }

    @Test
    public void testRemove() {
        // Given
        Map2D<String, String, String> table = HashMap2D.from(TEST_DATA);
        assertTrue(table.containsKeys("a", "b"));
        assertTrue(table.reverse().containsKeys("b", "a"));

        // When
        table.remove("a", "b");

        // Then
        assertFalse(table.containsKeys("a", "b"));
        assertFalse(table.reverse().containsKeys("b", "a"));
    }

    @Test
    public void testEntries() {
        // Given
        String expectedMergedText = Chainables
                .join("", chain(TEST_DATA)
                .transform(o -> String.join("", o)));

        // When
        Map2D<String, String, String> map2D = HashMap2D.from(TEST_DATA);
        Chainable<Entry2D<String, String, String>> allEntries = map2D.entries();
        String actualMergedText = allEntries
                .transform(e -> String.join("", e.primaryKey, e.secondaryKey, e.value))
                .join();

        // Then
        assertEquals(expectedMergedText, actualMergedText);
        assertEquals(TEST_DATA.length, map2D.values().count());
    }
}
