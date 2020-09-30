/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.martinsawicki.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Methods annotated with {@code @Experimental} may be removed or changed significantly in the future.
 */
@Target(ElementType.METHOD)
public @interface Experimental {
}
