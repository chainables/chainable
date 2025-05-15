/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.chainables.chainable;

import java.util.List;
import java.util.stream.Stream;

/**
 * A Java {@link List} that is also {@link Chainable}.
 * @author Martin Sawicki
 *
 * @param <T> the type of the stored items
 */
public interface ChainableList<T> extends Chainable<T>, List<T> {

    @SuppressWarnings("unchecked")
    @Override
    default boolean contains(Object item) {
        return Chainable.super.contains((T)item);
    }

    @Override
    default boolean isEmpty() {
        return Chainable.super.isEmpty();
    }

    @Override
    default Stream<T> stream() {
        return Chainable.super.stream();
    }

    /**
     * Adds all the specified {@code items} to this list.
     * @param items items to add
     * @return {@code true}} iff this operation resulted in a change to the list
     */
    boolean addAll(Iterable<? extends T> items);

    /**
     * Returns an unmodifiable view of this list.
     * @return an unmodifiable view of this list
     */
    Unmodifiable<T> unmodifiable();

    /**
     * An unmodifiable view of a {@link ChainableList}, that implements all of its members but throws {@link UnsupporterOperationException} when
     * accessing any methods that would change its contents otherwise.
     * @author Martin Sawicki
     *
     * @param <T> the type of the stored items
     */
    public interface Unmodifiable<T> extends ChainableList<T>{
    }
}
