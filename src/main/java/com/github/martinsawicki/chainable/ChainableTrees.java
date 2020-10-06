/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.martinsawicki.chainable;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the source of all the static methods underlying the default implementation of {@link ChainableTree} as well as some other conveniences.
 * @author Martin Sawicki
 */
public abstract class ChainableTrees {

    static class ChainableTreeImpl<T> implements ChainableTree<T> {
        private final T inner;
        private ChainableTree<T> parent = null;
        private List<ChainableTree<T>> children = null;
        private Chainable<ChainableTree<T>> childrenChain = Chainable.from(new ArrayList<>());

        protected ChainableTreeImpl(T inner) {
            this.inner = inner;
        }

        @Override
        public Chainable<ChainableTree<T>> children() {
            // TODO: Should this just use caching? Use just one children reference?
            if (this.children == null) {
                this.children = this.childrenChain.toList();
            }

            return Chainable.from(this.children);
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
