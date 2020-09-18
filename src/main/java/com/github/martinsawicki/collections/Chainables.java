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
import java.util.Map;
import java.util.function.Function;
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

        /**
         * Determines whether this chain contains any items.
         * @return {@code true} if not empty (i.e. the opposite of {@link #isEmpty()})
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Any()}</td></tr>
         * </table>
         */
        default boolean any() {
            return !Chainables.isNullOrEmpty(this);
        }

        /**
         * Determines whether this chain contains any items.
         * @return {@code true} if empty, else {@code false}
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Any()}, but negated</td></tr>
         * </table>
         * @see #any()
         */
        default boolean isEmpty() {
            return Chainables.isNullOrEmpty(this);
        }

        /**
         * Counts the items in this chain.
         * <p>
         * This triggers a full traversal/evaluation of the items.
         * @return total number of items
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#count()}</td></tr>
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Count()}</td></tr>
         * </table>
         */
        default int size() {
            return Chainables.count(this);
        }

        /**
         * Transforms this chain into a list, tigerring a full evaluation.
         * @return a new list containing all the items
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.ToList()}</td></tr>
         * </table>
         */
        default List<T> toList() {
            return Chainables.toList(this);
        }

        /**
         * Transforms each item into another item, of a possibly different type, by applying the specified {@code transformer}
         * @param transformer
         * @return the resulting items from the transformation
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#map(Function)}</td></tr>
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Select()}</td></tr>
         * </table>
         * @see #transformAndFlatten(Function)
         */
        default <O> Chainable<O> transform(Function<T, O> transformer) {
            return Chainables.transform(this, transformer);
        }

        /**
         * Transforms each item into several other items, possibly of a different type, using the specified {@code transformer}.
         * @param transformer
         * @return the resulting items from the transformation
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#flatMap(Function)}</td></tr>
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.SelectMany()}</td></tr>
         * </table>
         * @see #transform(Function)
         */
        default <O> Chainable<O> transformAndFlatten(Function<T, Iterable<O>> transformer) {
            return Chainables.transformAndFlatten(this, transformer);
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

    /**
     * Determines whether the specified iterable contains at least one element.
     *
     * @param iterable the {@link java.lang.Iterable} to check
     * @return {@code true} if the specified {@code iterable} has at least one item
     * @see Chainable#any()
     */
    public static <V> boolean any(Iterable<V> iterable) {
        return !isNullOrEmpty(iterable);
    }

    /**
     * Counts the number of items, forcing a complete traversal.
     *
     * @param items an items to count
     * @return the number of items
     * @see Chainable#size()
     */
    public static <T> int count(Iterable<T> items) {
        if (items == null) {
            return 0;
        }

        if (items instanceof Collection<?>) {
            return ((Collection<?>)items).size();
        }

        Iterator<T> iter = items.iterator();
        int size = 0;
        while (iter.hasNext()) {
            iter.next();
            size++;
        }

        return size;
    }


    /**
     * @param iterable the {@link java.lang.Iterable} to check
     * @return {@code true} if the specified {@code iterable} is null or empty, else false
     */
    public static boolean isNullOrEmpty(Iterable<?> iterable) {
        return (iterable != null) ? !iterable.iterator().hasNext() : true;
    }

    /**
     * @param iterables
     * @return {@code true} if any of the specified {@code iterables} are null or empty
     */
    public static boolean isNullOrEmptyEither(Iterable<?>...iterables) {
        for (Iterable<?> iterable : iterables) {
            if (Chainables.isNullOrEmpty(iterable)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param map
     * @return {@code true} if the specified {@code map} is null or empty
     */
    public static <K, V> boolean isNullOrEmpty(Map<K,V> map) {
        return (map != null) ? map.isEmpty() : true;
    }

    /**
     * @param iterator
     * @return {@code true} if the specified {@code iterator} is null or empty
     */
    public static <V> boolean isNullOrEmpty(Iterator<V> iterator) {
        return (iterator != null) ? !iterator.hasNext() : true;
    }

    /**
     * Joins the items produced by the specified {@code iterator} into a single string, invoking {@code toString()) on each item,
     * separating each string with the specified {@code delimiter}, skipping {@code null} values.
     * @param delimiter the text to insert between items
     * @param iterator the iterator to traverse
     * @return the joined string
     */
    public static <T> String join(String delimiter, Iterator<T> iterator) {
        if (iterator == null) {
            return null;
        }

        StringBuilder info = new StringBuilder();
        while (iterator.hasNext()) {
            T next = iterator.next();
            if (next != null) {
                info
                    .append(next.toString())
                    .append(delimiter);

            }
        }

        if (info.length() > delimiter.length()) {
            info.setLength(info.length() - delimiter.length());
        }

        return info.toString();
    }

    /**
     * Joins the items in specified {@code stream} into a single string, applying a {@code toString()} to each item and separating them with the specified
     * {@code delimiter, skipping {@code null} values..
     * @param delimiter the text to insert between consecutive strings
     * @param stream the stream whose items are to be joined
     * @return the joined string
     */
    public static <T> String join(String delimiter, Stream<T> stream) {
        return (stream != null) ? Chainables.join(delimiter, stream.iterator()) : null;
    }

    /**
     * Joins the specified {@code items} into a single string, invoking {@code toString()}) on each, separating them with the specified {@code delimiter},
     * skipping {@code null} values.
     * @param delimiter the text to insert between the items
     * @param items the items to join
     * @return the joined string
     */
    public static <T> String join(String delimiter, Iterable<T> items) {
        return join(delimiter, items.iterator());
    }

    /**
     * @param items
     * @return
     * @see Chainable#toList()
     */
    public static <T> List<T> toList(Iterable<T> items) {
        if (items == null) {
            return null;
        } else if (items instanceof List<?>) {
            return (List<T>) items;
        } else {
            List<T> list = new ArrayList<>();
            for (T item : items) {
                list.add(item);
            }

            return list;
        }
    }

    /**
     * Uses the specified transformer function to transform the specified items and returns the resulting items.
     * @param items items to be transformed (LINQ: select())
     * @param transformer function performing the transformation
     * @return the transformed items
     * @see Chainable#transform(Function)
     */
    public static <I, O> Chainable<O> transform(Iterable<I> items, Function<I, O> transformer) {
        if (items == null || transformer == null) {
            return null;
        }

        // TODO: transform should perhaps ignore NULL?
        return Chainable.from(new Iterable<O>() {
            @Override
            public Iterator<O> iterator() {
                return new Iterator<O>() {
                    Iterator<I> iterator = items.iterator();

                    @Override
                    public boolean hasNext() {
                        return this.iterator.hasNext();
                    }

                    @Override
                    public O next() {
                        return (this.iterator.hasNext()) ?
                                transformer.apply(this.iterator.next()) : null;
                    }
                };
            }
        });
    }

    /**
     * @param items
     * @param transformer
     * @return
     * @see Chainable#transformAndFlatten(Function)
     */
    public static <I, O> Chainable<O> transformAndFlatten(Iterable<I> items, Function<I, Iterable<O>> transformer) {
        if (items == null || transformer == null) {
            return null;
        }

        return Chainable.from(new Iterable<O>() {
            @Override
            public Iterator<O> iterator() {
                return new Iterator<O>() {
                    private final Iterator<I> iterIn = items.iterator();
                    private Iterator<O> iterOut = null;
                    private boolean stopped = false;

                    @Override
                    public boolean hasNext() {
                        if (stopped) {
                            return false;
                        } else if (!Chainables.isNullOrEmpty(this.iterOut)) {
                            return true;
                        } else {
                            while (this.iterIn.hasNext()) {
                                I itemIn = this.iterIn.next();
                                Iterable<O> results = transformer.apply(itemIn);
                                if (!Chainables.isNullOrEmpty(results)) {
                                    this.iterOut = results.iterator();
                                    return true;
                                }
                            }

                            this.stopped = true;
                            return false;
                        }
                    }

                    @Override
                    public O next() {
                        return this.hasNext() ? this.iterOut.next() : null;
                    }
                };
            }
        });
    }
}

