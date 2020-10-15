/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.martinsawicki.chainable;

import java.util.function.Function;
import java.util.function.Predicate;

import com.github.martinsawicki.chainable.ChainableTrees.ChainableTreeImpl;

/**
 * A lazily evaluated, functional programming-based tree, where each node has children exposed as {@link Chainable} chains,
 * as well as a number of convenience methods.
 * <P>
 * Each "node" of the tree is itself a tree, so each tree has children and a parent that are trees themselves.
 * <p>
 * Note that in many operations that perform navigation relative to a specific tree, instances of tree nodes are compared to each other using
 * the {@code equals()} method applied to their {@link ChainableTree#value()}, since the tree instances themselves may be different across different
 * invocations of this method, even if referring to the same location in the tree. This implies that the values carried by the tree nodes should
 * be unique across the entire tree, that is there should be no two nodes within a tree for which {@code equals()} would return {@code true}.
 * Otherwise, the behavior is undefined, as this restriction is not explicitly enforced.

 * @author Martin Sawicki
 *
 * @param <T> type of values to wrap
 */
public interface ChainableTree<T> {
    /**
     * Traverses the tree in a breadth-first fashion returning a chain of encountered nodes.
     * @return the resulting chain of visited tree nodes
     */
    default Chainable<ChainableTree<T>> breadthFirst() {
        return ChainableTrees.breadthFirst(this);
    }

    /**
     * Traverses the tree in a breadth-first fashion returning a chain of encountered nodes, but excluding th edescendants that meet the
     * specified {@code condition}.
     * <p>
     * In other words, the node that satisfies this condition is included in the returned chain, but its descendants are not.
     * @param condition the condition for a node to satisfy so that its descendants would not be traversed
     * @return the resulting chain of visited tree nodes
     */
    default Chainable<ChainableTree<T>> breadthFirstNotBelow(Predicate<ChainableTree<T>> condition) {
        return ChainableTrees.breadthFirstNotBelow(this, condition);
    }

    /**
     * Returns the direct children of this tree.
     * <p>
     * The values of the children are lazily evaluated on each incomplete traversal, but once a complete traversal occurs, the values of the children
     * become fixed (cached).
     * @return direct children of this tree
     */
    Chainable<ChainableTree<T>> children();

    /**
     * Traverses this tree in a breadth-first fashion returning a chain of encountered nodes.
     * @return the resulting chain of visited tree nodes
     */
    default Chainable<ChainableTree<T>> depthFirst() {
        return ChainableTrees.depthFirst(this);
    }

    /**
     * Traverses this tree in a depth-first fashion returning a chain of encountered nodes, but excluding the descendants of nodes that meet the
     * specified {@code condition}.
     * <p>
     * In other words, the node that satisfies this condition is included in the returned chain, but its descendants are not.
     * @param condition the condition for a node to satisfy so that its descendants would not be traversed
     * @return the resulting chain of visited tree nodes
     */
    default Chainable<ChainableTree<T>> depthFirstNotBelow(Predicate<ChainableTree<T>> condition) {
        return ChainableTrees.depthFirstNotBelow(this, condition);
    }

    /**
     * Returns the wrapped value.
     * @return the wrapped value
     */
    T value();

    /**
     * Returns the parent of this tree, or {@code null} if this is the root node.
     * @return the parent of this tree
     */
    ChainableTree<T> parent();

    /**
     * Returns a chain of siblings preceding this tree node.
     * @return a chain of sibling tree nodes preceding this one
     * @see #successors()
     * @see #siblings()
     */
    default Chainable<ChainableTree<T>> predecessors() {
        return ChainableTrees.predecessors(this);
    }

    /**
     * Returns a chain of all the siblings of this tree node
     * @return a chain of all the sibling tree nodes of this one
     * @see #predecessors()
     * @see #successors()
     */
    default Chainable<ChainableTree<T>> siblings() {
        return ChainableTrees.siblings(this);
    }

    /**
     * Returns a chain of siblings following this tree node.
     * @return a chain of sibling tree nodes following this one
     * @see #siblings()
     * @see #predecessors()
     */
    default Chainable<ChainableTree<T>> successors() {
        return ChainableTrees.successors(this);
    }

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
     * Wraps the values generated lazily by the specified {@code childExtractor} into child trees of this tree, appending them to the existing
     * children of this tree.
     * @param childExtractor a function that returns child values based on the parent value it is being fed
     * @return self
     */
    ChainableTree<T> withChildValueExtractor(Function<T, Iterable<T>> childExtractor);

    /**
     * Appends trees with the specified wrapped {@code childValues} to the children of this tree.
     * @param childValues child values to wrap in trees and appends to the children of this tree
     * @return self
     */
    ChainableTree<T> withChildValues(Iterable<T> childValues);

    /**
     * Appends trees with the specified wrapped {@code childValues} to the children of this tree.
     * @param childValues child values to wrap in trees and appends to the children of this tree
     * @return self
     */
    @SuppressWarnings("unchecked")
    ChainableTree<T> withChildValues(T... childValues);

    /**
     * Removes all children from this tree.
     * @return self
     */
    ChainableTree<T> withoutChildren();

    /**
     * Creates a new tree (a single node) with the specified wrapped {@code value}.
     * @param value the value to wrap in the new tree node
     * @return
     */
    static <T> ChainableTree<T> withValue(T value) {
        return new ChainableTreeImpl<T>(value);
    }

    /**
     * Extracts the chain of values from the specified {@code trees}.
     * @param trees the trees to extract values from
     * @return a chain of values from the specified {@code trees}
     */
    static <T> Chainable<T> values(Iterable<ChainableTree<T>> trees) {
        return ChainableTrees.values(trees);
    }
}