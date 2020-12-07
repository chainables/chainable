/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.chainables.chainable;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * This is the source of all the static methods underlying the default implementation of {@link ChainableTree} as well as some other conveniences.
 * @author Martin Sawicki
 */
public abstract class ChainableTrees {

    static class ChainableTreeImpl<T> implements ChainableTree<T> {
        private final T value;
        private ChainableTree<T> parent = null;
        private Chainable<ChainableTree<T>> childrenChain = Chainable.from(new ArrayList<>());

        private ChainableTreeImpl(T inner) {
            this.value = inner;
        }

        static protected <T> ChainableTreeImpl<T> withRoot(T value) {
            return new ChainableTreeImpl<T>(value);
        }

        @Override
        public Chainable<ChainableTree<T>> children() {
            return this.childrenChain;
        }

        @Override
        public ChainableTreeImpl<T> clone() {
            ChainableTreeImpl<T> clone = new ChainableTreeImpl<>(this.value);
            clone.parent = this.parent;
            clone.childrenChain = this.childrenChain; //TODO: This breaks stuff - why?
            return clone;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (!(obj instanceof ChainableTree<?>)) {
                return false;
            }

            ChainableTree<?> tree = (ChainableTree<?>) obj;
            if (tree.value() == null) {
                return this.value == null;
            } else if (this.value == null) {
                return false;
            } else {
                return this.value.equals(tree.value());
            }
        }

        @Override
        public int hashCode() {
            return (this.value == null) ? 0 : this.value.hashCode();
        }

        @Override
        public T value() {
            return this.value;
        }

        @Override
        public ChainableTree<T> parent() {
            return this.parent;
        }

        @Override
        public String toString() {
            return (this.value == null) ? null : this.value.toString();
        }

        @Override
        public ChainableTreeImpl<T> withChildren(Iterable<ChainableTree<T>> children) {
            if (Chainables.any(children)) {
                // Link children to parent
                Chainable<ChainableTree<T>> newChildren = Chainables
                        .applyAsYouGo(children, c -> ((ChainableTreeImpl<T>)c).withParent(this))
                        .cached(); // Cached after first complete evaluation

                // Attach to end of parent's children
                this.childrenChain = this.childrenChain.concat(newChildren);
            } else {
                this.childrenChain = Chainable.empty();
            }

            return this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public ChainableTreeImpl<T> withChildren(ChainableTree<T>... children) {
            return this.withChildren(Chainable.from(children));
        }

        @Override
        public ChainableTreeImpl<T> withChildValues(Iterable<T> childValues) {
            return this.withChildren(Chainable
                    .from(childValues)
                    .transform(c -> ChainableTree.withRoot(c)));
        }

        @SuppressWarnings("unchecked")
        @Override
        public ChainableTreeImpl<T> withChildValues(T... childValues) {
            return this.withChildValues(Chainable.from(childValues));
        }

        @Override
        public ChainableTreeImpl<T> withChildValueExtractor(Function<T, Iterable<T>> childExtractor) {
            return (childExtractor == null) ? this : this.withChildren(Chainable
                    .from(childExtractor.apply(this.value())) // Generate child values based on parent's value
                    .transform(c -> ChainableTree
                            .withRoot(c) // Wrap each child value with tree...
                            .withChildValueExtractor(childExtractor))); // ... and pass the extractor on to it to generate its own children
        }

        @Override
        public ChainableTreeImpl<T> withChildValueExtractor(BiFunction<T, Long, Iterable<T>> childExtractor) {
            return withChildValueExtractor(childExtractor, 1); // Root is 0, so first level of children is 1
        }

        private ChainableTreeImpl<T> withChildValueExtractor(BiFunction<T, Long, Iterable<T>> childExtractor, long level) {
            return childExtractor == null ? this : this.withChildren(Chainable
                    .from(childExtractor.apply(this.value(), level)) // Generate child values at lower depth
                    .transform(c -> ChainableTreeImpl
                            .withRoot(c)
                            .withChildValueExtractor(childExtractor, level + 1)));
        }

        @Override
        public ChainableTree<T> withoutChildren() {
            return this.withChildren((Iterable<ChainableTree<T>>) null);
        }

        ChainableTreeImpl<T> withParent(ChainableTree<T> parent) {
            this.parent = parent;
            return this;
        }
    }

