/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.github.martinsawicki.function;

import java.util.function.Function;

/**
 * Functional interface deriving from {@link Function}, but only expecting {@link String} return values.
 *
 * @param <T>
 */
@FunctionalInterface
public interface ToStringFunction<T> extends Function<T, String> {
}