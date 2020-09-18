/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.martinsawicki.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * This is the source of all the static methods underlying the default implementation of {@link Chainable} as well as some other conveniences.
 * @author Martin Sawicki
 *
 */
public final class Chainables {
    private Chainables() {
        throw new AssertionError("Not instantiable, just stick to the static methods.");
    }

    public interface Chainable<T> extends Iterable<T> {
        /**
         * Returns an empty chain.
         * @return an empty {@link Chainable}
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#empty()}</td></tr>
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Empty()}</td></tr>
         * </table>
         * @see #any()
         */
        static <T> Chainable<T> empty() {
            return Chain.empty();
        }

        /**
         * Creates a new chain from the specified {@code items} in a "lazy" fashion, i.e. not traversing/evaluating the items, just holding an internal reference
         * to them.
         * @param items
         * @return a {@link Chainable} wrapper for the specified {@code items}
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>{@link Collection#stream()} but operating on {@link Iterable}, so not requiring a {@link Collection} as a starting point</td></tr>
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.AsEnumerable()}</td></tr>
         * </table>
         */
        static <T> Chainable<T> from(Iterable<T> items) {
            return Chain.from(items);
        }

        /**
         * Creates a new chain from the specified {@code items} in a "lazy" fashion, i.e. not traversing/evaluating/copying the items, just holding an internal reference
         * to them.
         * @param items
         * @return an {@link Chainable} wrapper for the specified {@code items}
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#of(Object...)}</td></tr>
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.AsEnumerable()}</td></tr>
         * </table>
         */
        @SafeVarargs
        static <T> Chainable<T> from(T...items) {
            return Chain.from(items);
        }

        /**
         * Creates a new chain from the specified {@code stream}, which supports multiple traversals, just like a standard {@link java.lang.Iterable},
         * even though the underlying {@link java.lang.stream.Stream} does not.
         * <p>
         * Note that upon subsequent traversals of the chain, the original stream is not recomputed, but rather its values as obtained during its
         * first traversal are cached internally and used for any subsequent traversals.
         * @param stream
         * @return a chain based on the specified {@code stream}
         */
        static <T> Chainable<T> from(Stream<T> stream) {
            if (stream == null) {
                return Chainable.empty();
            }

            return Chainable.from(new Iterable<T>() {

                List<T> cache = new ArrayList<>();
                Iterator<T> iter = stream.iterator();

                @Override
                public Iterator<T> iterator() {
                    if (this.iter == null) {
                        return this.cache.iterator();
                    } else {
                        return new Iterator<T>() {
                            @Override
                            public boolean hasNext() {
                                if (iter.hasNext()) {
                                    return true;
                                } else {
                                    iter = null;
                                    return false;
                                }
                            }

                            @Override
                            public T next() {
                                T next = iter.next();
                                cache.add(next);
                                return next;
                            }
                        };
                    }
                }
            });
        }
    }

    private static class Chain<T> implements Chainable<T> {
        protected Iterable<T> iterable;

        private Chain(Iterable<T> iterable) {
            this.iterable = (iterable != null) ? iterable : new ArrayList<>();
        }

        static <T> Chain<T> empty() {
            return Chain.from(Collections.emptyList());
        }

        static <T> Chain<T> from(Iterable<T> iterable) {
            if (iterable instanceof Chain<?>) {
                return (Chain<T>) iterable;
            } else {
                return new Chain<>(iterable);
            }
        }

        @SuppressWarnings("unchecked")
        static <T> Chain<T> from(T...items) {
            return Chain.from(new Iterable<T>() {

                @Override
                public Iterator<T> iterator() {
                    return new Iterator<T>() {
                        final T[] sourceItems = items;
                        int nextIndex = 0;
                        @Override
                        public boolean hasNext() {
                            return (this.sourceItems != null) ? (this.nextIndex < this.sourceItems.length) : false;
                        }

                        @Override
                        public T next() {
                            this.nextIndex++;
                            return this.sourceItems[this.nextIndex-1];
                        }
                    };
                }
            });
        }

        @Override
        public Iterator<T> iterator() {
            return this.iterable.iterator();
        }

        @Override
        public String toString() {
            return this.iterable.toString();
        }
    }
}

