/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.testing.annotation;

import java.lang.annotation.*;

/**
 * Container annotation for multiple {@link WithPermission} annotations.
 *
 * @since 1.0.0
 * @author Supatuck
 */
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WithPermissions {

    /**
     * The permission annotations.
     *
     * @return the array of WithPermission annotations
     */
    WithPermission[] value();
}
