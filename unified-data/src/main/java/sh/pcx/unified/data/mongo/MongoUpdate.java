/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.mongo;

import com.mongodb.client.model.Updates;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fluent builder for MongoDB update operations.
 *
 * <p>This builder provides a type-safe, fluent API for constructing
 * MongoDB update documents with support for all common update operators.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple set update
 * Bson update = MongoUpdate.create()
 *     .set("name", "Steve")
 *     .set("balance", 1000.0)
 *     .build();
 *
 * mongoService.updateOne("players", filter, update);
 *
 * // Increment and set
 * Bson update = MongoUpdate.create()
 *     .inc("level", 1)
 *     .inc("experience", -100)
 *     .set("lastLevelUp", Instant.now())
 *     .build();
 *
 * // Array operations
 * Bson update = MongoUpdate.create()
 *     .push("achievements", "first_kill")
 *     .addToSet("badges", "warrior")
 *     .pull("warnings", "expired_warning")
 *     .build();
 *
 * // Complex update with multiple operators
 * Bson update = MongoUpdate.create()
 *     .set("status", "active")
 *     .inc("loginCount", 1)
 *     .currentDate("lastLogin")
 *     .unset("temporaryData")
 *     .min("fastestTime", newTime)
 *     .max("highScore", newScore)
 *     .build();
 *
 * // Rename and remove fields
 * Bson update = MongoUpdate.create()
 *     .rename("oldField", "newField")
 *     .unset("deprecatedField")
 *     .build();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This builder is NOT thread-safe. Create a new builder for each update.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MongoService#update()
 */
public final class MongoUpdate {

    private final List<Bson> updates = new ArrayList<>();

    private MongoUpdate() {
        // Use static factory method
    }

    /**
     * Creates a new update builder.
     *
     * @return a new update builder
     * @since 1.0.0
     */
    @NotNull
    public static MongoUpdate create() {
        return new MongoUpdate();
    }

    // ===========================================
    // Set Operations
    // ===========================================

