/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.chainables.chainable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.chainables.chainable.Chainable;
import com.github.chainables.chainable.ChainableTree;
import com.github.chainables.chainable.ChainableTrees;

/**
 * Unit tests
 */
public class ChainableTreeTest {
    @SuppressWarnings("unchecked")
    ChainableTree<String> testTree = ChainableTree.withValue("1").withChildren(
            ChainableTree.withValue("1.1").withChildValues("1.1.1", "1.1.2"),
            ChainableTree.withValue("1.2").withChildValues("1.2.1", "1.2.2"));

    private static ChainableTree<String> treeFrom(String[][][] data) {
        assertNotNull(data);
        assertTrue(data.length >= 1);
        assertTrue(data[0].length >= 1);
        assertTrue(data[0][0].length >= 1);

        String rootKey = data[0][0][0];

        final Map<String, String[]> dataMap = new HashMap<>();
        for (String[][] entry : data) {
            String key = (String) entry[0][0];
            String[] values = (String[]) entry[1];
            dataMap.put(key, values);
        }

        return ChainableTree
                .withValue(rootKey)
                .withChildValueExtractor(s -> Chainable.from(dataMap.get(s)));
    }

    @Test
    public void testAncestors() {
        // Given
        String expected = "1.1, 1";
        ChainableTree<String> node = testTree.firstWhere(t -> "1.1.1".equals(t.value()));

        // When
        String actual = node.ancestors().join(", ");

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testBreadthFirst() {
        // Given
        String expected = "1, 1.1, 1.2, 1.1.1, 1.1.2, 1.2.1, 1.2.2";

        // When
        String actual = testTree.breadthFirst().join(", ");

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testBreadthFirstNotBelow() {
        // Given
        String expected = "1, 1.1, 1.2, 1.2.1, 1.2.2";

        // When
        String actual = testTree
                .breadthFirstNotBelow(t -> "1.1".equals(t.value()))
                .join(", ");

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testChildren() {
        // Given
        int firstRunLength = 2, secondRunLength = 3;
        ChainableTree<Long> root = ChainableTree.withValue(0l);
        Chainable<Long> randomChildValues1 = Chainable
                .empty()
                .chain(v -> Math.round(Math.random() * 10))
                .first(firstRunLength)
                .cast(Long.class);

        Chainable<Long> randomChildValues2 = Chainable
                .empty()
                .chain(v -> Math.round(Math.random() * 10))
                .first(secondRunLength)
                .cast(Long.class);

        // When
        Chainable<ChainableTree<Long>> children1 = randomChildValues1.transform(v -> ChainableTree.withValue(v));
        Chainable<ChainableTree<Long>> children2 = randomChildValues2.transform(v -> ChainableTree.withValue(v));

        root
            .withChildren(children1)
            .withChildren(children2);

        String actual1 = root.children().join();
        String actual2 = root.children().join();

        // Then
        assertEquals(firstRunLength + secondRunLength, root.children().count());
        assertEquals(actual1, actual2); // Ensure children are evaluated only once and cached from thereon
    }

    @Test
    public void testDecendants() {
        // Given
        String expected = "1.1, 1.2, 1.1.1, 1.1.2, 1.2.1, 1.2.2";

        // When
        String actual = testTree.descendants().join(", ");

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testDepthFirst() {
        // Given
        String expected = "1, 1.1, 1.1.1, 1.1.2, 1.2, 1.2.1, 1.2.2";

        // When
        String actual = testTree.depthFirst().join(", ");

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testDepthFirstNotBelow() {
        // Given
        String expected = "1, 1.1, 1.1.1, 1.1.2, 1.1.3, 1.2, 1.2.1, 1.2.2, 1.2.3, 1.3, 1.3.1, 1.3.2, 1.3.3";
        ChainableTree<String> tree = ChainableTree
                .withValue("1")
                .withChildValueExtractor(p -> Chainable
                        .empty()
                        .chainIndexed((c, i) -> String.format("%s.%s", p, Long.toString(i + 1)))
                        .first(3)
                        .cast(String.class)); // Limit the number, otherwise infinite

        // When
        String actual = tree
                .depthFirstNotBelow(t -> t.value().length() == 5)
                .join(", ");

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testFirstWhere() {
        // Given
        String expected = "1.2";

        // When
        String actual = testTree.firstWhere(t -> expected.equals(t.value())).value();

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testIsUnder() {
        // Given
        ChainableTree<String> startTree = testTree.firstWhere(t -> "1.1.2".equals(t.value()));

        // Then
        assertTrue(startTree.isUnder(t -> "1.1".equals(t.value())));
        assertTrue(startTree.isUnder(t -> "1".equals(t.value())));
        assertFalse(startTree.isUnder(t -> "1.1.2".equals(t.value())));
        assertFalse(startTree.isUnder(t -> "1.1.1".equals(t.value())));
        assertFalse(startTree.isUnder(t -> "1.2.1".equals(t.value())));
    }

    @Test
    public void testReverseSiblings() {
        // Given
        String expected = "1.2, 1.1";
        ChainableTree<String> tree = treeFrom(new String[][][] {
            { { "1" }, { "1.1", "1.2", "1.3", "1.4" } }
        });

        ChainableTree<String> treeNode = tree.firstWhere(t -> "1.3".equals(t.value()));

        // When
        String actual = treeNode
                .predecessors()
                .reverse()
                .join(", ");

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testSiblings() {
        // Given
        ChainableTree<String> node12 = testTree.firstWhere(t -> "1.2".equals(t.value()));
        ChainableTree<String> node11 = testTree.firstWhere(t -> "1.1".equals(t.value()));

        // When
        String actualSiblings = node12.siblings().join();
        String actualPredecessors = node12.predecessors().join();
        String actualSuccessors = node11.successors().join();

        // Then
        assertEquals("1.1", actualSiblings);
        assertEquals("1.1", actualPredecessors);
        assertEquals("1.2", actualSuccessors);
    }

    @Test
    public void testSkipping() {
        // Given
        String expected = "1.1.2, 1.2, 1.2.1, 1.2.2";

        // When
        String actual = ChainableTrees
                .values(testTree.depthFirst())
                .notBeforeEquals("1.1.2")
                .join(", ");

        // Then
        assertEquals(expected, actual);        
    }

    @Test
    public void testSuccessors() {
        // Given
        String expected = "1.2";

        // When
        String actual = testTree
                        .children()
                        .first()
                        .successors()
                        .join(", ");

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testTerminals() {
        // Given
        String expected = "1.1.1, 1.1.2, 1.2.1, 1.2.2";

        // When
        String actual = testTree.terminals().join(", ");

        // Then
        assertEquals(expected, actual);        
    }

    @Test
    public void testUpAsLongAs() {
        // Given
        String startValue = "1.1.1", expectedValue = "1.1", conditionValue = "1";

        // When
        ChainableTree<String> start = testTree.firstWhere(t -> startValue.equals(t.value()));
        assertNotNull(start);
        ChainableTree<String> found = start.upAsLongAs(t -> !conditionValue.equals(t.value()));

        // Then
        assertNotNull(found);
        assertEquals(expectedValue, found.value());
    }

    @Test
    public void testUpUntil() {
        // Given
        String startValue = "1.1.1", expectedValue = "1";

        // When
        ChainableTree<String> start = testTree.firstWhere(t -> startValue.equals(t.value()));
        assertNotNull(start);
        ChainableTree<String> found = start.upUntil(t -> expectedValue.equals(t.value()));

        // Then
        assertNotNull(found);
        assertEquals(expectedValue, found.value());
    }

    @SuppressWarnings("unchecked")
    @Test
    // TODO: Make this richer
    public void testWithoutChildren() {
        // Given
        ChainableTree<String> tree = ChainableTree.withValue("A").withChildren(
                ChainableTree.withValue("A.1")
                    .withChildren(
                            ChainableTree.withValue("A.1.1"),
                            ChainableTree.withValue("A.1.2")),
                ChainableTree.withValue("A.2")
                    .withChildren(
                            ChainableTree.withValue("A.2.1"),
                            ChainableTree.withValue("A.2.2")));

        // When / Then
        assertEquals(2, tree.children().count());
        assertEquals(0, tree.withoutChildren().children().count());
    }
}
