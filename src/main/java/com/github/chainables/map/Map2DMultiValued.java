/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.chainables.map;

import java.util.Set;

import com.github.chainables.chainable.Chainable;

/**
 * A 2D map derived from {@link com.github.chainables.Map2D} which can store multiple unique values at the location given by
 * the primary and secondary key pair.
 * @author Martin Sawicki
 *
 * @param <K1> the primary key type
 * @param <K2> the secondary key type
 * @param <V> the stored value type
 */
public interface Map2DMultiValued<K1, K2, V> extends Map2D<K1, K2, Set<V>> {
    /**
     * Put the specified one or more {@code values} at the location of the specified {@code primaryKey} and {@code secondaryKey}, in addition to
     * whatever values may already be stored at that location.
     * <p>
     * Note that only unique values are stored. 
     * @param primaryKey the primary key of the location to put the specified {@code values} at
     * @param secondaryKey the secondary key of the location to put the specified {@code values} at
     * @param values the values to store at the specified location
     * @return self
     */
    @SuppressWarnings("unchecked")
    Map2DMultiValued<K1, K2, V> putInValues(K1 primaryKey, K2 secondaryKey, V...values);

    /**
     * Gets a flattened chain of all the stored values.
     * @return a flattened chain of all the stored values
     */
    Chainable<V> valuesFlattened();

    /**
     * Removes the specified {@code value} from the values at the location determined by the specified {@code primaryKey} and {@code secondaryKey}
     * @param primaryKey the primary key of the location to remove the specified {@code value} from
     * @param secondaryKey the secondary key of the location to remove the specified {@code value} from
     * @param value the value to remove
     * @return {@code true} iff the specified {@code value} was actually among the values stored at the specified location
     */
    boolean removeFromValues(K1 primaryKey, K2 secondaryKey, V value);

    /**
     * Checks whether the location determined by the specified {@code primaryKey} and {@code secondaryKey} contains the specified {@code value}.
     * @param primaryKey the primary key of the location to check
     * @param secondaryKey the secondary key of the location to check
     * @param value the value to check for
     * @return {@code true} iff the specified {@code value} exists among the values at the specified location
     */
    boolean containsValue(K1 primaryKey, K2 secondaryKey, V value);
}
