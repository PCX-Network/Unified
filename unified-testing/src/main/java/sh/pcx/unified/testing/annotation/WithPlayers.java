/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.testing.annotation;

import java.lang.annotation.*;

/**
 * Container annotation for multiple {@link WithPlayer} annotations.
 *
 * @since 1.0.0
 * @author Supatuck
 */
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WithPlayers {

    /**
     * The player annotations.
     *
     * @return the array of WithPlayer annotations
     */
    WithPlayer[] value();
}
