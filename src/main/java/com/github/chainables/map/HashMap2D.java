/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.chainables.map;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.github.chainables.chainable.Chainable;
import com.github.chainables.chainable.Chainables;

import java.util.Set;

/**
 * A {@link java.util.HashMap}-based implementation of the 2 dimensional {@link Map2D}.
 * @author Martin Sawicki
 *
 * @param <K1> primary key type
 * @param <K2> secondary key type
 * @param <V> value type
 */
public class HashMap2D<K1, K2, V> implements Map2D<K1, K2, V> {
	private final HashMap<K1, Map<K2, V>> maps = new HashMap<>();
	private final HashMap2D<K2, K1, V> reverse;

	private final V defaultValue;
	private K1 lastKey1 = null;
	private Map<K2, V> lastSubmap = null;

	/**
	 * Creates a new 2D map based on the specified 2-dimensional {@code array}.
	 * <p>
	 * Each row in the {@code array} represents a row in the map.
	 * The first value (at index 0) is used as the primary key.
	 * The second value (at index 1) is used as the secondary key.
	 * The third value (at index 2) is used as the value to associate with the specfied keys.
	 * @param array a two-dimensional array as described earlier
	 * @return a new 2D map initialized with the specified {@code array}
	 */
    public static <K1, K2, V> Map2D<K1, K2, V> from(Object[][] array) {
	    return new HashMap2D<K1, K2, V>().putAll(array);
	}

    @Override
    public Map2D<K1, K2, V> clear() {
        return this.clear(true);
    }

    /**
     * Creates a new 2D map with {@code null} as the default value.
     */
    public HashMap2D() {
        this(null);
    }

    /**
     * Creates a new 2D map with the specified {@code defaultValue} returned for locations where no other value has been put.
     * @param defaultValue the value to return for locations where no other value has been put
     */
    public HashMap2D(V defaultValue) {
        this.defaultValue = defaultValue;
        this.reverse = new HashMap2D<>(this, defaultValue);
    }

    private Map2D<K1, K2, V> clear(boolean updateReverse) {
        this.maps.clear();
        this.lastKey1 = null;
        this.lastSubmap = null;
        if (updateReverse) {
            this.reverse.clear(false);
        }

        return this;
    }

	private HashMap2D(HashMap2D<K2, K1, V> reverse, V defaultValue) {
		this.defaultValue = defaultValue;
		this.reverse = reverse;
	}

	@Override
	public V get(K1 key1, K2 key2) {
		Map<K2, V> subMap = null;
		if (key1 == null || key2 == null) {
			return this.defaultValue;
		} else if (key1 == this.lastKey1) {
			// Return from cache
			subMap = this.lastSubmap;
		} else {
			// Update cache
			this.lastSubmap = subMap = this.maps.get(key1);
			this.lastKey1 = key1;
		}

		if (subMap == null) {
			this.lastKey1 = null;
			return this.defaultValue;
		}

		V value = subMap.get(key2);
		return (value != null) ? value : this.defaultValue;
	}

    @Override
    public Map2D<K1, K2, V> putAll(K1 key, Map<K2, V> row) {
        return this.putAll(key, row, true);
    }

    private Map2D<K1, K2, V> putAll(K1 key, Map<K2, V> row, boolean updateReverse) {
        if (key == null) {
            return this;
        }

        if (row == null) {
            this.maps.remove(key);
        } else {
            this.maps.put(key, row);
        }

        this.lastSubmap = row;
        this.lastKey1 = key;

        if (updateReverse) {
            for (Entry<K2, V> entry : row.entrySet()) {
                this.reverse.put(entry.getKey(), key, entry.getValue(), false);
            }
        }

        return this;
    }

	private V put(K1 key1, K2 key2, V value, boolean updateReverse) {
		if (key1 == null || key2 == null) {
			return null;
		}

		// Map K1 to K2
		Map<K2, V> subMap = this.maps.get(key1);
		if (subMap == null) {
			subMap = new HashMap<>();
			this.maps.put(key1, subMap);
		}

        // Update cache
		this.lastSubmap = subMap;
		this.lastKey1 = key1;

		V previous = subMap.put(key2, value);

		if (updateReverse) {
			this.reverse.put(key2, key1, value, false);
		}

		return previous;
	}

