/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.chainables.chainable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.UnaryOperator;

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

    @Override
    public boolean addAll(Iterable<? extends T> items) {
        boolean added = false;
        for (T item : items) {
            added |= this.add(item);
        }

        return added;
    }


    @Override
    public ChainableList.Unmodifiable<T> unmodifiable() {
        return new Unmodifiable<>(this);
    }

    /**
     * An implementation of {@link ChainableList.Unmodifiable}.
     * @author Martin Sawicki
     *
     * @param <U> the type of the stored items
     */
    public class Unmodifiable<U> implements ChainableList.Unmodifiable<U> {
        final ChainList<U> list;

        protected Unmodifiable(ChainList<U> list) {
            this.list = list;
        }

        @Override
        public void add(int index, U element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean add(U e) {
            return false;
        }

        @Override
        public boolean addAll(Iterable<? extends U> items) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends U> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(int index, Collection<? extends U> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public U remove(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void replaceAll(UnaryOperator<U> operator) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public U set(int index, U element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void sort(Comparator<? super U> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChainableList.Unmodifiable<U> unmodifiable() {
            return this;
        }

        @Override
        public Iterator<U> iterator() {
            return this.list.iterator();
        }

        @Override
        public int size() {
            return this.list.size();
        }

        @Override
        public Object[] toArray() {
            return this.list.toArray();
        }

        @Override
        public <V> V[] toArray(V[] a) {
            return this.list.toArray(a);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return this.list.containsAll(c);
        }

        @Override
        public U get(int index) {
            return this.list.get(index);
        }

        @Override
        public int indexOf(Object o) {
            return this.list.indexOf(o);
        }

        @Override
        public int lastIndexOf(Object o) {
            return this.list.lastIndexOf(o);
        }

        @Override
        public ListIterator<U> listIterator() {
            return this.list.listIterator();
        }

        @Override
        public ListIterator<U> listIterator(int index) {
            return this.list.listIterator(index);
        }

        @Override
        public List<U> subList(int fromIndex, int toIndex) {
            return Collections.unmodifiableList(this.list.subList(fromIndex, toIndex));
        }
    }
}
