/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.chainables.tuple;

import java.util.Objects;

/**
 * A tuple of two values, {@code x} and {@code y}.
 * @author Martin Sawicki
 *
 * @param <X> type of value {@code x}
 * @param <Y> type of value {@code y}
 */
public class Pair<X, Y> {
	public final X x;
	public final Y y;

	/**
	 * Creates a new pair of the specified two values.
	 * @param x one value
	 * @param y another value
	 */
	public Pair(X x, Y y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Creates a new pair of the specified two values.
	 * @param x one value
	 * @param y another value
	 * @return a pair of the specified values
	 */
	public static <X, Y> Pair<X, Y> from(X x, Y y) {
	    return new Pair<>(x, y);
	}

	/**
	 * Checks if both values are {@code null}.
	 * @return {@code true} if both values are {@code null}
	 */
	public boolean isEmpty() {
		return this.x == null && this.y == null;
	}

	@Override
	public String toString() {
	    return String.format(
	            "<%s, %s>",
	            Objects.toString(this.x, "(null)"),
	            Objects.toString(this.y, "(null)"));
	}

	@Override
	public boolean equals(Object o) {
	    return (o instanceof Pair) ? Objects.equals(x, ((Pair<?,?>)o).x) && Objects.equals(y, ((Pair<?,?>)o).y) : false;
    }

	@Override
	public int hashCode() {
	    return 31 * Objects.hashCode(x) + Objects.hashCode(y);
    }
}