    /**
     * Sets a field to a value.
     *
     * @param field the field name
     * @param value the value to set
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate set(@NotNull String field, @Nullable Object value) {
        Objects.requireNonNull(field, "Field cannot be null");
        updates.add(Updates.set(field, value));
        return this;
    }

    /**
     * Sets a field only if the document is being inserted (upsert).
     *
     * @param field the field name
     * @param value the value to set on insert
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate setOnInsert(@NotNull String field, @Nullable Object value) {
        Objects.requireNonNull(field, "Field cannot be null");
        updates.add(Updates.setOnInsert(field, value));
        return this;
    }

    /**
     * Removes a field from the document.
     *
     * @param field the field name to remove
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate unset(@NotNull String field) {
        Objects.requireNonNull(field, "Field cannot be null");
        updates.add(Updates.unset(field));
        return this;
    }

    /**
     * Removes multiple fields from the document.
     *
     * @param fields the field names to remove
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate unset(@NotNull String... fields) {
        for (String field : fields) {
            unset(field);
        }
        return this;
    }

    /**
     * Renames a field.
     *
     * @param oldName the current field name
     * @param newName the new field name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate rename(@NotNull String oldName, @NotNull String newName) {
        Objects.requireNonNull(oldName, "Old name cannot be null");
        Objects.requireNonNull(newName, "New name cannot be null");
        updates.add(Updates.rename(oldName, newName));
        return this;
    }

    // ===========================================
    // Numeric Operations
    // ===========================================

    /**
     * Increments a numeric field by a value.
     *
     * @param field the field name
     * @param value the value to increment by (can be negative)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate inc(@NotNull String field, @NotNull Number value) {
        Objects.requireNonNull(field, "Field cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");
        updates.add(Updates.inc(field, value));
        return this;
    }

    /**
     * Increments a numeric field by 1.
     *
     * @param field the field name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate inc(@NotNull String field) {
        return inc(field, 1);
    }

    /**
     * Decrements a numeric field by 1.
     *
     * @param field the field name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate dec(@NotNull String field) {
        return inc(field, -1);
    }

    /**
     * Multiplies a numeric field by a value.
     *
     * @param field the field name
     * @param value the multiplier
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate mul(@NotNull String field, @NotNull Number value) {
        Objects.requireNonNull(field, "Field cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");
        updates.add(Updates.mul(field, value));
        return this;
    }

    /**
     * Sets a field to the smaller of its current value or the given value.
     *
     * @param field the field name
     * @param value the value to compare
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate min(@NotNull String field, @NotNull Object value) {
        Objects.requireNonNull(field, "Field cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");
        updates.add(Updates.min(field, value));
        return this;
    }

    /**
     * Sets a field to the larger of its current value or the given value.
     *
     * @param field the field name
     * @param value the value to compare
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate max(@NotNull String field, @NotNull Object value) {
        Objects.requireNonNull(field, "Field cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");
        updates.add(Updates.max(field, value));
        return this;
    }

    // ===========================================
    // Date Operations
    // ===========================================

    /**
     * Sets a field to the current date/time.
     *
     * @param field the field name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate currentDate(@NotNull String field) {
        Objects.requireNonNull(field, "Field cannot be null");
        updates.add(Updates.currentDate(field));
        return this;
    }

    /**
     * Sets a field to the current timestamp.
     *
     * @param field the field name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate currentTimestamp(@NotNull String field) {
        Objects.requireNonNull(field, "Field cannot be null");
        updates.add(Updates.currentTimestamp(field));
        return this;
    }

    // ===========================================
    // Array Operations
    // ===========================================

    /**
     * Adds a value to the end of an array.
     *
     * @param field the array field name
     * @param value the value to add
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate push(@NotNull String field, @Nullable Object value) {
        Objects.requireNonNull(field, "Field cannot be null");
        updates.add(Updates.push(field, value));
        return this;
    }

    /**
     * Adds multiple values to the end of an array.
     *
     * @param field  the array field name
     * @param values the values to add
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate pushEach(@NotNull String field, @NotNull List<?> values) {
        Objects.requireNonNull(field, "Field cannot be null");
        Objects.requireNonNull(values, "Values cannot be null");
        updates.add(Updates.pushEach(field, values));
        return this;
    }

    /**
     * Adds a value to an array only if it doesn't already exist.
     *
     * @param field the array field name
     * @param value the value to add
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate addToSet(@NotNull String field, @Nullable Object value) {
        Objects.requireNonNull(field, "Field cannot be null");
        updates.add(Updates.addToSet(field, value));
        return this;
    }

    /**
     * Adds multiple values to an array, only if they don't already exist.
     *
     * @param field  the array field name
     * @param values the values to add
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate addEachToSet(@NotNull String field, @NotNull List<?> values) {
        Objects.requireNonNull(field, "Field cannot be null");
        Objects.requireNonNull(values, "Values cannot be null");
        updates.add(Updates.addEachToSet(field, values));
        return this;
    }

    /**
     * Removes all occurrences of a value from an array.
     *
     * @param field the array field name
     * @param value the value to remove
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate pull(@NotNull String field, @Nullable Object value) {
        Objects.requireNonNull(field, "Field cannot be null");
        updates.add(Updates.pull(field, value));
        return this;
    }

    /**
     * Removes array elements matching a filter.
     *
     * @param field  the array field name
     * @param filter the filter to match elements to remove
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate pullByFilter(@NotNull String field, @NotNull Bson filter) {
        Objects.requireNonNull(field, "Field cannot be null");
        Objects.requireNonNull(filter, "Filter cannot be null");
        updates.add(Updates.pullByFilter(filter));
        return this;
    }

    /**
     * Removes all occurrences of multiple values from an array.
     *
     * @param field  the array field name
     * @param values the values to remove
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate pullAll(@NotNull String field, @NotNull List<?> values) {
        Objects.requireNonNull(field, "Field cannot be null");
        Objects.requireNonNull(values, "Values cannot be null");
        updates.add(Updates.pullAll(field, values));
        return this;
    }

    /**
     * Removes the first element from an array.
     *
     * @param field the array field name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate popFirst(@NotNull String field) {
        Objects.requireNonNull(field, "Field cannot be null");
        updates.add(Updates.popFirst(field));
        return this;
    }

    /**
     * Removes the last element from an array.
     *
     * @param field the array field name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate popLast(@NotNull String field) {
        Objects.requireNonNull(field, "Field cannot be null");
        updates.add(Updates.popLast(field));
        return this;
    }

    // ===========================================
    // Bitwise Operations
    // ===========================================

    /**
     * Performs a bitwise AND operation.
     *
     * @param field the field name
     * @param value the value to AND with
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate bitwiseAnd(@NotNull String field, int value) {
        Objects.requireNonNull(field, "Field cannot be null");
        updates.add(Updates.bitwiseAnd(field, value));
        return this;
    }

    /**
     * Performs a bitwise AND operation.
     *
     * @param field the field name
     * @param value the value to AND with
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate bitwiseAnd(@NotNull String field, long value) {
        Objects.requireNonNull(field, "Field cannot be null");
        updates.add(Updates.bitwiseAnd(field, value));
        return this;
    }

    /**
     * Performs a bitwise OR operation.
     *
     * @param field the field name
     * @param value the value to OR with
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate bitwiseOr(@NotNull String field, int value) {
        Objects.requireNonNull(field, "Field cannot be null");
        updates.add(Updates.bitwiseOr(field, value));
        return this;
    }

    /**
     * Performs a bitwise OR operation.
     *
     * @param field the field name
     * @param value the value to OR with
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate bitwiseOr(@NotNull String field, long value) {
        Objects.requireNonNull(field, "Field cannot be null");
        updates.add(Updates.bitwiseOr(field, value));
        return this;
    }

    /**
     * Performs a bitwise XOR operation.
     *
     * @param field the field name
     * @param value the value to XOR with
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate bitwiseXor(@NotNull String field, int value) {
        Objects.requireNonNull(field, "Field cannot be null");
        updates.add(Updates.bitwiseXor(field, value));
        return this;
    }

    /**
     * Performs a bitwise XOR operation.
     *
     * @param field the field name
     * @param value the value to XOR with
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate bitwiseXor(@NotNull String field, long value) {
        Objects.requireNonNull(field, "Field cannot be null");
        updates.add(Updates.bitwiseXor(field, value));
        return this;
    }

    // ===========================================
    // Combination Methods
    // ===========================================

    /**
     * Adds a raw update operation.
     *
     * @param update the update to add
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate with(@NotNull Bson update) {
        Objects.requireNonNull(update, "Update cannot be null");
        updates.add(update);
        return this;
    }

    /**
     * Combines this builder with another.
     *
     * @param other the other builder
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoUpdate combine(@NotNull MongoUpdate other) {
        Objects.requireNonNull(other, "Other cannot be null");
        updates.addAll(other.updates);
        return this;
    }

    // ===========================================
    // Build Method
    // ===========================================

    /**
     * Builds the combined update document.
     *
     * @return the combined update
     * @throws IllegalStateException if no updates have been added
     * @since 1.0.0
     */
    @NotNull
    public Bson build() {
        if (updates.isEmpty()) {
            throw new IllegalStateException("No updates have been added");
        } else if (updates.size() == 1) {
            return updates.get(0);
        } else {
            return Updates.combine(updates);
        }
    }

    /**
     * Checks if any updates have been added.
     *
     * @return true if updates have been added
     * @since 1.0.0
     */
    public boolean isEmpty() {
        return updates.isEmpty();
    }

    /**
     * Returns the number of update operations added.
     *
     * @return the update count
     * @since 1.0.0
     */
    public int size() {
        return updates.size();
    }
}