    /**
     * @param tree
     * @return a chain of the ancestors of the specified {@code tree}, starting with the immediate parent, all the way up to the root
     * @see ChainableTree#ancestors()
     */
    public static <T> Chainable<ChainableTree<T>> ancestors(ChainableTree<T> tree) {
        return (tree == null || tree.parent() == null) ? Chainable.empty() : Chainable
                .from(tree.parent())
                .chain(t -> t.parent());
    }

    /**
     * @param root
     * @return a chain of tree nodes, starting with the specified {@code tree} and its children, traversed in a breadth-first fashion
     * @see ChainableTree#breadthFirst()
     */
    public static <T> Chainable<ChainableTree<T>> breadthFirst(ChainableTree<T> root) {
        return Chainable
                .from(root)
                .breadthFirst(t -> t.children());
    }

    /**
     * @param root
     * @return a chain of all the descendants listed in a breadth first fashion, starting with the children of the specified {@code tree}
     * @see ChainableTree#descendants()
     */
    public static <T> Chainable<ChainableTree<T>> descendants(ChainableTree<T> root) {
        return (root == null) ? Chainable.empty() : breadthFirst(root).afterFirst();
    }

    /**
     * @param tree
     * @return a chain of tree nodes, starting with the specified {@code tree} and its children, traversed in a pre-order depth-first fashion
     * @see ChainableTree#depthFirst()
     */
    public static <T> Chainable<ChainableTree<T>> depthFirst(ChainableTree<T> tree) {
        return Chainable
                .from(tree)
                .depthFirst(t -> t.children());
    }

    /**
     * @param tree
     * @param condition
     * @return a chain of tree nodes, starting with the specified {@code tree} and its children, traversed in a pre-order depth-first fashion, but not below the tree nodes
     * that satisfy the specified {@code condition}
     * @see ChainableTree#depthFirstNotBelow(Predicate)
     */
    public static <T> Chainable<ChainableTree<T>> depthFirstNotBelow(ChainableTree<T> tree, Predicate<ChainableTree<T>> condition) {
        return Chainable
                .from(tree)
                .depthFirstNotBelow(t -> t.children(), condition);
    }

    /**
     * @param tree
     * @param condition
     * @return the first tree node starting with the specified {@code tree} and below, that satisfies the specified condition, found in a breadth-first fashion
     * @see ChainableTree#firstWhere(Predicate)
     */
    public static <T> ChainableTree<T> firstWhere(ChainableTree<T> tree, Predicate<ChainableTree<T>> condition) {
        return (tree == null || condition == null) ? null : breadthFirst(tree).firstWhere(condition);
    }

    /**
     * @param tree
     * @param value
     * @return the first tree node starting with the specified {@code tree} and below, whose value is equal to the specified {@code value}
     * @see ChainableTree#firstWithValue(Object)
     */
    public static <T> ChainableTree<T> firstWithValue(ChainableTree<T> tree, T value) {
        return (tree == null) ? null : breadthFirst(tree).firstWhere(t -> Objects.equals(t.value(), value));
    }

    /**
     * @param ancestor
     * @param value
     * @return {@code true} iff any of the descendants of the specified {@code tree} have the specified {@code value}
     * @see ChainableTree#isAbove(Object)
     */
    public static <T> boolean isAbove(ChainableTree<T> ancestor, T value) {
        return (ancestor == null) ? false : ChainableTrees.values(descendants(ancestor)).contains(value);
    }

    /**
     * @param tree
     * @param descendantCondition
     * @return {@code true} iff any of the descendants of the specified {@code tree} meet the specified {@code descendantCondition}
     * @see ChainableTree#isAbove(Predicate)
     */
    public static <T> boolean isAbove(ChainableTree<T> tree, Predicate<ChainableTree<T>> descendantCondition) {
        return (tree == null || descendantCondition == null) ? false : descendants(tree).anyWhere(descendantCondition);
    }

    /**
     * @param tree
     * @param ancestorCondition
     * @return {@code true} iff any of the ancestors of the specified {@code tree} meet the specified {@code ancestorCondition}
     * @see ChainableTree#isBelow(Predicate)
     */
    public static <T> boolean isBelow(ChainableTree<T> tree, Predicate<ChainableTree<T>> ancestorCondition) {
        return (tree == null | ancestorCondition == null) ? false : ancestors(tree).anyWhere(ancestorCondition);
    }

