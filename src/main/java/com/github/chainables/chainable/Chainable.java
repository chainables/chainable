/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.chainables.chainable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

import com.github.chainables.annotation.Experimental;
import com.github.chainables.chainable.Chainables.Chain;
import com.github.chainables.function.ToStringFunction;

/**
 * {@link Chainable} is a fluent interface-style sub type of {@link java.lang.Iterable} with additional methods facilitating the use of the
 * iterator pattern, functional programming and lazy evaluation, intended for achieving code that is more succinct, readable, simpler to implement
 * and sometimes faster than its non-lazy/non-functional equivalent.
 * <p>
 * It is intended to be a rich, `Iterable`-based alternative to Java's `Stream` and Google's *guava*.
 * <p>
 * See the project's home site at http://www.github.com/chainables/chainable for more information.
 *
 * @author Martin Sawicki
 *
 * @param <T> the type of items in the chain
 */
public interface Chainable<T> extends Iterable<T> {
    /**
     * Returns an empty chain.
     * @return an empty chain
     * @chainables.similar
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
     * Returns an empty chain whose items are expected to be of the specified {@code clazz} type.
     * @param clazz the expected type of the items in the chain
     * @return an empty chain expected to contain items of the specified {@code clazz} type.
     */
    static <T> Chainable<T> empty(Class<T> clazz) {
        return empty().cast(clazz);
    }

    /**
     * Creates a new chain from the specified {@code items} in a "lazy" fashion, not traversing/evaluating the items,
     * just holding internal references to them.
     * @param items the items to create the chain from
     * @return a chain for the specified {@code items}
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link Collection#stream()} but operating on {@link Iterable}, so not requiring a {@link Collection} as its starting point</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.AsEnumerable()}</td></tr>
     * </table>
     */
    static <T> Chainable<T> from(Iterable<? extends T> items) {
        return Chain.from(items);
    }

    /**
     * Creates a new chain such that its iterator is a new iterator instance created by the specified {@code iteratorSupplier}
     * @param iteratorSupplier an iterator supplying function
     * @return the resulting chain
     */
    static <T> Chainable<T> fromIterator(Supplier<Iterator<T>> iteratorSupplier) {
        return Chain.from(iteratorSupplier);
    }

    /**
     * Creates a new chain from the specified {@code items} array,
     * @param items the items to create a chain from
     * @return an {@link Chainable} wrapper for the specified {@code items}
     * @chainables.similar
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
     * Creates a new chain from the specified {@code stream} that supports multiple traversals, just like a
     * standard {@link java.lang.Iterable}, even though the underlying {@link java.util.stream.Stream} does not.
     * <p>
     * Note that upon subsequent traversals of the chain, none of the items in the original stream are evaluated twice,
     * but rather their values are cached internally and used for any subsequent traversals, even if previous traversals of the chain
     * were incomplete.
     * @param stream the stream to create a chain from
     * @return a chain based on the specified {@code stream}
     */
    static <T> Chainable<T> from(Stream<? extends T> stream) {
        if (stream == null) {
            return Chainable.empty();
        }

        return Chainable.from(new Iterable<T>() {

            List<T> cache = new ArrayList<>();
            Iterator<? extends T> streamIter = stream.iterator();

            @Override
            public Iterator<T> iterator() {
                if (this.streamIter == null) {
                    return this.cache.iterator();
                } else {
                    return new Iterator<T>() {
                        int nextCacheIndex = 0;

                        @Override
                        public boolean hasNext() {
                            if (cache.size() > this.nextCacheIndex) {
                                return true;
                            } else if (streamIter.hasNext()) {
                                return true;
                            } else {
                                streamIter = null;
                                return false;
                            }
                        }

                        @Override
                        public T next() {
                            if (cache.size() > this.nextCacheIndex) {
                                return cache.get(this.nextCacheIndex++);
                            } else if (streamIter == null) {
                                return null;
                            } else {
                                T next = streamIter.next();
                                cache.add(next);
                                this.nextCacheIndex++;
                                return next;
                            }
                        }
                    };
                }
            }
        });
    }

    /**
     * Splits the specified {@code text} into a individual characters.
     * @param text the text to split
     * @return a chain of characters
     * @see #split(String, String)
     * @see #split(String, String, boolean)
     * @see #join()
     */
    static Chainable<String> split(String text) {
        return Chainables.split(text);
    }

    /**
     * Splits the specified {@code text} using the specified {@code delimiterChars}.
     * <p>
     * Each of the characters in the specified {@code delimiterCharacters} is used as a separator individually.
     * @param text the text to split
     * @param delimiterCharacters the characters to use to split the specified {@code text}
     * @return the split strings, including the delimiters
     * @see #split(String)
     * @see #split(String, String, boolean)
     * @see #join()
     */
    static Chainable<String> split(String text, String delimiterCharacters) {
        return Chainables.split(text, delimiterCharacters);
    }

    /**
     * Splits the specified {@code text} using the specified {@code delimiterChars}, optionally including the delimiter characters in the
     * resulting chain.
     * <p>
     * Each of the characters in the specified {@code delimiterCharacters} is used as a separator individually.
     * @param text the text to split
     * @param delimiterCharacters the characters to use to split the specified {@code text}
     * @param includeDelimiters if {@code true}, the delimiter chars are included in the returned results, otherwise they're not
     * @return the split strings
     * @see #split(String)
     * @see #split(String, String)
     * @see #join()
     */
    static Chainable<String> split(String text, String delimiterCharacters, boolean includeDelimiters) {
        return Chainables.split(text, delimiterCharacters, includeDelimiters);
    }

