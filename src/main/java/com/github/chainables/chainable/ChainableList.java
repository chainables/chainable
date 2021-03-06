/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.chainables.chainable;

import java.util.List;
import java.util.stream.Stream;

/**
 * A Java {@link List} that is also {@link Chainable}.
 * @author msawicki
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
    default boolean addAll(Iterable<T> items) {
        boolean added = false;
        for (T item : items) {
            added |= this.add(item);
        }

        return added;
    }
}
