/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.chainables.chainable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import com.github.chainables.chainable.Chainables.CachedChain;

class Chain<T> implements Chainable<T> {
    protected Iterable<? extends T> iterable;

    Chain(Iterable<? extends T> iterable) {
        this.iterable = (iterable != null) ? iterable : new ArrayList<>();
    }

    static <T> Chain<T> empty() {
        return Chain.from(new ArrayList<>());
    }

    @SuppressWarnings("unchecked")
    static <T> Chain<T> from(Iterable<? extends T> iterable) {
        if (iterable == null) {
            return Chain.empty();
        } else if (iterable instanceof Chain<?>) {
            return (Chain<T>) iterable;
        } else if (iterable instanceof CachedChain<?>) {
            return (CachedChain<T>) iterable;
        } else {
            return new Chain<T>(iterable);
        }
    }

    static <T> Chain<T> from(Supplier<Iterator<T>> iteratorSupplier) {
        return (iteratorSupplier == null) ? Chain.empty() : Chain.from(new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return iteratorSupplier.get();
            }
        });
    }

    @SuppressWarnings("unchecked")
    static <T> Chain<T> from(T...items) {
        return Chain.from(new Iterable<T>() {

            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    final T[] sourceItems = items;
                    int nextIndex = 0;
                    @Override
                    public boolean hasNext() {
                        return (this.sourceItems != null) ? (this.nextIndex < this.sourceItems.length) : false;
                    }

                    @Override
                    public T next() {
                        this.nextIndex++;
                        return this.sourceItems[this.nextIndex-1];
                    }
                };
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public Iterator<T> iterator() {
        return (Iterator<T>) this.iterable.iterator();
    }

    @Override
    public String toString() {
        return Chainables.join(", ", this);
    }

    @Override
    public T get(long index) {
        return (this.iterable instanceof List<?>) ? ((List<? extends T>) this.iterable).get(Math.toIntExact(index)) : Chainables.get(this, index);
    }
}