    /**
     * Returns a chain of items after the first one in this chain.
     * @return items following the first one
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#skip(long)} with 1 as the number to skip</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.Skip()}</td></tr>
     * </table>
     */
    default Chainable<T> afterFirst() {
        return Chainables.afterFirst(this);
    }

    /**
     * Returns a chain after skipping the first specified number of items.
     * @param number the number of initial items to skip
     * @return the remaining chain
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#skip(long)}</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.Skip()}</td></tr>
     * </table>
     */
    default Chainable<T> afterFirst(long number) {
        return Chainables.afterFirst(this, number);
    }

    /**
     * Determines whether all the items in this chain satisfy the specified {@code condition}.
     * @param condition the condition for all the items to satisfy
     * @return {@code true} if all items satisfy the specified {@code condition}, otherwise {@code false}
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#allMatch(Predicate)}</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.All()}</td></tr>
     * </table>
     */
    default boolean allWhere(Predicate<? super T> condition) {
        return Chainables.allWhere(this, condition);
    }

    /**
     * Determines whether all the items in this chain satisfy any of the specified {@code conditions}
     * @param conditions the choice of conditions for the items to satisfy
     * @return {@code true} if all items satisfy at least one of the {@code conditions}, otherwise {@code false}
     * @see #allWhere(Predicate)
     */
    @SuppressWarnings("unchecked")
    default boolean allWhereEither(Predicate<? super T>...conditions) {
        return Chainables.allWhereEither(this, conditions);
    }

    /**
     * Determines whether this chain contains any items.
     * @return {@code true} if not empty (i.e. the opposite of {@link #isEmpty()})
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.Any()}</td></tr>
     * </table>
     */
    default boolean any() {
        return !Chainables.isNullOrEmpty(this);
    }

    /**
     * Determines whether any of the items in this chain satisfy the specified {@code condition}.
     * @param condition the condition to satisfy
     * @return {@code true} if there are any items that satisfy the specified {@code condition}
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#anyMatch(Predicate)}</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.Any(Func)}</td></tr>
     * </table>
     */
    default boolean anyWhere(Predicate<? super T> condition) {
        return Chainables.anyWhere(this, condition);
    }

