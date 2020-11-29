/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.chainables.chainable;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.github.chainables.annotation.Experimental;
import com.github.chainables.function.ToStringFunction;

/**
 * This is the source of all the static methods underlying the default implementation of {@link Chainable} as well as some other conveniences.
 * @author Martin Sawicki
 *
 */
public final class Chainables {
    private Chainables() {
        throw new AssertionError("Not instantiable, just stick to the static methods.");
    }

    static class CachedChain<T> extends Chain<T> {
        List<T> cache = null;

        @SuppressWarnings("unchecked")
        static <T> Chain<T> from(Iterable<? extends T> iterable) {
            if (iterable instanceof CachedChain<?>) {
                return (CachedChain<T>) iterable;
            } else {
                return new CachedChain<>(iterable);
            }
        }

        private CachedChain(Iterable<? extends T> iterable) {
            super(iterable);
        }

        @Override
        public T get(long index) {
            return (this.cache != null) ? this.cache.get(Math.toIntExact(index)) : super.get(index);
        }

        @Override
        public long count() {
            return (this.cache != null) ? this.cache.size() : Chainables.count(this);
        }

        @Override
        public Iterator<T> iterator() {
            if (this.cache != null) {
                // Cache already filled so return from it
                return this.cache.iterator();
            } else {
                return new Iterator<T>() {
                    Iterator<? extends T> iter = iterable.iterator();
                    List<T> tempCache = new ArrayList<>();

                    @Override
                    public boolean hasNext() {
                        if (iter.hasNext()) {
                            return true;
                        } else if (cache == null) {
                            // The first iterator to fill the cache wins
                            cache = tempCache;
                        }

                        return false;
                    }

                    @Override
                    public T next() {
                        T next = iter.next();
                        tempCache.add(next);
                        return next;
                    }
                };
            }
        }
    }

    static class Chain<T> implements Chainable<T> {
        protected Iterable<? extends T> iterable;

        private Chain(Iterable<? extends T> iterable) {
            this.iterable = (iterable != null) ? iterable : new ArrayList<>();
        }

        static <T> Chain<T> empty() {
            return Chain.from(new ArrayList<>());
        }

        @SuppressWarnings("unchecked")
        static <T> Chain<T> from(Iterable<? extends T> iterable) {
            if (iterable == null) {
                return Chain.empty();
            } else if (iterable instanceof Chain<?>) {
                return (Chain<T>) iterable;
            } else if (iterable instanceof CachedChain<?>) {
                return (CachedChain<T>) iterable;
            } else {
                return new Chain<T>(iterable);
            }
        }

        static <T> Chain<T> from(Supplier<Iterator<T>> iteratorSupplier) {
            return (iteratorSupplier == null) ? Chain.empty() : Chain.from(new Iterable<T>() {
                @Override
                public Iterator<T> iterator() {
                    return iteratorSupplier.get();
                }
            });
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

        @SuppressWarnings("unchecked")
        @Override
        public Iterator<T> iterator() {
            return (Iterator<T>) this.iterable.iterator();
        }

        @Override
        public String toString() {
            return Chainables.join(", ", this);
        }

        @Override
        public T get(long index) {
            return (this.iterable instanceof List<?>) ? ((List<? extends T>) this.iterable).get(Math.toIntExact(index)) : Chainables.get(this, index);
        }
    }

    private static class ChainableQueueImpl<T> extends Chain<T> implements ChainableQueue<T> {

        final Deque<T> queue = new LinkedList<>();
        Iterable<? extends T> originalIterable;
        final Iterator<? extends T> initialIter;

        private ChainableQueueImpl(Iterable<? extends T> iterable) {
            // Specified iterable becomes initial head, but joined with actual FIFO queue
            super(null);
            this.originalIterable = iterable;
            this.iterable = iterable;
            this.initialIter = iterable.iterator();
        }

