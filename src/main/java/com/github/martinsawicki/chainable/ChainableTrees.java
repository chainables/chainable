/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.martinsawicki.chainable;

import java.util.ArrayList;
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

        protected ChainableTreeImpl(T inner) {
            this.value = inner;
        }

        @Override
        public Chainable<ChainableTree<T>> children() {
            return this.childrenChain;
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
            if (children != null) {
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
                    .transform(c -> ChainableTree.withValue(c)));
        }

        @SuppressWarnings("unchecked")
        @Override
        public ChainableTreeImpl<T> withChildValues(T... childValues) {
            return this.withChildValues(Chainable.from(childValues));
        }

        @Override
        public ChainableTreeImpl<T> withChildValueExtractor(Function<T, Iterable<T>> childExtractor) {
            if (childExtractor != null) {
                return this.withChildren(Chainable
                        .from(childExtractor.apply(this.value()))
                        .transform(c -> ChainableTree.withValue(c).withChildValueExtractor(childExtractor)));
            } else {
                return this;
            }
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
     * @param root
     * @return
     * @see ChainableTree#breadthFirst()
     */
    public static <T> Chainable<ChainableTree<T>> breadthFirst(ChainableTree<T> root) {
        return Chainable.from(root).breadthFirst(t -> t.children());
    }

    /**
     * @param tree
     * @param condition
     * @return
     * @see ChainableTree#breadthFirstNotBelow(Predicate)
     */
    public static <T> Chainable<ChainableTree<T>> breadthFirstNotBelow(ChainableTree<T> tree, Predicate<ChainableTree<T>> condition) {
        return Chainable.from(tree).breadthFirstNotBelow(t -> t.children(), condition);
    }

    /**
     * @param tree
     * @return
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
     * @return
     * @see ChainableTree#depthFirstNotBelow(Predicate)
     */
    public static <T> Chainable<ChainableTree<T>> depthFirstNotBelow(ChainableTree<T> tree, Predicate<ChainableTree<T>> condition) {
        return Chainable.from(tree).depthFirstNotBelow(t -> t.children(), condition);
    }

    /**
     * @param tree
     * @return
     * @see ChainableTree#predecessors()
     */
    public static <T> Chainable<ChainableTree<T>> predecessors(final ChainableTree<T> tree) {
        return (tree == null || tree.parent() == null) ? Chainable.empty() : Chainable
                .from(tree.parent().children())
                .before(c -> tree.equals(c));
    }

    /**
     * @param tree
     * @return
     * @see ChainableTree#successors()
     */
    public static <T> Chainable<ChainableTree<T>> successors(ChainableTree<T> tree) {
        return (tree == null || tree.parent() == null) ? Chainable.empty() : Chainable
                .from(tree.parent().children())
                .notBefore(c -> tree.equals(c))
                .afterFirst();
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
