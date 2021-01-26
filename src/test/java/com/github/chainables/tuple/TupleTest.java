/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.chainables.tuple;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Unit tests
 */
public class TupleTest {
    Pair<String, String> stringPair = Pair.from("A", "B");
    Pair<String, String> stringPairSame = Pair.from("A", "B");
    Pair<String, String> pairNullY = Pair.from("A", null);
    Pair<String, String> pairNullYSame = Pair.from("A", null);
    Pair<String, String> pairNullX = Pair.from(null, "B");
    Pair<String, String> pairNullXSame = Pair.from(null, "B");

    Pair<String, Integer> stringIntPair = Pair.from("A", 1);
    Pair<String, Integer> stringIntPairSame = Pair.from("A", 1);
    Pair<String, Integer> stringIntPairDiff = Pair.from("B", 1);
    Pair<String, Integer> stringIntPairDiff2 = Pair.from("A", 2);

    @Test
    public void testPair() {
        // Given
        Object[][] testCases = {
            { stringPair,           stringPairSame,         true },
            { pairNullX,            pairNullXSame,          true },
            { pairNullY,            pairNullYSame,          true },
            { stringIntPair,        stringIntPairSame,      true },
            { stringIntPair,        stringIntPairDiff,      false },
            { stringIntPair,        stringIntPairDiff2,     false },
            { stringIntPairDiff,    stringIntPairDiff2,     false },
            { stringIntPair,        stringPair,             false },
        };

        // When / Then
        for (Object[] testCase : testCases) {
            Pair<?, ?> pair1 = (Pair<?, ?>) testCase[0];
            Pair<?, ?> pair2 = (Pair<?, ?>) testCase[1];
            boolean equality = (boolean) testCase[2];

            assertNotNull(pair1);
            assertNotNull(pair2);
            assertEquals(equality, pair1.equals(pair2));
            assertEquals(equality, pair2.equals(pair1));
        }
    }
}
