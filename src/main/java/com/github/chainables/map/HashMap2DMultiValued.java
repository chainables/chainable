/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.chainables.map;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.github.chainables.chainable.Chainable;
import com.github.chainables.chainable.Chainables;

public class HashMap2DMultiValued<K1, K2, V> extends HashMap2D<K1, K2, Set<V>> implements Map2DMultiValued<K1, K2, V> {
    @SuppressWarnings("unchecked")
    @Override
    public Map2DMultiValued<K1, K2, V> putInValues(K1 key1, K2 key2, V...values) {
        if (values != null) {
            final Set<V> currentSet = this.get(key1, key2);
            currentSet.addAll(Arrays.asList(values));
        }

        return this;
    }

    @Override
    public Chainable<V> valuesFlattened() {
        return Chainables.transformAndFlatten(this.values(), s -> s);
    }

    @Override
    public boolean removeFromValues(K1 key1, K2 key2, V value) {
        Set<V> currentSet = super.get(key1, key2);
        if (currentSet != null) {
            return currentSet.remove(value);
        } else {
            return false;
        }
    }

    @Override
    public boolean containsValue(K1 key1, K2 key2, V value) {
        Set<V> currentSet = super.get(key1, key2);
        if (currentSet != null) {
            return currentSet.contains(value);
        } else {
            return false;
        }
    }

    @Override
    public Set<V> get(K1 key1, K2 key2) {
        // Always return a non-empty set
        Set<V> currentSet = super.get(key1, key2);
        if (currentSet == null) {
            currentSet = new HashSet<>();
            super.put(key1, key2, currentSet);
        }

        return currentSet;
    }
}
