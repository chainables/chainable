/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.chainables.map;

import java.util.Set;

public interface Map2DMultiValued<K1, K2, V> extends Map2D<K1, K2, Set<V>> {
    @SuppressWarnings("unchecked")
    Map2DMultiValued<K1, K2, V> putInValues(K1 key1, K2 key2, V...value);
    Iterable<V> valuesFlattened();
    boolean removeFromValues(K1 key1, K2 key2, V value);
    boolean containsValue(K1 key1, K2 key2, V value);
}
