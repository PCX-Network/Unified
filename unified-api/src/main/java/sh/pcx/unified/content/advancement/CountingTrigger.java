/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.advancement;

import org.jetbrains.annotations.NotNull;

/**
 * A trigger that wraps another trigger and requires a count of completions.
 *
 * @param delegate the wrapped trigger
 * @param count the required count
 * @since 1.0.0
 */
public record CountingTrigger(Trigger delegate, int count) implements Trigger {
    @Override
    @NotNull
    public String getType() {
        return delegate.getType();
    }

    @Override
    @NotNull
    public Trigger count(int newCount) {
        return new CountingTrigger(delegate, newCount);
    }
}
