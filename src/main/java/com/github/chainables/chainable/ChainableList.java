/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.chainables.chainable;

import java.util.List;
import java.util.stream.Stream;

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
}
