/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.chainables.chainable;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import com.github.chainables.chainable.ChainableTrees.ChainableTreeImpl;

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
public interface ChainableTree<T> extends Cloneable {

    /**
     * Returns the chain of ancestors of this tree node starting with its parent.
     * @return a chain of ancestors, starting with the parent
     * @see #descendants()
     */
    default Chainable<ChainableTree<T>> ancestors() {
        return ChainableTrees.ancestors(this);
    }

    /**
     * Traverses the tree in a breadth-first fashion returning a chain of encountered nodes.
     * @return the resulting chain of visited tree nodes
     */
    default Chainable<ChainableTree<T>> breadthFirst() {
        return ChainableTrees.breadthFirst(this);
    }

    /**
     * Returns the direct children of this tree.
     * <p>
     * The values of the children are lazily evaluated on each incomplete traversal, but once a complete traversal occurs, the values of the children
     * become fixed (cached).
     * @return direct children of this tree
     */
    Chainable<ChainableTree<T>> children();

    ChainableTree<T> clone();

    /**
     * Traverses this tree in a depth-first (pre-order) fashion returning a chain of encountered nodes.
     * @return the resulting chain of visited tree nodes
     */
    default Chainable<ChainableTree<T>> depthFirst() {
        return ChainableTrees.depthFirst(this);
    }

    /**
     * Traverses this tree in a depth-first (pre-order) fashion returning a chain of encountered nodes, but excluding the descendants of nodes that meet the
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
     * Returns the chain of all the descendants of this tree node, in a breadth-first order
     * @return all descendants of this tree
     * @see #ancestors()
     */
    default Chainable<ChainableTree<T>> descendants() {
        return ChainableTrees.descendants(this);
    }

    /**
     * Finds the first tree node that satisfies the specified {@code condition}, based on a breadth-first traversal.
     * @param condition the condition for the sought tree node to satisfy
     * @return the first tree node satisfying the specified {@code condition}
     * @see #firstWithValue(Object)
     */
    default ChainableTree<T> firstWhere(Predicate<ChainableTree<T>> condition) {
        return ChainableTrees.firstWhere(this, condition);
    }

    
    /**
     * Finds the first tree node with the specified {@code value}, based on a breadth-first traversal.
     * @param value the value to search for
     * @return the first found tree node with the specified {@code value}, based on a breadth-first traversal
     * @see #firstWhere(Predicate)
     */
    default ChainableTree<T> firstWithValue(T value) {
        return ChainableTrees.firstWithValue(this, value);
    }

    /**
     * Checks whether the specified {@code value} is the value of a tree node under this tree.
     * @param value the value to look for
     * @return true if the specified {@code value} is the value of a tree node that is under this tree.
     */
    default boolean isAbove(T value) {
        return ChainableTrees.isAbove(this, value);
    }

    /**
     * Checks whether a tree node satisfying the specified {@code condition} is under this tree.
     * @param condition the condition for the tree node under this one to satisfy
     * @return true if there is a tree node under this tree that satisfies the specified {@code condition}
     */
    default boolean isAbove(Predicate<ChainableTree<T>> condition) {
        return ChainableTrees.isAbove(this, condition);
    }

    /**
     * Checks whether this tree node is a descendant of a tree node satisfying the specified ancestor condition.
     * @param ancestorCondition the condition that this node's ancestor has to meet for this node to be under it
     * @return true if the tree node is a descendant of the tree node satisfying the specified ancestor condition.
     */
    default boolean isBelow(Predicate<ChainableTree<T>> ancestorCondition) {
        return ChainableTrees.isBelow(this, ancestorCondition);
    }

    /**
     * Checks whether this tree node is under (that is a descendant of) of a tree node with the specified {@code value}.
     * @param value the value for the ancestor tree node to contain
     * @return true if this tree node is under a tree node with the specified {@code value}
     */
    default boolean isBelow(T value) {
        return ChainableTrees.isBelow(this, value);
    }

    /**
     * Returns the subset of the specified {@code tree} without the children of the tree nodes that satisfy the specified {@code condition}.
     * @param tree the tree to search
     * @param condition the condition to satisfy for a node for its children to be excluded from the returned subset
     * @return a subset view of the specified tree
     */
    default ChainableTree<T> notBelow(Predicate<ChainableTree<T>> condition) {
        return ChainableTrees.notBelow(this, condition);
    }