    /**
     * @param tree
     * @param value
     * @return {@code true} iff any of the ancestors of the specified {@code tree} have the specified {@code value}
     * @see ChainableTree#isBelow(Object)
     */
    public static <T> boolean isBelow(ChainableTree<T> tree, T value) {
        return (tree == null) ? false : ChainableTrees.values(ancestors(tree)).contains(value);
    }

    /**
     * @param tree
     * @param condition
     * @return a tree whose root is the specified {@code tree} and whose descendants are clones of the descendants of this tree, but not below
     * the descendants that meet the specified {@code condition}
     * @see ChainableTree#notBelowWhere(Predicate)
     */
    public static <T> ChainableTree<T> notBelowWhere(ChainableTree<T> tree, Predicate<ChainableTree<T>> condition) {
        return (tree == null || condition == null) ? tree : tree
                .clone()
                .withoutChildren()
                .withChildren(condition.test(tree)
                    ? Chainable.empty() // No children if condition is satisfied
                    : Chainables.transform(tree.children(), c -> notBelowWhere(c, condition))); // Otherwise, trim children recursively
    }

    /**
     * @param tree
     * @param depthAwareCondition
     * @return a tree whose root is the specified {@code tree} and whose descendants are clones of the descendants of this tree, but not below
     * the descendants that meet the specified {@code condition}, which is passed the parent tree and its depth relative to the root
     * @see ChainableTree#notBelowWhere(BiPredicate)
     */
    public static <T> ChainableTree<T> notBelowWhere(ChainableTree<T> tree, BiPredicate<ChainableTree<T>, Long> depthAwareCondition) {
        return notBelowWhere(tree, depthAwareCondition, 0);
    }

    private static <T> ChainableTree<T> notBelowWhere(ChainableTree<T> tree, BiPredicate<ChainableTree<T>, Long> depthAwareCondition, long curDepth) {
        return (tree == null || depthAwareCondition == null) ? tree : tree
                .clone()
                .withoutChildren()
                .withChildren(depthAwareCondition.test(tree, curDepth)
                        ? Chainable.empty() // No children if condition satisfied
                        : tree.children().transform(c -> notBelowWhere(c, depthAwareCondition, curDepth + 1))); // Grand-children at two levels deeper
    }

    /**
     * @param tree
     * @param condition
     * @return a tree whose root is the specified {@code tree} and whose descendants are clones of the descendants of this tree, but without those
     * that satisfy the specified {@code condition}; instead they are replaced with those of its children or descendants that do not satisfy it
     * @see ChainableTree#notWhere(Predicate)
     */
    public static <T> ChainableTree<T> notWhere(ChainableTree<T> tree, Predicate<ChainableTree<T>> condition) {
        return (tree == null || condition == null) ? tree : tree
                .clone()
                .withoutChildren()
                .withChildren(tree
                        .children()
                        .transformAndFlatten(c -> c
                                .depthFirstNotBelow(cc -> !condition.test(cc))
                                .notWhere(cc -> condition.test(cc))
                                .transform(cc -> notWhere(cc, condition))));
    }

    /**
     * @param tree
     * @param condition
     * @return a tree whose root is the specified {@code tree} and whose descendants are clones of the descendants of this tree and satisfy the
     * specified {@code condition}, whereas the ones that don't are replaced with those of their children or descendants that do
     * @see ChainableTree#where(Predicate)
     */
    public static <T> ChainableTree<T> where(ChainableTree<T> tree, Predicate<ChainableTree<T>> condition) {
        return (tree == null || condition == null) ? tree : tree
                .clone()
                .withoutChildren()
                .withChildren(tree
                        .children()
                        .transformAndFlatten(c -> c
                                .depthFirstNotBelow(cc -> condition.test(cc))
                                .where(cc -> condition.test(cc))
                                .transform(cc -> where(cc, condition))));
    }

    /**
     * @param tree
     * @return the node preceding the specified {@code tree} relative to its parent, if any, or {@code null} otherwise
     * @see ChainableTree#predecessor()
     */
    public static <T> ChainableTree<T> predecessor(ChainableTree<T> tree) {
        return predecessors(tree).last();        
    }

