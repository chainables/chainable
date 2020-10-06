/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.martinsawicki.chainable;

import com.github.martinsawicki.chainable.ChainableTrees.ChainableTreeImpl;

/**
 * A lazily evaluated, functional programming-based tree, where each node has children exposed as {@link Chainable} chains,
 * as well as a number of convenience methods.
 * <P>
 * Note that each node of the tree is itself a tree, so each tree has children and a parent that are trees themselves.
 * @author Martin Sawicki
 *
 * @param <T> type of values to wrap
 */
public interface ChainableTree<T> {
    /**
     * Returns the direct children of this tree.
     * <p>
     * The values of the children are lazily evaluated on each incomplete traversal, but once a complete traversal occurs, the values of the children
     * become fixed (cached).
     * @return direct children of this tree
     */
    Chainable<ChainableTree<T>> children();

    /**
     * Returns the wrapped value.
     * @return the wrapped value
     */
    T inner();

    /**
     * Returns the parent of this tree, or {@code null} if this is the root node.
     * @return the parent of this tree
     */
    ChainableTree<T> parent();

    /**
     * Appends the specified trees to the children of this tree, if any.
     * <p>
     * Passing {@code null} clears the existing children and makes the node childless.
     * @param children the direct children to append to the existing children of this tree
     * @return self
     */
    ChainableTree<T> withChildren(Iterable<ChainableTree<T>> children);

    /**
     * Appends the specified trees to the children of this tree, if any.
     * @param children the direct children to append to the existing children of this tree
     * @return self
     */
    @SuppressWarnings("unchecked")
    ChainableTree<T> withChildren(ChainableTree<T>... children);

    /**
     * Creates a new tree (a single node) with the specified wrapped {@code value}.
     * @param value the value to wrap in the new tree node
     * @return
     */
    static <T> ChainableTree<T> withValue(T value) {
        return new ChainableTreeImpl<T>(value);
    }
}