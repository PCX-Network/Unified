/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.core.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class providing common mathematical operations.
 *
 * <p>This class contains static methods for random number generation, clamping,
 * interpolation, distance calculations, and other common math operations used
 * in game development.
 *
 * <h2>Random Number Generation</h2>
 * <pre>{@code
 * // Random integer in range [1, 100]
 * int random = MathUtils.randomInt(1, 100);
 *
 * // Random double in range [0.0, 1.0)
 * double randomDouble = MathUtils.randomDouble();
 *
 * // Random double in range [min, max)
 * double range = MathUtils.randomDouble(5.0, 10.0);
 *
 * // Percentage chance check
 * if (MathUtils.chance(0.25)) { // 25% chance
 *     // Do something
 * }
 * }</pre>
 *
 * <h2>Clamping and Bounds</h2>
 * <pre>{@code
 * // Clamp value between min and max
 * double clamped = MathUtils.clamp(value, 0.0, 1.0);
 *
 * // Wrap value within range (for circular values like angles)
 * double angle = MathUtils.wrap(450.0, 0.0, 360.0); // Returns 90.0
 * }</pre>
 *
 * <h2>Interpolation</h2>
 * <pre>{@code
 * // Linear interpolation
 * double lerped = MathUtils.lerp(0.0, 100.0, 0.5); // Returns 50.0
 *
 * // Inverse linear interpolation
 * double t = MathUtils.inverseLerp(0.0, 100.0, 50.0); // Returns 0.5
 *
 * // Smooth step interpolation
 * double smooth = MathUtils.smoothStep(0.0, 1.0, t);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods are thread-safe and use {@link ThreadLocalRandom} for random
 * number generation.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class MathUtils {

    /**
     * The value of PI.
     */
    public static final double PI = Math.PI;

    /**
     * The value of 2 * PI (TAU).
     */
    public static final double TAU = 2.0 * Math.PI;

    /**
     * Conversion factor from degrees to radians.
     */
    public static final double DEG_TO_RAD = Math.PI / 180.0;

    /**
     * Conversion factor from radians to degrees.
     */
    public static final double RAD_TO_DEG = 180.0 / Math.PI;

    /**
     * Small value for floating-point comparisons.
     */
    public static final double EPSILON = 1e-6;

    private MathUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ==================== Random Number Generation ====================

    /**
     * Generates a random integer in the range [min, max] (inclusive).
     *
     * @param min the minimum value (inclusive)
     * @param max the maximum value (inclusive)
     * @return a random integer in the specified range
     * @throws IllegalArgumentException if min is greater than max
     * @since 1.0.0
     */
    public static int randomInt(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("min (" + min + ") must be <= max (" + max + ")");
        }
        if (min == max) {
            return min;
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /**
     * Generates a random integer in the range [0, max] (inclusive).
     *
     * @param max the maximum value (inclusive)
     * @return a random integer in the range [0, max]
     * @throws IllegalArgumentException if max is negative
     * @since 1.0.0
     */
    public static int randomInt(int max) {
        return randomInt(0, max);
    }

    /**
     * Generates a random long in the range [min, max] (inclusive).
     *
     * @param min the minimum value (inclusive)
     * @param max the maximum value (inclusive)
     * @return a random long in the specified range
     * @throws IllegalArgumentException if min is greater than max
     * @since 1.0.0
     */
    public static long randomLong(long min, long max) {
        if (min > max) {
            throw new IllegalArgumentException("min (" + min + ") must be <= max (" + max + ")");
        }
        if (min == max) {
            return min;
        }
        return ThreadLocalRandom.current().nextLong(min, max + 1);
    }

    /**
     * Generates a random double in the range [0.0, 1.0).
     *
     * @return a random double between 0.0 (inclusive) and 1.0 (exclusive)
     * @since 1.0.0
     */
    public static double randomDouble() {
        return ThreadLocalRandom.current().nextDouble();
    }

    /**
     * Generates a random double in the range [min, max).
     *
     * @param min the minimum value (inclusive)
     * @param max the maximum value (exclusive)
     * @return a random double in the specified range
     * @throws IllegalArgumentException if min is greater than or equal to max
     * @since 1.0.0
     */
    public static double randomDouble(double min, double max) {
        if (min >= max) {
            throw new IllegalArgumentException("min (" + min + ") must be < max (" + max + ")");
        }
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    /**
     * Generates a random float in the range [0.0, 1.0).
     *
     * @return a random float between 0.0 (inclusive) and 1.0 (exclusive)
     * @since 1.0.0
     */
    public static float randomFloat() {
        return ThreadLocalRandom.current().nextFloat();
    }

    /**
     * Generates a random float in the range [min, max).
     *
     * @param min the minimum value (inclusive)
     * @param max the maximum value (exclusive)
     * @return a random float in the specified range
     * @throws IllegalArgumentException if min is greater than or equal to max
     * @since 1.0.0
     */
    public static float randomFloat(float min, float max) {
        if (min >= max) {
            throw new IllegalArgumentException("min (" + min + ") must be < max (" + max + ")");
        }
        return ThreadLocalRandom.current().nextFloat(min, max);
    }

    /**
     * Returns true with the given probability.
     *
     * @param probability the probability (0.0 = never, 1.0 = always)
     * @return true if the random check passes
     * @since 1.0.0
     */
    public static boolean chance(double probability) {
        if (probability <= 0.0) return false;
        if (probability >= 1.0) return true;
        return randomDouble() < probability;
    }

    /**
     * Returns true with the given percentage chance.
     *
     * @param percent the percentage chance (0-100)
     * @return true if the random check passes
     * @since 1.0.0
     */
    public static boolean percentChance(double percent) {
        return chance(percent / 100.0);
    }

    /**
     * Generates a random boolean.
     *
     * @return a random boolean value
     * @since 1.0.0
     */
    public static boolean randomBoolean() {
        return ThreadLocalRandom.current().nextBoolean();
    }

    /**
     * Generates a random Gaussian (normally distributed) value with mean 0 and std dev 1.
     *
     * @return a random Gaussian value
     * @since 1.0.0
     */
    public static double randomGaussian() {
        return ThreadLocalRandom.current().nextGaussian();
    }

    /**
     * Generates a random Gaussian value with the specified mean and standard deviation.
     *
     * @param mean   the mean value
     * @param stdDev the standard deviation
     * @return a random Gaussian value
     * @since 1.0.0
     */
    public static double randomGaussian(double mean, double stdDev) {
        return mean + randomGaussian() * stdDev;
    }

    // ==================== Clamping and Bounds ====================

    /**
     * Clamps a value between a minimum and maximum.
     *
     * @param value the value to clamp
     * @param min   the minimum value
     * @param max   the maximum value
     * @return the clamped value
     * @since 1.0.0
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Clamps a value between a minimum and maximum.
     *
     * @param value the value to clamp
     * @param min   the minimum value
     * @param max   the maximum value
     * @return the clamped value
     * @since 1.0.0
     */
    public static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Clamps a value between a minimum and maximum.
     *
     * @param value the value to clamp
     * @param min   the minimum value
     * @param max   the maximum value
     * @return the clamped value
     * @since 1.0.0
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Clamps a value between a minimum and maximum.
     *
     * @param value the value to clamp
     * @param min   the minimum value
     * @param max   the maximum value
     * @return the clamped value
     * @since 1.0.0
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Clamps a value between 0 and 1.
     *
     * @param value the value to clamp
     * @return the clamped value
     * @since 1.0.0
     */
    public static double clamp01(double value) {
        return clamp(value, 0.0, 1.0);
    }

    /**
     * Clamps a value between 0 and 1.
     *
     * @param value the value to clamp
     * @return the clamped value
     * @since 1.0.0
     */
    public static float clamp01(float value) {
        return clamp(value, 0.0f, 1.0f);
    }

    /**
     * Wraps a value within a range (for circular values like angles).
     *
     * @param value the value to wrap
     * @param min   the minimum value (inclusive)
     * @param max   the maximum value (exclusive)
     * @return the wrapped value
     * @since 1.0.0
     */
    public static double wrap(double value, double min, double max) {
        double range = max - min;
        if (range <= 0) {
            throw new IllegalArgumentException("max must be greater than min");
        }
        double result = ((value - min) % range);
        if (result < 0) {
            result += range;
        }
        return result + min;
    }

    /**
     * Wraps a value within a range (for circular values like angles).
     *
     * @param value the value to wrap
     * @param min   the minimum value (inclusive)
     * @param max   the maximum value (exclusive)
     * @return the wrapped value
     * @since 1.0.0
     */
    public static int wrap(int value, int min, int max) {
        int range = max - min;
        if (range <= 0) {
            throw new IllegalArgumentException("max must be greater than min");
        }
        int result = ((value - min) % range);
        if (result < 0) {
            result += range;
        }
        return result + min;
    }

    // ==================== Interpolation ====================

    /**
     * Linearly interpolates between two values.
     *
     * @param start the start value (when t = 0)
     * @param end   the end value (when t = 1)
     * @param t     the interpolation factor (0.0 to 1.0)
     * @return the interpolated value
     * @since 1.0.0
     */
    public static double lerp(double start, double end, double t) {
        return start + (end - start) * t;
    }

    /**
     * Linearly interpolates between two values.
     *
     * @param start the start value (when t = 0)
     * @param end   the end value (when t = 1)
     * @param t     the interpolation factor (0.0 to 1.0)
     * @return the interpolated value
     * @since 1.0.0
     */
    public static float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }

    /**
     * Linearly interpolates between two values with clamped t.
     *
     * @param start the start value (when t = 0)
     * @param end   the end value (when t = 1)
     * @param t     the interpolation factor (clamped to 0.0 to 1.0)
     * @return the interpolated value
     * @since 1.0.0
     */
    public static double lerpClamped(double start, double end, double t) {
        return lerp(start, end, clamp01(t));
    }

    /**
     * Calculates the inverse linear interpolation factor.
     *
     * <p>Given a value between start and end, returns the t factor that would
     * produce that value using lerp.
     *
     * @param start the start value
     * @param end   the end value
     * @param value the value to find the factor for
     * @return the interpolation factor
     * @since 1.0.0
     */
    public static double inverseLerp(double start, double end, double value) {
        if (Math.abs(end - start) < EPSILON) {
            return 0.0;
        }
        return (value - start) / (end - start);
    }

    /**
     * Remaps a value from one range to another.
     *
     * @param value    the value to remap
     * @param fromMin  the source range minimum
     * @param fromMax  the source range maximum
     * @param toMin    the target range minimum
     * @param toMax    the target range maximum
     * @return the remapped value
     * @since 1.0.0
     */
    public static double remap(double value, double fromMin, double fromMax, double toMin, double toMax) {
        double t = inverseLerp(fromMin, fromMax, value);
        return lerp(toMin, toMax, t);
    }

    /**
     * Smooth step interpolation with ease-in and ease-out.
     *
     * @param edge0 the lower edge
     * @param edge1 the upper edge
     * @param x     the value to interpolate
     * @return the smoothly interpolated value
     * @since 1.0.0
     */
    public static double smoothStep(double edge0, double edge1, double x) {
        double t = clamp01((x - edge0) / (edge1 - edge0));
        return t * t * (3.0 - 2.0 * t);
    }

    /**
     * Smoother step interpolation with second-order derivatives equal to zero at edges.
     *
     * @param edge0 the lower edge
     * @param edge1 the upper edge
     * @param x     the value to interpolate
     * @return the smoothly interpolated value
     * @since 1.0.0
     */
    public static double smootherStep(double edge0, double edge1, double x) {
        double t = clamp01((x - edge0) / (edge1 - edge0));
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    // ==================== Distance and Geometry ====================

    /**
     * Calculates the squared distance between two 3D points.
     *
     * <p>Use this for distance comparisons to avoid the expensive square root.
     *
     * @param x1 the x coordinate of the first point
     * @param y1 the y coordinate of the first point
     * @param z1 the z coordinate of the first point
     * @param x2 the x coordinate of the second point
     * @param y2 the y coordinate of the second point
     * @param z2 the z coordinate of the second point
     * @return the squared distance
     * @since 1.0.0
     */
    public static double distanceSquared(double x1, double y1, double z1,
                                         double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Calculates the distance between two 3D points.
     *
     * @param x1 the x coordinate of the first point
     * @param y1 the y coordinate of the first point
     * @param z1 the z coordinate of the first point
     * @param x2 the x coordinate of the second point
     * @param y2 the y coordinate of the second point
     * @param z2 the z coordinate of the second point
     * @return the distance
     * @since 1.0.0
     */
    public static double distance(double x1, double y1, double z1,
                                  double x2, double y2, double z2) {
        return Math.sqrt(distanceSquared(x1, y1, z1, x2, y2, z2));
    }

    /**
     * Calculates the squared distance between two 2D points.
     *
     * @param x1 the x coordinate of the first point
     * @param y1 the y coordinate of the first point
     * @param x2 the x coordinate of the second point
     * @param y2 the y coordinate of the second point
     * @return the squared distance
     * @since 1.0.0
     */
    public static double distanceSquared2D(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return dx * dx + dy * dy;
    }

    /**
     * Calculates the distance between two 2D points.
     *
     * @param x1 the x coordinate of the first point
     * @param y1 the y coordinate of the first point
     * @param x2 the x coordinate of the second point
     * @param y2 the y coordinate of the second point
     * @return the distance
     * @since 1.0.0
     */
    public static double distance2D(double x1, double y1, double x2, double y2) {
        return Math.sqrt(distanceSquared2D(x1, y1, x2, y2));
    }

    /**
     * Calculates the length of a 3D vector.
     *
     * @param x the x component
     * @param y the y component
     * @param z the z component
     * @return the vector length
     * @since 1.0.0
     */
    public static double length(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }

    /**
     * Calculates the length of a 2D vector.
     *
     * @param x the x component
     * @param y the y component
     * @return the vector length
     * @since 1.0.0
     */
    public static double length2D(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }

    // ==================== Angle Utilities ====================

    /**
     * Converts degrees to radians.
     *
     * @param degrees the angle in degrees
     * @return the angle in radians
     * @since 1.0.0
     */
    public static double toRadians(double degrees) {
        return degrees * DEG_TO_RAD;
    }

    /**
     * Converts radians to degrees.
     *
     * @param radians the angle in radians
     * @return the angle in degrees
     * @since 1.0.0
     */
    public static double toDegrees(double radians) {
        return radians * RAD_TO_DEG;
    }

    /**
     * Normalizes an angle to the range [0, 360).
     *
     * @param degrees the angle in degrees
     * @return the normalized angle
     * @since 1.0.0
     */
    public static double normalizeAngle(double degrees) {
        return wrap(degrees, 0.0, 360.0);
    }

    /**
     * Calculates the shortest angle difference between two angles.
     *
     * @param from the starting angle in degrees
     * @param to   the target angle in degrees
     * @return the shortest angle difference (-180 to 180)
     * @since 1.0.0
     */
    public static double angleDifference(double from, double to) {
        double diff = normalizeAngle(to - from);
        if (diff > 180.0) {
            diff -= 360.0;
        }
        return diff;
    }

    /**
     * Linearly interpolates between two angles taking the shortest path.
     *
     * @param from the starting angle in degrees
     * @param to   the target angle in degrees
     * @param t    the interpolation factor (0.0 to 1.0)
     * @return the interpolated angle
     * @since 1.0.0
     */
    public static double lerpAngle(double from, double to, double t) {
        double diff = angleDifference(from, to);
        return normalizeAngle(from + diff * t);
    }

    // ==================== Comparison Utilities ====================

    /**
     * Checks if two floating-point values are approximately equal.
     *
     * @param a       the first value
     * @param b       the second value
     * @param epsilon the maximum difference for equality
     * @return true if the values are approximately equal
     * @since 1.0.0
     */
    public static boolean approximately(double a, double b, double epsilon) {
        return Math.abs(a - b) <= epsilon;
    }

    /**
     * Checks if two floating-point values are approximately equal using default epsilon.
     *
     * @param a the first value
     * @param b the second value
     * @return true if the values are approximately equal
     * @since 1.0.0
     */
    public static boolean approximately(double a, double b) {
        return approximately(a, b, EPSILON);
    }

    /**
     * Returns the sign of a number (-1, 0, or 1).
     *
     * @param value the value to check
     * @return -1 if negative, 0 if zero, 1 if positive
     * @since 1.0.0
     */
    public static int sign(double value) {
        if (value < 0) return -1;
        if (value > 0) return 1;
        return 0;
    }

    /**
     * Returns the sign of a number (-1 or 1, never 0).
     *
     * @param value the value to check
     * @return -1 if negative or zero, 1 if positive
     * @since 1.0.0
     */
    public static int signNonZero(double value) {
        return value >= 0 ? 1 : -1;
    }

    // ==================== Miscellaneous ====================

    /**
     * Calculates the average of an array of values.
     *
     * @param values the values to average
     * @return the average, or 0 if the array is empty
     * @since 1.0.0
     */
    public static double average(double... values) {
        if (values == null || values.length == 0) {
            return 0.0;
        }
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    /**
     * Calculates the sum of an array of values.
     *
     * @param values the values to sum
     * @return the sum, or 0 if the array is empty
     * @since 1.0.0
     */
    public static double sum(double... values) {
        if (values == null || values.length == 0) {
            return 0.0;
        }
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return sum;
    }

    /**
     * Finds the minimum value in an array.
     *
     * @param values the values to search
     * @return the minimum value
     * @throws IllegalArgumentException if the array is empty
     * @since 1.0.0
     */
    public static double min(double... values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Cannot find minimum of empty array");
        }
        double min = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] < min) {
                min = values[i];
            }
        }
        return min;
    }

    /**
     * Finds the maximum value in an array.
     *
     * @param values the values to search
     * @return the maximum value
     * @throws IllegalArgumentException if the array is empty
     * @since 1.0.0
     */
    public static double max(double... values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Cannot find maximum of empty array");
        }
        double max = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] > max) {
                max = values[i];
            }
        }
        return max;
    }

    /**
     * Rounds a value to the specified number of decimal places.
     *
     * @param value    the value to round
     * @param decimals the number of decimal places
     * @return the rounded value
     * @since 1.0.0
     */
    public static double round(double value, int decimals) {
        if (decimals < 0) {
            throw new IllegalArgumentException("decimals must be non-negative");
        }
        double multiplier = Math.pow(10, decimals);
        return Math.round(value * multiplier) / multiplier;
    }

    /**
     * Floors a value to the specified number of decimal places.
     *
     * @param value    the value to floor
     * @param decimals the number of decimal places
     * @return the floored value
     * @since 1.0.0
     */
    public static double floor(double value, int decimals) {
        if (decimals < 0) {
            throw new IllegalArgumentException("decimals must be non-negative");
        }
        double multiplier = Math.pow(10, decimals);
        return Math.floor(value * multiplier) / multiplier;
    }

    /**
     * Ceils a value to the specified number of decimal places.
     *
     * @param value    the value to ceil
     * @param decimals the number of decimal places
     * @return the ceiled value
     * @since 1.0.0
     */
    public static double ceil(double value, int decimals) {
        if (decimals < 0) {
            throw new IllegalArgumentException("decimals must be non-negative");
        }
        double multiplier = Math.pow(10, decimals);
        return Math.ceil(value * multiplier) / multiplier;
    }
}