    /**
     * @param tree
     * @return all the nodes that precede the specified {@code tree} within its parent, starting with the first one
     * @see ChainableTree#predecessors()
     */
    public static <T> Chainable<ChainableTree<T>> predecessors(final ChainableTree<T> tree) {
        return (tree == null || tree.parent() == null) ? Chainable.empty() : Chainable
                .from(tree.parent().children())
                .before(c -> Objects.equals(tree, c));
    }

    /**
     * @param tree
     * @return all the other tree nodes that are children of the same parent as the specified {@code tree}
     * @see ChainableTree#siblings()
     */
    public static <T> Chainable<ChainableTree<T>> siblings(ChainableTree<T> tree) {
        return (tree == null || tree.parent() == null) ? Chainable.empty() : Chainable
                .from(tree.parent().children())
                .where(c -> !Objects.equals(c, tree));
    }

    /**
     * @param tree
     * @return the node following the specified {@code tree} relative to its parent, if any, or {@code null} otherwise
     * @see ChainableTree#successor()
     */
    public static <T> ChainableTree<T> successor(ChainableTree<T> tree) {
        return successors(tree).first();        
    }

    /**
     * @param tree
     * @return all the nodes that follow the specified {@code tree} within its parent, starting with the first one
     * @see ChainableTree#successors()
     */
    public static <T> Chainable<ChainableTree<T>> successors(ChainableTree<T> tree) {
        return (tree == null || tree.parent() == null) ? Chainable.empty() : Chainable
                .from(tree.parent().children())
                .notBefore(c -> Objects.equals(tree, c))
                .afterFirst();
    }

    /**
     * @param root
     * @return the terminal descendants of the specified {@code root} tree, that is those that do not have children
     * @see ChainableTree#terminals()
     */
    public static <T> Chainable<ChainableTree<T>> terminals(ChainableTree<T> root) {
        return (root != null) ? depthFirst(root).where(t -> Chainables.isNullOrEmpty(t.children())) : null;
    }

    /**
     * @param tree
     * @param condition
     * @return the last ancestor that satisfies the specified {@code condition} starting with the specified {@code tree} itself and going up (so the result may
     * be the specified {@code tree} itself, if its parent does not meet the {@code condition})
     * @see ChainableTree#upAsLongAs(Predicate)
     */
    public static <T> ChainableTree<T> upAsLongAs(ChainableTree<T> tree, Predicate<ChainableTree<T>> condition) {
        return (tree == null || condition == null) ? tree : Chainable
                .from(tree)
                .concat(ancestors(tree))
                .before(t -> !condition.test(t))
                .last();
    }

    /**
     * @param tree
     * @param condition
     * @return the first ancestor that satisfies the specified {@code condition} starting with the specified {@code tree} itself and going up (so the
     * result may be the {@code tree} itself if it already satisfies the {@code condition}
     * @see ChainableTree#upUntil(Predicate)
     */
    public static <T> ChainableTree<T> upUntil(ChainableTree<T> tree, Predicate<ChainableTree<T>> condition) {
        return (tree == null || condition == null) ? tree : Chainable
                .from(tree)
                .concat(ancestors(tree))
                .firstWhere(condition);
    }

    /**
     * @param tree
     * @param conditions
     * @return the first ancestor that satisfies any of the specified {@code conditions} starting with the specified {@code tree} itself and going up (so the
     * result may be the {@code tree} itself if it already satisfies the {@code conditions}
     * @see ChainableTree#upUntilEither(Predicate...)
     */
    @SafeVarargs
    public static <T> ChainableTree<T> upUntilEither(ChainableTree<T> tree, Predicate<ChainableTree<T>>...conditions) {
        return (tree == null || conditions == null) ? tree : Chainable
                .from(tree)
                .concat(ancestors(tree))
                .firstWhereEither(conditions);
    }

    /**
     * Returns the inner wrapped values of the specified {@code trees}.
     * @param trees the tree nodes to extract wrapped values from
     * @return the wrapper inner values of the specified tree nodes
     */
    public static <T> Chainable<T> values(Iterable<ChainableTree<T>> trees) {
        return (trees == null) ? Chainable.empty() : Chainable
                .from(trees)
                .transform(t -> t.value());
    }
}
