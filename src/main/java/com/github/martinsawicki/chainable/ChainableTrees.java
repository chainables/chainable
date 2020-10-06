/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.martinsawicki.chainable;

/**
 * This is the source of all the static methods underlying the default implementation of {@link ChainableTree} as well as some other conveniences.
 * @author Martin Sawicki
 */
public abstract class ChainableTrees {

    static class ChainableTreeImpl<T> implements ChainableTree<T> {
        private final T inner;
        ChainableTree<T> parent = null;

        protected ChainableTreeImpl(T inner) {
            this.inner = inner;
        }

        @Override
        public T inner() {
            return this.inner;
        }

        @Override
        public ChainableTree<T> parent() {
            return this.parent;
        }
    }
}
