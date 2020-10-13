/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.martinsawicki.chainable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests
 */
public class ChainableTreeTest {
    @SuppressWarnings("unchecked")
    ChainableTree<String> testTree = ChainableTree.withValue("1").withChildren(
            ChainableTree.withValue("1.1").withChildValues("1.1.1", "1.1.2"),
            ChainableTree.withValue("1.2").withChildValues("1.2.1", "1.2.2"));

    @Test
    public void testBreadthFirst() {
        // Given
        String expected = "1, 1.1, 1.2, 1.1.1, 1.1.2, 1.2.1, 1.2.2";

        // When
        String actual = ChainableTree.values(testTree.breadthFirst()).join(", ");

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testBreadthFirstNotBelow() {
        // Given
        String expected = "1, 1.1, 1.2, 1.2.1, 1.2.2";

        // When
        String actual = ChainableTree.values(testTree.breadthFirstNotBelow(t -> "1.1".equals(t.value()))).join(", ");

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

        String actual1 = ChainableTrees.values(root.children()).join();
        String actual2 = ChainableTrees.values(root.children()).join();

        // Then
        assertEquals(firstRunLength + secondRunLength, root.children().count());
        assertEquals(actual1, actual2); // Ensure children are evaluated only once and cached from thereon
    }

    @Test
    public void testDepthFirst() {
        // Given
        String expected = "1, 1.1, 1.1.1, 1.1.2, 1.2, 1.2.1, 1.2.2";

        // When
        String actual = ChainableTrees.values(testTree.depthFirst()).join(", ");

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
                        .chain((c, i) -> String.format("%s.%s", p, Long.toString(i + 1)))
                        .first(3)
                        .cast(String.class)); // Limit the number, otherwise infinite

        // When
        String actual = ChainableTree.values(tree.depthFirstNotBelow(t -> t.value().length() == 5))
                .join(", ");

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void testSkipping() {
        // Given
        String expected = "1.1.2, 1.2, 1.2.1, 1.2.2";

        // When
        String actual = ChainableTrees.values(testTree.depthFirst()).notBeforeEquals("1.1.2").join(", ");

        // Then
        assertEquals(expected, actual);        
    }

    @Test
    public void testSuccessors() {
        // Given
        String expected = "1.2";

        // When
        String actual = ChainableTree.values(testTree.children().first().successors()).join(", ");

        // Then
        assertEquals(expected, actual);
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
