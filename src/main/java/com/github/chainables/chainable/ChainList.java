/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.chainables.chainable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * An implementation of {@link ChainableList} based on an {@link ArrayList}.
 * @author Martin Sawicki
 *
 * @param <T> the type of stored items
 */
public class ChainList<T> extends ArrayList<T> implements ChainableList<T> {
    private static final long serialVersionUID = 1L;

    public ChainList() { }

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
    public Unmodifiable subList(int fromIndex, int toIndex) {
        return this.unmodifiable(fromIndex, toIndex);
    }

    @Override
    public Unmodifiable unmodifiable() {
        return new Unmodifiable();
    }

    @Override
    public Unmodifiable unmodifiable(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= this.size() || toIndex <= fromIndex || toIndex > this.size()) {
            throw new IndexOutOfBoundsException();
        } else {
            return new Unmodifiable(fromIndex, toIndex);
        }
    }

    /**
     * An implementation of {@link ChainableList.Unmodifiable}.
     * @author Martin Sawicki
     *
     * @param <U> the type of the stored items
     */
    public class Unmodifiable implements ChainableList.Unmodifiable<T> {
        final int size;
        final int offset;
        final boolean isWhole;

        protected Unmodifiable() {
            this(0, ChainList.this.size());
        }

        protected Unmodifiable(int fromIndex, int toIndex) {
            this.offset = fromIndex;
            this.size = toIndex - fromIndex;
            isWhole = this.offset == 0 && this.size == ChainList.this.size();
        }

        @Override
        public void add(int index, T element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean add(T e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Iterable<? extends T> items) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends T> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(int index, Collection<? extends T> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public T remove(int index) {
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
        public void replaceAll(UnaryOperator<T> operator) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public T set(int index, T element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void sort(Comparator<? super T> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChainList<T>.Unmodifiable unmodifiable() {
            return this;
        }

        @Override
        public ChainList<T>.Unmodifiable unmodifiable(int fromIndex, int toIndex) {
            confirmRange(fromIndex, toIndex);
            return ChainList.this.unmodifiable(this.offset + fromIndex, this.offset + toIndex);
        }

        @Override
        public int size() {
            return this.size;
        }

        @Override
        public Object[] toArray() {
            Object[] array = new Object[this.size];
            for (int i = 0; i < this.size; i++) {
                array[i] = ChainList.this.get(this.offset + i);
            }

            return array;
        }

        @Override
        public <V> V[] toArray(V[] a) {
            if (this.isWhole) {
                // Whole list
                return ChainList.this.toArray(a);
            } else {
                // TODO?
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public boolean contains(Object item) {
            if (this.isWhole) {
                return ChainList.this.contains(item);
            } else {
                for (int i = 0; i < this.size; i++) {
                    if (Objects.equals(item, ChainList.this.get(i + this.offset))) {
                        return true;
                    }
                }

                return false;
            }
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            boolean resultOr = false;
            for (Object item : c) {
                for (int i = this.offset; i < this.offset + this.size; i++) {
                    resultOr |= Objects.equals(item, ChainList.this.get(i));
                    if (resultOr) {
                        break;
                    }
                }

                if (!resultOr) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public T get(int index) {
            confirmRange(index, index + 1);
            return ChainList.this.get(this.offset + index);
        }

        @Override
        public int indexOf(Object o) {
            if (this.isWhole) {
                return ChainList.this.indexOf(o);
            } else {
                for (int i = 0; i < this.size; i++) {
                    if (Objects.equals(o, ChainList.this.get(i + this.offset))) {
                        return i;
                    }
                }
            }

            return -1;
        }

        @Override
        public int lastIndexOf(Object o) {
            if (this.isWhole) {
                return ChainList.this.lastIndexOf(o);
            } else {
                for (int i = this.size - 1; i >= 0; i--) {
                    if (Objects.equals(o, ChainList.this.get(i + this.offset))) {
                        return i;
                    }
                }
            }

            return -1;
        }

        @Override
        public Iterator<T> iterator() {
            if (this.isWhole) {
                return ChainList.this.iterator();
            } else {
                return ChainList.this.afterFirst(this.offset).first(this.size).iterator();
            }
        }

        @Override
        public ListIterator<T> listIterator() {
            if (this.isWhole) {
                return ChainList.this.listIterator();
            } else {
                // TODO
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public ListIterator<T> listIterator(int index) {
            if (this.isWhole) {
                return ChainList.this.listIterator(index);
            } else {
                // TODO
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public ChainList<T>.Unmodifiable subList(int fromIndex, int toIndex) {
            return this.unmodifiable(fromIndex, toIndex);
        }

        private void confirmRange(int fromIndex, int toIndex) {
            if (fromIndex < 0 || fromIndex >= this.size || toIndex <= fromIndex || toIndex > this.size) {
                throw new IndexOutOfBoundsException();
            }
        }
    }
}
