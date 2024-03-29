/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.chainables.chainable;

import java.util.ArrayList;

/**
 * An implementation of {@link ChainableList} based on an {@link ArrayList}.
 * @author Martin Sawicki
 *
 * @param <T> the type of stored items
 */
public class ChainList<T> extends ArrayList<T> implements ChainableList<T> {
    private static final long serialVersionUID = 1L;
    public ChainList() {
    }

    public ChainList(T[] items) {
        if (items != null) {
            for (T item : items) {
                this.add(item);
            }
        }
    }

    public ChainList(Iterable<? extends T> items) {
        if (items != null) {
            for (T item : items) {
                this.add(item);
            }
        }
    }
}