    /**
     * Determines whether any of the items in this chain satisfy any of the specified {@code conditions}.
     * @param conditions the conditions to satisfy
     * @return true if there are any items that satisfy any of the specified {@code conditions}
     * @see #anyWhere(Predicate)
     */
    @SuppressWarnings("unchecked")
    default boolean anyWhereEither(Predicate<? super T>... conditions) {
        return Chainables.anyWhereEither(this, conditions);
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
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#forEach(Consumer)}</td></tr>
     * </table>
     */
    default Chainable<T> apply(Consumer<? super T> action) {
        return Chainables.apply(this, action);
    }

    /**
     * Applies the specified {@code action} to each item one by one lazily, that is without triggering a full
     * evaluation of the entire chain, but only to the extent that the returned chain is evaluated using another function.
     * @param action
     * @return self
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#peek(Consumer)}</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.Select()}</td></tr>
     * </table>
     * @see #apply()
     * @see #apply(Consumer)
     */
    default Chainable<T> applyAsYouGo(Consumer<? super T> action) {
        return Chainables.applyAsYouGo(this, action); // TODO: shouldn't this call applyAsYouGo?
    }

    /**
     * Sorts in the ascending order by an automatically detected key based on the first item in the chain.
     * <P>
     * If the item type in the chain is {@link String}, or {@link Double}, or {@link Long}, then the value is used as the key.
     * For other types, the return value of {@code toString()} is used as the key.
     * <P>
     * Note this triggers a full traversal/evaluation of the chain.
     * @return sorted items
     * @see #descending()
     */
    default Chainable<T> ascending() {
        return Chainables.ascending(this);
    }

    /**
     * Sorts the items in this chain in the ascending order based on the {@link String}
     * keys returned by the specified {@code keyExtractor} applied to each item.
     * <P>
     * Note this triggers a full traversal/evaluation of the chain.
     * @param keyExtractor
     * @return sorted items
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#sorted(Comparator)}, but specific to {@link String} outputs</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.ThenBy()}</td></tr>
     * </table>
     * @see #descending(ToStringFunction)
     */
    default Chainable<T> ascending(ToStringFunction<? super T> keyExtractor) {
        return Chainables.ascending(this, keyExtractor);
    }

    /**
     * Sorts the items in this chain in the ascending order based on the {@link Long}
     * keys returned by the specified {@code keyExtractor} applied to each item.
     * <P>
     * Note this triggers a full traversal/evaluation of the chain.
     * @param keyExtractor
     * @return sorted items
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#sorted(Comparator)}, but specific to {@link Long} outputs</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.ThenBy()}</td></tr>
     * </table>
     * @see #descending(ToLongFunction)
     */
    default Chainable<T> ascending(ToLongFunction<? super T> keyExtractor) {
        return Chainables.ascending(this, keyExtractor);
    }

    /**
     * Sorts the items in this chain in the ascending order based on the {@link Double}
     * keys returned by the specified {@code keyExtractor} applied to each item.
     * <P>
     * Note this triggers a full traversal/evaluation of the chain.
     * @param keyExtractor
     * @return sorted items
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#sorted(Comparator)}, but specific to {@link Double} outputs</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.ThenBy()}</td></tr>
     * </table>
     * @see #descending(ToDoubleFunction)
     */
    default Chainable<T> ascending(ToDoubleFunction<? super T> keyExtractor) {
        return Chainables.ascending(this, keyExtractor);
    }

    /**
     * Returns a chain of the initial items from this chain that satisfy the specified {@code condition}, stopping before the first item that does not.
     * <p>
     * For example, if the chain consists of { 1, 3, 5, 6, 7, 9, ...} and the {@code condition} checks for the oddity of each number,
     * then the returned chain will consist of only { 1, 3, 5 }
     * @param condition
     * @return items <i>before</i> the first one that fails the specified {@code condition}
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.TakeWhile()}</td></tr>
     * </table>
     * @see #asLongAsEquals(Object)
     */
    default Chainable<T> asLongAs(Predicate<? super T> condition) {
        return (condition == null) ? this : this.before(condition.negate());
    }

    /**
     * Returns a chain of the initial items from this chain that are equal to the specified {@code value}, stopping before the first item that is not.
     * <p>
     * For example, if the chain consists of { 1, 1, 2, 1, 1, ...} and the {@code value} is 1 then the returned chain will be { 1, 1 }.
     * @param value value to match
     * @return items <i>before</i> the first one that is not equal to the specified {@code value}
     * @see #asLongAs(Predicate)
     */
    default Chainable<T> asLongAsEquals(T value) {
        return Chainables.asLongAsEquals(this, value);
    }

    /**
     * Returns a chain of items from this chain that are of the same type as the specified {@code example}.
     * <p>
     * For example, consider a mixed collection of super-classes and subclasses, or hybrid interfaces.
     * @param example
     * @return only those items that are of the same type as the specified {@code example}
     */
    default <O> Chainable<O> ofType(O example) {
        return Chainables.ofType(this, example);
    }

    /**
     * Returns a chain of initial items from this chain before the first one that satisfies the specified {@code condition}.
     * <p>
     * For example, if this chain consists of { 1, 3, 2, 5, 6 } and the {@code condition} returns {@code true} for even numbers, then the resulting chain
     * will consist of { 1, 3 }.
     * @param condition
     * @return the initial items before and not including the one that meets the specified {@code condition}
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.TakeWhile(), but with a negated predicate}</td></tr>
     * </table>
     * @see #notBefore(Predicate)
     * @see #notAsLongAs(Predicate)
     * @see #asLongAs(Predicate)
     * @see #notAfter(Predicate)
     */
    default Chainable<T> before(Predicate<? super T> condition) {
        return Chainables.before(this, condition);
    }

    /**
     * Returns a chain of initial items from this chain before the specified {@code value}.
     * @param item
     * @return the initial items until one is encountered that is the same as the specified {@code item}
     * @see #before(Predicate)
     */
    default Chainable<T> beforeValue(T item) {
        return Chainables.beforeValue(this, item);
    }

    /**
     * Traverses the items in this chain in a breadth-first order as if it were a tree, where for each item, the items output by the specified
     * {@code childExtractor} applied to it are appended at the end of the chain.
     * <p>
     * To indicate the absence of children for an item, the child extractor may output {@code null}.
     * <p>
     * The traversal protects against potential cycles by not visiting items that satisfy the equality ({@code equals()})
     * check against an item already seen before.
     * @param childExtractor
     * @return resulting chain
     * @see #breadthFirstNotBelow(Function, Predicate)
     * @see #breadthFirstAsLongAs(Function, Predicate)
     * @see #depthFirst(Function)
     */
    default Chainable<T> breadthFirst(Function<? super T, Iterable<T>> childExtractor) {
        return Chainables.breadthFirst(this, childExtractor);
    }

    /**
     * Traverses the items in this chain in a breadth-first order as if it were a tree, where for each item, the items output by the specified
     * {@code childExtractor} applied to it are appended at the end of the chain, <i>up to and including</i> the parent item that satisfies the specified
     * {@code condition}, but not its descendants that would be otherwise returned by the {@code childExtractor}.
     * <p>
     * It can be thought of trimming the breadth-first traversal of a hypothetical tree right below the level of the item satisfying
     * the {@code condition}, but continuing with other items in the chain.
     * <p>
     * To indicate the absence of children for an item, the child extractor may output {@code null}.
     * <p>
     * The traversal protects against potential cycles by not visiting items that satisfy the equality ({@code equals()}) check against an item already seen before.
     * @param childExtractor
     * @param condition
     * @return resulting chain
     * @see #breadthFirstAsLongAs(Function, Predicate)
     * @see #breadthFirst(Function)
     * @see #depthFirst(Function)
     */
    default Chainable<T> breadthFirstNotBelow(Function<? super T, Iterable<T>> childExtractor, Predicate<? super T> condition) {
        return Chainables.breadthFirstNotBelow(this, childExtractor, condition);
    }

    /**
     * Traverses the items in this chain in a breadth-first order as if it's a tree, where for each item, only those of its children returned by
     * the specified {@code childTraverser} are appended to the end of the chain that satisfy the specified {@code condition}.
     * <p>
     * It can be thought of trimming the breadth-first traversal of a hypothetical tree right above the level of each item satisfying
     * the {@code condition}, but continuing with other items in the chain.
     * @param childExtractor
     * @param condition
     * @return resulting chain
     * @see #breadthFirstNotBelow(Function, Predicate)
     * @see #breadthFirst(Function)
     * @see #depthFirst(Function)
     */
    default Chainable<T> breadthFirstAsLongAs(Function<? super T, Iterable<T>> childExtractor, Predicate<? super T> condition) {
        return Chainables.breadthFirstAsLongAs(this, childExtractor, condition);
    }

    /**
     * Creates a chain that caches its evaluated items, once a full traversal is completed, so that subsequent traversals no longer
     * re-evaluate each item but fetch them directly from the cache populated by the first traversal.
     * <p>
     * Note that if the first traversal is only partial (i.e. it does not reach the end) the cache is not yet activated, so the next traversal will still
     * re-evaluate each item from the beginning, as if run for the first time.
     * <p>
     * If there are multiple iterators used from the chain, the first iterator to complete the traversal wins as far as cache population goes.
     * The remaining iterators will continue unaffected, but their results, if different from the results of the first finished
     * iterator, will be ignored for caching purposes.
     * @return a chain that, upon the completion of the first full traversal, behaves like a fixed value list
     */
    default Chainable<T> cached() {
        return Chainables.cached(this);
    }

    /**
     * Casts the items in this chain to the specified class.
     * @param clazz
     * @return items as cast to the type indicated by the specified {@code clazz}
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#map(Function)}, where the specified function casts each item to the specified type</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.Cast()}</td></tr>
     * </table>
     */
    default <T2> Chainable<T2> cast(Class<T2> clazz) {
        return Chainables.cast(this, clazz);
    }

    /**
     * Appends to the chain the result of the specified {@code nextItemExtractor} applied to the last item, unless the last item is null.
     * <p>
     * If the {@code nextItemExtractor} returns {@code null}, that is considered as the end of the chain and is not included in the resulting chain.
     * <p>
     * If applied to an empty chain, the behavior is the same as if applied to a chain where the last value is {@code null}, which means that if
     * the extractor returns null as a result as well, then the resulting chain is still de-facto empty. But the extractor can use this {@code null} as
     * an opportunity to create a non-empty chain out of an empty one.
     * @param nextItemExtractor a function returning the next item given the item it is fed, or null if it is the first item
     * @return resulting chain
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#iterate(Object, java.util.function.UnaryOperator)}, except that
     * the "seed" is just the last item of the underlying chain, or {@code null} if empty.</td></tr>
     * </table>
     */
    default Chainable<T> chain(UnaryOperator<T> nextItemExtractor) {
        return Chainables.chain(this, nextItemExtractor);
    }

    /**
     * Appends to the chain the result of the specified {@code nextItemExtractorFromLastTwo} applied to the last two items, passed to the function
     * chronological order.
     * <p>
     * If the {@code nextItemExtractorFromLastTwi} returns {@code null}, that is considered as the end of the chain and is not included in the resulting chain.
     * <p>
     * If applied to an empty chain, the behavior is the same as if applied to a chain where the last two values are {@code null}, which means that if
     * the extractor returns {@code null} as a result as well, then the resulting chain is still de-facto empty. But the extractor can use this {@code null} as
     * an opportunity to create a non-empty chain out of an empty one.
     * <p>
     * If applied to a chain that contains only one item, then the first argument to the {@code nextItemExtractorFromLastTwo}, which represents the earlier of
     * the last two items, will be passed as {@code null}.
     * @param nextItemExtractorFromLastTwo
     * @return the resulting chain
     */
    default Chainable<T> chain(BinaryOperator<T> nextItemExtractorFromLastTwo) {
        return Chainables.chain(this, nextItemExtractorFromLastTwo);
    }

    /**
     * Works the same way as {@link #chain(UnaryOperator)}, except that the specified {@code nextItemExtractor} will also be fed its index in the chain,
     * starting with 0.
     * @param nextItemExtractor a function returning the next item given the item it is fed or null if it is the first item, and its index in the chain,
     * startint with 0
     * @return the resulting chain
     */
    default Chainable<T> chainIndexed(BiFunction<T, Long, T> nextItemExtractor) {
        return Chainables.chainIndexed(this, nextItemExtractor);
    }

    /**
     * If the last of this chain satisfies the specified {@code condition}, then the result of the specified {@code nextItemExtractor}
     * applied to that last item is appended to the chain.
     * @param condition
     * @param nextItemExtractor
     * @return resulting {@link Chainable}
     * @see #chain(UnaryOperator)
     */
    default Chainable<T> chainIf(Predicate<T> condition, UnaryOperator<T> nextItemExtractor) {
        return Chainables.chainIf(this, condition, nextItemExtractor);
    }

    /**
     * Collects all the items into the specified collection.
     * @param targetCollection
     * @return self
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>Note this is NOT like {@link java.util.stream.Stream#collect(java.util.stream.Collector)}</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.ToList()} and the like</td></tr>
     * </table>
     */
    default Chainable<T> collectInto(Collection<T> targetCollection) {
        return Chainables.collectInto(this, targetCollection);
    }

    /**
     * Appends the specified {@code items} to this chain.
     * @param items
     * @return the chain resulting from appending the specified {@code items} to this chain
     * @chainables.similar
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
     * @param iterables
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
     * @chainables.similar
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
     */
    default Chainable<T> concat(Function<? super T, Iterable<T>> lister) {
        return Chainables.concat(this, lister);
    }

    /**
     * Determines whether this chain contains the specified {@code item}.
     * @param item the item to look for
     * @return {@code true} if this contains the specified {@code item}
     * @chainables.similar
     * <table summary="Similar to:">
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
     * Counts the items in this chain.
     * <p>
     * This triggers a full traversal/evaluation of the items. If the expected number, maximum or minimum is known and the goal is only to
     * confirm the expectation, it should be generally more efficient to use {@link #isCountAtLeast(long)}, {@link #isCountAtMost(long)} or {{@link #isCountExactly(long)}
     * for that purpose, especially if the chain is defined dynamically/functionally and is potentially infinite.
     * @return total number of items
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#count()}</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.Count()}</td></tr>
     * </table>
     * @see #isCountAtLeast(long)
     * @see #isCountAtMost(long)
     * @see #isCountExactly(long)
     */
    default long count() {
        return Chainables.count(this);
    }

    /**
     * Traverses the items in a depth-first (pre-order) manner, by visiting the children of each item in the chain, as returned by the
     * specified {@code childExtractor} before visting its siblings, in a de-facto recursive manner.
     * <p>
     * For items that do not have children, the {@code childExtractor} may return {@code null}.
     * <p>
     * Note that the traversal protects against potential cycles by not visiting items that satisfy the equality ({@code equals()}) check against an item already seen before.
     * @param childExtractor
     * @return resulting chain
     * @see #breadthFirst(Function)
     */
    default Chainable<T> depthFirst(Function<T, Iterable<T>> childExtractor) {
        return Chainables.depthFirst(this, childExtractor);
    }

    /**
     * Traverses the items in this chain in a depth-first (pre-order) order as if it were a tree, where for each item, the items output by the specified
     * {@code childExtractor} applied to it are inserted at the beginning of the chain, <i>up to and including</i> the parent item that satisfies
     * the specified {@code condition}, but not its descendants that would be otherwise returned by the {@code childExtractor}.
     * <p>
     * It can be thought of trimming the depth-first traversal of a hypothetical tree right below the level of the item satisfying
     * the {@code condition}, but continuing with other items in the chain.
     * <p>
     * To indicate the absence of children for an item, the child extractor may output {@code null}.
     * <p>
     * The traversal protects against potential cycles by not visiting items that satisfy the equality ({@code equals()}) check against
     * an item already seen before.
     * @param childExtractor
     * @param condition
     * @return resulting chain
     */
    default Chainable<T> depthFirstNotBelow(Function<? super T, Iterable<T>> childExtractor, Predicate<? super T> condition) {
        return Chainables.depthFirstNotBelow(this, childExtractor, condition);
    }

    /**
     * Sorts in the descending order by an automatically detected key based on the first item in the chain.
     * <P>
     * If the item type in the chain is {@link String}, or {@link Double}, or {@link Long}, then the value is used as the key.
     * For other types, the return value of {@code toString()} is used as the key.
     * <P>
     * Note this triggers a full traversal/evaluation of the chain.
     * @return sorted items
     * @see #ascending()
     */
    default Chainable<T> descending() {
        return Chainables.descending(this);
    }

    /**
     * Sorts the items in this chain in the descending order based on the {@link Long}
     * keys returned by the specified {@code keyExtractor} applied to each item.
     * <P>
     * Note this triggers a full traversal/evaluation of the chain.
     * @param keyExtractor
     * @return sorted items
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#sorted(Comparator)}, but specific to {@link Long} outputs</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.ThenByDescending()}</td></tr>
     * </table>
     * @see #ascending(ToLongFunction)
     */
    default Chainable<T> descending(ToLongFunction<? super T> keyExtractor) {
        return Chainables.descending(this, keyExtractor);
    }

    /**
     * Sorts the items in this chain in the descending order based on the {@link Double}
     * keys returned by the specified {@code keyExtractor} applied to each item.
     * <P>
     * Note this triggers a full traversal/evaluation of the chain.
     * @param keyExtractor
     * @return sorted items
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#sorted(Comparator)}, but specific to {@link Double} outputs</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.ThenByDescending()}</td></tr>
     * </table>
     * @see #ascending(ToDoubleFunction)
     */
    default Chainable<T> descending(ToDoubleFunction<? super T> keyExtractor) {
        return Chainables.descending(this, keyExtractor);
    }

    /**
     * Sorts the items in this chain in the descending order based on the {@link String}
     * keys returned by the specified {@code keyExtractor} applied to each item.
     * <P>
     * Note this triggers a full traversal/evaluation of the chain.
     * @param keyExtractor
     * @return sorted items
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#sorted(Comparator)}, but specific to {@link String} outputs</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.ThenByDescending()}</td></tr>
     * </table>
     * @see #ascending(ToStringFunction)
     */
    default Chainable<T> descending(ToStringFunction<? super T> keyExtractor) {
        return Chainables.descending(this, keyExtractor);
    }

    /**
     * Returns a chain of items from this chain that are not duplicated.
     * @return items that are unique (no duplicates)
     * @chainables.similar
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
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.Distinct()} with a custom comparer</td></tr>
     * </table>
     */
    default <V> Chainable<T> distinct(Function<? super T, V> keyExtractor) {
        return Chainables.distinct(this, keyExtractor);
    }

    /**
     * Determines whether this chain ends with the members of the specified {@code suffix} in the specific order they are returned.
     * <p>
     * This triggers a full traversal/evaluation of the chain.
     * @param suffix items to match to the end of the chain
     * @return {@code true} if this chain ends with the specified {@code suffix}
     * @see #endsWithEither(Iterable...)
     * @see #startsWith(Iterable)
     */
    default boolean endsWith(Iterable<T> suffix) {
        return Chainables.endsWith(this, suffix);
    }

    /**
     * Determines whether this chain ends with any of the specified {@code suffixes}.
     * <p>
     * This triggers a full traversal/evaluation of the chain.
     * @param suffixes
     * @return {@code true} if this ends with any one of the specified {@code suffixes} of items in its specific order
     * @see #endsWith(Iterable)
     */
    @SuppressWarnings("unchecked")
    default boolean endsWithEither(Iterable<T>...suffixes) {
        return Chainables.endsWithEither(this, suffixes);
    }

    /**
     * Determines whether this chain consists of the same items, in the same order, as those in the specified {@code items}, triggering a full traversal/evaluation of the chain if needed.
     * @param items
     * @return {@code true} the items match exactly
     * @chainables.similar
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
     * Returns the first item in the chain.
     * @return the first item or {@code null} if none
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#findFirst()}</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.FirstOrDefault()}</td></tr>
     * </table>
     */
    default T first() {
        return Chainables.first(this);
    }

    /**
     * Returns the first {@code count} of items in this chain.
     * @param count
     * @return the specified {@code count} of items from the beginning
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#limit(long)}</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.Take()}</td></tr>
     * </table>
     */
    default Chainable<T> first(long count) {
        return Chainables.first(this, count);
    }

    /**
     * Returns the first item satisfying the specified {@code condition}, or {@code null} if none.
     * @param condition the condition for the returned item to satisfy
     * @return the first item satisfying the specified {@code condition}, or {@code null} if none.
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>a combination of {@link java.util.stream.Stream#filter(Predicate)} and {@link java.util.stream.Stream#findFirst()}</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.FirstOrDefault()}</td></tr>
     * </table>
     * @see #firstWhereEither(Predicate...)
     */
    default T firstWhere(Predicate<? super T> condition) {
        return Chainables.firstWhereEither(this, (T)null, condition);
    }

    /**
     * Returns the first item satisfying the specified {@code condition}, or {@code defaultValue} if none.
     * @param condition the condition for the returned item to satisfy
     * @param defaultValue the value to return if no item satisfying the specified {@code condition} was found.
     * @return the first item satisfying the specified {@code condition}, or {@code defaultValue} if none.
     */
    default T firstWhere(Predicate<? super T> condition, T defaultValue) {
        return Chainables.firstWhereEither(this, defaultValue, condition);
    }

    /**
     * Returns the first item satisfying any of the specified {@code conditions} or {@code null} if none found.
     * @param conditions the conditions any of which the returned item must satisfy
     * @return the first item satisfying any of the specified {@code conditions} or {@code null} if none
     * @see #firstWhere(Predicate)
     */
    @SuppressWarnings("unchecked")
    default T firstWhereEither(Predicate<? super T>... conditions) {
        return Chainables.firstWhereEither(this, conditions);
    }

    /**
     * Returns the first item satisfying any of the specified {@code conditions} or {@code defaultValue} if none found.
     * @param defaultValue the value to return if no item satisfying any of the specified {@code conditions} has been found
     * @param conditions the conditions any of which the returned item must satisfy
     * @return the first item satisfying any of the specified {@code conditions} or {@code defaultValue} if none
     */
    @SuppressWarnings("unchecked")
    default T firstWhereEither(T defaultValue, Predicate<? super T>... conditions) {
        return Chainables.firstWhereEither(this, defaultValue, conditions);
    }

    /**
     * Fetches the item in the chain at the specified {@code index}, traversing/evaluating the chain as needed until that index is reached.
     * @param index the index of the item to retrieve from this chain
     * @return the item at the specified index
     */
    default T get(long index) {
        return Chainables.get(this, index);
    }

    /**
     * Interleaves the items of the specified {@code iterables}.
     * <p><b>Example:</b>
     * <table summary="Example:">
     * <tr><td>{@code items1}:</td><td>1, 3, 5</td></tr>
     * <tr><td>{@code items2}:</td><td>2, 4, 6</td></tr>
     * <tr><td><i>result:</i></td><td>1, 2, 3, 4, 5, 6</td></tr>
     * </table>
     * @param iterables iterables to merge by interleaving
     * @return items from the interleaved merger of the specified {@code iterables}
     */
    @SuppressWarnings("unchecked")
    default Chainable<T> interleave(Iterable<T>...iterables) {
        return Chainables.interleave(this, iterables);
    }

    /**
     * Determines whether this chain contains at least the specified {@code min} number of items, stopping the traversal as soon as that can be determined.
     * @param min
     * @return {@code true} if there are at least the specified {@code min} number of items in this chain
     * @see #isCountAtMost(long)
     * @see #isCountExactly(long)
     * @see #count()
     */
    default boolean isCountAtLeast(long min) {
        return Chainables.isCountAtLeast(this, min);
    }

    /**
     * Determines whether this chain contains no more than the specified {@code max} number of items, stopping the traversal as soon as that can be determined.
     * @param max
     * @return {@code true} if there are at most the specified {@code max} number of items
     * @see #isCountAtLeast(long)
     * @see #isCountExactly(long)
     * @see #count()
     */
    default boolean isCountAtMost(long max) {
        return Chainables.isCountAtMost(this, max);
    }

    /**
     * Checks lazily whether this chain has exactly the specified {@code number} of items.
     * <p>
     * If here are more items than the expecte number, the traversal/evaluation of the chain will stop right after the first item past the expected number.
     * @param number the expected number of items
     * @return {@code true} if the number of items in the chain is equal exactly to the specified {@code number}
     * @see #isCountAtLeast(long)
     * @see #isCountAtMost(long)
     * @see #count()
     */
    default boolean isCountExactly(long number) {
        return Chainables.isCountExactly(this, number);
    }

    /**
     * Determines whether this chain contains any items.
     * @return {@code true} if empty, else {@code false}
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.Any()}, but negated</td></tr>
     * </table>
     * @see #any()
     */
    default boolean isEmpty() {
        return Chainables.isNullOrEmpty(this);
    }

    /**
     * Enables the item existence check to be performed iteratively, emitting {@code null} values as long as the item is not <i>yet</i> found,
     * and ultimately emitting either {@code true} if the item is found, or otherwise {@code false} if the end has been reached.
     * @param item item to search for
     * @return a {@link Chainable} consisting of {@code null} values as long as the search is not completed, and ultimately either {@code true} or {@code false}
     * @see #contains(Object)
     */
    @Experimental
    default Chainable<Boolean> iterativeContains(T item) {
        return Chainables.iterativeContains(this, item);
    }

    /**
     * Joins all the members of the chain into a string with no delimiters, calling each member's {@code toString()} method.
     * @return the merged string
     * @see #join(String)
     */
    default String join() {
        return Chainables.join("", this);
    }

    /**
     * Joins all the members of the chain into a string with the specified {@code delimiter}, calling each member's {@code toString()} method.
     * @param delimiter the delimiter to insert between the members
     * @return the resulting string
     * @see #join()
     */
    default String join(String delimiter) {
        return Chainables.join(delimiter, this);
    }

    /**
     * Returns the last item in this chain.
     * <p>
     * This triggers a full traversal/evaluation of all the items.
     * @return the last item
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.LastOrDefault()}</td></tr>
     * </table>
     */
    default T last() {
        return Chainables.last(this);
    }

    /**
     * Returns the last {@code count} items from the end of this chain.
     * <p>
     * This triggers a full tarversal/evaluation of all the items.
     * @param count number of items to return from the end
     * @return up to the specified {@code count} of items from the end (or fewer if the chain is shorter than that)
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.TakeLast()}</td></tr>
     * </table>
     */
    default Chainable<T> last(int count) {
        return Chainables.last(this, count);
    }

    /**
     * Returns the item tha has the highest value extracted by the specified {@code valueExtractor} in this chain.
     * <p>
     * This triggers a full traversal/evaluation of the items.
     * @param valueExtractor
     * @return the item for which the specified {@code valueExtrator} returns the highest value
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#max(Comparator)}</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.Max()}</td></tr>
     * </table>
     * @see #min(Function)
     */
    default T max(Function<? super T, Double> valueExtractor) {
        return Chainables.max(this, valueExtractor);
    }

    /**
     * Returns the item that has the lowest value extracted by the specified {@code valueExtractor} in this chain.
     * <p>
     * This triggers a full traversal/evaluation of the items.
     * @param valueExtractor
     * @return the item for which the specified {@code valueExtrator} returns the lowest value
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#min(Comparator)}</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.Min()}</td></tr>
     * </table>
     * @see #max(Function)
     */
    default T min(Function<? super T, Double> valueExtractor) {
        return Chainables.min(this, valueExtractor);
    }

    /**
     * Determines whether none of the items in this chain satisfy the specified {@code condition}.
     * @param condition
     * @return {@code true} if there are no items that meet the specified {@code condition}
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#noneMatch(Predicate)}</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.Where()}, but with a negated predicate</td></tr>
     * </table>
     * @see #noneWhereEither(Predicate...)
     */
    default boolean noneWhere(Predicate<? super T> condition) {
        return Chainables.noneWhere(this, condition);
    }

    /**
     * Determines whether none of the items in this chain satisfy any of the specified {@code conditions}.
     * @param conditions
     * @return {@code true} if there are no items that meet any of the specified {@code conditions}
     * @see #noneWhere(Predicate)
     */
    @SuppressWarnings("unchecked")
    default boolean noneWhereEither(Predicate<? super T>... conditions) {
        return Chainables.noneWhereEither(this, conditions);
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
    default Chainable<T> notAfter(Predicate<? super T> condition) {
        return Chainables.notAfter(this, condition);
    }

    /**
     * Returns the remaining items from this chain starting with the first one that does NOT meet the specified {@code condition}.
     * <p>
     * For example, if the chain consists of { 1, 3, 5, 2, 7, 9, ... } and the {@code condition} returns {@code true} for odd numbers,
     * then the resulting chain will be { 2, 7, 9, ... }.
     * @param condition
     * @return items starting with the first one where the specified {@code condition} is no longer met
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.SkipWhile()}</td></tr>
     * </table>
     * @see #notAfter(Predicate)
     * @see #notBefore(Predicate)
     * @see #asLongAs(Predicate)
     * @see #before(Predicate)
     */
    default Chainable<T> notAsLongAs(Predicate<? super T> condition) {
        return Chainables.notAsLongAs(this, condition);
    }

    /**
     * Returns the remaining items from this chain starting with the first one that is NOT the specified {@code item}.
     * @param item
     * @return items starting with the first one that is not the specified {@code item}
     * (i.e. skipping the initial items that are)
     * @see #notAsLongAs(Predicate)
     */
    default Chainable<T> notAsLongAsValue(T item) {
        return Chainables.notAsLongAsValue(this, item);
    }

    /**
     * Returns a chain of remaining items from this chain starting with the first item that satisfies the specified {@code condition} and followed by all the remaining items.
     * <p>
     * For example, if the items are { 1, 3, 5, 2, 7, 9, ...} and the {@code condition} returns {@code true} for items that are even numbers, then the resulting
     * chain will consist of { 2, 7, 9, ... }.
     * @param condition
     * @return items starting with the one where the specified {@code condition} is met
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.SkipWhile()}, but with a negated predicate</td></tr>
     * </table>
     * @see #notAfter(Predicate)
     * @see #asLongAs(Predicate)
     * @see #notAsLongAs(Predicate)
     */
    default Chainable<T> notBefore(Predicate<? super T> condition) {
        return Chainables.notBefore(this, condition);
    }

    /**
     * Returns a chain of remaining items from this chain starting with the specified {@code item}.
     * @param item
     * @return the remaining items in this chain starting with the specified {@code item}, if any
     * @see #notBefore(Predicate)
     * @see #notAsLongAsValue(Object)
     * @see #notAsLongAs(Predicate)
     * @see #notAfter(Predicate)
     * @see #asLongAs(Predicate)
     */
    default Chainable<T> notBeforeEquals(T item) {
        return Chainables.notBeforeEquals(this, item);
    }

    /**
     * Returns the items from this chain that do not satisy the specified {@code condition}.
     * @param condition
     * @return items that do not meet the specified {@code condition}
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#filter(Predicate)}, but with a negated predicate</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.Where()}, but with a negated predicate</td></tr>
     * </table>
     * @see #where(Predicate)
     */
    default Chainable<T> notWhere(Predicate<? super T> condition) {
        return Chainables.notWhere(this, condition);
    }

    /**
     * Returns a chain with each item from this chain replaced with items of the same type returned by the specified {@code replacer}.
     * <p>
     * Whenever the replacer returns {@code null}, the item is skipped (de-facto removed) from the resulting chain altogether.
     * @param replacer
     * @return replacement items
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#flatMap(Function)}, but with the return type the same as the input type</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.Select()}, but with the return type the same as the input type</td></tr>
     * </table>
     * @see #transformAndFlatten(Function)
     */
    default Chainable<T> replace(Function<? super T, Iterable<T>> replacer) {
        return Chainables.replace(this, replacer);
    }

    /**
     * Returns a chain where the items are in the opposite order to this chain.
     * <p>
     * This triggers a full traversal/evaluation of the items.
     * @return items in the opposite order
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.Reverse()}</td></tr>
     * </table>
     */
    default Chainable<T> reverse() {
        return Chainables.reverse(this);
    }

    /**
     * Determines whether the initial items in this chain are the same and in the same order as in the specified {@code prefix}.
     * @param prefix
     * @return {@code true} if this starts with the exact sequence of items in the {@code prefix}
     * @see #endsWith(Iterable)
     * @see #startsWithEither(Iterable...)
     */
    default boolean startsWith(Iterable<T> prefix) {
        return Chainables.startsWithEither(this, prefix);
    }

    /**
     * Determines whether the initial items in this chain are the same and in the same order any of the specified {@code prefixes}.
     * @param prefixes
     * @return true if this starts with any of the specified {@code prefixes} of items
     * @see #startsWith(Iterable)
     */
    @SuppressWarnings("unchecked")
    default boolean startsWithEither(Iterable<T>... prefixes) {
        return Chainables.startsWithEither(this, prefixes);
    }

    /**
     * Creates a stream from this chain.
     * @return a stream representing this chain.
     */
    default Stream<T> stream() {
        return Chainables.toStream(this);
    }

    /**
     * Computes the sum of values generated by the specified {@code valueExtractor} applied to each item in this chain.
     * <p>
     * This trighers a full traversal/evaluation of the items.
     * @param valueExtractor
     * @return sum of all the values returned by the specified {@code valueExtractor} applied to each item
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#reduce(java.util.function.BinaryOperator)} or {@link java.util.stream.Stream#collect(java.util.stream.Collector)}, but specifically for summation</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.Aggregate()}, but specifically for summation</td></tr>
     * </table>
     */
    default long sum(Function<? super T, Long> valueExtractor) {
        return Chainables.sum(this, valueExtractor);
    }

    /**
     * Transforms this chain into a list, tigerring a full evaluation.
     * @return a new list containing all the items
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.ToList()}</td></tr>
     * </table>
     */
    default List<T> toList() {
        return Chainables.toList(this);
    }

    /**
     * Puts the items from this chain into a map indexed by the specified {@code keyExtractor} applied to each item.
     * @param keyExtractor
     * @return a map of the items indexed by the key produced by the specified {@code keyExtractor}
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.ToDictionary()}</td></tr>
     * </table>
     */
    default <K> Map<K, T> toMap(Function<? super T, K> keyExtractor) {
        return Chainables.toMap(this, keyExtractor);
    }

    /**
     * Create a {@link ChainableQueue} with the current items as the initial contents of the queue, but not yet traversed/evaluated.
     * @return a mutable {@link ChainableQueue} with the current items as the initial contents of the queue
     */
    @Experimental
    default ChainableQueue<T> toQueue() {
        return Chainables.toQueue(this);
    }

    /**
     * Transforms each item into another item, of a possibly different type, by applying the specified {@code transformer}
     * @param transformer
     * @return the resulting items from the transformation
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#map(Function)}</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.Select()}</td></tr>
     * </table>
     * @see #transformAndFlatten(Function)
     */
    default <O> Chainable<O> transform(Function<? super T, O> transformer) {
        return Chainables.transform(this, transformer);
    }

    /**
     * Transforms each item into several other items, possibly of a different type, using the specified {@code transformer}.
     * @param transformer
     * @return the resulting items from the transformation
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#flatMap(Function)}</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.SelectMany()}</td></tr>
     * </table>
     * @see #transform(Function)
     */
    default <O> Chainable<O> transformAndFlatten(Function<? super T, Iterable<O>> transformer) {
        return Chainables.transformAndFlatten(this, transformer);
    }

    /**
     * Returns a chain of items from this chain that satisfy the specified {@code condition}.
     * @param condition
     * @return matching items
     * @chainables.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#filter(Predicate)}</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.Where()}</td></tr>
     * </table>
     */
    default Chainable<T> where(Predicate<? super T> condition) {
        return Chainables.whereEither(this, condition);
    }

    /**
     * Returns a chain of items from this chain that satisfy any of the specified {@code conditions}.
     * @param conditions
     * @return items that meet any of the specified {@code conditions}
     * @see #where(Predicate)
     */
    @SuppressWarnings("unchecked")
    default Chainable<T> whereEither(Predicate<? super T>... conditions) {
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