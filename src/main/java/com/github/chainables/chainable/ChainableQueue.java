/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.chainables.chainable;

import com.github.chainables.annotation.Experimental;

/**
 * A simple FIFO queue supporting the {@link Chainable} interface.
 *
 * @author Martin Sawicki
 * @param <T>
 */
@Experimental
public interface ChainableQueue<T> extends Chainable<T> {
    /**
     * Removes the first item from the queue, without affecting the underlying chain.
     * @return the first item
     */
    T removeFirst();

    /**
     * Adds the specified {@code items} to the end of the queue.
     * <p>
     * Note that the original chain the queue was created from is not affected, as new items are added to a hidden
     * separate collection concatenated with that original chain.
     * @param items
     * @return self
     */
    @SuppressWarnings("unchecked")
    ChainableQueue<T> withLast(T...items);

    /**
     * Adds the specified {@code items} to the end of the queue.
     * <p>
     * Note that the original chain the queue was created from is not affected, as new items are added to a hidden
     * separate collection concatenated with that original chain.
     * @param items
     * @return self
     */
    ChainableQueue<T> withLast(Iterable<T> items);
}