	@Override
	public V put(K1 key1, K2 key2, V value) {
		return this.put(key1, key2, value, true);
	}

	@Override
	public Set<K1> keySetPrimary() {
		return Collections.unmodifiableSet(this.maps.keySet());
	}

	@Override
	public Map<K2, V> mapFrom(K1 key1) {
	   if (key1 == this.lastKey1) {
		    // Return from cache
		    return this.lastSubmap;
		}

		Map<K2, V> subMap = this.maps.get(key1);
		if (subMap != null) {
		    // Update cache
		    this.lastKey1 = key1;
		    this.lastSubmap = subMap;
		} else {
		    subMap = Collections.emptyMap();
		}

		return subMap;
	}

	@Override
	public Map<K1, Map<K2, V>> maps() {
		return this.maps;
	}

	@Override
	public boolean containsKeys(K1 key1, K2 key2) {
		Map<K2, V> subMap = this.maps.get(key1);
		return (subMap != null) ? subMap.containsKey(key2) : false;
	}

	private V remove(K1 key1, K2 key2, boolean updateReverse) {
		// TODO Update cache?
		if (key1 == null || key2 == null) {
			return this.defaultValue;
		}

		// Clear cached value if needed
		if (this.lastKey1 == key1) {
			this.lastKey1 = null;
			this.lastSubmap = null;
		}

		// Remove from submap
		Map<K2, V> subMap = this.maps.get(key1);
		if (subMap != null) {
			V removedValue = subMap.remove(key2);
			if (updateReverse) {
				// Remove from reverse table if this is not the reverse one
				removedValue = this.reverse.remove(key2, key1, false);
			}

			// Remove the submap itself if empty
			if (subMap.isEmpty()) {
			    this.maps.remove(key1);
			}

			return removedValue;
		} else {
			return this.defaultValue;
		}
	}

	@Override
	public V remove(K1 key1, K2 key2) {
		return this.remove(key1, key2, true);
	}

	@Override
	public Map2D<K2, K1, V> reverse() {
		return this.reverse;
	}


	@Override
	public Chainable<V> values() {
	    return Chainable
	            .from(this.maps.values())
	            .transformAndFlatten(m -> m.values());
	}

    @Override
    public Chainable<Entry2D<K1, K2, V>> entries() {
        return Chainable.from(new Iterable<Entry2D<K1, K2, V>>() {
            @Override
            public Iterator<Entry2D<K1, K2, V>> iterator() {
                return new Iterator<Entry2D<K1,K2,V>>() {
                    Iterator<Entry<K1, Map<K2, V>>> iter1 = HashMap2D.this.maps.entrySet().iterator();
                    K1 key1 = null;
                    Iterator<Entry<K2, V>> iter2 = null;

                    @Override
                    public boolean hasNext() {
                        if (this.iter2 != null && this.iter2.hasNext()) {
                            return true;
                        } else {
                            while (this.iter1.hasNext()) {
                                Entry<K1, Map<K2, V>> entry = this.iter1.next();
                                if (entry == null) {
                                    continue;
                                } else if (entry.getValue() == null) {
                                    continue;
                                }

                                this.key1 = entry.getKey();
                                this.iter2 = entry.getValue().entrySet().iterator();

                                if (!Chainables.isNullOrEmpty(this.iter2)) {
                                    return true;
                                }
                            }

                            return false;
                        }
                    }

                    @Override
                    public Entry2D<K1, K2, V> next() {
                        if (this.hasNext()) {
                            Entry<K2, V> entry = this.iter2.next();
                            return new Entry2D<K1, K2, V>(this.key1, entry.getKey(), entry.getValue());
                        } else {
                            return null;
                        }
                    }
                };
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map2D<K1, K2, V> putAll(Object[][] array) {
        if (array != null) {
            for (Object[] row : array) {
                if (row.length >= 3) {
                    K1 key1 = (K1) row[0];
                    K2 key2 = (K2) row[1];
                    V value = (V) row[2];
                    this.put(key1, key2, value);
                }
            }
        }

        return this;
    }
}
