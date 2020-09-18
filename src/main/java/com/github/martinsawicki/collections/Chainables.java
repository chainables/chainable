/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.martinsawicki.collections;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * This is the source of all the static methods underlying the default implementation of {@link Chainable} as well as some other conveniences.
 * @author Martin Sawicki
 *
 */
public final class Chainables {
    private Chainables() {
        throw new AssertionError("Not instantiable, just stick to the static methods.");
    }

    public interface Chainable<T> extends Iterable<T> {

    }

    private static class Chain<T> implements Chainable<T> {
        protected Iterable<T> iterable;

        private Chain(Iterable<T> iterable) {
            this.iterable = (iterable != null) ? iterable : new ArrayList<>();
        }

        @Override
        public Iterator<T> iterator() {
            return this.iterable.iterator();
        }

        @Override
        public String toString() {
            return this.iterable.toString();
        }
    }
}