        @Override
        public T removeFirst() {
            if (this.originalIterable == null) {
                // No more original iterable, so just queue left
                return this.queue.removeFirst();
            } else if (!this.initialIter.hasNext()) {
                // Empty original iterable, just queue left, so eliminate the original iterable altogether
                this.iterable = this.queue;
                this.originalIterable = null;
                return this.queue.removeFirst();
            } else {
                // Original iterable still around, so fetch from it and adjust the merger
                T first = this.initialIter.next();
                this.originalIterable = Chainables.afterFirst(this.originalIterable); // Makes traversal increasingly long, but ChainableQueue should not really be traversed like this if the initial iterable is very large...
                this.iterable = Chainables.concat(this.originalIterable, this.queue);
                return first;
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public ChainableQueueImpl<T> withLast(T...items) {
            if (items.length == 1 ) {
                this.queue.addLast(items[0]);
            } else {
                this.queue.addAll(Arrays.asList(items));
            }

            if (!isNullOrEmpty(this.originalIterable)) {
                this.iterable = Chainables.concat(this.originalIterable, this.queue);
            }

            return this;
        }

        @Override
        public ChainableQueue<T> withLast(Iterable<T> items) {
            this.queue.addAll(Chainable.from(items).toList());
            return this;
        }
    }

    /**
     * @param items
     * @return the chain of remaining items after the first of the specified {@code items}
     * @see Chainable#afterFirst()
     */
    public static <V> Chainable<V> afterFirst(Iterable<? extends V> items) {
        return afterFirst(items, 1);
    }

    /**
     * @param items
     * @param number
     * @return the chain of remaining items after the first {@code number} of them in the specified {@code items}
     */
    public static <V> Chainable<V> afterFirst(Iterable<? extends V> items, long number) {
        return (items == null) ? Chainable.empty() : Chainable.fromIterator(() -> new Iterator<V>() {
            final Iterator<? extends V> iter = items.iterator();
            long skippedNum = 0;

            @Override
            public boolean hasNext() {
                if (!this.iter.hasNext()) {
                    return false;
                } else if (this.skippedNum >= number) {
                    return this.iter.hasNext();
                } else {
                    while (skippedNum < number && this.iter.hasNext()) {
                        this.iter.next(); // Skip the next item
                        this.skippedNum++;
                    }

                    return this.skippedNum >= number && this.iter.hasNext();
                }
            }

            @Override
            public V next() {
                return this.iter.next();
            }
        });
    }

    /**
     * @param items
     * @param condition
     * @return {@code true} iff all of the specified {@code items} satisfy the specified {@code condition}
     * @see Chainable#allWhere(Predicate)
     */
    public static <T> boolean allWhere(Iterable<? extends T> items, Predicate<? super T> condition) {
        return allWhereEither(items, condition);
    }

    /**
     * @param items
     * @param conditions
     * @return {@code true} iff all of the specified {@code items} satisfy any of the specified {@code conditions}
     * @see Chainable#allWhereEither(Predicate...)
     */
    @SafeVarargs
    public static <T> boolean allWhereEither(Iterable<? extends T> items, Predicate<? super T>... conditions) {
        if (items == null) {
            return false;
        } else {
            Chainable<Predicate<? super T>> conds = Chainable.from(conditions);
            return noneWhere(items, i -> !conds.anyWhere(c -> c.test(i)));
        }
    }

    /**
     * Determines whether the specified iterable contains at least one element.
     *
     * @param items the items to check
     * @return {@code true} iff the specified {@code items} contain at least one member
     * @see Chainable#any()
     */
    public static <V> boolean any(Iterable<? extends V> items) {
        return !isNullOrEmpty(items);
    }

    /**
     * @param items
     * @param condition
     * @return {@code true} iff the specified {@code items} contain at least one member that satisfies the specified {@code condition}
     * @see Chainable#anyWhere(Predicate)
     */
    public static <T> boolean anyWhere(Iterable<? extends T> items, Predicate<? super T> condition) {
        return anyWhereEither(items, condition);
    }

    /**
     * @param items
     * @param conditions
     * @return {@code true} iff the specified {@code items} contain at least one member that satisfies any of the specified {@code conditions}
     * @see Chainable#anyWhereEither(Predicate...)
     */
    @SafeVarargs
    public static <T> boolean anyWhereEither(Iterable<? extends T> items, Predicate<? super T>...conditions) {
        if (conditions == null) {
            return true;
        } else {
            for (Predicate<? super T> condition : conditions) {
                if (Chainables.firstWhereEither(items, (T)null, condition) != null) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * @param items
     * @param action
     * @return the resulting chain after applying the specified {@code action} to all of the specified {@code items}, fully traversing and evaluating them
     * @see Chainable#apply(Consumer)
     */
    public static <T> Chainable<T> apply(Iterable<? extends T> items, Consumer<? super T> action) {
        if (items == null) {
            return null;
        } else if (action == null) {
            return Chainable.from(items);
        }

        // Apply to all
        List<? extends T> itemsList = Chainables.toList(items);
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
     * @return the resulting chain from fully traversing and evaluating all of the members of specified {@code items}
     * @see Chainable#apply()
     */
    public static <T> Chainable<T> apply(Iterable<? extends T> items) {
        return apply(items, o -> {}); // NOP
    }

    /**
     * @param items
     * @param action
     * @return the chain resulting from lazily applying the specified {@code action} to each of the specified {@code items}
     * @see Chainable#applyAsYouGo(Consumer)
     */
    public static <T> Chainable<T> applyAsYouGo(Iterable<? extends T> items, Consumer<? super T> action) {
        if (items == null) {
            return null;
        } else if (action == null) {
            return Chainable.from(items);
        } else {
            return Chainable.fromIterator(() -> new Iterator<T>() {
                final private Iterator<? extends T> itemIter = items.iterator();

                @Override
                public boolean hasNext() {
                    return this.itemIter.hasNext();
                }

                @Override
                public T next() {
                    T item = this.itemIter.next();
                    action.accept(item);
                    return item;
                }
            });
        }
    }

    /**
     * @param items items to sort
     * @return a chain of the specified {@code items} fully traversed, evaluated and sorted in the ascending order based on their default comparator
     * @see Chainable#ascending()
     */
    public static <T> Chainable<T> ascending(Iterable<? extends T> items) {
        return sorted(items, true);
    }

    /**
     * @param items
     * @param keyExtractor
     * @return a chain of the specified {@code items} fully traversed, evaluated and sorted in the ascending order based on the output of the specified
     * {@link java.lang.String}-returning {@code keyExtractor} applied to each item
     * @see Chainable#ascending(ToStringFunction)
     */
    public static <T> Chainable<T> ascending(Iterable<? extends T> items, ToStringFunction<? super T> keyExtractor) {
        return sortedBy(items, keyExtractor, true);
    }

    /**
     * @param items
     * @param keyExtractor
     * @return a chain of the specified {@code items} fully traversed, evaluated and sorted in the ascending order based on the output of the specified
     * {@link java.lang.Long}-returning {@code keyExtractor} applied to each item
     * @see Chainable#ascending(ToLongFunction)
     */
    public static <T> Chainable<T> ascending(Iterable<? extends T> items, ToLongFunction<? super T> keyExtractor) {
        return sortedBy(items, keyExtractor, true);
    }

    /**
     * @param items
     * @param keyExtractor
     * @return a chain of the specified {@code items} fully traversed, evaluated and sorted in the ascending order based on the output of the specified
     * {@link java.lang.Double}-returning {@code keyExtractor} applied to each item
     * @see Chainable#ascending(ToDoubleFunction)
     */
    public static <T> Chainable<T> ascending(Iterable<? extends T> items, ToDoubleFunction<? super T> keyExtractor) {
        return sortedBy(items, keyExtractor, true);
    }

    /**
     * Returns items up to the last one that still satisfies the specified {@code condition}.
     * @param items items to evaluate
     * @param condition the condition for the returned initial chain of items to satisfy
     * @return a chain of items up to the last one that still satisfies the specified {@code condition}
     */
    public static <T> Chainable<T> asLongAs(Iterable<? extends T> items, Predicate<? super T> condition) {
        return (condition == null) ? Chainable.from(items) : before(items, condition.negate());
    }

    /**
     * Returns items up to the last one that is equal to the specified item.
     * @param items items to return from
     * @param item the item that the initial chain of returned items must be equal to
     * @return chain of initial items up to the last one that are equal to the specified item
     */
    public static <T> Chainable<T> asLongAsEquals(Iterable<? extends T> items, T item) {
        return asLongAs(items, o -> o == item);
    }

    /**
     * Returns items before the first item satisfying the specified condition is encountered.
     * @param items items to return from
     * @param condition the condition that stops further items from being returned
     * @return chain of items before the specified condition is satisfied
     * @see Chainable#before(Predicate)
     */
    public static <T> Chainable<T> before(Iterable<? extends T> items, Predicate<? super T> condition) {
        if (items == null) {
            return null;
        } else if (condition == null) {
            return Chainable.from(items);
        }

        return Chainable.fromIterator(() -> new Iterator<T>() {
            private final Iterator<? extends T> iterator = items.iterator();
            private T nextItem = null;
            boolean stopped = false;

            @Override
            public boolean hasNext() {
                if (this.stopped) {
                    return false;
                } else if (this.nextItem != null) {
                    return true;
                } else if (!this.iterator.hasNext()) {
                    return false;
                } else {
                    this.nextItem = this.iterator.next();
                    if (condition.test(this.nextItem)) {
                        this.stopped = true;
                        return false;
                    } else {
                        return true;
                    }
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
        });
    }

    /**
     * Returns items until the specified item is encountered.
     * @param items items to return from
     * @param item the item which, when encountered, will stop the rest of the items from being returned
     * @return chain of items before the specified item is encountered
     * @see Chainable#beforeValue(Object)
     */
    public static <T> Chainable<T> beforeValue(Iterable<? extends T> items, T item) {
        return before(items, o -> o==item);
    }

    /**
     * @param items
     * @param childTraverser
     * @return chain of items based on the specified {@code items} in the order of breadth-first traversal, where the specified {@code childTraverser} returns the
     * next set of items to explore downstream from each item (that is, analogous to children of a tree node)
     * @see Chainable#breadthFirst(Function)
     */
    public static <T> Chainable<T> breadthFirst(Iterable<? extends T> items, Function<? super T, Iterable<? extends T>> childTraverser) {
        return traverse(items, childTraverser, true);
    }

    /**
     * @param items
     * @param childTraverser
     * @param condition
     * @return chain of items based on the specified {@code items} in the order of breadth-first traversal, where the specified {@code childTraverser} returns the
     * next set of items to explore downstream from each item (that is, analogous to children of a tree node), but not below items that satisfy the
     * specified {@code condition}
     * @see Chainable#breadthFirstNotBelow(Function, Predicate)
     */
    public static <T> Chainable<T> breadthFirstNotBelow(
            Iterable<? extends T> items,
            Function<? super T, Iterable<? extends T>> childTraverser,
            Predicate<? super T> condition) {
        return notBelow(items, childTraverser, condition, true);
    }

    /**
     * @param items
     * @param childTraverser
     * @param condition
     * @return chain of items based on the specified {@code items} in the order of breadth-first traversal, where the specified {@code childTraverser} returns the
     * next set of items to explore downstream from each item (that is, analogous to children of a tree node), as long as that item satisfies the
     * specified {@code condition}
     * @see Chainable#breadthFirstAsLongAs(Function, Predicate)
     */
    public static <T> Chainable<T> breadthFirstAsLongAs(
            Iterable<? extends T> items,
            Function<? super T, Iterable<T>> childTraverser,
            Predicate<? super T> condition) {
        final Predicate<? super T> appliedCondition = (condition != null) ? condition : (o -> true);
        return breadthFirst(items, o -> whereEither(childTraverser.apply(o), c -> Boolean.TRUE.equals(appliedCondition.test(c))));
    }

    /**
     * @param items
     * @return a chain which - instead of re-evaluating all the items upon each re-traversal - will cache the values obtained during the first complete traversal
     * and return those, rather than re-evaluating during subsequent traversals, de-facto functioning as a list thereon
     * @see Chainable#cached()
     */
    public static <T> Chainable<T> cached(Iterable<? extends T> items) {
        return (items == null) ? Chainable.empty() : CachedChain.from(items);
    }

    /**
     * @param items
     * @param clazz
     * @return a chain based on the specified {@code items} consisting of those items cast to the specified type ({@code clazz})
     * @see Chainable#cast(Class)
     */
    public static <T1, T2> Chainable<T2> cast(Iterable<? extends T1> items, Class<T2> clazz) {
        return (items == null || clazz == null) ? Chainable.from() : transform(items, o -> clazz.cast(o));
    }

    /**
     * @param item
     * @param nextItemExtractor
     * @return a chain starting with the specified {@code item} and each consecutive item as generated by the specified {@code nextItemExtractor} applied to
     * the previous item in the chain as its input parameter
     * @see Chainable#chain(UnaryOperator)
     */
    public static <T> Chainable<? super T> chain(T item, UnaryOperator<? super T> nextItemExtractor) {
        return chain(Chainable.from(item), nextItemExtractor);
    }

    /**
     * @param item
     * @param nextItemExtractor
     * @return a chain starting with the specified {@code item} and each consecutive item as generated by the specified {@code nextItemExtractor} applied to
     * the previous item in the chain and the current numerical index as its input parameters
     * @see Chainable#chainIndexed(BiFunction)
     */
    public static <T> Chainable<T> chainIndexed(T item, BiFunction<? super T, Long, T> nextItemExtractor) {
        return chainIndexed(Chainable.from(item), nextItemExtractor);
    }

    /**
     * @param items items to start the chain with
     * @param nextItemExtractor
     * @return a chain consisting of the specified {@code items}, followed by items generated by the specified {@code nextItemExtractor} applied to
     * the last two items as its input parameters
     * @see Chainable#chain(BinaryOperator)
     */
    public static <T> Chainable<T> chain(Iterable<? extends T> items, BinaryOperator<T> nextItemExtractor) {
        return (items == null || nextItemExtractor == null) ? Chainable.from(items) : Chainable.fromIterator(() -> new Iterator<T>() {
            Iterator<? extends T> iter = items.iterator();
            T next = null;
            T prev = null;
            boolean isFetched = false; // If iter is empty, pretend it starts with null
            boolean isStopped = false;

            @Override
            public boolean hasNext() {
                if (isStopped) {
                    return false;
                } else if (isFetched) {
                    return true;
                } else if (isNullOrEmpty(this.iter)) {
                    // Seed iterator already finished so start the chaining
                    this.iter = null;
                    T temp = this.next;
                    this.next = nextItemExtractor.apply(this.prev, this.next);
                    this.prev = temp;
                    if (this.next == null) {
                        isStopped = true;
                        isFetched = false;
                        return false;
                    } else {
                        isFetched = true;
                        return true;
                    }
                } else {
                    this.prev = this.next;
                    this.next = iter.next();
                    isFetched = true;
                    return true;
                }
            }

            @Override
            public T next() {
                T temp = this.next;
                isFetched = false;
                return temp;
            }
        });
    }

    /**
     * @param items items to start the chain with
     * @param nextItemExtractor
     * @return a chain consisting of the specified {@code items}, followed by items generated by the specified {@code nextItemExtractor} applied to
     * the last item and the numerical index of the item to be generated as its arguments
     * @see Chainable#chainIndexed(BiFunction)
     */
    public static <T> Chainable<T> chainIndexed(Iterable<? extends T> items, BiFunction<? super T, Long, T> nextItemExtractor) {
        return (items == null || nextItemExtractor == null) ? Chainable.from(items) : Chainable.fromIterator(() -> new Iterator<T>() {
            Iterator<? extends T> iter = items.iterator();
            T next = null;
            boolean isFetched = false; // If iter is empty, pretend it starts with null
            boolean isStopped = false;
            long index = 0;

            @Override
            public boolean hasNext() {
                if (isStopped) {
                    return false;
                } else if (isFetched) {
                    return true;
                } else if (isNullOrEmpty(this.iter)) {
                    // Seed iterator already finished so start the chaining
                    this.iter = null;
                    this.next = nextItemExtractor.apply(this.next, index);
                    if (this.next == null) {
                        isStopped = true;
                        isFetched = false;
                        return false;
                    } else {
                        isFetched = true;
                        return true;
                    }
                } else {
                    this.next = iter.next();
                    isFetched = true;
                    return true;
                }
            }

            @Override
            public T next() {
                T temp = this.next;
                isFetched = false;
                index++;
                return temp;
            }
        });
    }

    /**
     * @param items
     * @param nextItemExtractor
     * @return a chain consisting of the specified {@code items}, followed by items generated by the specified {@code nextItemExtractor} applied to
     * the last item as its argument (or null if no preceding items)
     * @see Chainable#chain(UnaryOperator)
     */
    public static <T> Chainable<T> chain(Iterable<? extends T> items, UnaryOperator<T> nextItemExtractor) {
        return (items == null || nextItemExtractor == null) ? Chainable.from(items) : Chainable.fromIterator(() -> new Iterator<T>() {
            Iterator<? extends T> iter = items.iterator();
            T next = null;
            boolean isFetched = false; // If iter is empty, pretend it starts with null
            boolean isStopped = false;

            @Override
            public boolean hasNext() {
                if (isStopped) {
                    return false;
                } else if (isFetched) {
                    return true;
                } else if (isNullOrEmpty(this.iter)) {
                    // Seed iterator already finished so start the chaining
                    this.iter = null;
                    this.next = nextItemExtractor.apply(this.next);
                    if (this.next == null) {
                        isStopped = true;
                        isFetched = false;
                        return false;
                    } else {
                        isFetched = true;
                        return true;
                    }
                } else {
                    this.next = iter.next();
                    isFetched = true;
                    return true;
                }
            }

            @Override
            public T next() {
                T temp = this.next;
                isFetched = false;
                return temp;
            }
        });
    }

    /**
     * @param items
     * @param condition
     * @param nextItemExtractor
     * @return a chain consisting of the specified {@code items}, followed by items generated by the specified {@code nextItemExtractor} applied to
     * the last item as its argument (or null if no preceding items), as long as the specified {@code condition} applied to the last item is satisfied
     * @see Chainable#chainIf(Predicate, UnaryOperator)
     */
    public static <T> Chainable<T> chainIf(Iterable<? extends T> items, Predicate<? super T> condition, UnaryOperator<T> nextItemExtractor) {
        return (items == null || nextItemExtractor == null) ? Chainable.from(items) : Chainable.fromIterator(() -> new Iterator<T>() {
            final Iterator<? extends T> iter = items.iterator();
            private T next = null;
            private boolean nextReady = false;

            @Override
            public boolean hasNext() {
                if (this.nextReady) {
                    return this.nextReady;
                } else if (this.iter.hasNext()) {
                    this.next = this.iter.next();
                    this.nextReady = true;
                    return this.nextReady;
                } else if (condition == null) {
                    // At end of current iterator but no condition specified
                    this.next = nextItemExtractor.apply(this.next);
                    this.nextReady = this.next != null;
                    return this.nextReady;
                } else if (condition.test(this.next)) {
                    // At end of current iterator and condition for the chaining is met
                    this.next = nextItemExtractor.apply(this.next);
                    this.nextReady = true;
                    return this.nextReady;
                } else {
                    // At end of current iterator and condition for chaining is not met
                    return this.nextReady = false;
                }
            }

            @Override
            public T next() {
                if (this.hasNext()) {
                    this.nextReady = false;
                    return this.next;
                } else {
                    return null;
                }
            }
        });
    }

    /**
     * @param items
     * @param targetCollection
     * @return a chain consisting of the specified {@code items}
     * @see Chainable#collectInto(Collection)
     */
    public static <T> Chainable<T> collectInto(Iterable<? extends T> items, Collection<? super T> targetCollection) {
        return (items == null || targetCollection == null) ? Chainable.from(items) : applyAsYouGo(items, o -> targetCollection.add(o));
    }

    /**
     * @param items
     * @param lister
     * @return a chain that begins with the specified {@code items} which are followed by items generated by the specified {@code lister} applied
     * to the last item
     * @see Chainable#concat(Function)
     */
    public static <T> Chainable<T> concat(Iterable<? extends T> items, Function<? super T, Iterable<? extends T>> lister) {
        return (lister == null || items == null) ? Chainable.from(items) : Chainable.fromIterator(() -> new Iterator<T>() {
            private final Iterator<? extends T> iter1 = items.iterator();
            private Iterator<? extends T> iter2 = null;

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
                    Iterable<? extends T> items2 = lister.apply(item);
                    this.iter2 = (Chainables.isNullOrEmpty(items2)) ? null : items2.iterator();
                    return item;
                } else {
                    return this.iter2.next();
                }
            }
        });
    }

    /**
     * Concatenates the two iterables, by first iterating through the first iterable
     * and the through the second.
     * @param items1 the first iterable
     * @param items2 the second iterable
     * @return a chain starting with the specified {@code items1} followed by the specified {@code items2}
     */
    // TODO Should this be removed now that concat(...) exists?
    public static <T> Chainable<T> concat(Iterable<? extends T> items1, Iterable<? extends T> items2) {
        if (items1 == null && items2 == null) {
            return null;
        } else if (Chainables.isNullOrEmpty(items1)) {
            return Chainable.from(items2);
        } else if (Chainables.isNullOrEmpty(items2)) {
            return Chainable.from(items1);
        } else {
            return Chainable.fromIterator(() -> new Iterator<T>() {
                private final Iterator<? extends T> iter1 = items1.iterator();
                private final Iterator<? extends T> iter2 = items2.iterator();

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
            });
        }
    }

    /**
     * Concatenates the specified iterable with the specified single item.
     * @param items the items to append the specified {@code item} to
     * @param item the item to append to the specified {@code items}
     * @return a chain of the specified {@code items} followed by the specified {@code item}
     */
    public static <T> Chainable<T> concat(Iterable<? extends T> items, T item) {
        return concat(items, (Iterable<? extends T>) Arrays.asList(item));
    }

    /**
     * @param itemSequences
     * @return a chain where the specified {@code itemSequences} have been concatenated in the order specified
     * @see Chainable#concat(Iterable...)
     */
    @SafeVarargs
    public static <T> Chainable<T> concat(Iterable<? extends T>...itemSequences) {
        return (isNullOrEmpty(itemSequences)) ? Chainable.empty() : Chainable.fromIterator(() -> new Iterator<T>() {
            private int i = 0;
            private Iterator<? extends T> curIter = null;

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
        });
    }

    /**
     * @param item
     * @param items
     * @return a chain starting with the specified {@code item} and with the specified {@code items} following it
     * @see Chainable#concat(Iterable)
     */
    public static <T> Chainable<T> concat(T item, Iterable<? extends T> items) {
        return concat((Iterable<T>) Arrays.asList(item), items);
    }

    /**
     * @param container
     * @param item
     * @return {@code true} iff the specified {@code item} is among the members of the specified {@code container}
     */
    public static <T> boolean contains(Iterable<? extends T> container, T item) {
        if (container == null) {
            return false;
        } else if (!(container instanceof Set<?>)) {
            return !isNullOrEmpty(whereEither(container, i -> i.equals(item)));
        } else if (item == null) {
            return false;
        } else {
            return ((Set<?>) container).contains(item);
        }
    }

    /**
     * @param container
     * @param items
     * @return {@code true} iff all of the specified {@code items} exist in the specified {@code container}
     * @see Chainable#containsAll(Object...)
     */
    @SafeVarargs
    public static <T> boolean containsAll(T[] container, T...items) {
        return containsAll(Chainable.from(container), items);
    }

    /**
     * @param container
     * @param items
     * @return {@code true} iff all of the specified {@code items} exist in the specified {@code items}
     * @see Chainable#containsAll(Object...)
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public static <T> boolean containsAll(Iterable<? extends T> container, T...items) {
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
     * @return {@code true} iff any of the specified {@code items} are among the members of the specified {@code container}
     * @see Chainable#containsAny(Object...)
     */
    @SafeVarargs
    public static <T> boolean containsAny(Iterable<? extends T> container, T...items) {
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
     * @return {@code true} iff the specified {@code container} contains any of the specified {@code items}
     * @see Chainable#containsAny(Object...)
     */
    @SafeVarargs
    public static <T> boolean containsAny(T[] container, T...items) {
        return containsAny(Chainable.from(container), items);
    }

    /**
     * @param items
     * @param contents
     * @return {@code true} iff this chain contains no items other than those that equal one of the specified {@code contents}
     */
    @SafeVarargs
    public static <T> boolean containsOnly(Iterable<? extends T> items, T...contents) {

        if (Chainables.isNullOrEmpty(items)) {
            return true;
        } else if (Chainables.isNullOrEmpty(contents)) {
            return false;
        }

        Set<T> searchSet = new HashSet<>(Arrays.asList(contents));

        for (T item : items) {
            if (!searchSet.contains(item)) {
                return false;
            }
        }

        return true;
    }

    /**
     * @param items
     * @param subarray
     * @return {@code true} iff the specified {@code items} contain the specified {@code subarray} in that same contiguous sequence
     * @see Chainable#containsSubarray(Iterable)
     */
    public static <T> boolean containsSubarray(Iterable<? extends T> items, Iterable<? extends T> subarray) {
        if (items == null) {
            return false;
        } else if (isNullOrEmpty(subarray)) {
            return true;
        }

        // Brute force evaluation of everything (TODO: make it lazy and faster?)
        List<? extends T> subList = toList(subarray);
        List<? extends T> itemsCached = toList(items);

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
     * Counts the number of items, forcing a complete traversal/evaluation of the specified {@code items}.
     * @param items an items to count
     * @return the number of items
     * @see Chainable#count()
     */
    public static <T> long count(Iterable<? extends T> items) {
        if (items == null) {
            return 0;
        } else if (items instanceof Collection<?>) {
            return ((Collection<?>)items).size();
        }

        Iterator<? extends T> iter = items.iterator();
        long i = 0;
        for (i = 0; iter.hasNext(); i++) {
            iter.next();
        }

        return i;
    }

    /**
     * @param items
     * @param childExtractor
     * @return a chain of items in the pre-order depth-first order based on the specified {@code childExtractor}
     * @see Chainable#depthFirst(Function)
     */
    public static <T> Chainable<T> depthFirst(Iterable<? extends T> items, Function<? super T, Iterable<? extends T>> childExtractor) {
        return traverse(items, childExtractor, false);
    }

    /**
     * @param items
     * @param childExtractor
     * @param condition
     * @return a chain of items in the pre-order depth-first order based on the specified {@code childExtractor}, but not deeper than the items satisfying the
     * specified {@code condition}
     * @see Chainable#depthFirstNotBelow(Function, Predicate)
     */
    public static <T> Chainable<T> depthFirstNotBelow(
            Iterable<? extends T> items,
            Function<? super T, Iterable<? extends T>> childExtractor,
            Predicate<? super T> condition) {
        return notBelow(items, childExtractor, condition, false);
    }

    /**
     * @param items items to sort
     * @return a chain of items sorted in the descending order based on an automatically detected key
     * @see Chainable#descending()
     */
    public static <T> Chainable<T> descending(Iterable<? extends T> items) {
        return sorted(items, false);
    }

    /**
     * @param items
     * @param keyExtractor
     * @return a chain of items sorted in the descending order based on the {@link java.lang.String} keys output by the specified {@code keyExtractor}
     * applied to each item
     * @see Chainable#descending(ToStringFunction)
     */
    public static <T> Chainable<T> descending(Iterable<? extends T> items, ToStringFunction<? super T> keyExtractor) {
        return sortedBy(items, keyExtractor, false);
    }

    /**
     * @param items
     * @param keyExtractor
     * @return a chain of items sorted in the descending order based on the {@link java.lang.Long} keys output by the specified {@code keyExtractor}
     * applied to each item
     * @see Chainable#descending(ToLongFunction)
     */
    public static <T> Chainable<T> descending(Iterable<? extends T> items, ToLongFunction<? super T> keyExtractor) {
        return sortedBy(items, keyExtractor, false);
    }

    /**
     * @param items
     * @param comparable
     * @return a chain of items sorted in the descending order based on the {@link java.lang.Double} keys output by the specified {@code keyExtractor}
     * applied to each item
     * @see Chainable#descending(ToDoubleFunction)
     */
    public static <T> Chainable<T> descending(Iterable<? extends T> items, ToDoubleFunction<? super T> comparable) {
        return sortedBy(items, comparable, false);
    }

    /**
     * Lists directories under the specified {@code directory}.
     * @param directory the root directory to list the sub-directories of
     * @return a chain of {@link java.io.File} references to sub-directories
     */
    public static Chainable<File> directoriesFromPath(File directory) {
        return (directory == null) ? Chainable.empty() : Chainable.from(new Iterable<File>() {
            @Override
            public Iterator<File> iterator() {
                try {
                    return new Iterator<File>() {
                        final Path path = directory.toPath();
                        final DirectoryStream<Path> stream = (directory.isDirectory()) ? Files.newDirectoryStream(path) : null;
                        final Iterator<Path> pathIter = (stream != null) ? stream.iterator() : null;
                        File nextDirFile = null;

                        @Override
                        public boolean hasNext() {
                            if (this.pathIter == null) {
                                return false;
                            } else if (this.nextDirFile != null) {
                                return true;
                            }

                            while(pathIter.hasNext()) {
                                Path path = pathIter.next();
                                if (path == null) {
                                    continue;
                                }

                                this.nextDirFile = new File(path.toUri());
                                if (this.nextDirFile.isDirectory()) {
                                    return true;
                                } else {
                                    this.nextDirFile = null;
                                }
                            }

                            try {
                                this.stream.close();
                            } catch (IOException e) {
                            }

                            return false;
                        }

                        @Override
                        public File next() {
                            if (this.hasNext()) {
                                File next = this.nextDirFile;
                                this.nextDirFile = null;
                                return next;
                            } else {
                                return null;
                            }
                        }
                    };
                } catch (IOException e) {
                    return null;
                }
            }
        });
    }

    /**
     * @param items
     * @param keyExtractor
     * @return a chain of items that are all distinct
     * @see Chainable#distinct(Function)
     */
    @SuppressWarnings("unchecked")
    public static <T, V> Chainable<T> distinct(Iterable<? extends T> items, Function<? super T, V> keyExtractor) {
        return (keyExtractor == null) ? (Chainable<T>) distinct(items) : Chainable.fromIterator(() -> new Iterator<T>() {
            final Map<V, T> seen = new HashMap<>();
            final Iterator<? extends T> iter = items.iterator();
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
        });
    }

    /**
     * @param items
     * @return a chain of items that are all distinct
     * @see Chainable#distinct()
     */
    public static <T> Chainable<T> distinct(Iterable<? extends T> items) {
        return Chainable.fromIterator(() -> new Iterator<T>() {
            final Set<T> seen = new HashSet<>();
            final Iterator<? extends T> iter = items.iterator();
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
        });
    }

    /**
     * @param items
     * @param suffix
     * @return {@code true} iff the specified {@code items} end with the sequence of consecutive items in the specified {@code suffix}
     * @see Chainable#endsWith(Iterable)
     */
    public static <T> boolean endsWith(Iterable<? extends T> items, Iterable<? extends T> suffix) {
        return endsWithEither(items, suffix);
    }

    /**
     * @param items
     * @param suffixes
     * @return {@code true} iff the specified {@code items} end with a sequence of consecutive items in any of the specified {@code suffixes}
     * @see Chainable#endsWithEither(Iterable...)
     */
    @SafeVarargs
    public static <T> boolean endsWithEither(Iterable<? extends T> items, Iterable<? extends T>...suffixes) {
        if (isNullOrEmpty(items)) {
            return false;
        } else if (suffixes == null) {
            return false;
        }

        List<? extends T> itemList = toList(items);
        for (Iterable<? extends T> suffix : suffixes) {
            // Check each suffix
            List<? extends T> suffixSequence = Chainables.toList(suffix);
            if (suffixSequence.size() > itemList.size()) {
                // If different size, assume non-match and check the next suffix
                continue;
            }

            Iterator<? extends T> suffixIter = suffixSequence.iterator();
            int i = 0;
            boolean matching = true;
            for (i = itemList.size() - suffixSequence.size(); i < itemList.size(); i++) {
                if (!suffixIter.hasNext()) {
                    matching = false;
                    break;
                }

                T suffixItem = suffixIter.next();
                T item = itemList.get(i);
                if (suffixItem == null && item == null) {
                    // Items both null so matching so far...
                    continue;
                } else if (suffixItem == null || item == null) {
                    // Items no longer matching so bail out
                    matching = false;
                    break;
                } else if (!suffixItem.equals(item)) {
                    matching = false;
                    break;
                }
            }

            if (matching) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param items1
     * @param items2
     * @return {@code true} iff each of the specified {@code items1} equals the corresponding member of the specified {@code items2} in that exact order
     * @see Chainable#equals(Iterable)
     */
    public static <T> boolean equal(Iterable<? extends T> items1, Iterable<? extends T> items2) {
        if (items1 == items2) {
            return true;
        } else if (items1 == null || items2 == null) {
            return false;
        } else {
            Iterator<? extends T> iterator1 = items1.iterator();
            Iterator<? extends T> iterator2 = items2.iterator();
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
     * Returns the first item from the specified items or {@code null} if no items.
     * @param items items to return the first item from
     * @return the first item
     * @see Chainable#first()
     */
    public static <T> T first(Iterable<? extends T> items) {
        if (items == null) {
            return null;
        } else {
            Iterator<? extends T> iter = items.iterator();
            if (!iter.hasNext()) {
                return null;
            } else {
                return iter.next();
            }
        }
    }

    /**
     * @param items
     * @param number
     * @return the first {@code number} of items
     * @see Chainable#first(long)
     */
    public static <T> Chainable<T> first(Iterable<? extends T> items, long number) {
        return (items == null) ? Chainable.empty() : Chainable.fromIterator(() -> new Iterator<T>() {
            Iterator<? extends T> iter = items.iterator();
            long returnedCount = 0;

            @Override
            public boolean hasNext() {
                return (returnedCount >= number) ? false : this.iter.hasNext();
            }

            @Override
            public T next() {
                this.returnedCount++;
                return this.iter.next();
            }
        });
    }

    /**
     * Finds the first item satisfying the specified condition or returns the specified {@code defaultValue} if not found.
     * @param items
     * @param conditions
     * @return the first item satisfying any of the specified {@code conditions} if one exists, or the specified {@code defaultValue} if none
     * @see Chainable#firstWhereEither(Object, Predicate...)
     */
    @SafeVarargs
    public static <V> V firstWhereEither(Iterable<? extends V> items, V defaultValue, Predicate<? super V>... conditions) {
        if (items == null) {
            return null;
        } else if (conditions == null) {
            return first(items);
        } else {
            for (V item : items) {
                for (Predicate<? super V> condition : conditions) {
                    if (condition.test(item)) {
                        return item;
                    }
                }
            }
        }

        return defaultValue;
    }

    /**
     * Finds the first item satisfying the specified condition or {@code null} if not found.
     * @param items
     * @param conditions
     * @return the first item satisfying any of the specified {@code conditions} if one exists, or {@code null} if none
     * @see Chainable#firstWhereEither(Predicate...)
     */
    @SafeVarargs
    public static <V> V firstWhereEither(Iterable<? extends V> items, Predicate<? super V>... conditions) {
        return firstWhereEither(items, null, conditions);
    }

    /**
     * @param items
     * @param index
     * @return the item that is at the position indicated by the specified {@code index}, or {@code null} if there are fewer items than that
     * @see Chainable#get(long)
     */
    public static <T> T get(Iterable<? extends T> items, long index) {
        return afterFirst(items, index).first();
    }

    /**
     * @param items1
     * @param items2
     * @return
     * @see Chainable#interleave(Iterable...)
     */
    @SafeVarargs
    public static <T> Chainable<T> interleave(Iterable<T> items1, Iterable<T>...items2) {
        return (items1 == null || items2 == null) ? Chainable.empty() : Chainable.fromIterator(() -> new Iterator<T>() {
            Deque<Iterator<? extends T>> iters = new LinkedList<>(Chainable
                    .from(items1.iterator())
                    .concat(Chainable.from(items2).transform(i -> i.iterator()))
                    .toList());

            @Override
            public boolean hasNext() {
                while (!this.iters.isEmpty() && !this.iters.peek().hasNext()) {
                    this.iters.removeFirst();
                }

                return !this.iters.isEmpty();
            }

            @Override
            public T next() {
                Iterator<? extends T> iter = this.iters.removeFirst();
                if (iter != null) {
                    this.iters.addLast(iter);
                    return iter.next();
                } else {
                    return null;
                }
            }
        });
    }

    /**
     * @param items
     * @param min
     * @return true if there are at least the specified {@code min} number of {@code items}, stopping the traversal as soon as that can be determined
     * @see Chainable#isCountAtLeast(long)
     */
    public static <T> boolean isCountAtLeast(Iterable<? extends T> items, long min) {
        if (min <= 0) {
            return true;
        } else if (items == null) {
            return false;
        }

        Iterator<? extends T> iter = items.iterator();
        while (min > 0 && iter.hasNext()) {
            iter.next();
            min--;
        }

        return min == 0;
    }

    /**
     * @param items
     * @param max
     * @return true if there are at most the specified {@code max} number of {@code items}, stopping the traversal as soon as that can be determined
     * @see Chainable#isCountAtMost(long)
     */
    public static <T> boolean isCountAtMost(Iterable<? extends T> items, long max) {
        if (items == null && max >= 0) {
            return true;
        } else if (items == null) {
            return false;
        }

        Iterator<? extends T> iter = items.iterator();
        while (max > 0 && iter.hasNext()) {
            iter.next();
            max--;
        }

        return max >= 0 && !iter.hasNext();
    }

    /**
     * @param items
     * @param count
     * @return
     * @see Chainable#isCountExactly(long)
     */
    public static <T> boolean isCountExactly(Iterable<? extends T> items, long count) {
        if (items == null) {
            return count == 0;
        } else if (items instanceof Collection<?>) {
            return ((Collection<?>)items).size() == count;
        }

        Iterator<? extends T> iter = items.iterator();
        long i = 0;
        while (iter.hasNext()) {
            iter.next();
            i++;
            if (i == count) {
                return !iter.hasNext();
            }
        }

        return i == count;
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
     * @param text text to check
     * @return {@code true} if the specified {@code text} is {@code null} or empty.
     */
    public static boolean isNullOrEmpty(String text) {
        return (text != null) ? text.isEmpty() : true;
    }

    /**
     * @param iterables
     * @return {@code true} if any of the specified {@code iterables} are null or empty
     */
    public static boolean isNullOrEmptyEither(Iterable<?>...iterables) {
        for (Iterable<?> iterable : iterables) {
            if (isNullOrEmpty(iterable)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param container
     * @param item
     * @return
     * @see Chainable#iterativeContains(Object)
     */
    @Experimental
    public static <T> Chainable<Boolean> iterativeContains(Iterable<? extends T> container, T item) {
        if (container == null) {
            return Chainable.from(false);
        } else if (container instanceof Set<?> && item != null) {
            return Chainable.from(((Set<?>) container).contains(item));
        } else {
            return Chainable
                    .from(container)
                    .transform(i -> i == null ? item == null : i.equals(item))
                    .transform(b -> Boolean.TRUE.equals(b) ? true : null)
                    .notAfter(b -> Boolean.TRUE.equals(b))
                    .chainIf(b -> b == null, b -> false); // Last item is false
        }
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
    public static <V> boolean isNullOrEmpty(Iterator<? extends V> iterator) {
        return (iterator != null) ? !iterator.hasNext() : true;
    }

    /**
     * Joins the items produced by the specified {@code iterator} into a single string, invoking {@code toString()} on each item,
     * separating each string with the specified {@code delimiter}, skipping {@code null} values.
     * @param delimiter the text to insert between items
     * @param iterator the iterator to traverse
     * @return the joined string
     */
    public static <T> String join(String delimiter, Iterator<? extends T> iterator) {
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
     * {@code delimiter}, skipping {@code null} values..
     * @param delimiter the text to insert between consecutive strings
     * @param stream the stream whose items are to be joined
     * @return the joined string
     */
    public static <T> String join(String delimiter, Stream<T> stream) {
        return (stream != null) ? Chainables.join(delimiter, stream.iterator()) : null;
    }

    /**
     * @param items
     * @return
     * @see Chainable#last()
     */
    public static <T> T last(Iterable<? extends T> items) {
        T last = null;
        if (isNullOrEmpty(items)) {
            // Skip
        } else if (items instanceof List<?>) {
            // If list, then faster lookup
            List<? extends T> list = (List<? extends T>)items;
            last = list.get(list.size() - 1);
        } else {
            // Else, slow lookup
            Iterator<? extends T> iter = items.iterator();
            while (iter.hasNext()) {
                last = iter.next();
            }
        }

        return last;
    }

    /**
     * @param items
     * @param count
     * @return
     * @see Chainable#last(int)
     */
    public static <T> Chainable<T> last(Iterable<? extends T> items, long count) {
        return (items == null) ? Chainable.empty() : Chainable.fromIterator(() -> new Iterator<T>() {
            final List<? extends T> list = Chainables.toList(items);
            final int size = this.list.size();
            long next = this.size - count;

            @Override
            public boolean hasNext() {
                return (this.list != null) ? this.next >= 0 && this.next < this.size : false;
            }

            @Override
            public T next() {
                return this.list.get((int) this.next++);
            }
        });
    }

    /**
     * Joins the specified {@code items} into a single string, invoking {@code toString()}) on each, separating them with the specified {@code delimiter},
     * skipping {@code null} values.
     * @param delimiter the text to insert between the items
     * @param items the items to join
     * @return the joined string
     * @see Chainable#join(String)
     */
    public static <T> String join(String delimiter, Iterable<? extends T> items) {
        return join(delimiter, items.iterator());
    }

    /**
     * Joins the specified {@code items} into a single string, invoking {@code toString()}) on each, separating them with the specified {@code delimiter},
     * skipping {@code null} values.
     * @param delimiter the text to insert between the items
     * @param items the items to join
     * @return the joined string
     * @see Chainable#join(String)
     */
    public static <T> String join(String delimiter, T[] items) {
        return join(delimiter, Arrays.asList(items).iterator());
    }

    /**
     * @param items
     * @param valueExtractor
     * @return
     * @see Chainable#max(Function)
     */
    public static <T> T max(Iterable<? extends T> items, Function<? super T, Double> valueExtractor) {
        Double max = null;
        T maxItem = null;
        if (!isNullOrEmpty(items)) {
            for (T item : items) {
                Double number = valueExtractor.apply(item);
                if (max == null || number > max) {
                    max = number;
                    maxItem = item;
                }
            }
        }

        return maxItem;
    }

    /**
     * @param items
     * @param valueExtractor
     * @return
     * @see Chainable#min(Function)
     */
    public static <T> T min(Iterable<? extends T> items, Function<? super T, Double> valueExtractor) {
        Double min = null;
        T minItem = null;
        if (!Chainables.isNullOrEmpty(items)) {
            for (T item : items) {
                Double number = valueExtractor.apply(item);
                if (min == null || number < min) {
                    min = number;
                    minItem = item;
                }
            }
        }

        return minItem;
    }

    /**
     * @param items
     * @param condition
     * @return
     * @see Chainable#noneWhere(Predicate)
     */
    public static <T> boolean noneWhere(Iterable<? extends T> items, Predicate<? super T> condition) {
        return Chainables.noneWhereEither(items, condition);
    }

    /**
     * @param items
     * @param conditions
     * @return
     * @see Chainable#noneWhereEither(Predicate...)
     */
    @SafeVarargs
    public static <T> boolean noneWhereEither(Iterable<? extends T> items, Predicate<? super T>... conditions) {
        return !Chainables.anyWhereEither(items, conditions);
    }

    /**
     * Returns items until and including the first item satisfying the specified condition, and no items after that
     * @param items items to return from
     * @param condition the condition that the last item needs to meet
     * @return items before and including the first item where the specified condition is satisfied
     * @see Chainable#notAfter(Predicate)
     */
    public static <T> Chainable<T> notAfter(Iterable<? extends T> items, Predicate<? super T> condition) {
        if (items == null) {
            return null;
        } else if (condition == null) {
            return Chainable.from(items);
        }

        return Chainable.fromIterator(() -> new Iterator<T>() {
            private final Iterator<? extends T> iterator = items.iterator();
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
        });
    }

    /**
     * @param items
     * @param condition
     * @return
     * @see Chainable#notAsLongAs(Predicate)
     */
    public static <T> Chainable<T> notAsLongAs(Iterable<? extends T> items, Predicate<? super T> condition) {
        return (items != null) ? notBefore(items, condition.negate()) : null;
    }

    /**
     * @param items
     * @param value
     * @return
     * @see Chainable#notAsLongAsValue(Object)
     */
    public static <T> Chainable<T> notAsLongAsValue(Iterable<? extends T> items, T value) {
        return notBefore(items, o -> o!=value);
    }

    /**
     * @param items
     * @param condition
     * @return
     * @see Chainable#notBefore(Predicate)
     */
    static <T> Chainable<T> notBefore(Iterable<? extends T> items, Predicate<? super T> condition) {
        if (items == null) {
            return null;
        } else if (condition == null) {
            return Chainable.from(items);
        } else {
            return Chainable.fromIterator(() -> new Iterator<T>() {
                final Iterator<? extends T> iterator = items.iterator();
                T nextItem = null;
                boolean start = false;

                @Override
                public boolean hasNext() {
                    if (this.nextItem != null) {
                        return true;
                    } else if (!this.iterator.hasNext()) {
                        this.nextItem = null;
                        return false;
                    } else if (this.start) {
                        this.nextItem = this.iterator.next();
                        return true;
                    } else {
                        while (this.iterator.hasNext()) {
                            this.nextItem = this.iterator.next();
                            if (condition.test(this.nextItem)) {
                                this.start = true;
                                break;
                            } else {
                                this.nextItem = null;
                            }
                        }

                        return this.nextItem != null;
                    }
                }

                @Override
                public T next() {
                    if (!this.hasNext()) {
                        return null;
                    }

                    T item = this.nextItem;
                    this.nextItem = null;
                    return item;
                }
            });
        }
    }

    /**
     * Returns the rest of the specified items starting with the specified item, if found.
     * @param items items to skip over
     * @param item item to skip until
     * @return the rest of the items
     * @see Chainable#notBeforeEquals(Object)
     */
    public static <T> Chainable<T> notBeforeEquals(Iterable<? extends T> items, T item) {
        return notBefore(items, (Predicate<T>)(o -> o == item));
    }

    private static <T> Chainable<T> notBelow(
            Iterable<? extends T> items,
            Function<? super T, Iterable<? extends T>> childTraverser,
            Predicate<? super T> condition,
            boolean breadthFirst) {
        final Predicate<? super T> appliedCondition = (condition != null) ? condition : (o -> false);
        return traverse(items, o -> Boolean.FALSE.equals(appliedCondition.test(o)) ? childTraverser.apply(o) : Chainable.empty(), breadthFirst);
    }

    /**
     * @param items
     * @param condition
     * @return
     * @see Chainable#notWhere(Predicate)
     */
    public static final <T> Chainable<T> notWhere(Iterable<? extends T> items, Predicate<? super T> condition) {
        return (condition != null) ? Chainables.whereEither(items, condition.negate()) : Chainable.from(items);
    }

    /**
     * @param items
     * @param example
     * @return
     * @see Chainable#ofType(Object)
     */
    @SuppressWarnings("unchecked")
    public static <T, O> Chainable<O> ofType(Iterable<? extends T> items, O example) {
        Class<? extends Object> clazz = example.getClass();
        return (Chainable<O>) withoutNull(items)
                .transform(i -> (clazz.isAssignableFrom(i.getClass())) ? clazz.cast(i) : null)
                .withoutNull();
    }

    /**
     * @param items
     * @param replacer
     * @return
     * @see Chainable#replace(Function)
     */
    public static <T> Chainable<T> replace(Iterable<? extends T> items, Function<? super T, Iterable<? extends T>> replacer) {
        return transformAndFlatten(items, replacer).withoutNull();
    }

    /**
     * @param items
     * @return
     * @see Chainable#reverse()
     */
    public static <T> Chainable<T> reverse(Iterable<? extends T> items) {
        return (items == null) ? Chainable.empty() : Chainable.fromIterator(() -> new Iterator<T>() {
            List<? extends T> list = Chainables.toList(items);
            int nextIndex = list.size() - 1;

            @Override
            public boolean hasNext() {
                return (nextIndex >= 0);
            }

            @Override
            public T next() {
                return (this.hasNext()) ? list.get(this.nextIndex--) : null;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> Chainable<T> sorted(Iterable<? extends T> items, boolean ascending) {
        T item;

        if (isNullOrEmpty(items)) {
            return Chainable.from(items);
        } else if (null != (item = items.iterator().next()) && item instanceof Number) {
            // Sniff if first item is a number
            return (Chainable<T>) sortedBy((Iterable<Number>) items, (Number n) -> n != null ? n.doubleValue() : null, ascending);
        } else {
            // Not a number so fall back on String
            return (Chainable<T>) sortedBy((Iterable<Object>) items, (Object o) -> o != null ? o.toString() : null, ascending);
        }
    }

    private static <T> Chainable<T> sortedBy(Iterable<? extends T> items, BiFunction<? super T, ? super T, Integer> comparator, boolean ascending) {
        if (items == null || comparator == null) {
            return Chainable.from(items);
        }

        List<T> list = toList(items);
        list.sort(new Comparator<T>() {

            @Override
            public int compare(T o1, T o2) {
                return (ascending) ? comparator.apply(o1, o2) : comparator.apply(o2, o1);
            }
        });

        return Chainable.from(list);
    }

    private static <T> Chainable<T> sortedBy(Iterable<? extends T> items, ToStringFunction<? super T> keyExtractor, boolean ascending) {
        return (keyExtractor == null) ? Chainable.from(items) : sortedBy(items, (o1, o2) -> Objects.compare(
                    keyExtractor.apply(o1),
                    keyExtractor.apply(o2),
                    Comparator.comparing(String::toString)), ascending);
    }

    private static <T> Chainable<T> sortedBy(Iterable<? extends T> items, ToLongFunction<? super T> keyExtractor, boolean ascending) {
        return (keyExtractor == null) ? Chainable.from(items) : sortedBy(items, (o1, o2) -> Long.compare(
                        keyExtractor.applyAsLong(o1),
                        keyExtractor.applyAsLong(o2)),
                    ascending);
    }

    private static <T> Chainable<T> sortedBy(Iterable<? extends T> items, ToDoubleFunction<? super T> keyExtractor, boolean ascending) {
        return (keyExtractor == null)
                ? Chainable.from(items)
                : sortedBy(items, (o1, o2) -> Double.compare(
                        keyExtractor.applyAsDouble(o1),
                        keyExtractor.applyAsDouble(o2)), ascending);
    }

    /**
     * Splits the specified {@code text} into a individual characters.
     * @param text the text to split
     * @return a chain of characters
     */
    public static Chainable<String> split(String text) {
        return (text == null || text.isEmpty()) ? Chainable.empty() : Chainable.fromIterator(() -> new Iterator<String>() {
            StringCharacterIterator iter = new StringCharacterIterator(text);
            String next = null;
            boolean stopped = false;

            @Override
            public boolean hasNext() {
                if (this.stopped) {
                    return false;
                } else if (this.next != null) {
                    return true;
                } else {
                    char c = iter.current();
                    iter.next();
                    if (c == StringCharacterIterator.DONE) {
                        this.stopped = true;
                        this.next = null;
                        return false;
                    } else {
                        this.next = String.valueOf(c);
                        return true;
                    }
                }
            }

            @Override
            public String next() {
                if (!this.stopped && this.hasNext()) {
                    String temp = this.next;
                    this.next = null;
                    return temp;
                } else {
                    return null;
                }
            }
        });
    }

    /**
     * Splits the specified {@code text} using the specified {@code delimiterChars}.
     * @param text the text to split
     * @param delimiterCharacters
     * @return the split strings, including the delimiters
     */
    public static Chainable<String> split(String text, String delimiterCharacters) {
        return split(text, delimiterCharacters, true);
    }

    /**
     * Splits the specified {@code text} using the specified {@code delimiterChars}.
     * <p>
     * Each of the characters in the specified {@code delimiterCharacters} is used as a separator individually.
     * @param text the text to split
     * @param delimiterCharacters the characters to use to split the specified {@code text}
     * @param includeDelimiters if {@code true}, the delimiter chars are included in the returned results, otherwise they're not
     * @return the split strings
     */
    public static Chainable<String> split(String text, String delimiterCharacters, boolean includeDelimiters) {
        return (text == null || delimiterCharacters == null) ? Chainable.empty() : Chainable.fromIterator(() -> new Iterator<String>() {
            StringTokenizer tokenizer = new StringTokenizer(text, delimiterCharacters, includeDelimiters);

            @Override
            public boolean hasNext() {
                return this.tokenizer.hasMoreTokens();
            }

            @Override
            public String next() {
                return this.tokenizer.nextToken();
            }
        });
    }

    /**
     * @param items
     * @param prefixes
     * @return
     * @see Chainable#startsWithEither(Iterable...)
     */
    @SafeVarargs
    public static <T> boolean startsWithEither(Iterable<? extends T> items, Iterable<? extends T>... prefixes) {
        if (isNullOrEmpty(items)) {
            return false;
        } else if (prefixes == null) {
            return false;
        }

        for (Iterable<? extends T> prefix : prefixes) {
            Iterator<? extends T> prefixIterator = prefix.iterator();
            for (T item : items) {
                if (!prefixIterator.hasNext()) {
                    return true;
                }

                T prefixItem = prefixIterator.next();
                if (prefixItem == item) {
                    continue;
                } else if (prefixItem == null) {
                    break;
                } else if (!prefixItem.equals(item)) {
                    break;
                }
            }

            // Nothing left in prefix to match so it's a match
            if (!prefixIterator.hasNext()) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param items
     * @param valueExtractor
     * @return
     * @see Chainable#sum(Function)
     */
    public static <T> long sum(Iterable<? extends T> items, Function<? super T, Long> valueExtractor) {
        int sum = 0;
        if (!Chainables.isNullOrEmpty(items)) {
            Chainable<Long> numbers = withoutNull(items).transform(valueExtractor);
            for (Long number : numbers) {
                if (number != null) {
                    sum += number;
                }
            }
        }

        return sum;
    }

    /**
     * @param items
     * @return
     */
    public static String[] toArray(Iterable<String> items) {
        long len;
        if (items == null) {
            len = 0;
        } else {
            len = count(items);
        }

        String[] array = new String[(int) len];
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
    @SuppressWarnings("unchecked")
    public static <T> List<T> toList(Iterable<? extends T> items) {
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
     * @param items
     * @param keyExtractor
     * @return
     * @see Chainable#toMap(Function)
     */
    public static <K, V> Map<K, V> toMap(Iterable<? extends V> items, Function<? super V, K> keyExtractor) {
        if (items == null || keyExtractor == null) {
            return Collections.emptyMap();
        }

        final Map<K, V> map = new HashMap<>();
        apply(items, i -> map.put(keyExtractor.apply(i), i));
        return map;
    }

    /**
     * @param items
     * @return
     * @see Chainable#toQueue()
     */
    @Experimental
    public static <T> ChainableQueue<T> toQueue(Iterable<? extends T> items) {
        return new ChainableQueueImpl<>(items);
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
    public static <I, O> Chainable<O> transform(Iterable<? extends I> items, Function<? super I, O> transformer) {
        return (items == null || transformer == null) ? Chainable.empty() : Chainable.fromIterator(() -> new Iterator<O>() {
            Iterator<? extends I> iterator = items.iterator();

            @Override
            public boolean hasNext() {
                if (Chainables.isNullOrEmpty(this.iterator)) {
                    this.iterator = null;
                    return false;
                } else {
                    return true;
                }
            }

            @Override
            public O next() {
                return transformer.apply(this.iterator.next());
            }
        });
    }

    /**
     * @param items
     * @param transformer
     * @return
     * @see Chainable#transformAndFlattenArray(Function)
     */
    public static <I, O> Chainable<O> transformAndFlattenArray(Iterable<? extends I> items, Function<? super I, O[]> transformer) {
        return transformAndFlatten(items, o -> Chainable.from(transformer.apply(o)));
    }

    /**
     * @param items
     * @param transformer
     * @return
     * @see Chainable#transformAndFlatten(Function)
     */
    public static <I, O> Chainable<O> transformAndFlatten(Iterable<? extends I> items, Function<? super I, Iterable<? extends O>> transformer) {
        return (items == null || transformer == null) ? Chainable.empty() : Chainable.fromIterator(() -> new Iterator<O>() {
            private final Iterator<? extends I> iterIn = items.iterator();
            private Iterator<? extends O> iterOut = null;
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
                        Iterable<? extends O> results = transformer.apply(itemIn);
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
        });
    }

    private static <T> Chainable<T> traverse(
            Iterable<? extends T> initials,
            Function<? super T, Iterable<? extends T>> childTraverser,
            boolean breadthFirst) {
        return (initials == null || childTraverser == null) ? Chainable.empty() : Chainable.fromIterator(() -> new Iterator<T>() {
            Deque<Iterator<? extends T>> iterators = new LinkedList<>(Arrays.asList(initials.iterator()));
            T nextItem = null;
            Set<T> seenValues = new HashSet<>();

            @Override
            public boolean hasNext() {
                // TODO Trips up on NULL values "by design" - should it?
                while (this.nextItem == null && !iterators.isEmpty()) {
                    Iterator<? extends T> currentIterator = iterators.peekFirst();
                    if (Chainables.isNullOrEmpty(currentIterator)) {
                        iterators.pollFirst();
                        continue;
                    }

                    do {
                        this.nextItem = currentIterator.next();
                        if (this.seenValues.contains(this.nextItem)) {
                            this.nextItem = null; // Protect against infinite loops
                        }
                    } while (this.nextItem == null && currentIterator.hasNext());

                    if (this.nextItem != null) {
                        // Protect against cycles based on inner
                        this.seenValues.add(this.nextItem);
                    }
                }

                return null != this.nextItem;
            }

            @Override
            public T next() {
                Iterable<? extends T> nextChildren = childTraverser.apply(this.nextItem);
                if (!Chainables.isNullOrEmpty(nextChildren)) {
                    if (breadthFirst) {
                        this.iterators.addLast(nextChildren.iterator());
                    } else {
                        this.iterators.addFirst(nextChildren.iterator());
                    }
                }

                T returnValue = this.nextItem;
                this.nextItem = null;
                return returnValue;
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
    public static final <T> Chainable<T> whereEither(Iterable<? extends T> items, Predicate<? super T>... predicates) {
        if (items == null) {
            return Chainable.empty();
        } else if (predicates == null || predicates.length == 0) {
            return Chainable.from(items);
        }

        return Chainable.fromIterator(() -> new Iterator<T>() {
            final Iterator<? extends T> innerIterator = items.iterator();
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

                    for (Predicate<? super T> predicate : predicates) {
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
        });
    }

    /**
     * @param items
     * @return chain without null values
     * @see Chainable#withoutNull()
     */
    public static <T> Chainable<T> withoutNull(Iterable<? extends T> items) {
        return (items != null) ? whereEither(items, i -> i != null) : null;
    }
}

