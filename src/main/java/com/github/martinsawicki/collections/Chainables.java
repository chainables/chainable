/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.martinsawicki.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
         * Ensures all items are traversed, forcing any of the predecessors in the chain to be fully evaluated.
         * <p>This is somewhat similar to {@link #toList()}, except that what is returned is still a {@link Chainable}.
         * @return self
         */
        default Chainable<T> apply() {
            return Chainables.apply(this);
        }

        /**
         * Applies the specified {@code action} to all the items in this chain, triggering a full evaluation of all the items.
         * @param action
         * @return self
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#forEach(Consumer)}</td></tr>
         * </table>
         */
        default Chainable<T> apply(Consumer<T> action) {
            return Chainables.apply(this, action);
        }

        /**
         * Applies the specified {@code action} to each item one by one lazily, i.e. without triggering a full evaluation of the entire {@link Chainable},
         * but only to the extent that the returned {@link Chainable} is evaluated using another function.
         * @param action
         * @return self
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#peek(Consumer)}</td></tr>
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Select()}</td></tr>
         * </table>
         */
        default Chainable<T> applyAsYouGo(Consumer<T> action) {
            return Chainables.applyAsYouGo(this, action); // TODO: shouldn't this call applyAsYouGo?
        }

        /**
         * Appends the specified {@code items} to this chain.
         * @param items
         * @return the chain resulting from appending the specified {@code items} to this chain
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#concat(Stream, Stream)}, except that this is a chainable method that concatenates the specified {@code items}
         * to the {@link Chainable} it is invoked on)</td></tr>
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Concat()}</td></tr>
         * </table>
         * @see #concat(Object)
         */
        default Chainable<T> concat(Iterable<T> items) {
            return Chainables.concat(this, items);
        }

        /**
         * Appends the items from the specified {@code iterables} to this chain, in the order they are provided.
         * @param itemSequences
         * @return the current items with the specified {@code itemSequences} added to the end
         * @see #concat(Iterable)
         */
        @SuppressWarnings("unchecked")
        default Chainable<T> concat(Iterable<T>...iterables) {
            return this.concat(Chainables.concat(iterables));
        }

        /**
         * Appends the specified {@code item} to this chain.
         * @param item
         * @return the chain resulting from appending the specified single {@code item} to this chain
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Append()}</td></tr>
         * </table>
         * @see #concat(Iterable)
         */
        default Chainable<T> concat(T item) {
            return Chainables.concat(this, item);
        }

        /**
         * Appends the items produced by the specified {@code lister} applied to the last item in this chain.
         * @param lister
         * @return the resulting chain
         * @see #concat(Iterable)
         * @see #chain(Function)
         */
        default Chainable<T> concat(Function<T, Iterable<T>> lister) {
            return Chainables.concat(this, lister);
        }

        /**
         * Determines whether this chain contains the specified {@code item}.
         * @param item the item to look for
         * @return {@code true} if this contains the specified {@code item}
         * @sawicki.similar
         * <table summary="Similar to:">\
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Contains()}</td></tr>
         * </table>
         * @see #containsAll(Object...)
         * @see #containsAny(Object...)
         * @see #containsSubarray(Iterable)
         */
        default boolean contains(T item) {
            return Chainables.contains(this, item);
        }

        /**
         * Determines whether this chain contains all of the specified {@code items}.
         * @param items items to search for
         * @return {@code true} if this chain contains all the specified {@code items}
         * @see #contains(Object)
         * @see #containsAny(Object...)
         */
        @SuppressWarnings("unchecked")
        default boolean containsAll(T...items) {
            return Chainables.containsAll(this, items);
        }

        /**
         * Determines whether this chain contains any of the specified {@code items}.
         * @param items items to search for
         * @return {@code true} if this contains any of the specified {@code items}
         * @see #contains(Object)
         * @see #containsAll(Object...)
         */
        @SuppressWarnings("unchecked")
        default boolean containsAny(T...items) {
            return Chainables.containsAny(this, items);
        }

        /**
         * Determines whether this chain contains items in the specified {@code subarray} in that exact order.
         * @param subarray
         * @return true if this contains the specified {@code subarray} of items
         * (i.e. appearing consecutively at any point)
         * @see #contains(Object)
         * @see #containsAll(Object...)
         * @see #containsAny(Object...)
         */
        default boolean containsSubarray(Iterable<T> subarray) {
            return Chainables.containsSubarray(this, subarray);
        }

        /**
         * Returns a chain of items from this chain that are not duplicated.
         * @return items that are unique (no duplicates)
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#distinct()}</td></tr>
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Distinct()}</td></tr>
         * </table>
         */
        default Chainable<T> distinct() {
            return Chainables.distinct(this);
        }

        /**
         * Returns a chain of items from this chain without duplicate keys, as returned by the specified {@code keyExtractor}.
         * <P>
         * In case of duplicates, the first item survives.
         * @param keyExtractor
         * @return first items whose keys, as extracted by the specified {@code keyExtractor}, are unique
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Distinct()} with a custom comparer</td></tr>
         * </table>
         */
        default <V> Chainable<T> distinct(Function<T, V> keyExtractor) {
            return Chainables.distinct(this, keyExtractor);
        }

        /**
         * Determines whether this chain consists of the same items, in the same order, as those in the specified {@code items}, triggering a full traversal/evaluation of the chain if needed.
         * @param items
         * @return {@code true} the items match exactly
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.SequenceEqual()}</td></tr>
         * </table>
         * @see #equalsEither(Iterable...)
         */
        default boolean equals(Iterable<T> items) {
            return Chainables.equal(this, items);
        }

        /**
         * Determines whether this chain consists of the same items, in the same order, as in any of the specified {@code iterables}.
         * <p>
         * This triggers a full traversal/evaluation of the chain if needed.
         * @param iterables
         * @return true if the underlying items are the same as those in any of the specified {@code iterables}
         * in the same order
         * @see #equalsEither(Iterable...)
         */
        @SuppressWarnings("unchecked")
        default boolean equalsEither(Iterable<T>...iterables) {
            if (iterables == null) {
                return false;
            } else {
                for (Iterable<T> iterable : iterables) {
                    if (Chainables.equal(this, iterable)) {
                        return true;
                    }
                }
            }

            return false;
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
         * Returns a chain of initial items from this chain upto and including the fist item that satisfies the specified {@code condition}, and none after it.
         * <p>
         * For example, if the items are { 1, 3, 5, 2, 7, 9, ...} and the {@code condition} is true when the item is an even number, then the resulting chain
         * will consist of { 1, 3, 5, 2 }.
         * @param condition
         * @return the resulting items
         * @see #notBefore(Predicate)
         * @see #asLongAs(Predicate)
         * @see #notAsLongAs(Predicate)
         */
        default Chainable<T> notAfter(Predicate<T> condition) {
            return Chainables.notAfter(this, condition);
        }

        /**
         * Returns the items from this chain that do not satisy the specified {@code condition}.
         * @param condition
         * @return items that do not meet the specified {@code condition}
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#filter(Predicate)}, but with a negated predicate</td></tr>
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Where()}, but with a negated predicate</td></tr>
         * </table>
         * @see #where(Predicate)
         */
        default Chainable<T> notWhere(Predicate<T> condition) {
            return Chainables.notWhere(this, condition);
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
         * Creates a stream from this chain.
         * @return a stream representing this chain.
         */
        default Stream<T> stream() {
            return Chainables.toStream(this);
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

        /**
         * Returns a chain of items from this chain that satisfy the specified {@code condition}.
         * @param condition
         * @return matching items
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#filter(Predicate)}</td></tr>
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Where()}</td></tr>
         * </table>
         */
        default Chainable<T> where(Predicate<T> condition) {
            return Chainables.whereEither(this, condition);
        }

        /**
         * Returns a chain of items from this chain that satisfy any of the specified {@code conditions}.
         * @param conditions
         * @return items that meet any of the specified {@code conditions}
         * @see #where(Predicate)
         */
        @SuppressWarnings("unchecked")
        default Chainable<T> whereEither(Predicate<T>... conditions) {
            return Chainables.whereEither(this, conditions);
        }

        /**
         * Filters out {@code null} values from the underlying {@link Chainable}.
         * @return non-null items
         */
        default Chainable<T> withoutNull() {
            return Chainables.withoutNull(this);
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
     * @param items
     * @param action
     * @return
     * @see Chainable#apply(Consumer)
     */
    public static <T> Chainable<T> apply(Iterable<T> items, Consumer<T> action) {
        if (items == null) {
            return null;
        } else if (action == null) {
            return Chainable.from(items);
        }

        // Apply to all
        List<T> itemsList = Chainables.toList(items);
        for (T item : itemsList) {
            try {
                action.accept(item);
            } catch (Exception e) {
                // TODO What to do with exceptions
                // String s = e.getMessage();
            }
        }

        return Chainable.from(itemsList);
    }

    /**
     * @param items
     * @return
     * @see Chainable#apply()
     */
    public static <T> Chainable<T> apply(Iterable<T> items) {
        return apply(items, o -> {}); // NOP
    }

    /**
     * @param items
     * @param action
     * @return
     * @see Chainable#applyAsYouGo(Consumer)
     */
    public static <T> Chainable<T> applyAsYouGo(Iterable<T> items, Consumer<T> action) {
        if (items == null) {
            return null;
        } else if (action == null) {
            return Chainable.from(items);
        } else {
            return Chainable.from(new Iterable<T>() {
                @Override
                public Iterator<T> iterator() {
                    return new Iterator<T>() {
                        final private Iterator<T> itemIter = items.iterator();

                        @Override
                        public boolean hasNext() {
                            return this.itemIter.hasNext();
                        }

                        @Override
                        public T next() {
                            if (this.hasNext()) {
                                T item = this.itemIter.next();
                                action.accept(item);
                                return item;
                            } else {
                                return null;
                            }
                        }
                    };
                }
            });
        }
    }

    /**
     * @param items
     * @param lister
     * @return
     * @see Chainable#concat(Function)
     */
    public static <T> Chainable<T> concat(Iterable<T> items, Function<T, Iterable<T>> lister) {
        if (lister == null) {
            return Chainable.from(items);
        } else if (items == null) {
            return null;
        }

        return Chainable.from(new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {

                return new Iterator<T>() {
                    private final Iterator<T> iter1 = items.iterator();
                    private Iterator<T> iter2 = null;

                    @Override
                    public boolean hasNext() {
                        return this.iter1.hasNext() || !Chainables.isNullOrEmpty(this.iter2);
                    }

                    @Override
                    public T next() {
                        if (!this.hasNext()) {
                            return null;
                        } else if (Chainables.isNullOrEmpty(this.iter2)) {
                            T item = this.iter1.next();
                            Iterable<T> items2 = lister.apply(item);
                            this.iter2 = (Chainables.isNullOrEmpty(items2)) ? null : items2.iterator();
                            return item;
                        } else {
                            return this.iter2.next();
                        }
                    }
                };
            }
        });
    }

    /**
     * Concatenates the two iterables, by first iterating through the first iterable
     * and the through the second.
     * @param items1 the first iterable
     * @param items2 the second iterable
     * @return concatenated iterable
     */
    // TODO Should this be removed now that concat(...) exists?
    public static <T> Chainable<T> concat(Iterable<T> items1, Iterable<T> items2) {
        if (items1 == null && items2 == null) {
            return null;
        } else if (Chainables.isNullOrEmpty(items1)) {
            return Chainable.from(items2);
        } else if (Chainables.isNullOrEmpty(items2)) {
            return Chainable.from(items1);
        } else {
            return Chainable.from(new Iterable<T>() {

                @Override
                public Iterator<T> iterator() {
                    return new Iterator<T>() {
                        private final Iterator<T> iter1 = items1.iterator();
                        private final Iterator<T> iter2 = items2.iterator();

                        @Override
                        public boolean hasNext() {
                            return this.iter1.hasNext() || this.iter2.hasNext();
                        }

                        @Override
                        public T next() {
                            if (this.iter1.hasNext()) {
                                return this.iter1.next();
                            } else if (this.iter2.hasNext()) {
                                return this.iter2.next();
                            } else {
                                return null;
                            }
                        }
                    };
                }
            });
        }
    }

    /**
     * Concatenates the specified iterable with the specified single item.
     *
     * @param items
     *            the iterable to concatenate the single item with
     * @param item
     *            the item to concatenate
     * @return the resulting concatenation
     */
    public static <T> Chainable<T> concat(Iterable<T> items, T item) {
        return concat(items, (Iterable<T>) Arrays.asList(item));
    }

    /**
     * @param itemSequences
     * @return
     * @see Chainable#concat(Iterable...)
     */
    @SafeVarargs
    public static <T> Chainable<T> concat(Iterable<T>...itemSequences) {
        if (Chainables.isNullOrEmpty(itemSequences)) {
            return Chainable.empty();
        } else {
            return Chainable.from(new Iterable<T>() {
                @Override
                public Iterator<T> iterator() {
                    return new Iterator<T>() {
                        private int i = 0;
                        private Iterator<T> curIter = null;

                        @Override
                        public boolean hasNext() {
                            // Get the next non-empty iterator
                            while (Chainables.isNullOrEmpty(this.curIter) && i < itemSequences.length) {
                                this.curIter = (itemSequences[i] != null) ? itemSequences[i].iterator() : null;
                                i++;
                            }

                            return (this.curIter != null) ? this.curIter.hasNext() : false;
                        }

                        @Override
                        public T next() {
                            return (this.hasNext()) ? this.curIter.next() : null;
                        }
                    };
                }
            });
        }
    }

    /**
     * @param item
     * @param items
     * @return
     * @see Chainable#concat(Iterable)
     */
    public static <T> Chainable<T> concat(T item, Iterable<T> items) {
        return concat((Iterable<T>) Arrays.asList(item), items);
    }

    /**
     * @param container
     * @param item
     * @return true if the specified {@code item} is among the members of the specified {@code container}, else false
     */
    public static <T> boolean contains(Iterable<T> container, T item) {
        if (container == null) {
            return false;
        } else if (!(container instanceof Set<?>)) {
            return !Chainables.isNullOrEmpty(Chainables.whereEither(container, i -> i.equals(item)));
        } else if (item == null) {
            return false;
        } else {
            return ((Set<?>) container).contains(item);
        }
    }

    /**
     * @param container
     * @param items
     * @return
     * @see Chainable#containsAll(Object...)
     */
    @SafeVarargs
    public static <T> boolean containsAll(T[] container, T...items) {
        return containsAll(Chainable.from(container), items);
    }

    /**
     * @param container
     * @param items
     * @return
     * @see Chainable#containsAll(Object...)
     */
    @SafeVarargs
    public static <T> boolean containsAll(Iterable<T> container, T...items) {
        Set<T> searchSet = new HashSet<>(Arrays.asList(items));
        if (container == null) {
            return false;
        } else if (items == null) {
            return true;
        } else if (container instanceof Set<?>) {
            // Fast path for wrapped sets
            return ((Set<T>) container).containsAll(searchSet);
        } else {
            for (T item : container) {
                searchSet.remove(item);
                if (searchSet.isEmpty()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @param container
     * @param items
     * @return true if any of the specified {@code items} are among the members of the specified {@code container}
     * @see Chainable#containsAny(Object...)
     */
    @SafeVarargs
    public static <T> boolean containsAny(Iterable<T> container, T...items) {
        if (container == null) {
            return false;
        } else if (items == null) {
            return true;
        }

        for (T item : items) {
            if (Chainables.contains(container, item)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param container
     * @param items
     * @return
     * @see Chainable#containsAny(Object...)
     */
    @SafeVarargs
    public static <T> boolean containsAny(T[] container, T...items) {
        return containsAny(Chainable.from(container), items);
    }

    /**
     * @param items
     * @param subarray
     * @return
     * @see Chainable#containsSubarray(Iterable)
     */
    public static <T> boolean containsSubarray(Iterable<T> items, Iterable<T> subarray) {
        if (items == null) {
            return false;
        } else if (Chainables.isNullOrEmpty(subarray)) {
            return true;
        }

        // Brute force evaluation of everything (TODO: make it lazy and faster?)
        List<T> subList = Chainables.toList(subarray);
        List<T> itemsCached = Chainables.toList(items);

        for (int i = 0; i < itemsCached.size() - subList.size(); i++) {
            boolean matched = true;
            for (int j = 0; j < subList.size(); j++) {
                if (!Objects.equals(itemsCached.get(i+j), subList.get(j))) {
                    matched = false;
                    break;
                }
            }

            if (matched) {
                return true;
            }
        }

        return false;
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
     * @param items
     * @param keyExtractor
     * @return
     * @see Chainable#distinct(Function)
     */
    public static <T, V> Chainable<T> distinct(Iterable<T> items, Function<T, V> keyExtractor) {
        return (keyExtractor == null) ? distinct(items) : Chainable.from(new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    final Map<V, T> seen = new HashMap<>();
                    final Iterator<T> iter = items.iterator();
                    T next = null;
                    V value = null;
                    boolean hasNext = false;

                    @Override
                    public boolean hasNext() {
                        if (this.hasNext) {
                            return true;
                        }

                        while (this.iter.hasNext()) {
                            this.next = this.iter.next();
                            this.value = keyExtractor.apply(this.next);
                            if (!seen.containsKey(this.value)) {
                                this.hasNext = true;
                                return true;
                            }
                        }

                        return this.hasNext = false;
                    }

                    @Override
                    public T next() {
                        if (this.hasNext()) {
                            this.seen.put(this.value, this.next);
                            this.hasNext = false;
                            return this.next;
                        } else {
                            return null;
                        }
                    }
                };
            }});
    }

    /**
     * @param items
     * @return
     * @see Chainable#distinct()
     */
    public static <T> Chainable<T> distinct(Iterable<T> items) {
        return Chainable.from(new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    final Set<T> seen = new HashSet<>();
                    final Iterator<T> iter = items.iterator();
                    T next = null;

                    @Override
                    public boolean hasNext() {
                        if (this.next != null) {
                            return true;
                        }

                        while (this.iter.hasNext()) {
                            this.next = this.iter.next();
                            if (seen.contains(this.next)) {
                                this.next = null;
                            } else {
                                return true;
                            }
                        }

                        this.next = null;
                        return false;
                    }

                    @Override
                    public T next() {
                        if (!this.hasNext()) {
                            return null;
                        } else if (this.next != null) {
                            T item = this.next;
                            this.seen.add(item);
                            this.next = null;
                            return item;
                        } else {
                            return null;
                        }
                    }
                };
            }
        });
    }

    /**
     * @param items1
     * @param items2
     * @return
     * @see Chainable#equals(Iterable)
     */
    public static <T> boolean equal(Iterable<T> items1, Iterable<T> items2) {
        if (items1 == items2) {
            return true;
        } else if (items1 == null || items2 == null) {
            return false;
        } else {
            Iterator<T> iterator1 = items1.iterator();
            Iterator<T> iterator2 = items2.iterator();
            while (iterator1.hasNext() && iterator2.hasNext()) {
                if (!iterator1.next().equals(iterator2.next())) {
                    return false;
                }
            }

            if (iterator1.hasNext() || iterator2.hasNext()) {
                // One is longer than the other
                return false;
            } else {
                return true;
            }
        }
    }

    /**
     * Determines whether the specified array is empty or null.
     * @param array the array to check
     * @return {@code true} if the specified array is null or empty, else {@code false}
     */
    public static boolean isNullOrEmpty(Object[] array) {
        return (array != null) ? array.length == 0 : true;
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
     * Returns items until and including the first item satisfying the specified condition, and no items after that
     * @param items items to return from
     * @param condition the condition that the last item needs to meet
     * @return items before and including the first item where the specified condition is satisfied
     * @see Chainable#notAfter(Predicate)
     */
    public static <T> Chainable<T> notAfter(Iterable<T> items, Predicate<T> condition) {
        if (items == null) {
            return null;
        } else if (condition == null) {
            return Chainable.from(items);
        }

        return Chainable.from(new Iterable<T>() {

            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    private final Iterator<T> iterator = items.iterator();
                    private T nextItem = null;
                    boolean stopped = false;

                    @Override
                    public boolean hasNext() {
                        if (this.stopped) {
                            // Last item if any
                            return this.nextItem != null;
                        } else if (this.nextItem != null) {
                            return true;
                        } else if (!this.iterator.hasNext()) {
                            this.stopped = true;
                            this.nextItem = null;
                            return false;
                        } else {
                            this.nextItem = this.iterator.next();
                            if (condition.test(this.nextItem)) {
                                this.stopped = true;
                            }

                            return true;
                        }
                    }

                    @Override
                    public T next() {
                        if (this.hasNext()) {
                            T item = this.nextItem;
                            this.nextItem = null;
                            return item;
                        } else {
                            return null;
                        }
                    }
                };
            }
        });
    }

    /**
     * @param items
     * @param condition
     * @return
     * @see Chainable#notWhere(Predicate)
     */
    public static final <T> Chainable<T> notWhere(Iterable<T> items, Predicate<T> condition) {
        return (condition != null) ? Chainables.whereEither(items, condition.negate()) : Chainable.from(items);
    }

    /**
     * @param items
     * @return
     */
    public static String[] toArray(Iterable<String> items) {
        int len;
        if (items == null) {
            len = 0;
        } else {
            len = count(items);
        }

        String[] array = new String[len];
        int i = 0;
        for (String item : items) {
            array[i++] = item;
        }

        return array;
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
     * Converts the specified {@code items} into a sequential stream.
     * @param items the items to convert into a stream
     * @return the resulting stream
     * @see Chainable#stream()
     */
    public static <T> Stream<T> toStream(Iterable<T> items) {
        return StreamSupport.stream(items.spliterator(), false);
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

    /**
     * @param items
     * @param predicates
     * @return
     * @see Chainable#whereEither(Predicate...)
     */
    @SafeVarargs
    public static final <T> Chainable<T> whereEither(Iterable<T> items, Predicate<T>... predicates) {
        if (items == null) {
            return null;
        } else if (predicates == null || predicates.length == 0) {
            return Chainable.from(items);
        }

        return Chainable.from(new Iterable<T>() {

            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    final Iterator<T> innerIterator = items.iterator();
                    T nextItem = null;
                    boolean stopped = false;

                    @Override
                    public boolean hasNext() {
                        if (this.stopped) {
                            return false;
                        } else if (this.nextItem != null) {
                            return true;
                        }

                        while (this.innerIterator.hasNext()) {
                            this.nextItem = this.innerIterator.next();

                            // Skip over null items TODO: really?
                            if (this.nextItem == null) {
                                continue;
                            }

                            for (Predicate<T> predicate : predicates) {
                                if (predicate.test(this.nextItem)) {
                                    return true;
                                }
                            }
                        }

                        this.nextItem = null;
                        this.stopped = true;
                        return false;
                    }

                    @Override
                    public T next() {
                        if (this.hasNext()) {
                            T item = this.nextItem;
                            this.nextItem = null;
                            return item;
                        } else {
                            return null;
                        }
                    }
                };
            }
        });
    }

    /**
     * @param items
     * @return
     * @see Chainable#withoutNull()
     */
    public static <T> Chainable<T> withoutNull(Iterable<T> items) {
        return (items != null) ? Chainable.from(items).where(i -> i != null) : null;
    }
}