    /**
     * Returns a tree that is made of only those tree nodes of this tree that do not satisfy the specified {@code condition}, but other than that,
     * their ancestor-descendant hierarchy is preserved.
     * <p>
     * For example, if some children of a given tree node satisfy the specified condition, they are nt included in the resulting tree, but their
     * children that do not satisfy it are, as siblings of their removed parent node siblings.
     * @param condition the condition for tree nodes to satisfy to not be included in the tree
     * @return the resulting tree without the nodes satisfying the specified condition
     */
    default ChainableTree<T> notWhere(Predicate<ChainableTree<T>> condition) {
        return ChainableTrees.notWhere(this, condition);
    }

    /**
     * Returns the parent of this tree, or {@code null} if this is the root node.
     * @return the parent of this tree
     */
    ChainableTree<T> parent();

    /**
     * Returns the sibling immediately preceding this tree node, or {@code null} if none.
     * @return the sibling immediately preceding this tree node, or {@code null} if none.
     */
    default ChainableTree<T> predecessor() {
        return ChainableTrees.predecessor(this);
    }

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
     * Returns the sibling immediately following this tree node, or {@code null} if none.
     * @return the sibling immediately following this tree node, or {@code null} if none.
     */
    default ChainableTree<T> successor() {
        return ChainableTrees.successor(this);
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
     * Returns the terminal nodes of this tree, that is nodes that do not have any children.
     * @return terminal nodes of this tree
     */
    default Chainable<ChainableTree<T>> terminals() {
        return ChainableTrees.terminals(this);
    }

    /**
     * Returns the last ancestor up the chain of ancestors, but starting with this tree, that satisfies the specified {@code condition}.
     * <p>
     * If the entire ancestor chain satisfies the specified condition, the topmost tree node (root) is returned.
     * @param condition the condition that the last ancestor up the chain of ancestors is to satisfy
     * @return the last ancestor up the ancestor chain starting with this tree itself, that satisfies the specified condition
     * @see #upUntil(Predicate)
     * @see #upUntilEither(Predicate...)
     * @see #ancestors()
     */
    default ChainableTree<T> upAsLongAs(Predicate<ChainableTree<T>> condition) {
        return ChainableTrees.upAsLongAs(this, condition);
    }

    /**
     * Returns the first ancestor up the chain of ancestors, but starting with this tree, that satisfies the specified {@code condition}.
     * @param condition the condition that the first ancestor up the chain of ancestors is to satisfy
     * @return the first ancestor up the ancestor chain starting with this tree itself, that satisfies the specified condition, or {@code null} if
     * no such ancestor is found
     * @see #upUntilEither(Predicate...)
     * @see #upAsLongAs(Predicate)
     * @see #ancestors()
     */
    default ChainableTree<T> upUntil(Predicate<ChainableTree<T>> condition) {
        return ChainableTrees.upUntil(this, condition);
    }

    /**
     * Returns the first ancestor up the chain of ancestors, but starting with this tree, that satisfies any of the specified {@code conditions}.
     * @param conditions a set of conditions for the sought ancestor to satisfy any of
     * @return the first ancestor up the ancestor chain starting with this tree itself, that satisfies any of the specified conditions, or {@code null} if
     * no such ancestor is found
     * @see #upUntil(Predicate)
     * @see #upAsLongAs(Predicate)
     * @see #ancestors()
     */
    @SuppressWarnings("unchecked")
    default ChainableTree<T> upUntilEither(Predicate<ChainableTree<T>>...conditions) {
        return ChainableTrees.upUntilEither(this, conditions);
    }

    /**
     * Returns the wrapped value.
     * @return the wrapped value
     */
    T value();

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
     * Wraps the values generated lazily by the specified {@code childExtractor} into child trees of this tree, appending them to the existing
     * children of this tree, passing the current depth relative to this tree node to the extractor.
     * @param childExtractor a function accepting the value of the parent and the children's depth level relative to this tree node and returning child values
     * @return self
     */
    ChainableTree<T> withChildValueExtractor(BiFunction<T, Long, Iterable<T>> childExtractor);

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
    static <T> ChainableTree<T> withRoot(T value) {
        return ChainableTreeImpl.withRoot(value);
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