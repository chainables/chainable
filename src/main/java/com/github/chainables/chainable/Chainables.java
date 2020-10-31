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

        static <T> Chain<T> from(Iterable<T> iterable) {
            if (iterable instanceof CachedChain<?>) {
                return (CachedChain<T>) iterable;
            } else {
                return new CachedChain<>(iterable);
            }
        }

        private CachedChain(Iterable<T> iterable) {
            super(iterable);
        }

        @Override
        public T get(long index) {
            return (this.cache != null) ? this.cache.get(Math.toIntExact(index)) : super.get(index);
        }

        @Override
        public Iterator<T> iterator() {
            if (this.cache != null) {
                // Cache already filled so return from it
                return this.cache.iterator();
            } else {
                return new Iterator<T>() {
                    Iterator<T> iter = iterable.iterator();
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
        protected Iterable<T> iterable;

        private Chain(Iterable<T> iterable) {
            this.iterable = (iterable != null) ? iterable : new ArrayList<>();
        }

        static <T> Chain<T> empty() {
            return Chain.from(new ArrayList<>());
        }

        static <T> Chain<T> from(Iterable<T> iterable) {
            if (iterable instanceof Chain<?>) {
                return (Chain<T>) iterable;
            } else if (iterable instanceof CachedChain<?>) {
                return (CachedChain<T>) iterable;
            } else {
                return new Chain<>(iterable);
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

        @Override
        public Iterator<T> iterator() {
            return this.iterable.iterator();
        }

        @Override
        public String toString() {
            return Chainables.join(", ", this);
        }

        @Override
        public T get(long index) {
            return (this.iterable instanceof List<?>) ? ((List<T>) this.iterable).get(Math.toIntExact(index)) : Chainables.get(this, index);
        }
    }

    private static class ChainableQueueImpl<T> extends Chain<T> implements ChainableQueue<T> {

        final Deque<T> queue = new LinkedList<>();
        Iterable<T> originalIterable;
        final Iterator<T> initialIter;

        private ChainableQueueImpl(Iterable<T> iterable) {
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

            if (!Chainables.isNullOrEmpty(this.originalIterable)) {
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
     * @return
     * @see Chainable#afterFirst()
     */
    public static <V> Chainable<V> afterFirst(Iterable<V> items) {
        return afterFirst(items, 1);
    }

    /**
     * @param items
     * @param number
     * @return
     */
    public static <V> Chainable<V> afterFirst(Iterable<V> items, long number) {
        return (items == null) ? Chainable.empty() : Chainable.fromIterator(() -> new Iterator<V>() {
            final Iterator<V> iter = items.iterator();
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
     * @param conditions
     * @return
     * @see Chainable#allWhereEither(Predicate...)
     */
    @SafeVarargs
    public static <T> boolean allWhereEither(Iterable<T> items, Predicate<? super T>... conditions) {
        if (items == null) {
            return false;
        } else {
            Chainable<Predicate<? super T>> conds = Chainable.from(conditions);
            return Chainables.noneWhere(items, i -> !conds.anyWhere(c -> c.test(i)));
        }
    }

    /**
     * @param items
     * @param condition
     * @return
     * @see Chainable#allWhere(Predicate)
     */
    public static <T> boolean allWhere(Iterable<T> items, Predicate<? super T> condition) {
        return allWhereEither(items, condition);
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
     * @param condition
     * @return
     * @see Chainable#anyWhere(Predicate)
     */
    public static <T> boolean anyWhere(Iterable<T> items, Predicate<? super T> condition) {
        return Chainables.anyWhereEither(items, condition);
    }

    /**
     * @param items
     * @param conditions
     * @return
     * @see Chainable#anyWhereEither(Predicate...)
     */
    @SafeVarargs
    public static <T> boolean anyWhereEither(Iterable<T> items, Predicate<? super T>...conditions) {
        if (conditions == null) {
            return true;
        } else {
            for (Predicate<? super T> condition : conditions) {
                if (Chainables.firstWhereEither(items, condition) != null) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * @param items
     * @param action
     * @return
     * @see Chainable#apply(Consumer)
     */
    public static <T> Chainable<T> apply(Iterable<T> items, Consumer<? super T> action) {
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
    public static <T> Chainable<T> applyAsYouGo(Iterable<T> items, Consumer<? super T> action) {
        if (items == null) {
            return null;
        } else if (action == null) {
            return Chainable.from(items);
        } else {
            return Chainable.fromIterator(() -> new Iterator<T>() {
                final private Iterator<T> itemIter = items.iterator();

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
     * @return sorted items
     * @see Chainable#ascending()
     */
    public static <T> Chainable<T> ascending(Iterable<T> items) {
        return sorted(items, true);
    }

    /**
     * @param items
     * @param keyExtractor
     * @return
     * @see Chainable#ascending(ToStringFunction)
     */
    public static <T> Chainable<T> ascending(Iterable<T> items, ToStringFunction<? super T> keyExtractor) {
        return sortedBy(items, keyExtractor, true);
    }

    /**
     * @param items
     * @param keyExtractor
     * @return
     * @see Chainable#ascending(ToLongFunction)
     */
    public static <T> Chainable<T> ascending(Iterable<T> items, ToLongFunction<? super T> keyExtractor) {
        return sortedBy(items, keyExtractor, true);
    }

    /**
     * @param items
     * @param keyExtractor
     * @return
     * @see Chainable#ascending(ToDoubleFunction)
     */
    public static <T> Chainable<T> ascending(Iterable<T> items, ToDoubleFunction<? super T> keyExtractor) {
        return sortedBy(items, keyExtractor, true);
    }

    /**
     * Returns items before the first one that does not satisfy the specified {@code condition}.
     * @param items items to return from
     * @param condition the condition for the returned items to satisfy
     * @return items before the first one is encountered taht no longer satisfies the specified condition
     */
    public static <T> Chainable<T> asLongAs(Iterable<T> items, Predicate<? super T> condition) {
        return (condition == null) ? Chainable.from(items) : before(items, condition.negate());
    }

    /**
     * Returns items before the first one that is not equal to the specified item.
     * @param items items to return from
     * @param item the item that returned items must be equal to
     * @return items before the first one is encountered that no longer equals the specified item
     */
    public static <T> Chainable<T> asLongAsEquals(Iterable<T> items, T item) {
        return asLongAs(items, o -> o == item);
    }

    /**
     * @param items
     * @param example
     * @return
     * @see Chainable#ofType(Object)
     */
    @SuppressWarnings("unchecked")
    public static <T, O> Chainable<O> ofType(Iterable<T> items, O example) {
        Class<? extends Object> clazz = example.getClass();
        return (Chainable<O>) Chainable
                .from(items)
                .withoutNull()
                .transform(i -> (clazz.isAssignableFrom(i.getClass())) ? clazz.cast(i) : null)
                .withoutNull();
    }

    /**
     * Returns items before the first item satisfying the specified condition is encountered.
     * @param items items to return from
     * @param condition the condition that stops further items from being returned
     * @return items before the specified condition is satisfied
     * @see Chainable#before(Predicate)
     */
    public static <T> Chainable<T> before(Iterable<T> items, Predicate<? super T> condition) {
        if (items == null) {
            return null;
        } else if (condition == null) {
            return Chainable.from(items);
        }

        return Chainable.fromIterator(() -> new Iterator<T>() {
            private final Iterator<T> iterator = items.iterator();
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
     * @return items before the specified item is encountered
     * @see Chainable#beforeValue(Object)
     */
    public static <T> Chainable<T> beforeValue(Iterable<T> items, T item) {
        return before(items, o -> o==item);
    }

    /**
     * @param items
     * @param childTraverser
     * @return
     * @see Chainable#breadthFirst(Function)
     */
    public static <T> Chainable<T> breadthFirst(Iterable<T> items, Function<? super T, Iterable<T>> childTraverser) {
        return traverse(items, childTraverser, true);
    }

    /**
     * @param items
     * @param childTraverser
     * @param condition
     * @return
     * @see Chainable#breadthFirstNotBelow(Function, Predicate)
     */
    public static <T> Chainable<T> breadthFirstNotBelow(
            Iterable<T> items,
            Function<? super T, Iterable<T>> childTraverser,
            Predicate<? super T> condition) {
        return notBelow(items, childTraverser, condition, true);
    }

    /**
     * @param items
     * @param childTraverser
     * @param condition
     * @return
     * @see Chainable#breadthFirstAsLongAs(Function, Predicate)
     */
    public static <T> Chainable<T> breadthFirstAsLongAs(
            Iterable<T> items,
            Function<? super T, Iterable<T>> childTraverser,
            Predicate<? super T> condition) {
        final Predicate<? super T> appliedCondition = (condition != null) ? condition : (o -> true);
        return breadthFirst(items, o -> Chainables.whereEither(childTraverser.apply(o), c -> Boolean.TRUE.equals(appliedCondition.test(c))));
    }

    /**
     * @param items
     * @return
     * @see Chainable#cached()
     */
    public static <T> Chainable<T> cached(Iterable<T> items) {
        return (items == null) ? Chainable.empty() : CachedChain.from(items);
    }

    /**
     * @param items
     * @param clazz
     * @return
     * @see Chainable#cast(Class)
     */
    public static <T1, T2> Chainable<T2> cast(Iterable<T1> items, Class<T2> clazz) {
        return (items == null || clazz == null) ? Chainable.from() : transform(items, o -> clazz.cast(o));
    }

    /**
     * @param item
     * @param nextItemExtractor
     * @return
     * @see Chainable#chain(UnaryOperator)
     */
    public static <T> Chainable<? super T> chain(T item, UnaryOperator<? super T> nextItemExtractor) {
        return chain(Chainable.from(item), nextItemExtractor);
    }

    /**
     * @param item
     * @param nextItemExtractor
     * @return
     * @see Chainable#chainIndexed(BiFunction)
     */
    public static <T> Chainable<T> chain(T item, BiFunction<? super T, Long, T> nextItemExtractor) {
        return chainIndexed(Chainable.from(item), nextItemExtractor);
    }

    /**
     * @param items
     * @param nextItemExtractorFromLastTwo
     * @return
     */
    public static <T> Chainable<T> chain(Iterable<T> items, BinaryOperator<T> nextItemExtractorFromLastTwo) {
        return (items == null || nextItemExtractorFromLastTwo == null) ? Chainable.from(items) : Chainable.fromIterator(() -> new Iterator<T>() {
            Iterator<T> iter = items.iterator();
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
                } else if (Chainables.isNullOrEmpty(this.iter)) {
                    // Seed iterator already finished so start the chaining
                    this.iter = null;
                    T temp = this.next;
                    this.next = nextItemExtractorFromLastTwo.apply(this.prev, this.next);
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
     * @param items
     * @param nextItemExtractor
     * @return
     * @see Chainable#chainIndexed(BiFunction)
     */
    public static <T> Chainable<T> chainIndexed(Iterable<T> items, BiFunction<? super T, Long, T> nextItemExtractor) {
        return (items == null || nextItemExtractor == null) ? Chainable.from(items) : Chainable.fromIterator(() -> new Iterator<T>() {
            Iterator<T> iter = items.iterator();
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
                } else if (Chainables.isNullOrEmpty(this.iter)) {
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
     * @return
     * @see Chainable#chain(UnaryOperator)
     */
    public static <T> Chainable<T> chain(Iterable<T> items, UnaryOperator<T> nextItemExtractor) {
        return (items == null || nextItemExtractor == null) ? Chainable.from(items) : Chainable.fromIterator(() -> new Iterator<T>() {
            Iterator<T> iter = items.iterator();
            T next = null;
            boolean isFetched = false; // If iter is empty, pretend it starts with null
            boolean isStopped = false;

            @Override
            public boolean hasNext() {
                if (isStopped) {
                    return false;
                } else if (isFetched) {
                    return true;
                } else if (Chainables.isNullOrEmpty(this.iter)) {
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
     * @return {@link Chainable#chainIf(Predicate, UnaryOperator)}
     */
    public static <T> Chainable<T> chainIf(Iterable<T> items, Predicate<? super T> condition, UnaryOperator<T> nextItemExtractor) {
        return (items == null || nextItemExtractor == null) ? Chainable.from(items) : Chainable.fromIterator(() -> new Iterator<T>() {
            final Iterator<T> iter = items.iterator();
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
     * @return
     * @see Chainable#collectInto(Collection)
     */
    public static <T> Chainable<T> collectInto(Iterable<T> items, Collection<T> targetCollection) {
        return (items == null || targetCollection == null) ? Chainable.from(items) : Chainables.applyAsYouGo(items, o -> targetCollection.add(o));
    }

    /**
     * @param items
     * @param lister
     * @return
     * @see Chainable#concat(Function)
     */
    public static <T> Chainable<T> concat(Iterable<T> items, Function<? super T, Iterable<T>> lister) {
        return (lister == null || items == null) ? Chainable.from(items) : Chainable.fromIterator(() -> new Iterator<T>() {
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
            return Chainable.fromIterator(() -> new Iterator<T>() {
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
        return (Chainables.isNullOrEmpty(itemSequences)) ? Chainable.empty() : Chainable.fromIterator(() -> new Iterator<T>() {
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
        });
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
     * @see Chainable#count()
     */
    public static <T> long count(Iterable<T> items) {
        if (items == null) {
            return 0;
        } else if (items instanceof Collection<?>) {
            return ((Collection<?>)items).size();
        }

        Iterator<T> iter = items.iterator();
        long i = 0;
        for (i = 0; iter.hasNext(); i++) {
            iter.next();
        }

        return i;
    }

    /**
     * @param items
     * @param childTraverser
     * @return
     * @see Chainable#depthFirst(Function)
     */
    public static <T> Chainable<T> depthFirst(Iterable<T> items, Function<? super T, Iterable<T>> childTraverser) {
        return traverse(items, childTraverser, false);
    }

    /**
     * @param items
     * @param childTraverser
     * @param condition
     * @return
     * @see Chainable#depthFirstNotBelow(Function, Predicate)
     */
    public static <T> Chainable<T> depthFirstNotBelow(
            Iterable<T> items,
            Function<? super T, Iterable<T>> childTraverser,
            Predicate<? super T> condition) {
        return notBelow(items, childTraverser, condition, false);
    }

    /**
     * @param items items to sort
     * @return sorted items
     * @see Chainable#descending()
     */
    public static <T> Chainable<T> descending(Iterable<T> items) {
        return sorted(items, false);
    }

    /**
     * @param items
     * @param keyExtractor
     * @return
     * @see Chainable#descending(ToStringFunction)
     */
    public static <T> Chainable<T> descending(Iterable<T> items, ToStringFunction<? super T> keyExtractor) {
        return sortedBy(items, keyExtractor, false);
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
     * @return
     * @see Chainable#descending(ToLongFunction)
     */
    public static <T> Chainable<T> descending(Iterable<T> items, ToLongFunction<? super T> keyExtractor) {
        return sortedBy(items, keyExtractor, false);
    }

    /**
     * @param items
     * @param comparable
     * @return
     * @see Chainable#descending(ToDoubleFunction)
     */
    public static <T> Chainable<T> descending(Iterable<T> items, ToDoubleFunction<? super T> comparable) {
        return sortedBy(items, comparable, false);
    }

    /**
     * @param items
     * @param keyExtractor
     * @return
     * @see Chainable#distinct(Function)
     */
    public static <T, V> Chainable<T> distinct(Iterable<T> items, Function<? super T, V> keyExtractor) {
        return (keyExtractor == null) ? distinct(items) : Chainable.fromIterator(() -> new Iterator<T>() {
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
        });
    }

    /**
     * @param items
     * @return
     * @see Chainable#distinct()
     */
    public static <T> Chainable<T> distinct(Iterable<T> items) {
        return Chainable.fromIterator(() -> new Iterator<T>() {
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
        });
    }

    /**
     * @param items
     * @param suffix
     * @return
     * @see Chainable#endsWith(Iterable)
     */
    public static <T> boolean endsWith(Iterable<T> items, Iterable<T> suffix) {
        return Chainables.endsWithEither(items, suffix);
    }

    /**
     * @param items
     * @param suffixes
     * @return
     * @see Chainable#endsWithEither(Iterable...)
     */
    @SafeVarargs
    public static <T> boolean endsWithEither(Iterable<T> items, Iterable<T>...suffixes) {
        if (Chainables.isNullOrEmpty(items)) {
            return false;
        } else if (suffixes == null) {
            return false;
        }

        List<T> itemList = Chainables.toList(items);
        for (Iterable<T> suffix : suffixes) {
            // Check each suffix
            List<T> suffixSequence = Chainables.toList(suffix);
            if (suffixSequence.size() > itemList.size()) {
                // If different size, assume non-match and check the next suffix
                continue;
            }

            Iterator<T> suffixIter = suffixSequence.iterator();
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
     * Returns the first item from the specified items or {@code null} if no items.
     * @param items items to return the first item from
     * @return the first item
     * @see Chainable#first()
     */
    public static <T> T first(Iterable<T> items) {
        if (items == null) {
            return null;
        } else {
            Iterator<T> iter = items.iterator();
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
     * @return the first number of items
     * @see Chainable#first(long)
     */
    public static <T> Chainable<T> first(Iterable<T> items, long number) {
        return (items == null) ? Chainable.empty() : Chainable.fromIterator(() -> new Iterator<T>() {
            Iterator<T> iter = items.iterator();
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
     * Finds the first item satisfying the specified condition.
     * @param items
     * @param conditions
     * @return
     * @see Chainable#firstWhereEither(Predicate...)
     */
    @SafeVarargs
    public static <V> V firstWhereEither(Iterable<V> items, Predicate<? super V>... conditions) {
        if (items == null) {
            return null;
        } else if (conditions == null) {
            return Chainables.first(items);
        } else {
            for (V item : items) {
                for (Predicate<? super V> condition : conditions) {
                    if (condition.test(item)) {
                        return item;
                    }
                }
            }
        }

        return null;
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
            Deque<Iterator<T>> iters = new LinkedList<Iterator<T>>(Chainable
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
                Iterator<T> iter = this.iters.removeFirst();
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
     * @return
     * @see Chainable#get(long)
     */
    public static <T> T get(Iterable<T> items, long index) {
        return afterFirst(items, index).first();
    }

    /**
     * @param items
     * @param min
     * @return true if there are at least the specified {@code min} number of {@code items}, stopping the traversal as soon as that can be determined
     * @see Chainable#isCountAtLeast(long)
     */
    public static <T> boolean isCountAtLeast(Iterable<T> items, long min) {
        if (min <= 0) {
            return true;
        } else if (items == null) {
            return false;
        }

        Iterator<T> iter = items.iterator();
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
    public static <T> boolean isCountAtMost(Iterable<T> items, long max) {
        if (items == null && max >= 0) {
            return true;
        } else if (items == null) {
            return false;
        }

        Iterator<T> iter = items.iterator();
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
    public static <T> boolean isCountExactly(Iterable<T> items, long count) {
        if (items == null) {
            return count == 0;
        } else if (items instanceof Collection<?>) {
            return ((Collection<?>)items).size() == count;
        }

        Iterator<T> iter = items.iterator();
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
     * @param container
     * @param item
     * @return
     * @see Chainable#iterativeContains(Object)
     */
    @Experimental
    public static <T> Chainable<Boolean> iterativeContains(Iterable<T> container, T item) {
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
    public static <V> boolean isNullOrEmpty(Iterator<V> iterator) {
        return (iterator != null) ? !iterator.hasNext() : true;
    }

    /**
     * Joins the items produced by the specified {@code iterator} into a single string, invoking {@code toString()} on each item,
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
    public static <T> T last(Iterable<T> items) {
        T last = null;
        if (Chainables.isNullOrEmpty(items)) {
            // Skip
        } else if (items instanceof List<?>) {
            // If list, then faster lookup
            List<T> list = (List<T>)items;
            last = list.get(list.size() - 1);
        } else {
            // Else, slow lookup
            Iterator<T> iter = items.iterator();
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
    public static <T> Chainable<T> last(Iterable<T> items, long count) {
        return (items == null) ? Chainable.empty() : Chainable.fromIterator(() -> new Iterator<T>() {
            final List<T> list = Chainables.toList(items);
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
     */
    public static <T> String join(String delimiter, Iterable<T> items) {
        return join(delimiter, items.iterator());
    }

    /**
     * @param items
     * @param valueExtractor
     * @return
     * @see Chainable#max(Function)
     */
    public static <T> T max(Iterable<T> items, Function<? super T, Double> valueExtractor) {
        Double max = null;
        T maxItem = null;
        if (!Chainables.isNullOrEmpty(items)) {
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
    public static <T> T min(Iterable<T> items, Function<? super T, Double> valueExtractor) {
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
    public static <T> boolean noneWhere(Iterable<T> items, Predicate<? super T> condition) {
        return Chainables.noneWhereEither(items, condition);
    }

    /**
     * @param items
     * @param conditions
     * @return
     * @see Chainable#noneWhereEither(Predicate...)
     */
    @SafeVarargs
    public static <T> boolean noneWhereEither(Iterable<T> items, Predicate<? super T>... conditions) {
        return !Chainables.anyWhereEither(items, conditions);
    }

    /**
     * Returns items until and including the first item satisfying the specified condition, and no items after that
     * @param items items to return from
     * @param condition the condition that the last item needs to meet
     * @return items before and including the first item where the specified condition is satisfied
     * @see Chainable#notAfter(Predicate)
     */
    public static <T> Chainable<T> notAfter(Iterable<T> items, Predicate<? super T> condition) {
        if (items == null) {
            return null;
        } else if (condition == null) {
            return Chainable.from(items);
        }

        return Chainable.fromIterator(() -> new Iterator<T>() {
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
        });
    }

    /**
     * @param items
     * @param condition
     * @return
     * @see Chainable#notAsLongAs(Predicate)
     */
    public static <T> Chainable<T> notAsLongAs(Iterable<T> items, Predicate<? super T> condition) {
        return (items != null) ? notBefore(items, condition.negate()) : null;
    }

    /**
     * @param items
     * @param value
     * @return
     * @see Chainable#notAsLongAsValue(Object)
     */
    public static <T> Chainable<T> notAsLongAsValue(Iterable<T> items, T value) {
        return notBefore(items, o -> o!=value);
    }

    /**
     * @param items
     * @param condition
     * @return
     * @see Chainable#notBefore(Predicate)
     */
    //##
    static <T> Chainable<T> notBefore(Iterable<T> items, Predicate<? super T> condition) {
        if (items == null) {
            return null;
        } else if (condition == null) {
            return Chainable.from(items);
        } else {
            return Chainable.fromIterator(() -> new Iterator<T>() {
                final Iterator<T> iterator = items.iterator();
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
    public static <T> Chainable<T> notBeforeEquals(Iterable<T> items, T item) {
        return notBefore(items, (Predicate<T>)(o -> o == item));
    }

    private static <T> Chainable<T> notBelow(
            Iterable<T> items,
            Function<? super T, Iterable<T>> childTraverser,
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
    public static final <T> Chainable<T> notWhere(Iterable<T> items, Predicate<? super T> condition) {
        return (condition != null) ? Chainables.whereEither(items, condition.negate()) : Chainable.from(items);
    }

    @SuppressWarnings("unchecked")
    private static <T> Chainable<T> sorted(Iterable<T> items, boolean ascending) {
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

    private static <T> Chainable<T> sortedBy(Iterable<T> items, BiFunction<? super T, ? super T, Integer> comparator, boolean ascending) {
        Chainable<T> i = Chainable.from(items);
        if (items == null || comparator == null) {
            return i;
        }

        List<T> list = i.toList();
        list.sort(new Comparator<T>() {

            @Override
            public int compare(T o1, T o2) {
                return (ascending) ? comparator.apply(o1, o2) : comparator.apply(o2, o1);
            }
        });

        return Chainable.from(list);
    }

    private static <T> Chainable<T> sortedBy(Iterable<T> items, ToStringFunction<? super T> keyExtractor, boolean ascending) {
        Chainable<T> i = Chainable.from(items);
        if (keyExtractor == null) {
            return i;
        } else {
            return sortedBy(i, (o1, o2) -> Objects.compare(
                    keyExtractor.apply(o1),
                    keyExtractor.apply(o2),
                    Comparator.comparing(String::toString)), ascending);
        }
    }

    private static <T> Chainable<T> sortedBy(Iterable<T> items, ToLongFunction<? super T> keyExtractor, boolean ascending) {
        Chainable<T> i = Chainable.from(items);
        if (keyExtractor == null) {
            return i;
        } else {
            return sortedBy(i, (o1, o2) -> Long.compare(
                        keyExtractor.applyAsLong(o1),
                        keyExtractor.applyAsLong(o2)),
                    ascending);
        }
    }

    private static <T> Chainable<T> sortedBy(Iterable<T> items, ToDoubleFunction<? super T> keyExtractor, boolean ascending) {
        Chainable<T> i = Chainable.from(items);
        if (keyExtractor == null) {
            return i;
        } else {
            return sortedBy(i, (o1, o2) -> Double.compare(keyExtractor.applyAsDouble(o1), keyExtractor.applyAsDouble(o2)), ascending);
        }
    }

    /**
     * @param items
     * @param replacer
     * @return
     * @see Chainable#replace(Function)
     */
    public static <T> Chainable<T> replace(Iterable<T> items, Function<? super T, Iterable<T>> replacer) {
        return transformAndFlatten(items, replacer).withoutNull();
    }

    /**
     * @param items
     * @return
     * @see Chainable#reverse()
     */
    public static <T> Chainable<T> reverse(Iterable<T> items) {
        return (items == null) ? Chainable.empty() : Chainable.fromIterator(() -> new Iterator<T>() {
            List<T> list = Chainables.toList(items);
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

    /**
     * Splits the specified {@code text} into a individual characters/
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
     * @param text
     * @param delimiterCharacters
     * @return the split strings, including the delimiters
     */
    public static Chainable<String> split(String text, String delimiterCharacters) {
        return split(text, delimiterCharacters, true);
    }

    /**
     * Splits the specified {@code text} using the specified {@code delimiterChars}.
     * @param text
     * @param delimiterCharacters
     * @param includeDelimiters if true, the delimiter chars are included in the returned results
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
    public static <T> boolean startsWithEither(Iterable<T> items, Iterable<T>... prefixes) {
        if (Chainables.isNullOrEmpty(items)) {
            return false;
        } else if (prefixes == null) {
            return false;
        }

        for (Iterable<T> prefix : prefixes) {
            Iterator<T> prefixIterator = prefix.iterator();
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
    public static <T> long sum(Iterable<T> items, Function<? super T, Long> valueExtractor) {
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
     * @param items
     * @param keyExtractor
     * @return
     * @see Chainable#toMap(Function)
     */
    public static <K, V> Map<K, V> toMap(Iterable<V> items, Function<? super V, K> keyExtractor) {
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
    public static <T> ChainableQueue<T> toQueue(Iterable<T> items) {
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
    public static <I, O> Chainable<O> transform(Iterable<I> items, Function<? super I, O> transformer) {
        return (items == null || transformer == null) ? Chainable.empty() : Chainable.fromIterator(() -> new Iterator<O>() {
            Iterator<I> iterator = items.iterator();

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
     * @see Chainable#transformAndFlatten(Function)
     */
    public static <I, O> Chainable<O> transformAndFlatten(Iterable<I> items, Function<? super I, Iterable<O>> transformer) {
        return (items == null || transformer == null) ? Chainable.empty() : Chainable.fromIterator(() -> new Iterator<O>() {
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
        });
    }

    private static <T> Chainable<T> traverse(
            Iterable<T> initials,
            Function<? super T, Iterable<T>> childTraverser,
            boolean breadthFirst) {
        return (initials == null || childTraverser == null) ? Chainable.empty() : Chainable.fromIterator(() -> new Iterator<T>() {
            Deque<Iterator<T>> iterators = new LinkedList<>(Arrays.asList(initials.iterator()));
            T nextItem = null;
            Set<T> seenValues = new HashSet<>();

            @Override
            public boolean hasNext() {
                // TODO Trips up on NULL values "by design" - should it?
                while (this.nextItem == null && !iterators.isEmpty()) {
                    Iterator<T> currentIterator = iterators.peekFirst();
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
                Iterable<T> nextChildren = childTraverser.apply(this.nextItem);
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
    public static final <T> Chainable<T> whereEither(Iterable<T> items, Predicate<? super T>... predicates) {
        if (items == null) {
            return Chainable.empty();
        } else if (predicates == null || predicates.length == 0) {
            return Chainable.from(items);
        }

        return Chainable.fromIterator(() -> new Iterator<T>() {
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
    public static <T> Chainable<T> withoutNull(Iterable<T> items) {
        return (items != null) ? Chainable.from(items).where(i -> i != null) : null;
    }
}

