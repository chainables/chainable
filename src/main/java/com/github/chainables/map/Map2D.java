/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.chainables.map;

import java.util.Map;
import java.util.Set;

import com.github.chainables.chainable.Chainable;

/**
 * Two-dimensional (2-key) map interface, that is a map where each value can be stored and accessed using two keys, and where a reverse key order mapping
 * is automatically maintaned.
 * <p>
 * For example, suppose {@code map2D} is a {@code Map2D<String, Integer, String>}, which means that its primary key is a string, the secondary key is
 * an integer, and the values are strings. Then the following example operations are possible:
 * <ul>
 * <li>Putting a value into it:
 * <p>
 * {@code map2D.put("myPrimaryKey", 3, "myValue");}
 * </li>
 * <p>
 * <li>Extracting a value:
 * <p>
 * {@code String value = map2D.get("myPrimaryKey", 3);}
 * </li>
 * <p>
 * <li>Extracting all the values at a given <i>primary</i> key as a {@link java.util.Map} indexed by the secondary key:
 * <p>
 * {@code Map<Integer, String> values = map2D.mapFrom("myPrimaryKey");}
 * </li>
 * <p>
 * <li>Extracting all the values at a given <i>secondary</i> key as a {@link java.util.Map} indexed by the primary key:
 * <p>
 * {@code Map<String, String> values = map2D.reverse().mapFrom(3);}
 * </li>
 * <p>
 * <li>Removing a value:
 * <p>
 * {@code map2D.remove("myPrimaryKey", 3);}
 * </li>
 * </ul>
 * @author Martin Sawicki
 *
 * @param <K1> the type of the primary key
 * @param <K2> the type of the secondary key
 * @param <V> the type of the values stored
 */
public interface Map2D<K1, K2, V> {
    /**
     * An individual entry in the map.
     * @author Martin Sawicki
     *
     * @param <K1> the type of the primary key
     * @param <K2> the type of the secondary key
     * @param <V> the type of the values
     */
    public static class Entry2D<K1, K2, V> {
        /**
         * The primary key of this entry
         */
        public final K1 primaryKey;

        /**
         * The secondary key of this entry
         */
        public final K2 secondaryKey;

        /**
         * The value stored by this entry
         */
        public final V value;
        public Entry2D(K1 primaryKey, K2 secondaryKey, V value) {
            this.primaryKey = primaryKey;
            this.secondaryKey = secondaryKey;
            this.value = value;
        }
    }

    /**
     * Retrieves the value at the specified {@code primaryKey} and {@code secondaryKey} .
     * @param primaryKey the primary key
     * @param secondaryKey the secondary key
     * @return the value retrieved, or {@code null} if no value to retrieve (or the value is {@code null})
     */
	V get(K1 primaryKey, K2 secondaryKey);

	/**
	 * Puts the value at the specified {@code primaryKey} and {@code secondaryKey}.
	 * @param primaryKey the primary key
	 * @param secondaryKey the secondary key
	 * @param value the value
	 * @return the previous value, or {@code null} if none
	 */
	V put(K1 primaryKey, K2 secondaryKey, V value);

	/**
	 * Imports the values from the specified 2D {@code array} by interpreting the 1st value of each row as the primary key,
	 * the 2nd value as the secondary key, and the 3rd value as the value, and ignoring the rest.
	 * <p>
	 * For example:
	 * <p>
	 * <code>map2D.putAll(new String[][] {{"a", "x", "ax"}, {"b", "y", "by"}});</code>
	 * @param array array to read from
	 * @return self
	 */
	Map2D<K1, K2, V> putAll(Object[][] array);

	/**
	 * Replaces the existing row at the specified {@code primaryKey} with the specified {@code row}, which must be a {@link java.util.Map} indexed
	 * by the secondary key.
	 * @param primaryKey the primary key to associate the row with
	 * @param row a {@link java.util.Map} indexed by the secondary key to be associated with the specified {@code primaryKey}
	 * @return self
	 */
    Map2D<K1, K2, V> putAll(K1 primaryKey, Map<K2, V> row);

    /**
     * Clears this map of all its entries.
     * @return self
     */
    Map2D<K1, K2, V> clear();

    /**
     * Checks whether the specified primary and secondary keys are in the map.
     * @param primaryKey the primary key to check
     * @param secondaryKey the secondary key to check
     * @return {@code true} if the keys exist
     */
	boolean containsKeys(K1 primaryKey, K2 secondaryKey);

	/**
	 * Retrieves the set of the primary keys.
	 * @return the primary keys
	 */
	Set<K1> keySetPrimary();

	/**
	 * Retrieves the secondary key-indexed map associated with the specified {@code primaryKey}.
	 * @param primaryKey the primary key to get the map from
	 * @return the map pointed by the specified primary key or an empty map if not found
	 */
	Map<K2, V> mapFrom(K1 primaryKey);

	/**
	 * Retrieves the primary key-indexed {@link java.util.Map} of secondary key-indexed maps underlying this 2D map.
	 * @return a map indexed by the primary key, of maps indexed by the secondary key
	 */
	Map<K1, Map<K2, V>> maps();

	/**
	 * Removes the entry from this 2D map at the location specified by {@code primaryKey} and {@code secondaryKey}.
	 * @param primaryKey the primary key of the entry
	 * @param secondaryKey the secondary key of the entry
	 * @return the value at the specified keys (or {@code null})
	 */
	V remove(K1 primaryKey, K2 secondaryKey);

	/**
	 * Retrieves the reverse of this map, that is: where the secondary key is the primary and vice versa.
	 * @return the reverse 2D map of this map
	 */
	Map2D<K2, K1, V> reverse();

	/**
	 * Retrieves all the values from the map.
	 * @return all the values in the map
	 */
	Chainable<V> values();

	/**
	 * Retrieves all the entries from this 2D map.
	 * @return all the entries in this 2D map
	 */
	Chainable<Entry2D<K1, K2, V>> entries();
}
