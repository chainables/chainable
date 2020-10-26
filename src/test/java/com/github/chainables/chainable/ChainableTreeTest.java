/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.chainables.chainable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.github.chainables.chainable.Chainable;
import com.github.chainables.chainable.ChainableTree;
import com.github.chainables.chainable.ChainableTrees;

/**
 * Unit tests
 */
public class ChainableTreeTest {
    @SuppressWarnings("unchecked")
    ChainableTree<String> testTree = ChainableTree.withRoot("1").withChildren(
            ChainableTree.withRoot("1.1").withChildValues("1.1.1", "1.1.2"),
            ChainableTree.withRoot("1.2").withChildValues("1.2.1", "1.2.2"));

    ChainableTree<String> infiniteTree = ChainableTree
            .withRoot("1")
            .withChildValueExtractor(p -> Chainable
                    .empty(String.class)
                    .chainIndexed((c, i) -> String.format("%s.%s", p, Long.toString(i + 1)))
                    .first(3)); // Limit the number of children

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
                .withRoot(rootKey)
                .withChildValueExtractor(s -> Chainable.from(dataMap.get(s)));
    }

    @Test
    public void testAncestors() {
        // Given
        String expected = "1.1, 1";
        ChainableTree<String> tree = testTree.firstWithValue("1.1.1");

        // When
        String actual = tree.ancestors().join(", ");

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
    public void testChildExtractor() {
        // Given
        final String expected = "1, 1.1, 1.2, 1.3, 1.1.1, 1.1.2, 1.1.3, 1.2.1, 1.2.2, 1.2.3";

        // When
        String actual = ChainableTrees.values(
                infiniteTree
                    .breadthFirst()
                    .first(10))
                .join(", ");

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testChildExtractorDepthAware() {
        // Given
        final String levelSep = "@";
        List<String> names = Arrays.asList(
                "0",
                "0.0",                        "0.1",                        "0.2",
                "0.0.0", "0.0.1", "0.0.2",    "0.1.0", "0.1.1", "0.1.2",    "0.2.0", "0.2.1", "0.2.2");

        // Make each entry look like 1.1@1, where the numbering as per the array comes before @, and the level comes after
        Chainable<String> expected = Chainable
                .from(names)
                .transform(s -> s + levelSep + (s.split("\\.").length - 1));

        // When
        Chainable<ChainableTree<String>> actualTrees = ChainableTree
                .withRoot("0" + levelSep + "0")
                .withChildValueExtractor((p, d) -> Chainable
                        .empty(String.class)
                        .chainIndexed((c, i) -> String.format("%s.%d%s%d", p.split(levelSep)[0], i, levelSep, d))
                        .first(3)) // 3 children each node
                .notBelow(t -> t.value().length() >= 7) // Stop tree traversal below nodes that have names equal or greater than 7
                .breadthFirst();

        Chainable<String> actual = ChainableTree.values(actualTrees);

        // Then
        assertTrue(actual.equals(expected));
    }

    @Test
    public void testChildren() {
        // Given
        int firstRunLength = 2, secondRunLength = 3;
        ChainableTree<Long> root = ChainableTree.withRoot(0l);
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
        Chainable<ChainableTree<Long>> children1 = randomChildValues1.transform(v -> ChainableTree.withRoot(v));
        Chainable<ChainableTree<Long>> children2 = randomChildValues2.transform(v -> ChainableTree.withRoot(v));

        root
            .withChildren(children1)
            .withChildren(children2);

        String actual1 = root.children().join();
        String actual2 = root.children().join();

        // Then
        assertTrue(root.children().isCountExactly(firstRunLength + secondRunLength));
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

        // When
        String actual = infiniteTree
                .depthFirstNotBelow(t -> t.value().length() == 5)
                .join(", ");

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testFirst() {
        // Given
        String expected = "1.2";

        // When
        String actual1 = testTree.firstWithValue(expected).value();
        String actual2 = testTree.firstWhere(t -> Objects.equals(expected, t.value())).value();

        // Then
        assertEquals(expected, actual1);
        assertEquals(expected, actual2);
    }

    @Test
    public void testIsAbove() {
        // Given
        ChainableTree<String> tree = testTree.firstWithValue("1.2");

        // Then
        assertTrue(testTree.isAbove("1.1"));
        assertTrue(testTree.isAbove("1.1.2"));
        assertFalse(testTree.isAbove("1"));
        assertFalse(testTree.isAbove("2"));
        assertTrue(testTree.isAbove(t -> t.value().length() > 2));
        assertFalse(testTree.isAbove(t -> t.value().length() > 10));
        assertTrue(tree.isAbove("1.2.2"));
        assertFalse(tree.isAbove("1"));
        assertFalse(tree.isAbove(tree.value()));
    }

    @Test
    public void testIsBelow() {
        // Given
        ChainableTree<String> startTree = testTree.firstWithValue("1.1.2");

        // Then
        assertTrue(startTree.isBelow(t -> "1.1".equals(t.value())));
        assertTrue(startTree.isBelow(t -> "1".equals(t.value())));
        assertFalse(startTree.isBelow(t -> "1.1.2".equals(t.value())));
        assertFalse(startTree.isBelow(t -> "1.1.1".equals(t.value())));
        assertFalse(startTree.isBelow(t -> "1.2.1".equals(t.value())));
    }

    @Test
    public void testNotBelow() {
        // Given
        final String expected = "1, 1.1, 1.1.1, 1.1.2, 1.1.3, 1.2, 1.2.1, 1.2.2, 1.2.3, 1.3, 1.3.1, 1.3.2, 1.3.3";
        final String expectedSuccessors = "1.1.2";
        final String sep = ", ";

        // When
        ChainableTree<String> tree = infiniteTree.notBelow(o -> o.value().length() >= 5);
        Chainable<ChainableTree<String>> successors = tree
                .terminals()
                .where(t -> t.predecessor() != null)
                .where(t -> Objects.equals("1.1.1", t.predecessor().value()));
        String actual = ChainableTrees.values(tree.depthFirst()).join(sep);
        String actualSuccessors = ChainableTrees.values(successors).join(", ");

        // Then
        assertTrue(tree.breadthFirst().isCountAtMost(expected.split(sep).length));
        assertEquals(expected, actual);
        assertNotNull(successors);
        assertTrue(successors.isCountExactly(1));
        assertEquals(expectedSuccessors, actualSuccessors);
    }

    //TODO @Test - currently it will hang, because the tree is infinitely deep
    public void testNotWhere() {
        // When
        ChainableTree<String> tree = infiniteTree.notWhere(t -> t.value().contains("2"));

        // Then
        assertTrue(ChainableTrees.values(tree.breadthFirst().first(6)).noneWhere(i -> i.contains("2")));
    }

    @Test
    public void testParent() {
        // Given
        ChainableTree<String> tree = testTree.firstWithValue("1.1.2");
        assertNotNull(tree);

        // When
        ChainableTree<String> parent = tree.parent();

        // Then
        assertNotNull(parent);
        assertEquals("1.1", parent.value());
        assertNotNull(parent.parent());
        assertEquals("1", parent.parent().value());
    }

    @Test
    public void testReverseSiblings() {
        // Given
        String expected = "1.2, 1.1";
        ChainableTree<String> tree = treeFrom(new String[][][] {
            { { "1" }, { "1.1", "1.2", "1.3", "1.4" } }
        });

        ChainableTree<String> treeNode = tree.firstWithValue("1.3");

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
        ChainableTree<String> node12 = infiniteTree.firstWithValue("1.2");
        ChainableTree<String> node11 = infiniteTree.firstWithValue("1.1");

        // When
        String actualSiblings = node12.siblings().join(", ");
        String actualPredecessors = node12.predecessors().join(", ");
        String actualSuccessors = node11.successors().join(", ");

        // Then
        assertEquals("1.1", actualPredecessors);
        assertEquals("1.2, 1.3", actualSuccessors);
        assertEquals("1.1, 1.3", actualSiblings);
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
        ChainableTree<String> start = testTree.firstWithValue(startValue);
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
        ChainableTree<String> start = testTree.firstWithValue(startValue);
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
        ChainableTree<String> tree = ChainableTree.withRoot("A").withChildren(
                ChainableTree.withRoot("A.1")
                    .withChildren(
                            ChainableTree.withRoot("A.1.1"),
                            ChainableTree.withRoot("A.1.2")),
                ChainableTree.withRoot("A.2")
                    .withChildren(
                            ChainableTree.withRoot("A.2.1"),
                            ChainableTree.withRoot("A.2.2")));

        // When / Then
        assertTrue(tree.children().isCountExactly(2));
        assertTrue(tree.withoutChildren().children().isCountExactly(0));
    }
}
